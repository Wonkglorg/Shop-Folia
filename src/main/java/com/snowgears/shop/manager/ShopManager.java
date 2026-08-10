package com.snowgears.shop.manager;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.db.ShopDatabase;
import com.snowgears.shop.migrate.PlayerShopsConfig;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.ShopCreationProcess;
import com.snowgears.shop.util.ShopLogger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ShopManager{
	private final Shop plugin;
	private final ShopLogger logger;
	@Getter
	private final ShopDatabase database;
	private final SettingsConfig settingsConfig;
	@Getter
	private final DisplayManager displayManager;
	/**
	 * All registered shops
	 */
	private final Map<UUID, AbstractShop> allShops = new ConcurrentHashMap<>();
	/**
	 * All shops by their sign location
	 */
	private final Map<BlockKey, AbstractShop> shopsBySign = new ConcurrentHashMap<>();
	
	/**
	 * All shops by their container location
	 */
	private final Map<BlockKey, AbstractShop> shopsByContainer = new ConcurrentHashMap<>();
	
	/**
	 * Shops by their chunk
	 */
	private final Map<ChunkKey, List<AbstractShop>> shopsByChunk = new ConcurrentHashMap<>();
	/**
	 * Shops linked to a player
	 */
	private final Map<UUID, List<AbstractShop>> playerShops = new ConcurrentHashMap<>();
	
	/**
	 * Shops that are currently in creation process, happens when a new shop sign has been placed but no items have been specified yet
	 */
	private final Map<BlockKey, AbstractShop> uninitialisedShops = new ConcurrentHashMap<>();
	
	/**
	 * Shops that get processed once the chunk they are assigned to loads
	 */
	private final Map<ChunkKey, List<AbstractShop>> unloadedShopsByChunk = new ConcurrentHashMap<>();
	/**
	 * All current shop owners
	 */
	@Getter
	private final Map<UUID, String> shopOwners = new ConcurrentHashMap<>();
	
	public ShopManager(Shop plugin) throws SQLException, IOException {
		this.plugin = plugin;
		this.settingsConfig = new SettingsConfig();
		this.logger = plugin.logger();
		this.displayManager = new DisplayManager(plugin, this);
		
		database = new ShopDatabase(plugin);
		if(settingsConfig.isMigrateOldData()){
			logger.info("Migrating Legacy files to database!");
			database.addShops(PlayerShopsConfig.loadLegacyShops());
			logger.info("Finished Migrating files to database!");
			settingsConfig.setMigrateOldData(false);
			settingsConfig.silentSave();
		}
		
	}
	
	public Set<AbstractShop> getShopsNearLocation(Location location, int chunkRadius) {
		if(chunkRadius < 0){
			throw new IllegalArgumentException("Chunk radius cannot be negative");
		}
		
		int chunkX = location.getBlockX() >> 4;
		int chunkZ = location.getBlockZ() >> 4;
		UUID worldId = location.getWorld().getUID();
		
		Set<AbstractShop> shops = new HashSet<>();
		
		for(int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++){
			for(int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++){
				shops.addAll(getShops(new ChunkKey(worldId, x, z)));
			}
		}
		
		return shops;
	}
	
	public AbstractShop getShopBySign(Location loc) {
		return shopsBySign.get(BlockKey.of(loc));
	}
	
	public AbstractShop getShopByContainer(Location loc) {
		return shopsByContainer.get(BlockKey.of(loc));
	}
	
	public List<AbstractShop> getShops(ChunkKey chunkKey) {
		return shopsByChunk.getOrDefault(chunkKey, new ArrayList<>());
	}
	
	public int getNumberOfShops() {
		return allShops.size();
	}
	
	public int getNumberOfShops(UUID playerId) {
		return playerShops.get(playerId).size();
	}
	
	public void loadShops() {
		getDatabase().getShops(false).thenAccept(shops -> {
			for(var shop : shops){
				addShop(shop);
				//if chunk its in is already loaded, calculate it here
				if(shop.isChunkLoaded()){
					loadShop(shop);
				} else {
					addUnloadedShopToChunkList(shop);
				}
			}
			logger.log(Level.INFO, "Loaded " + shops.size() + " Shops!");
			for(var hook : plugin.getShopServiceProvider().getShopLoadHooks()){
				hook.accept(allShops.values());
			}
		});
	}
	
	/**
	 * Adds a shop to the runtime cache
	 */
	private void addShop(AbstractShop shop) {
		allShops.put(shop.getId(), shop);
		shopsBySign.put(shop.getSignKey(), shop);
		shopsByContainer.put(shop.getContainerKey(), shop);
		shopsByChunk.computeIfAbsent(ChunkKey.of(shop.getSignLocation()), _ -> new ArrayList<>()).add(shop);
		if(shop.getSecondaryContainerLocation() != null){
			shopsByContainer.put(BlockKey.of(shop.getSecondaryContainerLocation()), shop);
		}
		
		//adds the shop to the players profile if they are online
		var onlineProfile = PlayerManager.getOnlineProfileIfCached(shop.getOwnerUUID());
		if(onlineProfile != null){
			onlineProfile.getOwnedShops().get(shop.getType()).add(shop);
		}
		
		playerShops.computeIfAbsent(shop.getOwnerUUID(), _ -> new ArrayList<>()).add(shop);
		shopOwners.putIfAbsent(shop.getOwnerUUID(), PlayerNameCache.getName(shop.getOwnerUUID()));
	}
	
	public void addUninitializedShop(AbstractShop shop) {
		uninitialisedShops.put(BlockKey.of(shop.getSignLocation()), shop);
	}
	
	public boolean isUninitializedShopSign(Location location) {
		return uninitialisedShops.containsKey(BlockKey.of(location));
	}
	
	public AbstractShop getUninitializedShopSign(Location location) {
		return uninitialisedShops.get(BlockKey.of(location));
	}
	
	public void removeUninitializedShop(Location location) {
		uninitialisedShops.remove(BlockKey.of(location));
	}
	
	@Internal
	public void addSecondaryShopLocation(Location location, AbstractShop shop) {
		shopsByContainer.putIfAbsent(BlockKey.of(location), shop);
	}
	
	@Internal
	public void removeSecondaryChestLocation(Location location, AbstractShop shop) {
		shopsByContainer.remove(BlockKey.of(location), shop);
	}
	
	/**
	 * Removes a shop from the runtime cache
	 */
	private void removeShop(AbstractShop shop) {
		allShops.remove(shop.getId(), shop);
		shopsBySign.remove(shop.getSignKey(), shop);
		shopsByContainer.remove(shop.getContainerKey(), shop);
		shopsByChunk.get(ChunkKey.of(shop.getSignLocation())).remove(shop);
		
		if(shop.getSecondaryContainerLocation() != null){
			shopsByContainer.remove(BlockKey.of(shop.getSecondaryContainerLocation()), shop);
		}
		var onlineProfile = PlayerManager.getOnlineProfileIfCached(shop.getOwnerUUID());
		if(onlineProfile != null){
			onlineProfile.getOwnedShops().get(shop.getType()).remove(shop);
		}
		
		if(playerShops.containsKey(shop.getOwnerUUID())){
			playerShops.get(shop.getOwnerUUID()).remove(shop);
			if(playerShops.get(shop.getOwnerUUID()).isEmpty()){
				shopOwners.remove(shop.getOwnerUUID());
			}
		} else {
			shopOwners.remove(shop.getOwnerUUID());
		}
	}
	
	/**
	 * Loads a shop chunk and reads out it's data
	 *
	 * @param shop
	 */
	private void loadShop(AbstractShop shop) {
		plugin.getFoliaLib().getScheduler().runAtLocation(shop.getSignLocation(), _ -> {
			if(shop.load()){
				unregisterShop(shop);
			}
		});
	}
	
	/**
	 * Registers a new shop and stores it in the database
	 */
	public void registerShop(AbstractShop shop) {
		addShop(shop);
		database.addShop(shop);
		database.logAction(shop.getOwner(), shop, ShopActionType.INIT);
	}
	
	/**
	 * Unregisters a shop and invalidates its database reference
	 */
	public void unregisterShop(AbstractShop shop) {
		removeShop(shop);
		database.removeShop(shop);
		if(shop.getDisplay() != null){
			shop.getDisplay().remove(null);
		}
	}
	
	public boolean isAllowedContainer(Block b) {
		return settingsConfig.getEnabledContainers().contains(b.getType());
	}
	
	/**
	 * Saves all shops that have pending changes and updates the stock cache for every shop
	 */
	public void saveAllShops() {
		database.updateShops(allShops.values().stream().filter(AbstractShop::needsSave).toList());
		database.cacheStockValues(allShops.values());
	}
	
	/**
	 * Adds shops to be processed in the future when the chunk loads here.
	 */
	private void addUnloadedShopToChunkList(AbstractShop shop) {
		ChunkKey chunkKey = ChunkKey.of(shop.getSignLocation());
		unloadedShopsByChunk.computeIfAbsent(chunkKey, _ -> new ArrayList<>()).add(shop);
	}
	
	public AbstractShop getShopByContainer(Block container) {
		if(!isAllowedContainer(container)){
			return null;
		}
		
		return shopsByContainer.get(BlockKey.of(container));
		
	}
	
	public List<AbstractShop> getShops(UUID playerId) {
		return playerShops.getOrDefault(playerId, new ArrayList<>());
	}
	
	public List<AbstractShop> getShops(Chunk chunk) {
		return shopsByChunk.get(ChunkKey.of(chunk));
	}
	
	/**
	 * Loads all unprocessed shops in this chunk.
	 */
	public void processUnloadedShopsInChunk(Chunk chunk) {
		ChunkKey chunkKey = ChunkKey.of(chunk);
		
		List<AbstractShop> shops = unloadedShopsByChunk.remove(chunkKey);
		
		if(shops == null || shops.isEmpty()){
			return;
		}
		
		for(AbstractShop shop : shops){
			plugin.getFoliaLib().getScheduler().runAtLocation(shop.getSignLocation(), task -> {
				if(!shop.load()){
					unregisterShop(shop);
				}
			});
		}
	}
	
	public boolean passesItemListCheck(ItemStack itemStack) {
		return plugin.getItemConfig().isValidItem(itemStack);
	}
	
	/**
	 * Checks for any outdated shops and removes them.
	 */
	public void removeOutdatedShops() {
		//delete all shops from players that have not played in X amount of hours (if configured)
		int hoursOfflineToRemoveShops = plugin.getSettingsConfig().getHoursOfflineToRemoveShops();
		if(hoursOfflineToRemoveShops != 0){
			for(var owner : plugin.getShopmanager().getShopOwners().entrySet()){
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner.getKey());
				if(offlinePlayer.getName() != null){
					long msSinceLastPlayed = System.currentTimeMillis() - offlinePlayer.getLastLogin();
					long hoursSinceLastPlayed = TimeUnit.MILLISECONDS.toHours(msSinceLastPlayed);
					
					if(hoursSinceLastPlayed >= hoursOfflineToRemoveShops){
						for(AbstractShop shop : plugin.getShopmanager().getShops(offlinePlayer.getUniqueId())){
							plugin.logger().notice("Deleting Shop because player " +
							                       offlinePlayer.getName() +
							                       " has not logged in within the required " +
							                       (int) hoursSinceLastPlayed +
							                       " hours! " +
							                       shop);
							plugin.getShopmanager().unregisterShop(shop);
						}
					}
				}
			}
		}
	}
	
	/**
	 * If a shop container at this location is currently in creation process
	 */
	public boolean isContainerInShopCreationProcess(Location location) {
		for(ShopCreationProcess process : PlayerManager.getPLAYER_SHOP_CREATION_STEP().values()){
			if(process.getClickedChest().getLocation().equals(location)){
				return true;
			}
		}
		return false;
	}
	
	public record BlockKey(UUID worldId, int x, int y, int z){
		
		public static BlockKey of(Block block) {
			return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
		}
		
		public static BlockKey of(Location location) {
			return new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		}
		
		public Location toLocation() {
			return new Location(Bukkit.getWorld(worldId), x, y, z);
		}
	}
	
	public record ChunkKey(UUID worldId, int x, int z){
		
		public static ChunkKey of(Chunk chunk) {
			return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
		}
		
		public static ChunkKey of(Location location) {
			return new ChunkKey(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
		}
	}
	
}
