package com.wonkglorg.minecraft.shop.manager;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager{
	
	private final NamespacedKey displayKey;
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
	 * Last processed position for each player.
	 */
	private final Map<UUID, PlayerPosition> lastProcessedPosition = new ConcurrentHashMap<>();
	
	/**
	 * Shops whose displays are currently visible to each player.
	 *
	 * Access to the Set should occur on the player's entity/region thread.
	 */
	private final Map<UUID, Set<AbstractShop>> visibleShopsByPlayer = new ConcurrentHashMap<>();
	
	public DisplayManager(Main plugin, ShopManager shopManager) {
		this.plugin = plugin;
		this.shopManager = shopManager;
		
		this.displayKey = new NamespacedKey(plugin, "display");
		
		displayTask = plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
			for(Player player : Bukkit.getOnlinePlayers()){
				processShopDisplaysNearPlayer(player, false);
			}
		}, 20, 200);
	}
	
	/**
	 * Clears all display state for a player.
	 */
	public void clearDisplaysForPlayer(Player player) {
		UUID playerId = player.getUniqueId();
		
		Set<AbstractShop> visible = visibleShopsByPlayer.remove(playerId);
		
		if(visible != null){
			for(AbstractShop shop : visible){
				try{
					shop.getDisplay().remove(player);
				} catch(Exception e){
					plugin.logger().warning("Failed to remove display for " + player.getName());
				}
			}
		}
		
		lastProcessedPosition.remove(playerId);
		playersBeingProcessed.remove(playerId);
	}
	
	/**
	 * Processes displays around a player.
	 *
	 * @param force if true forces the player to process regardless if they have moved enough or not
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
				
				if(!hasMovedEnough(playerId, location) && !force){
					return;
				}
				
				lastProcessedPosition.put(playerId,
						new PlayerPosition(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ()));
				
				updateVisibleDisplays(player, location, force);
				
			} catch(Exception e){
				plugin.logger().warning("Error processing shop displays for player " + player.getName());
				e.printStackTrace();
			} finally{
				playersBeingProcessed.remove(playerId);
			}
		}, 1);
	}
	
	private void updateVisibleDisplays(Player player, Location playerLocation, boolean force) {
		UUID playerId = player.getUniqueId();
		
		double maxDistance = plugin.getSettingsConfig().getMaxShopDisplayDistance();
		
		double maxDistanceSquared = maxDisplayDistanceSquared();
		
		int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
		
		Set<AbstractShop> nowVisible = new HashSet<>();
		
		UUID playerWorldId = playerLocation.getWorld().getUID();
		
		for(AbstractShop shop : shopManager.getShopsNearLocation(playerLocation, chunkRadius)){
			if(!isDisplayVisible(shop, playerWorldId, playerLocation, maxDistanceSquared)){
				continue;
			}
			
			nowVisible.add(shop);
		}
		
		Set<AbstractShop> previouslyVisible = visibleShopsByPlayer.get(playerId);
		
		if(previouslyVisible == null){
			previouslyVisible = Collections.emptySet();
		}
		for(AbstractShop shop : previouslyVisible){
			if(!nowVisible.contains(shop) || force){
				shop.getDisplay().remove(player);
			}
		}
		
		for(AbstractShop shop : nowVisible){
			if(!previouslyVisible.contains(shop) || force){
				shop.getDisplay().spawn(player);
			}
		}
		
		if(nowVisible.isEmpty()){
			visibleShopsByPlayer.remove(playerId);
		} else {
			visibleShopsByPlayer.put(playerId, nowVisible);
		}
	}
	
	public double maxDisplayDistanceSquared() {
		double maxDistance = plugin.getSettingsConfig().getMaxShopDisplayDistance();
		
		return maxDistance * maxDistance;
	}
	
	public boolean isDisplayVisible(AbstractShop shop, UUID playerWorldId, Location playerLocation, double maxDistanceSquared) {
		if(!shop.isLoaded()){
			return false;
		}
		
		AbstractDisplay display = shop.getDisplay();
		
		if(display == null || display.getType() == DisplayType.NONE){
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
	
	private boolean hasMovedEnough(UUID playerId, Location current) {
		PlayerPosition previous = lastProcessedPosition.get(playerId);
		
		if(previous == null){
			return true;
		}
		
		if(!previous.worldId().equals(current.getWorld().getUID())){
			return true;
		}
		
		return previous.distanceSquared(current) >= 4.0;
	}
	
	private void clearPlayerState(UUID playerId) {
		visibleShopsByPlayer.remove(playerId);
		lastProcessedPosition.remove(playerId);
	}
	
	public boolean isDisplay(Entity entity) {
		PersistentDataContainer persistentData = entity.getPersistentDataContainer();
		
		Integer dataDisplay = persistentData.get(displayKey, PersistentDataType.INTEGER);
		
		return dataDisplay != null && dataDisplay == 1;
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