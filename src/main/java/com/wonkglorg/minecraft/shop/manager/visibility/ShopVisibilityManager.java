package com.wonkglorg.minecraft.shop.manager;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
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
public class ShopVisibilityManager{
	
	@Setter
	private boolean isLoadingShops = true;
	
	private final Main plugin;
	private final ShopManager shopManager;
	
	@Getter
	private final WrappedTask displayTask;
	
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
	private final Set<ShopVisibilityListener> listeners = ConcurrentHashMap.newKeySet();
	
	public DisplayManager(Main plugin, ShopManager shopManager) {
		this.plugin = plugin;
		this.shopManager = shopManager;
		
		displayTask = plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
			if(isLoadingShops){
				return;
			}
			
			for(Player player : Bukkit.getOnlinePlayers()){
				processShopDisplaysNearPlayer(player, false);
			}
		}, 30, 200);
	}
	
	/**
	 * Registers a listener for shop visibility changes.
	 */
	public void addListener(ShopVisibilityListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Removes a previously registered visibility listener.
	 */
	public void removeListener(ShopVisibilityListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Returns the shops currently considered visible to the player.
	 *
	 * <p>The returned set is a snapshot and can therefore safely be used
	 * outside of the internal visibility tracking.</p>
	 */
	public Set<AbstractShop> getVisibleShops(Player player) {
		Set<AbstractShop> visible = visibleShopsByPlayer.get(player.getUniqueId());
		
		if(visible == null || visible.isEmpty()){
			return Collections.emptySet();
		}
		
		return Set.copyOf(visible);
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
		
		lastProcessedPosition.remove(playerId);
		playersBeingProcessed.remove(playerId);
	}
	
	/**
	 * Processes shops around a player.
	 *
	 * @param force if true, forces processing even if the player has not
	 * moved far enough since the previous check
	 */
	public void processShopDisplaysNearPlayer(Player player, boolean force) {
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
				
				updateVisibleShops(player, location, force);
				
			} catch(Exception e){
				plugin.logger().severe("Error processing shop visibility for player " + player.getName(), e);
			} finally{
				playersBeingProcessed.remove(playerId);
			}
		}, 1);
	}
	
	/**
	 * Determines which shops are currently visible and notifies listeners
	 * about changes.
	 */
	private void updateVisibleShops(Player player, Location playerLocation, boolean force) {
		UUID playerId = player.getUniqueId();
		
		Set<AbstractShop> nowVisible = findVisibleShops(playerLocation);
		
		Set<AbstractShop> previouslyVisible = visibleShopsByPlayer.get(playerId);
		
		if(previouslyVisible == null){
			previouslyVisible = Collections.emptySet();
		}
		
		/*
		 * Shops that are no longer visible.
		 */
		for(AbstractShop shop : previouslyVisible){
			if(!nowVisible.contains(shop)){
				notifyShopLeft(player, shop);
			}
		}
		
		/*
		 * Shops that have become visible.
		 */
		for(AbstractShop shop : nowVisible){
			if(!previouslyVisible.contains(shop)){
				notifyShopEntered(player, shop);
			} else if(force){
				/*
				 * The shop was already visible, but something requested
				 * a forced refresh.
				 */
				notifyShopRefreshed(player, shop);
			}
		}
		
		/*
		 * Store a new snapshot rather than allowing callers/listeners
		 * to mutate the internal set.
		 */
		if(nowVisible.isEmpty()){
			visibleShopsByPlayer.remove(playerId);
		} else {
			visibleShopsByPlayer.put(playerId, Collections.unmodifiableSet(new HashSet<>(nowVisible)));
		}
	}
	
	/**
	 * Finds all shops currently visible from the supplied location.
	 */
	private Set<AbstractShop> findVisibleShops(Location playerLocation) {
		double maxDistance = getMaxVisibilityDistance();
		double maxDistanceSquared = maxDistance * maxDistance;
		
		int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
		
		Set<AbstractShop> visible = new HashSet<>();
		
		UUID playerWorldId = playerLocation.getWorld().getUID();
		
		for(AbstractShop shop : shopManager.getShopsNearLocation(playerLocation, chunkRadius)){
			
			if(!isShopVisible(shop, playerWorldId, playerLocation, maxDistanceSquared)){
				continue;
			}
			
			visible.add(shop);
		}
		
		return visible;
	}
	
	/**
	 * Determines whether a shop is eligible to be considered visible.
	 *
	 * <p>Currently this contains the requirements used by the existing
	 * display system. If signs later have different requirements, this
	 * method can be made generic and display-specific checks can be moved
	 * into the relevant listener.</p>
	 */
	private boolean isShopVisible(AbstractShop shop, UUID playerWorldId, Location playerLocation, double maxDistanceSquared) {
		if(!shop.isLoaded()){
			return false;
		}
		
		AbstractDisplay display = shop.getDisplay();
		
		if(display.getType() == DisplayType.NONE){
			return false;
		}
		
		Location containerLocation = shop.getContainerLocation();
		
		if(containerLocation == null || containerLocation.getWorld() == null){
			return false;
		}
		
		if(!containerLocation.getWorld().getUID().equals(playerWorldId)){
			return false;
		}
		
		if(!display.isChunkLoaded()){
			return false;
		}
		
		return containerLocation.distanceSquared(playerLocation) <= maxDistanceSquared;
	}
	
	/**
	 * Returns the configured maximum shop visibility distance.
	 */
	public double getMaxVisibilityDistance() {
		return plugin.getSettingsConfig().getMaxShopDisplayDistance();
	}
	
	/**
	 * Returns the maximum shop visibility distance squared.
	 */
	public double getMaxVisibilityDistanceSquared() {
		double distance = getMaxVisibilityDistance();
		return distance * distance;
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
		
		return previous.distanceSquared(current) >= plugin.getSettingsConfig().getDisplayMovementThreshold();
	}
	
	/**
	 * Clears internal state without notifying listeners.
	 *
	 * <p>Used when the player is already offline and therefore cannot
	 * safely receive visibility callbacks.</p>
	 */
	private void clearPlayerState(UUID playerId) {
		visibleShopsByPlayer.remove(playerId);
		lastProcessedPosition.remove(playerId);
		playersBeingProcessed.remove(playerId);
	}
	
	/**
	 * Forces all online players to refresh their currently visible shops.
	 */
	public void reload() {
		for(Player player : Bukkit.getOnlinePlayers()){
			processShopDisplaysNearPlayer(player, true);
		}
	}
	
	private void notifyShopEntered(Player player, AbstractShop shop) {
		for(ShopVisibilityListener listener : listeners){
			try{
				listener.onShopEnter(player, shop);
			} catch(Exception e){
				plugin.logger().severe("Error notifying shop enter listener for " + player.getName(), e);
			}
		}
	}
	
	private void notifyShopLeft(Player player, AbstractShop shop) {
		for(ShopVisibilityListener listener : listeners){
			try{
				listener.onShopLeave(player, shop);
			} catch(Exception e){
				plugin.logger().severe("Error notifying shop leave listener for " + player.getName(), e);
			}
		}
	}
	
	private void notifyShopRefreshed(Player player, AbstractShop shop) {
		for(ShopVisibilityListener listener : listeners){
			try{
				listener.onShopRefresh(player, shop);
			} catch(Exception e){
				plugin.logger().severe("Error notifying shop refresh listener for " + player.getName(), e);
			}
		}
	}
	
	public Set<UUID> getPlayersSeeingShop(AbstractShop shop) {
		Set<UUID> players = new HashSet<>();
		
		for(Map.Entry<UUID, Set<AbstractShop>> entry : visibleShopsByPlayer.entrySet()){
			
			if(entry.getValue().contains(shop)){
				players.add(entry.getKey());
			}
		}
		
		return players;
	}
	
	public <T extends ShopVisibilityListener>  @NotNull T getListener(Class<T> listenerClass) {
		for(ShopVisibilityListener listener : listeners){
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