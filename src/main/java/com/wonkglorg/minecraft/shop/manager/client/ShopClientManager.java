package com.wonkglorg.minecraft.shop.manager.client;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.manager.ShopManager;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which shops are currently visible to each player and notifies
 * registered listeners when shops enter, leave, or refresh for a player.
 *
 * <p>This class is responsible only for visibility tracking. Listeners are
 * responsible for deciding what to do when a shop becomes visible.</p>
 */
public class ShopClientManager{
	
	@Setter
	private boolean isLoadingShops = true;
	
	private final ShopPlugin plugin;
	private final ShopManager shopManager;
	
	@Getter
	private WrappedTask displayTask;
	
	/**
	 * Queue of all online players to run bucket processing on
	 */
	private final Deque<UUID> playerProcessingQueue = new ArrayDeque<>();
	/**
	 * Players currently queued/being processed.
	 */
	@Getter
	private final Set<UUID> playersBeingProcessed = ConcurrentHashMap.newKeySet();
	
	/**
	 * Last position at which visibility was processed for each player.
	 */
	private final Map<UUID, PlayerPosition> lastProcessedPosition = new ConcurrentHashMap<>();
	
	/**
	 * Shops currently considered visible to each player.
	 *
	 * <p>The set should only be read/modified from the player's
	 * entity/region thread.</p>
	 */
	private final Map<UUID, Set<AbstractShop>> visibleShopsByPlayer = new ConcurrentHashMap<>();
	
	/**
	 * Objects interested in shop visibility changes.
	 */
	private final Set<ShopClientListener> listeners = ConcurrentHashMap.newKeySet();
	
	public ShopClientManager(ShopPlugin plugin, ShopManager shopManager) {
		this.plugin = plugin;
		this.shopManager = shopManager;
		reload();
	}
	
	/**
	 * Registers a listener for shop visibility changes.
	 */
	public void addListener(ShopClientListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Removes a previously registered visibility listener.
	 */
	public void removeListener(ShopClientListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Forces an update to this shop for all players in range of {@link SettingsConfig#getMaxShopProcessingDistanceChunks()}, In all {@link ShopClientListener} registered to this manager
	 */
	public void updateShop(AbstractShop shop) {
		var nearbyPlayers = shop.getSignLocation().getNearbyPlayers(plugin.getSettingsConfig().getMaxShopProcessingDistanceBlocks());
		for(var player : nearbyPlayers){
			for(var listener : listeners){
				listener.onShopCleanup(player, shop);
				listener.onShopEnter(player, shop);
			}
		}
	}
	
	/**
	 * Forces a cleanup for this shop for all players in view
	 */
	public void cleanupShop(AbstractShop shop) {
		var nearbyPlayers = shop.getSignLocation().getNearbyPlayers(plugin.getSettingsConfig().getMaxShopProcessingDistanceBlocks());
		for(var player : nearbyPlayers){
			for(var listener : listeners){
				listener.onShopCleanup(player, shop);
			}
		}
	}
	
	/**
	 * Forces the shop to be added as if it was just entering the players radius, this will call {@link ShopClientListener#onShopEnter(Player, AbstractShop)}, make sure the shop is cleaned up beforehand otherwise client packets might be leftover
	 */
	public void addShop(AbstractShop shop) {
		var nearbyPlayers = shop.getSignLocation().getNearbyPlayers(plugin.getSettingsConfig().getMaxShopProcessingDistanceBlocks());
		for(var player : nearbyPlayers){
			for(var listener : listeners){
				listener.onShopEnter(player, shop);
			}
		}
	}
	
	/**
	 * Forces an update to this shop for all players in range of {@link SettingsConfig#getMaxShopProcessingDistanceChunks()}, In only for the provided service listener registered to this manager
	 *
	 * @param service the service to call the update for
	 * @param shop the shop to update
	 */
	public <T extends ShopClientListener> void updateShop(Class<T> service, AbstractShop shop) {
		T listener = getListener(service);
		var nearbyPlayers = shop.getSignLocation().getNearbyPlayers(plugin.getSettingsConfig().getMaxShopProcessingDistanceBlocks());
		for(var player : nearbyPlayers){
			listener.onShopCleanup(player, shop);
			listener.onShopEnter(player, shop);
		}
	}
	
	/**
	 * Clears all visibility state for a player and notifies listeners that
	 * all previously visible shops have left their view.
	 */
	public void clearPlayer(Player player) {
		UUID playerId = player.getUniqueId();
		
		Set<AbstractShop> visible = visibleShopsByPlayer.remove(playerId);
		
		if(visible != null){
			for(AbstractShop shop : visible){
				notifyShopLeft(player, shop);
			}
		}
		
		clearPlayerState(player.getUniqueId());
	}
	
	/**
	 * Processes shops around a player.
	 *
	 * @param force if true, forces processing for alls hops around the player even if cached data already exists
	 */
	public void processShopsNearPlayer(Player player, boolean force) {
		UUID playerId = player.getUniqueId();
		
		if(!playersBeingProcessed.add(playerId)){
			return;
		}
		
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			try{
				if(!player.isOnline()){
					clearPlayerState(playerId);
					return;
				}
				
				Location location = player.getLocation();
				
				if(!force && !hasMovedEnough(playerId, location)){
					return;
				}
				
				lastProcessedPosition.put(playerId,
						new PlayerPosition(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ()));
				
				updateVisibleShops(player, force);
				
			} catch(Exception e){
				logger().severe("Error processing shop visibility for player " + player.getName(), e);
			} finally{
				playersBeingProcessed.remove(playerId);
			}
		}, 5);
	}
	
	/**
	 * Determines which shops are currently visible and notifies listeners
	 * about changes.
	 */
	private void updateVisibleShops(Player player, boolean force) {
		UUID playerId = player.getUniqueId();
		
		Set<AbstractShop> nowVisible = findVisibleShops(player.getLocation());
		Set<AbstractShop> previouslyVisible = visibleShopsByPlayer.get(playerId);
		//directly take all visible shops and refresh them no other checks
		if(force){
			clearPlayerState(playerId);
			if(previouslyVisible != null){
				for(var shop : previouslyVisible){
					notifyShopCleanup(player, shop);
				}
			}
			if(nowVisible.isEmpty()){
				visibleShopsByPlayer.remove(playerId);
			} else {
				visibleShopsByPlayer.put(playerId, nowVisible);
			}
			for(var shop : nowVisible){
				notifyShopEntered(player, shop);
			}
			return;
		}
		
		if(previouslyVisible == null){
			previouslyVisible = Collections.emptySet();
		}
		
		for(AbstractShop shop : previouslyVisible){
			if(!nowVisible.contains(shop)){
				notifyShopLeft(player, shop);
			}
		}
		
		for(AbstractShop shop : nowVisible){
			if(!previouslyVisible.contains(shop)){
				notifyShopEntered(player, shop);
			}
		}
		
		if(nowVisible.isEmpty()){
			visibleShopsByPlayer.remove(playerId);
		} else {
			visibleShopsByPlayer.put(playerId, nowVisible);
		}
	}
	
	/**
	 * Finds all shops currently visible from the supplied location.
	 * Visible means within the configured {@link SettingsConfig#getMaxShopProcessingDistanceBlocks()} x} radius
	 */
	private Set<AbstractShop> findVisibleShops(Location playerLocation) {
		int chunkRadius = plugin.getSettingsConfig().getMaxShopProcessingDistanceChunks();
		double maxDistance = plugin.getSettingsConfig().getMaxShopProcessingDistanceBlocks();
		double maxDistanceSquared = maxDistance * maxDistance;
		
		Set<AbstractShop> nearLocation = shopManager.getShopsNearLocation(playerLocation, chunkRadius);
		
		//the above just gives all shops within the chunk radius but we don't want to calculate shop client views for shops that are at y20 while the user is at y190
		nearLocation.removeIf(shop -> {
			Location shopLocation = shop.getSignLocation();
			
			if(shopLocation == null || shopLocation.getWorld() == null || !shopLocation.getWorld().equals(playerLocation.getWorld())){
				return true;
			}
			
			return shopLocation.distanceSquared(playerLocation) > maxDistanceSquared;
		});
		
		return nearLocation;
	}
	
	/**
	 * Checks whether the player has moved far enough to warrant another
	 * visibility calculation.
	 */
	private boolean hasMovedEnough(UUID playerId, Location current) {
		PlayerPosition previous = lastProcessedPosition.get(playerId);
		
		if(previous == null){
			return true;
		}
		
		if(!previous.worldId().equals(current.getWorld().getUID())){
			return true;
		}
		
		return previous.distanceSquared(current) >= plugin.getSettingsConfig().getShopProcessMovementThreshold();
	}
	
	/**
	 * Clears internal state without sending shop leave events to listeners
	 *
	 * <p>Used when the player is already offline and therefore cannot
	 * safely receive visibility callbacks.</p>
	 */
	private void clearPlayerState(UUID playerId) {
		visibleShopsByPlayer.remove(playerId);
		lastProcessedPosition.remove(playerId);
		playersBeingProcessed.remove(playerId);
		for(var listener : listeners){
			listener.clearData(playerId);
		}
	}
	
	/**
	 * Forces all online players to refresh their currently visible shops.
	 */
	public void reload() {
		if(displayTask != null){
			displayTask.cancel();
		}
		visibleShopsByPlayer.clear();
		lastProcessedPosition.clear();
		playersBeingProcessed.clear();
		for(Player player : Bukkit.getOnlinePlayers()){
			//clear all player data that needs clearing
			for(var listener : listeners){
				listener.onShopCleanup(player);
			}
			playerProcessingQueue.add(player.getUniqueId());
		}
		displayTask = plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
			if(isLoadingShops){
				return;
			}
			processNextPlayerBucket();
		}, 30, plugin.getSettingsConfig().getShopProcessInterval());
	}
	
	private void processNextPlayerBucket() {
		int bucketSize = plugin.getSettingsConfig().getShopProcessBucketSize();
		
		for(int i = 0; i < bucketSize && !playerProcessingQueue.isEmpty(); i++){
			UUID playerId = playerProcessingQueue.pollFirst();
			
			Player player = Bukkit.getPlayer(playerId);
			
			if(player == null || !player.isOnline()){
				continue;
			}
			
			processShopsNearPlayer(player, false);
			playerProcessingQueue.addLast(playerId);
		}
	}
	
	public void handlePlayerJoin(Player player) {
		playerProcessingQueue.addLast(player.getUniqueId());
	}
	
	public void handlePlayerQuit(Player player) {
		playerProcessingQueue.remove(player.getUniqueId());
		clearPlayerState(player.getUniqueId());
	}
	
	private void notifyShopEntered(Player player, AbstractShop shop) {
		for(ShopClientListener listener : listeners){
			try{
				listener.onShopEnter(player, shop);
			} catch(Exception e){
				logger().severe("Error notifying shop enter listener for " + player.getName(), e);
			}
		}
	}
	
	private void notifyShopLeft(Player player, AbstractShop shop) {
		for(ShopClientListener listener : listeners){
			try{
				listener.onShopLeave(player, shop);
			} catch(Exception e){
				logger().severe("Error notifying shop leave listener for " + player.getName(), e);
			}
		}
	}
	
	/**
	 * Notifies a cleanup for this shop for the given player
	 *
	 * @param player the player to cleanup any client side data
	 * @param shop the shop to cleanup
	 */
	private void notifyShopCleanup(Player player, AbstractShop shop) {
		for(ShopClientListener listener : listeners){
			try{
				listener.onShopCleanup(player, shop);
			} catch(Exception e){
				logger().severe("Error notifying shop cleanup listener for " + player.getName(), e);
			}
		}
	}
	
	public <T extends ShopClientListener> @NotNull T getListener(Class<T> listenerClass) {
		for(ShopClientListener listener : listeners){
			if(listenerClass.isInstance(listener)){
				return listenerClass.cast(listener);
			}
		}
		
		throw new IllegalStateException("No ShopVisibilityListener registered for " + listenerClass.getName());
	}
	
	private record PlayerPosition(UUID worldId, double x, double y, double z){
		double distanceSquared(Location location) {
			double dx = x - location.getX();
			double dy = y - location.getY();
			double dz = z - location.getZ();
			
			return dx * dx + dy * dy + dz * dz;
		}
	}
}