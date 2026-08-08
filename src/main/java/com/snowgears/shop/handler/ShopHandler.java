package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.PlayerShopsConfig;
import static com.snowgears.shop.config.PlayerShopsConfig.SHOPS_DATA_FOLDER;
import static com.snowgears.shop.config.PlayerShopsConfig.saveShops;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.UtilMethods;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ShopHandler{
	
	private final Shop plugin;
	private final SettingsConfig settingsConfig;
	
	private ConcurrentHashMap<Location, AbstractShop> allShops = new ConcurrentHashMap<>();
	private ConcurrentHashMap<UUID, List<Location>> playerShops = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, List<Location>> chunkShops = new ConcurrentHashMap<>(); //String key = world_x_z
	private ConcurrentHashMap<UUID, HashSet<Location>> playersWithActiveShopDisplays = new ConcurrentHashMap<>();
	private HashSet<UUID> playersProcessingShopDisplays = new HashSet<>();
	private HashMap<UUID, Location> playersActiveShopDisplayTag = new HashMap<>();
	
	//all loading of shops happens async at onEnable()
	//shops that still need to calculate their facing direction based on sign are considered "unloaded"
	//we will be loading these shops at time of chunkload and resaving them so they are saved with the 'facing' variable
	private ConcurrentHashMap<String, List<Location>> unloadedShopsByChunk = new ConcurrentHashMap<>();
	@Getter
	private UUID adminUUID;
	private BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	
	private ArrayList<ItemStack> itemListItems = new ArrayList<>();
	
	// Map to track player last processed locations for movement-based display updates
	private ConcurrentHashMap<UUID, Location> lastProcessedLocations = new ConcurrentHashMap<>();
	
	// Cache for player connections to avoid expensive reflection calls
	private ConcurrentHashMap<UUID, ServerPlayerConnection> playerConnectionCache = new ConcurrentHashMap<>();
	
	// Teleport cooldown map to prevent multiple display updates during teleportation
	private ConcurrentHashMap<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
	// Cooldown time in milliseconds (500ms = half a second)
	private static final long TELEPORT_COOLDOWN_MS = 500;
	
	public ShopHandler(Shop instance) {
		plugin = instance;
		settingsConfig = plugin.getSettingsConfig();
		adminUUID = UUID.randomUUID();
		loadShops();
	}
	
	public AbstractShop getShop(Location loc) {
		return allShops.get(loc);
	}
	
	public AbstractShop getShopByChest(Block shopChest) {
		if(isAllowedContainer(shopChest)){
			
			AbstractShop shop = null;
			InventoryHolder ih = null;
			
			//if the shop is a single chest or double chest, add the chest blocks to check
			if(shopChest.getState() instanceof Chest){
				Chest chest = (Chest) shopChest.getState();
				ih = chest.getInventory().getHolder();
				
				if(ih instanceof DoubleChest){
					
					DoubleChest dc = (DoubleChest) ih;
					Chest leftChest = (Chest) dc.getLeftSide();
					Chest rightChest = (Chest) dc.getRightSide();
					
					for(BlockFace direction : directions){
						shop = this.getShop(leftChest.getBlock().getRelative(direction).getLocation());
						if(shop != null){
							//make sure the shop sign you found is actually attached to the correct shop
							if(leftChest.getLocation().equals(shop.getChestLocation()) || rightChest.getLocation().equals(shop.getChestLocation())){
								return shop;
							}
						}
						shop = this.getShop(rightChest.getBlock().getRelative(direction).getLocation());
						if(shop != null){
							//make sure the shop sign you found is actually attached to the correct shop
							if(shop.getChestLocation().equals(leftChest.getLocation()) || shop.getChestLocation().equals(rightChest.getLocation())){
								return shop;
							}
						}
					}
					return null;
				}
			}
			
			for(BlockFace direction : directions){
				shop = this.getShop(shopChest.getRelative(direction).getLocation());
				if(shop != null){
					//make sure the shop sign you found is actually attached to the correct shop
					if(shopChest.getLocation().equals(shop.getChestLocation())){
						return shop;
					}
				}
			}
			return null;
		}
		
		return null;
	}
	
	public AbstractShop getShopTouchingBlock(Block block) {
		BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
		for(BlockFace face : faces){
			if(this.isAllowedContainer(block.getRelative(face))){
				Block shopChest = block.getRelative(face);
				for(BlockFace newFace : faces){
					if(shopChest.getRelative(newFace).getBlockData() instanceof WallSign){
						AbstractShop shop = getShop(shopChest.getRelative(newFace).getLocation());
						if(shop != null){
							return shop;
						}
					}
				}
			}
		}
		return null;
	}
	
	public void addShop(AbstractShop shop) {
		
		//this is to remove a bug that caused one shop to be saved to multiple files at one point
		AbstractShop s = getShop(shop.getSignLocation());
		if(s != null){
			return;
		}
		allShops.put(shop.getSignLocation(), shop);
		
		//adds the shop to the players profile if they are online
		var onlineProfile = PlayerManager.getOnlineProfileIfCached(shop.getOwnerUUID());
		if(onlineProfile != null){
			onlineProfile.getOwnedShops().get(shop.getType()).add(shop);
		}
		
		List<Location> playerShopLocations = getShopLocations(shop.getOwnerUUID());
		if(!playerShopLocations.contains(shop.getSignLocation())){
			playerShopLocations.add(shop.getSignLocation());
			playerShops.put(shop.getOwnerUUID(), playerShopLocations);
		}
		
		String chunkKey = UtilMethods.getChunkKey(shop.getSignLocation());
		List<Location> chunkShopLocations = getShopLocations(chunkKey);
		if(!chunkShopLocations.contains(shop.getSignLocation())){
			chunkShopLocations.add(shop.getSignLocation());
			chunkShops.put(chunkKey, chunkShopLocations);
		}
		
		//plugin.getGuiHandler().reloadPlayerHeadIcon(shop);
	}
	
	//This method should only be used by AbstractShop object to delete
	public void removeShop(AbstractShop shop, boolean forceSave) {
		boolean changed = false;
		if(allShops.containsKey(shop.getSignLocation())){
			allShops.remove(shop.getSignLocation());
			changed = true;
		}
		
		//removes the shop from the players profile if they are online
		var onlineProfile = PlayerManager.getOnlineProfileIfCached(shop.getOwnerUUID());
		if(onlineProfile != null){
			onlineProfile.getOwnedShops().get(shop.getType()).remove(shop);
		}
		
		if(playerShops.containsKey(shop.getOwnerUUID())){
			List<Location> playerShopLocations = getShopLocations(shop.getOwnerUUID());
			if(playerShopLocations.contains(shop.getSignLocation())){
				playerShopLocations.remove(shop.getSignLocation());
				if(playerShopLocations.isEmpty()){
					playerShops.remove(shop.getOwnerUUID());
				} else {
					playerShops.put(shop.getOwnerUUID(), playerShopLocations);
				}
				changed = true;
			}
		}
		String chunkKey = UtilMethods.getChunkKey(shop.getSignLocation());
		if(chunkShops.containsKey(chunkKey)){
			List<Location> chunkShopLocations = getShopLocations(chunkKey);
			if(chunkShopLocations.contains(shop.getSignLocation())){
				chunkShopLocations.remove(shop.getSignLocation());
				if(chunkShopLocations.isEmpty()){
					chunkShops.remove(chunkKey);
				} else {
					chunkShops.put(chunkKey, chunkShopLocations);
				}
				changed = true;
			}
		}
		
		if(changed){
			Shop.getPlugin().logger().debug("Removed Shop internally from ShopHandler: " + shop);
			// Immediate force save if there were any changes since we deleted a shop
			// Note that we don't pass forceSave down, it is only a flag on if we should trigger the save attempt immediately
			// we only hold off on doing this if we are bulk deleting shops for users to prevent repeated saves.
			// The forceSave flag should rarely be `false`, and you should be careful when setting it to false.
			if(forceSave){
				PlayerShopsConfig.saveShops(shop.getOwnerUUID(), true);
			}
		}
	}
	
	public List<AbstractShop> getAllShops() {
		return new ArrayList<>(allShops.values());
	}
	
	public List<AbstractShop> getShops(UUID player) {
		List<AbstractShop> shops = new ArrayList<>();
		for(Location shopSign : getShopLocations(player)){
			AbstractShop shop = getShop(shopSign);
			if(shop != null){
				shops.add(shop);
			}
		}
		return shops;
	}
	
	public List<AbstractShop> getShopsByItem(ItemStack itemStack) {
		List<AbstractShop> shops = new ArrayList<>();
		for(AbstractShop shop : allShops.values()){
			if(shop.getItemStack() != null && shop.getItemStack().getType() == itemStack.getType()){
				shops.add(shop);
			} else if(shop.getSecondaryItemStack() != null && shop.getSecondaryItemStack().getType() == itemStack.getType()){
				shops.add(shop);
			}
		}
		return shops;
	}
	
	// Note: this is resource intensive on large servers, maybe refactor at some point
	public List<OfflinePlayer> getShopOwners() {
		ArrayList<OfflinePlayer> owners = new ArrayList<>();
		for(UUID player : playerShops.keySet()){
			owners.add(Bukkit.getOfflinePlayer(player));
		}
		return owners;
	}
	
	public List<UUID> getShopOwnerUUIDs() {
		ArrayList<UUID> owners = new ArrayList<>();
		for(UUID player : playerShops.keySet()){
			owners.add(player);
		}
		return owners;
	}
	
	private List<Location> getShopLocations(UUID player) {
		List<Location> shopLocations;
		if(playerShops.containsKey(player)){
			shopLocations = playerShops.get(player);
		} else {
			shopLocations = new ArrayList<>();
		}
		return shopLocations;
	}
	
	private List<Location> getShopLocations(String chunkKey) {
		List<Location> shopLocations;
		if(chunkShops.containsKey(chunkKey)){
			shopLocations = chunkShops.get(chunkKey);
		} else {
			shopLocations = new ArrayList<>();
		}
		return shopLocations;
	}
	
	/**
	 * Gets shop locations near a specific location within a specified chunk radius
	 *
	 * @param location The center location to search around
	 * @param chunkRadius The radius (in chunks) to search around the center location
	 * A radius of 1 means a 3x3 chunk area, 2 means 5x5, etc.
	 * @return HashSet of shop locations in the surrounding chunks
	 */
	public HashSet<Location> getShopLocationsNearLocation(Location location, int chunkRadius) {
		if(chunkRadius < 0){
			throw new IllegalArgumentException("Chunk radius cannot be negative");
		}
		
		int chunkX = UtilMethods.getChunkX(location);
		int chunkZ = UtilMethods.getChunkZ(location);
		String worldName = location.getWorld().getName();
		
		HashSet<Location> shopsNearLocation = new HashSet<>();
		
		// Loop through all chunks in the specified radius
		for(int x = -chunkRadius; x <= chunkRadius; x++){
			for(int z = -chunkRadius; z <= chunkRadius; z++){
				String chunkKey = UtilMethods.createChunkKey(worldName, chunkX + x, chunkZ + z);
				List<Location> shopLocations = getShopLocations(chunkKey);
				shopsNearLocation.addAll(shopLocations);
			}
		}
		
		return shopsNearLocation;
	}
	
	/**
	 * Gets actual shop objects near a specific location within the default radius
	 *
	 * @param location The center location to search around
	 * @return List of shops in the surrounding chunks
	 */
	public List<AbstractShop> getShopsNearLocation(Location location) {
		return getShopsNearLocation(location, settingsConfig.getShopSearchRadius());
	}
	
	/**
	 * Gets actual shop objects near a specific location within a specified chunk radius
	 *
	 * @param location The center location to search around
	 * @param chunkRadius The radius (in chunks) to search around the center location
	 * @return List of shops in the surrounding chunks
	 */
	public List<AbstractShop> getShopsNearLocation(Location location, int chunkRadius) {
		List<AbstractShop> shopsNearLocation = new ArrayList<>();
		
		// Get shop locations in the specified radius
		for(Location shopLocation : getShopLocationsNearLocation(location, chunkRadius)){
			AbstractShop shop = getShop(shopLocation);
			if(shop != null){
				shopsNearLocation.add(shop);
			}
		}
		
		return shopsNearLocation;
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
	public HashSet<Location> getShopLocationsNearLocationWithinDistance(Location location, int chunkRadius, double maxDistanceSquared) {
		HashSet<Location> nearbyLocations = getShopLocationsNearLocation(location, chunkRadius);
		HashSet<Location> filteredLocations = new HashSet<>();
		
		// Filter by distance
		for(Location shopLocation : nearbyLocations){
			// Using distanceSquared is more efficient than distance
			try{
				if(location.distanceSquared(shopLocation) <= maxDistanceSquared){
					filteredLocations.add(shopLocation);
				}
			} catch(Exception e){
				// distanceSquared does not exist in MockBukkit and this is the easiest way to disable it
			}
		}
		
		return filteredLocations;
	}
	
	/**
	 * Gets actual shop objects near a specific location within a specified radius in blocks
	 *
	 * @param location The center location to search around
	 * @param chunkRadius The radius (in chunks) to search around the center location
	 * @param maxDistance The maximum distance (in blocks) to include shops
	 * @return List of shops within the distance limit
	 */
	public List<AbstractShop> getShopsNearLocationWithinDistance(Location location, int chunkRadius, double maxDistance) {
		List<AbstractShop> shops = new ArrayList<>();
		double maxDistanceSquared = maxDistance * maxDistance;
		
		for(Location shopLocation : getShopLocationsNearLocationWithinDistance(location, chunkRadius, maxDistanceSquared)){
			AbstractShop shop = getShop(shopLocation);
			if(shop != null){
				shops.add(shop);
			}
		}
		
		return shops;
	}
	
	public void processShopDisplaysNearPlayer(Player player) {
		// If the player is already being processed, don't start another process
		if(playersProcessingShopDisplays.contains(player.getUniqueId())){
			return;
		}
		
		// Get current player location
		Location currentLocation = player.getLocation();
		
		// Check if player has moved enough to warrant processing
		Location lastLocation = lastProcessedLocations.get(player.getUniqueId());
		double movementThreshold = settingsConfig.getDisplayMovementThreshold();
		
		// Skip processing if player hasn't moved enough and this isn't the first check
		if(lastLocation != null &&
		   lastLocation.getWorld().equals(currentLocation.getWorld()) &&
		   lastLocation.distanceSquared(currentLocation) < (movementThreshold * movementThreshold)){
			return;
		}
		
		// Mark player as being processed to prevent concurrent processing
		playersProcessingShopDisplays.add(player.getUniqueId());
		
		// Schedule display processing task at the player's entity
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			try{
				// Use a local variable for current location to avoid race conditions
				Location playerLocation = player.getLocation();
				
				// Update the last processed location immediately to prevent multiple processings
				lastProcessedLocations.put(player.getUniqueId(), playerLocation.clone());
				
				// Get all shop locations within the maximum display distance in one batch
				HashSet<Location> nearbyShopLocations = getShopLocationsNearLocationWithinDistance(playerLocation,
						settingsConfig.getShopSearchRadius(),
						settingsConfig.getMaxShopDisplayDistance() * settingsConfig.getMaxShopDisplayDistance());
				
				// Create a batch operation for all displays to minimize interference
				// This helps prevent the "bouncing" effect when displays are created one by one
				processBatchDisplayUpdates(player, playerLocation, nearbyShopLocations);
				
			} catch(Exception e){
				plugin.logger().warning("Error processing shop displays for player " + player.getName());
				e.printStackTrace();
			} finally{
				// Always ensure player is removed from processing list
				playersProcessingShopDisplays.remove(player.getUniqueId());
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
			AbstractShop shop = getShop(shopLocation);
			if(shop == null){
				continue;
			}
			
			double distance = playerLocation.distance(shop.getSignLocation());
			
			if(distance < settingsConfig.getMaxShopDisplayDistance()){
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
				}, batch * batchDelay); // Configurable delay between batches
			}
		}, 2); // 2 tick delay after removals
	}
	
	public void clearShopDisplaysNearPlayer(Player player) {
		if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
			playersWithActiveShopDisplays.remove(player.getUniqueId());
		}
		
		// Also remove player from last processed locations
		lastProcessedLocations.remove(player.getUniqueId());
		
		// Also remove from processing list to avoid any potential deadlocks
		playersProcessingShopDisplays.remove(player.getUniqueId());
		
		// Clear teleport cooldown as well
		teleportCooldowns.remove(player.getUniqueId());
		
		// Clear the cached connection when displays are removed
		removeCachedPlayerConnection(player);
	}
	
	/**
	 * Force shop display processing for a player, ignoring movement threshold checks.
	 * This should be called after teleportation or world changes.
	 *
	 * @param player The player to process shop displays for
	 */
	public void forceProcessShopDisplaysNearPlayer(Player player) {
		// Check if player is on teleport cooldown
		Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
		long currentTime = System.currentTimeMillis();
		
		// If player is on cooldown, skip this update
		if(lastTeleport != null && currentTime - lastTeleport < TELEPORT_COOLDOWN_MS){
			plugin.logger().debug("Skipping display update for " + player.getName() + " - on teleport cooldown");
			return;
		}
		
		// Set teleport cooldown
		teleportCooldowns.put(player.getUniqueId(), currentTime);
		
		// Remove from processing list if somehow still in there
		playersProcessingShopDisplays.remove(player.getUniqueId());
		
		// Remove any previous location tracking
		lastProcessedLocations.remove(player.getUniqueId());
		
		plugin.logger().debug("Force processing shop displays for " + player.getName() + " after teleport");
		
		// Clear all existing displays for this player to ensure a clean slate
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			if(player.isOnline()){
				if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
					HashSet<Location> displays = playersWithActiveShopDisplays.get(player.getUniqueId());
					if(displays != null){
						plugin.logger().debug("Removing " + displays.size() + " existing displays for " + player.getName());
						for(Location displayLoc : new HashSet<>(displays)){
							AbstractShop shop = getShop(displayLoc);
							if(shop != null){
								shop.getDisplay().remove(player);
							}
						}
					}
					playersWithActiveShopDisplays.remove(player.getUniqueId());
				}
				
				// Process displays after a short delay to ensure all removals are complete
				plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
					if(player.isOnline()){
						processShopDisplaysNearPlayer(player);
					}
				}, 10); // Increased from 5 to 10 ticks to ensure all chunks are loaded
			}
		}, 1);
	}
	
	public boolean hasActiveDisplay(Player player, Location shopSignLocation) {
		HashSet<Location> shops = playersWithActiveShopDisplays.get(player.getUniqueId());
		return shops != null && shops.contains(shopSignLocation);
	}
	
	public void addActiveShopDisplay(Player player, Location shopSignLocation) {
		HashSet<Location> shops;
		if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
			shops = playersWithActiveShopDisplays.get(player.getUniqueId());
		} else {
			shops = new HashSet<>();
		}
		shops.add(shopSignLocation);
		playersWithActiveShopDisplays.put(player.getUniqueId(), shops);
	}
	
	public void removeActiveShopDisplay(Player player, Location shopSignLocation) {
		HashSet<Location> shops;
		if(playersWithActiveShopDisplays.containsKey(player.getUniqueId())){
			shops = playersWithActiveShopDisplays.get(player.getUniqueId());
			shops.remove(shopSignLocation);
		} else {
			shops = new HashSet<>();
		}
		playersWithActiveShopDisplays.put(player.getUniqueId(), shops);
	}
	
	public void addActiveShopDisplayTag(Player player, Location shopSignLocation) {
		if(playersActiveShopDisplayTag.containsKey(player.getUniqueId())){
			Location oldShopSignLocation = playersActiveShopDisplayTag.get(player.getUniqueId());
			
			if(!oldShopSignLocation.equals(shopSignLocation)){
				AbstractShop oldShop = getShop(oldShopSignLocation);
				if(oldShop != null && oldShop.getDisplay() != null){
					// Use a separate task to remove the old display to avoid interference
					plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
						if(player.isOnline()){
							oldShop.getDisplay().removeDisplayEntities(player, true);
						}
					}, 1);
				}
			}
		}
		
		// Only update after a short delay to prevent visual glitches
		plugin.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			if(player.isOnline()){
				playersActiveShopDisplayTag.put(player.getUniqueId(), shopSignLocation);
			}
		}, 2);
	}
	
	private List<Location> getUnloadedShopsByChunk(String chunkKey) {
		List<Location> unloadedShopsInChunk;
		if(unloadedShopsByChunk.containsKey(chunkKey)){
			unloadedShopsInChunk = unloadedShopsByChunk.get(chunkKey);
		} else {
			unloadedShopsInChunk = new ArrayList<>();
		}
		return unloadedShopsInChunk;
	}
	
	public int getNumberOfShops() {
		return allShops.size();
	}
	
	public int getNumberOfShops(Player player) {
		return getShopLocations(player.getUniqueId()).size();
	}
	
	public int getNumberOfShops(UUID playerUUID) {
		return getShopLocations(playerUUID).size();
	}
	
	public int getNumberOfShops(ShopType shopType) {
		int shopsWithType = 0;
		for(AbstractShop shop : allShops.values()){
			if(shop.getType() == shopType){
				shopsWithType++;
			}
		}
		return shopsWithType;
	}
	
	public int getNumberOfShopDisplayTypes(DisplayType displayType) {
		int shopsWithDisplayType = 0;
		for(AbstractShop shop : allShops.values()){
			if(shop.getDisplay().getType() == displayType){
				shopsWithDisplayType++;
			}
		}
		return shopsWithDisplayType;
	}
	
	public Map<String, Integer> getShopContainerCounts() {
		int chestShops = 0;
		int barrelShops = 0;
		int shulkerBoxShops = 0;
		for(AbstractShop shop : allShops.values()){
			Material containerType = shop.getContainerType();
			if(containerType == null){
				continue;
			}
			if(containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST){
				chestShops++;
			}
			if(containerType == Material.BARREL){
				barrelShops++;
			}
			if(containerType.name().endsWith("SHULKER_BOX")){
				shulkerBoxShops++;
			}
		}
		// Return a map of the container types
		Map<String, Integer> containerTypes = new HashMap<>();
		containerTypes.put("Chest Shops", chestShops);
		containerTypes.put("Barrel Shops", barrelShops);
		containerTypes.put("Shulker Box Shops", shulkerBoxShops);
		return containerTypes;
	}
	
	public void removeAllDisplays(Player player) {
		for(AbstractShop shop : allShops.values()){
			shop.getDisplay().remove(player);
		}
	}
	
	public void removeLegacyDisplays() {
		for(World world : plugin.getServer().getWorlds()){
			for(Entity entity : world.getEntities()){
				if(DisplayUtil.isDisplay(entity)){
					entity.remove();
				}
				//make to sure to clear items from old version of plugin too
				else if(entity.getType() == EntityType.ITEM){
					ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
					if(UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())){
						entity.remove();
					}
				}
			}
		}
		for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
			for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)){
				if(UtilMethods.isChunkLoaded(shop.getChestLocation())){
					plugin.logger().debug("[ShopHander.removeLegacyDisplays] updateSign");
					shop.updateSign();
				}
			}
		}
	}
	
	public void loadShops() {
		plugin.getFoliaLib().getScheduler().runAsync(task -> {
			
			if(!Files.exists(SHOPS_DATA_FOLDER)){
				try{
					Files.createDirectories(SHOPS_DATA_FOLDER);
				} catch(IOException e){
					Shop.getPlugin().logger().severe("Unable to create shop directory." + e.getMessage());
					return;
				}
			}
			
			AtomicInteger numShopsLoaded = new AtomicInteger(0);
			PlayerNameCache.initialize();
			
			try(Stream<Path> walk = Files.walk(SHOPS_DATA_FOLDER)){
				walk.forEach(path -> {
					if(!Files.isRegularFile(path)){
						return;
					}
					if(!path.toString().endsWith(".yml")){
						return;
					}
					
					UUID playerUUID = null;
					String fileName = path.getFileName().toString().replace(".yml", "");
					try{
						//all files are saved as UUID.yml except for admin shops which are admin.yml
						if(!fileName.equals("admin")){
							playerUUID = UUID.fromString(fileName);
						} else {
							playerUUID = adminUUID;
						}
						PlayerShopsConfig config = new PlayerShopsConfig(SHOPS_DATA_FOLDER.resolve(fileName + ".yml"));
						for(var shop : config.loadShops()){
							numShopsLoaded.incrementAndGet();
							Shop.getPlugin().getFoliaLib().getScheduler().runAtLocation(shop.getSignLocation(), _ -> {
								try{
									boolean loadSuccess = shop.load();
									if(loadSuccess){
										addShop(shop);
									} else {
										plugin.logger().warning("Unable to load shop " + shop.getId());
									}
								} catch(Exception e){
									plugin.logger().severe("Unable to load shop " + shop + " in " + path.getFileName());
								}
							});
						}
						if(settingsConfig.isDebugForceResaveAll()){
							saveShops(playerUUID, true);
						}
					} catch(IllegalArgumentException iae){
						plugin.logger().severe("Unable to load file: '" + path + "' '" + path.getFileName() + "' is not a valid uuid!");
					}
				});
			} catch(IOException e){
				throw new RuntimeException(e);
			}
			Shop.getPlugin().logger().log(Level.INFO, "Loaded " + numShopsLoaded.get() + " Shops!");
		});
	}
	
	public boolean isAllowedContainer(Block b) {
		return settingsConfig.getEnabledContainers().contains(b.getType());
	}
	
	public static int saveAllShops() {
		Set<UUID> allPlayersWithShops = new HashSet<>();
		for(AbstractShop shop : Shop.getPlugin().getShopHandler().getAllShops()){
			allPlayersWithShops.add(shop.getOwnerUUID());
		}
		
		int numberUpdated = 0;
		int playersWithUpdate = 0;
		for(UUID player : allPlayersWithShops){
			int shopsUpdated = saveShops(player);
			
			if(shopsUpdated > 0){
				numberUpdated += shopsUpdated;
				playersWithUpdate++;
			}
		}
		if(playersWithUpdate > 0){
			Shop.getPlugin().logger().info("Saved " + playersWithUpdate + " Player Shop file updates to disk");
		}
		return numberUpdated;
	}
	
	public boolean passesItemListCheck(ItemStack is) {
		if(plugin.getItemListType() == ItemListType.NONE){
			return true;
		}
		
		for(ItemStack itemInList : itemListItems){
			if(itemInList.isSimilar(is)){
				if(plugin.getItemListType() == ItemListType.ALLOW_LIST){
					return true;
				} else if(plugin.getItemListType() == ItemListType.DENY_LIST){
					return false;
				}
			}
		}
		
		//item not similar to anything in our item list
		if(plugin.getItemListType() == ItemListType.ALLOW_LIST){
			return false;
		}
		
		return true;
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
	
	/**
	 * Rebuilds all shop displays in a chunk for nearby players
	 * Called after a chunk has been loaded to ensure displays are shown
	 *
	 * @param chunk The chunk that was loaded
	 */
	public void rebuildDisplaysInChunk(Chunk chunk) {
		// Get shop locations in this chunk
		String chunkKey = UtilMethods.getChunkKey(chunk);
		List<Location> shopLocations = getShopLocations(chunkKey);
		
		// Only proceed if this chunk has shops
		if(shopLocations.isEmpty()){
			return;
		}
		
		// Process for all players who might be able to see shops in this chunk
		for(Player player : chunk.getWorld().getPlayers()){ //todo:mjd replace with chunk player iteration and see if it stays the same?
			// Skip players who recently teleported
			Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
			if(lastTeleport != null && System.currentTimeMillis() - lastTeleport < TELEPORT_COOLDOWN_MS){
				plugin.logger().debug("Skipping chunk display update for " + player.getName() + " - on teleport cooldown");
				continue;
			}
			
			if(isPlayerNearChunk(player, chunk, settingsConfig.getMaxShopDisplayDistance())){
				plugin.logger().debug("Rebuilding shop displays for " + player.getName() + " in chunk " + chunkKey);
				
				// Don't force a refresh - just run the normal process which respects all the checks
				processShopDisplaysNearPlayer(player);
			}
		}
	}
	
	/**
	 * Gets the cached player connection for packet sending
	 *
	 * @param player The player to get connection for
	 * @return The player's network connection object
	 */
	public ServerPlayerConnection getCachedPlayerConnection(Player player) {
		UUID playerId = player.getUniqueId();
		// Check if we have a cached connection
		ServerPlayerConnection connection = playerConnectionCache.get(playerId);
		if(connection == null){
			playerConnectionCache.put(playerId, ((CraftPlayer) player).getHandle().connection);
		}
		return connection;
	}
	
	/**
	 * Removes the cached player connection
	 *
	 * @param player The player whose connection to remove
	 */
	public void removeCachedPlayerConnection(Player player) {
		playerConnectionCache.remove(player.getUniqueId());
	}
	
	/**
	 * Removes the cached player connection by UUID
	 *
	 * @param playerId UUID of the player whose connection to remove
	 */
	public void removeCachedPlayerConnection(UUID playerId) {
		playerConnectionCache.remove(playerId);
	}
	
	public AbstractDisplay createDisplay(Location signLocation) {
		return new Display(signLocation);
	}
}