package com.snowgears.shop.manager;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.manager.ShopManager.ChunkKey;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class DisplayManager{
	private final NamespacedKey displayKey;
	private final Shop plugin;
	private final ShopManager shopManager;
	private final Set<UUID> playersBeingProcessed = new ConcurrentSkipListSet<>();
	
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
	
	/**
	 * Rebuilds all shop displays in a chunk for nearby players
	 * Called after a chunk has been loaded to ensure displays are shown
	 *
	 * @param chunk The chunk that was loaded
	 */
	public void rebuildDisplaysInChunk(Chunk chunk) {
		List<AbstractShop> shopLocations = shopManager.getShops(chunk);
		
		// Only proceed if this chunk has shops
		if(shopLocations.isEmpty()){
			return;
		}
		
		// Process for all players who might be able to see shops in this chunk
		for(Player player : chunk.getPlayersSeeingChunk()){
			if(isPlayerNearChunk(player, chunk, Shop.getPlugin().getSettingsConfig().getMaxShopDisplayDistance())){
				plugin.logger().debug("Rebuilding shop displays for " + player.getName() + " in chunk " + chunk);
				
				// Don't force a refresh - just run the normal process which respects all the checks
				processShopDisplaysNearPlayer(player);
			}
		}
	}
	
	/**
	 * Checks if a player is within the specified distance of a chunk
	 *
	 * @param player The player to check
	 * @param chunk The chunk to check against
	 * @param maxDistance The maximum distance in blocks
	 * @return True if the player is near the chunk
	 */
	private boolean isPlayerNearChunk(Player player, Chunk chunk, double maxDistance) {
		if(!player.getWorld().equals(chunk.getWorld())){
			return false;
		}
		
		// Get chunk boundaries
		int minX = chunk.getX() << 4;
		int minZ = chunk.getZ() << 4;
		int maxX = minX + 15;
		int maxZ = minZ + 15;
		
		// Get player location
		Location playerLoc = player.getLocation();
		int playerX = playerLoc.getBlockX();
		int playerZ = playerLoc.getBlockZ();
		
		// Calculate minimum distance to chunk border
		double distance;
		
		// Player is outside chunk on X axis
		if(playerX < minX){
			distance = minX - playerX;
		} else if(playerX > maxX){
			distance = playerX - maxX;
		} else {
			// Player is within chunk X range
			distance = 0;
		}
		
		// Player is outside chunk on Z axis
		if(playerZ < minZ){
			double zDistance = minZ - playerZ;
			distance = Math.sqrt(distance * distance + zDistance * zDistance);
		} else if(playerZ > maxZ){
			double zDistance = playerZ - maxZ;
			distance = Math.sqrt(distance * distance + zDistance * zDistance);
		}
		
		return distance <= maxDistance;
	}
	
	public void processShopDisplaysNearPlayer(Player player) {
		// If the player is already being processed, don't start another process
		if(playersBeingProcessed.contains(player.getUniqueId())){
			return;
		}
		// Mark player as being processed to prevent concurrent processing
		playersBeingProcessed.add(player.getUniqueId());
		
		// Schedule display processing task at the player's entity
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			try{
				// Use a local variable for current location to avoid race conditions
				Location playerLocation = player.getLocation();
				
				// Get all shop locations within the maximum display distance in one batch
				Set<Location> nearbyShopLocations = getShopLocationsNearLocationWithinDistance(playerLocation,
						plugin.getSettingsConfig().getShopSearchRadius(),
						plugin.getSettingsConfig().getMaxShopDisplayDistance() * plugin.getSettingsConfig().getMaxShopDisplayDistance());
				
				// Create a batch operation for all displays to minimize interference
				// This helps prevent the "bouncing" effect when displays are created one by one
				processBatchDisplayUpdates(player, playerLocation, nearbyShopLocations);
				
			} catch(Exception e){
				plugin.logger().warning("Error processing shop displays for player " + player.getName());
				e.printStackTrace();
			} finally{
				// Always ensure player is removed from processing list
				playersBeingProcessed.remove(player.getUniqueId());
			}
		}, 1);
	}
	
	
	/**
	 * Process all shop displays in a single coordinated batch to minimize visual artifacts
	 *
	 * @param player The player to update displays for
	 * @param playerLocation The player's current location
	 * @param shopLocations Set of shop locations to process
	 */
	private void processBatchDisplayUpdates(Player player, Location playerLocation, HashSet<Location> shopLocations) {
		if(!player.isOnline()){
			return;
		}
		
		// Log the processing if in debug mode
		plugin.logger().debug("Processing batch display update for " +
		                      player.getName() +
		                      " at " +
		                      playerLocation.getWorld().getName() +
		                      " [" +
		                      playerLocation.getBlockX() +
		                      "," +
		                      playerLocation.getBlockY() +
		                      "," +
		                      playerLocation.getBlockZ() +
		                      "]" +
		                      " with " +
		                      shopLocations.size() +
		                      " nearby shops");
		
		// First, collect all displays that need to be shown and those that need to be removed
		HashSet<Location> displaysToShow = new HashSet<>();
		HashSet<Location> displaysToRemove = new HashSet<>();
		
		// Determine which displays to show and which to remove
		for(Location shopLocation : shopLocations){
			AbstractShop shop = shopManager.getShopBySign(shopLocation);
			if(shop == null){
				continue;
			}
			
			double distance = playerLocation.distance(shop.getSignLocation());
			
			if(distance < plugin.getSettingsConfig().getMaxShopDisplayDistance()){
				// Within display distance, should be shown
				displaysToShow.add(shopLocation);
			} else {
				// Too far, should be removed
				displaysToRemove.add(shopLocation);
			}
		}
		
		// Also identify any current displays that are no longer in range
		if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
			HashSet<Location> activeDisplays = new HashSet<>(playersWithActiveShopDisplays.get(player.getUniqueId()));
			for(Location displayLocation : activeDisplays){
				if(!shopLocations.contains(displayLocation)){
					displaysToRemove.add(displayLocation);
				}
			}
		}
		
		// Process removals first to prevent interference with new spawns
		for(Location locationToRemove : displaysToRemove){
			AbstractShop shop = getShop(locationToRemove);
			if(shop != null){
				shop.getDisplay().remove(player);
				removeActiveShopDisplay(player, locationToRemove);
			}
		}
		
		// Short delay before processing additions to ensure removals are complete
		// This helps prevent the visual "refresh" effect
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			// Now process additions in priority order (closest first)
			List<Map.Entry<Location, Double>> sortedLocations = new ArrayList<>();
			
			for(Location locationToShow : displaysToShow){
				if(!hasActiveDisplay(player, locationToShow)){
					double distance = playerLocation.distance(locationToShow);
					sortedLocations.add(new SimpleEntry<>(locationToShow, distance));
				}
			}
			
			// Sort by distance (closest first)
			sortedLocations.sort(Comparator.comparing(Map.Entry::getValue));
			
			// Process in distance order with small delays between batches to reduce visual clutter
			// Use configurable batch size from config
			int batchSize = settingsConfig.getDisplayBatchSize();
			int batchDelay = settingsConfig.getDisplayBatchDelay();
			int totalBatches = (sortedLocations.size() + batchSize - 1) / batchSize;
			
			plugin.logger().debug("Creating " + sortedLocations.size() + " displays in " + totalBatches + " batches for " + player.getName());
			
			for(int batch = 0; batch < totalBatches; batch++){
				final int currentBatch = batch;
				
				// Add a configurable delay between batches
				plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
					if(!player.isOnline()){
						return;
					}
					
					int startIndex = currentBatch * batchSize;
					int endIndex = Math.min(startIndex + batchSize, sortedLocations.size());
					
					for(int i = startIndex; i < endIndex; i++){
						Location locationToShow = sortedLocations.get(i).getKey();
						AbstractShop shop = getShop(locationToShow);
						
						if(shop != null && player.isOnline()){
							shop.getDisplay().spawn(player);
							addActiveShopDisplay(player, locationToShow);
						}
					}
				}, (long) batch * batchDelay); // Configurable delay between batches
			}
		}, 2); // 2 tick delay after removals
	}
	
	/**
	 * Gets shop locations near a specific location within a specified chunk radius,
	 * filtered by maximum distance in blocks.
	 *
	 * @param location The center location to search around
	 * @param chunkRadius The radius (in chunks) to search around the center location
	 * @param maxDistanceSquared The maximum squared distance (in blocks) to include shops
	 * Using squared distance avoids expensive square root calculations
	 * @return HashSet of shop locations within the distance limit
	 */
	public Set<Location> getShopLocationsNearLocationWithinDistance(Location location, int chunkRadius, double maxDistanceSquared) {
		Set<AbstractShop> nearbyLocations = getShopLocationsNearLocation(location, chunkRadius);
		Set<Location> filteredLocations = new HashSet<>();
		
		// Filter by distance
		for(AbstractShop shop : nearbyLocations){
			// Using distanceSquared is more efficient than distance
			if(location.distanceSquared(shop.getSignLocation()) <= maxDistanceSquared){
				filteredLocations.add(shop.getSignLocation());
			}
		}
		
		return filteredLocations;
	}
	
	/**
	 * Gets shop locations near a specific location within a specified chunk radius
	 *
	 * @param location The center location to search around
	 * @param chunkRadius The radius (in chunks) to search around the center location
	 * A radius of 1 means a 3x3 chunk area, 2 means 5x5, etc.
	 * @return HashSet of shop locations in the surrounding chunks
	 */
	public Set<AbstractShop> getShopLocationsNearLocation(Location location, int chunkRadius) {
		if(chunkRadius < 0){
			throw new IllegalArgumentException("Chunk radius cannot be negative");
		}
		
		int chunkX = UtilMethods.getChunkX(location);
		int chunkZ = UtilMethods.getChunkZ(location);
		UUID world = location.getWorld().getUID();
		
		HashSet<AbstractShop> shopsNearLocation = new HashSet<>();
		
		// Loop through all chunks in the specified radius
		for(int x = -chunkRadius; x <= chunkRadius; x++){
			for(int z = -chunkRadius; z <= chunkRadius; z++){
				List<AbstractShop> shopLocations = shopManager.getShops(new ChunkKey(world, chunkX + x, chunkZ + z));
				shopsNearLocation.addAll(shopLocations);
			}
		}
		
		return shopsNearLocation;
	}
	
	private boolean isDisplay(Entity entity) {
		PersistentDataContainer persistentData = entity.getPersistentDataContainer();
		Integer dataDisplay = persistentData.get(displayKey, PersistentDataType.INTEGER);
		if(dataDisplay == null){
			return false;
		}
		return (dataDisplay == 1);
	}
	
	public AbstractDisplay createDisplay(Location signLocation) {
		return new Display(signLocation);
	}
	
}
