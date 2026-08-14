package com.wonkglorg.minecraft.shop.manager;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import com.wonkglorg.minecraft.shop.shop.display.ShopDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager{
	private final NamespacedKey displayKey;
	private final Shop plugin;
	private final ShopManager shopManager;
	/**
	 * Players currently having their display visibility processed.
	 */
	private final Set<UUID> playersBeingProcessed = ConcurrentHashMap.newKeySet();
	
	/**
	 * Shops whose display packets have currently been sent to each player.
	 */
	private final Map<UUID, Set<AbstractShop>> visibleShopsByPlayer = new ConcurrentHashMap<>();
	
	public DisplayManager(Shop plugin, ShopManager shopManager) {
		this.plugin = plugin;
		this.shopManager = shopManager;
		displayKey = new NamespacedKey(Shop.getPlugin(), "display");
	}
	
	/**
	 * Removes all shop displays currently active
	 */
	public void removeAllDisplays() {
		for(World world : plugin.getServer().getWorlds()){
			for(Entity entity : world.getEntities()){
				if(isDisplay(entity)){
					entity.remove();
				}
			}
		}
	}
	
	public void clearDisplaysForPlayer(Player player) {
		Set<AbstractShop> shops = visibleShopsByPlayer.get(player.getUniqueId());
		if(shops != null && !shops.isEmpty()){
			for(var shop : shops){
				shop.getDisplay().remove(player);
			}
		}
	}
	
	public void processShopDisplaysNearPlayer(Player player) {
		UUID playerId = player.getUniqueId();
		
		// Prevent multiple overlapping processing tasks for the same player.
		if(!playersBeingProcessed.add(playerId)){
			return;
		}
		
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			try{
				if(!player.isOnline()){
					return;
				}
				
				Location playerLocation = player.getLocation();
				
				double maxDistance = plugin.getSettingsConfig().getMaxShopDisplayDistance();
				double maxDistanceSquared = maxDistance * maxDistance;
				
				// Use the display distance to determine how many chunks must be searched.
				int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
				
				Set<AbstractShop> nowVisible = new HashSet<>();
				
				for(AbstractShop shop : shopManager.getShopsNearLocation(playerLocation, chunkRadius)){
					if(!shop.isLoaded()){
						continue;
					}
					
					AbstractDisplay display = shop.getDisplay();
					
					if(display == null || display.getType() == DisplayType.NONE){
						continue;
					}
					
					Location containerLocation = shop.getContainerLocation();
					
					if(containerLocation == null || !containerLocation.getWorld().equals(playerLocation.getWorld())){
						continue;
					}
					
					if(containerLocation.distanceSquared(playerLocation) <= maxDistanceSquared){
						nowVisible.add(shop);
					}
				}
				
				Set<AbstractShop> previouslyVisible = visibleShopsByPlayer.computeIfAbsent(playerId, _ -> new HashSet<>());
				
				// Send packets for shops entering range.
				for(AbstractShop shop : nowVisible){
					if(previouslyVisible.add(shop)){
						shop.getDisplay().spawn(player);
					}
				}
				
				// Remove packets for shops leaving range.
				Iterator<AbstractShop> iterator = previouslyVisible.iterator();
				
				while(iterator.hasNext()){
					AbstractShop shop = iterator.next();
					
					if(!nowVisible.contains(shop)){
						shop.getDisplay().remove(player);
						iterator.remove();
					}
				}
				
				if(previouslyVisible.isEmpty()){
					visibleShopsByPlayer.remove(playerId, previouslyVisible);
				}
				
			} catch(Exception e){
				plugin.logger().warning("Error processing shop displays for player " + player.getName());
				e.printStackTrace();
			} finally{
				playersBeingProcessed.remove(playerId);
			}
		}, 1);
	}
	
	public boolean isDisplay(Entity entity) {
		PersistentDataContainer persistentData = entity.getPersistentDataContainer();
		Integer dataDisplay = persistentData.get(displayKey, PersistentDataType.INTEGER);
		if(dataDisplay == null){
			return false;
		}
		return (dataDisplay == 1);
	}
	
	public AbstractDisplay createDisplay(Location signLocation) {
		return new ShopDisplay(signLocation);
	}
	
}
