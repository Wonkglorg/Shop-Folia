package com.wonkglorg.minecraft.shop.manager;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.db.ShopDatabase;
import com.wonkglorg.minecraft.shop.event.PlayerPostInitializeShopEvent;
import com.wonkglorg.minecraft.shop.event.PlayerPreInitializeShopEvent;
import com.wonkglorg.minecraft.shop.manager.client.DisplayUpdateHandler;
import com.wonkglorg.minecraft.shop.manager.client.ShopClientManager;
import com.wonkglorg.minecraft.shop.manager.client.SignUpdateHandler;
import com.wonkglorg.minecraft.shop.migrate.MarketManagerDB;
import com.wonkglorg.minecraft.shop.migrate.PlayerShopsConfig;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopActionType;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import com.wonkglorg.minecraft.shop.shop.creation.SignCreationProcess;
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Keeps track of all shops and related functionality
 */
public class ShopManager{
	private final ShopPlugin plugin;
	private final ShopLogger logger;
	@Getter
	private final ShopDatabase database;
	private final SettingsConfig settingsConfig;
	@Getter
	private final ShopClientManager shopClientManager;
	//todo:mjd for any shop with a hopper feeding it periodically update the shop sign to reflect the current fill.
	/**
	 * All registered shops
	 */
	@Getter
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
	 * Players who are currently in shop creation
	 */
	private final Map<UUID, ShopCreationProcess> playersInShopCreation = new ConcurrentHashMap<>();
	
	/**
	 * Shops that get processed once the chunk they are assigned to loads
	 */
	private final Map<ChunkKey, List<AbstractShop>> unloadedShopsByChunk = new ConcurrentHashMap<>();
	/**
	 * All current shop owners
	 */
	@Getter
	private final Map<UUID, String> shopOwners = new ConcurrentHashMap<>();
	
	public ShopManager(ShopPlugin plugin) throws SQLException, IOException {
		this.plugin = plugin;
		this.settingsConfig = plugin.getSettingsConfig();
		this.logger = logger();
		this.shopClientManager = new ShopClientManager(plugin, this);
		
		database = new ShopDatabase(plugin);
		
		shopClientManager.addListener(new SignUpdateHandler());
		shopClientManager.addListener(new DisplayUpdateHandler());
	}
	
	private void migrateData() {
		if(!settingsConfig.isMigrateOldData()){
			return;
		}
		Path playerNameCache = ShopPlugin.getPlugin().getDataPath().getParent().resolve(Path.of("Shop-old", "Data", "playerNameCache.yml"));
		if(Files.exists(playerNameCache)){
			logger.info("Loading legacy name cache from yml file...");
			YamlConfiguration nameCache = YamlConfiguration.loadConfiguration(playerNameCache.toFile());
			Map<UUID, String> names = new HashMap<>();
			
			for(var entry : nameCache.getKeys(false)){
				names.put(UUID.fromString(entry), nameCache.getString(entry));
			}
			database.addLegacyPlayers(names);
			logger.info("Loaded legacy name cache from yml file...");
		}
		
		logger.info("Loading legacy shops from yml files...");
		List<AbstractShop> shopPluginLegacyShops = PlayerShopsConfig.loadLegacyShops();
		logger.info("Loaded %s shops from yml files!".formatted(shopPluginLegacyShops.size()));
		
		//the shops to insert and their "destruction time" as the value
		Map<UUID, AbstractShop> shopsToInsert = new HashMap<>();
		Map<UUID, Long> shopDeletionTimes = new HashMap<>();
		for(var shop : shopPluginLegacyShops){
			shopsToInsert.put(shop.getId(), shop);
			shopDeletionTimes.put(shop.getId(), 0L);
		}
		
		if(MarketManagerDB.containsDb()){
			logger.info("Found market manager Database to migrate!");
			MarketManagerDB managerDB = new MarketManagerDB();
			Map<AbstractShop, Boolean> migrationDb = managerDB.getShops();
			logger.info("Loaded market manager shop history!");
			for(var shop : migrationDb.keySet()){
				if(shopsToInsert.containsKey(shop.getId())){
					//sets creation date for shop as the shop plugin did not have such data but the market manager does
					shopsToInsert.get(shop.getId()).setCreationDate(shop.getCreationDate());
				} else {
					shopsToInsert.put(shop.getId(), shop);
					shopDeletionTimes.put(shop.getId(), System.currentTimeMillis());
				}
			}
			
			logger.info("Loading Market Manager Shop Transaction History");
			database.logLegacyTransactions(managerDB.getTransactions());
			logger.info("Loaded Market Manager Shop Transaction History");
		} else {
			logger.info("No market manager Database found for migration.");
		}
		database.addLegacyShops(shopsToInsert, shopDeletionTimes);
		settingsConfig.setMigrateOldData(false);
		settingsConfig.silentSave();
	}
	
	/**
	 * Gets all shops near the current location within the defined chunk radius
	 */
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
		List<AbstractShop> abstractShops = playerShops.get(playerId);
		if(abstractShops == null){
			return 0;
		}
		return abstractShops.size();
	}
	
	public void reload() {
		shopClientManager.setLoadingShops(true);
		if(!allShops.isEmpty()){
			//if shops already exist save them before doing a reload
			saveAllShops();
			shopClientManager.reload();
		}
		allShops.clear();
		shopsBySign.clear();
		shopsByContainer.clear();
		shopsByChunk.clear();
		unloadedShopsByChunk.clear();
		shopOwners.clear();
		
		migrateData();
		PlayerNameCache.initialize();
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
			shopClientManager.setLoadingShops(false);
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
	
	public void addPlayerShopCreation(Player player, ShopCreationProcess process) {
		playersInShopCreation.put(player.getUniqueId(), process);
		logger().debug("Shop Creation process started for: " + player.getName());
		//give player a limited amount of time to finish creating the shop until it is deleted
		plugin.getFoliaLib().getScheduler().runLater(() -> {
			logger().debug("Shop Creation timeout handle for: " + player.getName());
			//already canceled by something else no need to od it again
			if(process.isCancelled()){
				logger().debug("Shop Creation already cancelled");
				return;
			}
			//the shop has still not been initialized with an item from a player
			if(!process.isFinishedInitialisation()){
				logger().debug("Shop Creation timed out for player: " + player.getName());
				cancelShopCreationProcess(process.getPlayer());
			}
		}, 30 * 20); // 30 seconds * 20 ticks
	}
	
	public boolean isCreatingShop(Player player) {
		return playersInShopCreation.containsKey(player.getUniqueId());
	}
	
	public ShopCreationProcess getShopCreationProcess(Player player) {
		return playersInShopCreation.get(player.getUniqueId());
	}
	
	public void finishShopCreation(Player player, AbstractShop shop) {
		logger().debug("Removing player " + player.getName() + "from shop creation list");
		playersInShopCreation.remove(player.getUniqueId());
		logger().debug("Registering shop: " + shop);
		registerShop(shop);
	}
	
	/**
	 * If a shop container at this location is currently in creation process
	 */
	public boolean isContainerInShopCreationProcess(Location location) {
		for(var process : playersInShopCreation.values()){
			if(process.getContainer().getLocation().equals(location)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * If a shop container at this location is currently in creation process
	 */
	public ShopCreationProcess getShopCreationProcessForSign(Location location) {
		for(var process : playersInShopCreation.values()){
			if(process.getSign().getLocation().equals(location)){
				return process;
			}
		}
		return null;
	}
	
	/**
	 * Cancels the shop creation process for the player
	 */
	public void cancelShopCreationProcess(Player player) {
		ShopCreationProcess process = playersInShopCreation.remove(player.getUniqueId());
		logger().debug("Removing player " + player.getName() + "from shop creation list");
		if(process != null){
			langManager().request("interaction.issues.create.cancel").sendToAudience(player);
			Sign sign = process.getSign();
			plugin.getFoliaLib().getScheduler().runAtLocation(sign.getLocation(), _ -> {
				if(sign.getBlockData() instanceof WallSign){
					List<Component> lines = SignUpdateHandler.getSignLinesTimeout();
					SignSide side = sign.getSide(Side.FRONT);
					side.line(0, lines.get(0));
					side.line(1, lines.get(1));
					side.line(2, lines.get(2));
					side.line(3, lines.get(3));
					sign.update(true);
				}
			});
		}
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
	private CompletableFuture<AbstractShop> loadShop(AbstractShop shop) {
		CompletableFuture<AbstractShop> future = new CompletableFuture<>();
		if(shop.isLoaded()){
			future.complete(shop);
			return future;
		}
		plugin.getFoliaLib().getScheduler().runAtLocation(shop.getSignLocation(), _ -> {
			if(!shop.load()){
				unregisterShop(shop);
			}
			future.complete(shop);
		});
		return future;
	}
	
	/**
	 * Registers a new shop and stores it in the database
	 */
	public void registerShop(AbstractShop shop) {
		//loads the shop if not yet loaded needed to accurately store data in database
		loadShop(shop).thenAccept(s -> {
			addShop(shop);
			database.addShop(shop);
			database.logAction(shop.getOwner(), shop, ShopActionType.INIT);
			//schedules shop client updates one tick after creation, otherwise the initial "load" method of shops sometimes takes priority in showing the default shop state instead
			plugin.getFoliaLib().getScheduler().runAtLocationLater(shop.getSignLocation(), _ -> shopClientManager.updateShop(shop), 1);
		});
	}
	
	/**
	 * Unregisters a shop and invalidates its database reference
	 */
	public void unregisterShop(AbstractShop shop) {
		removeShop(shop);
		database.removeShop(shop);
		shopClientManager.cleanupShop(shop);
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
	
	public boolean shopInitialisation(ShopCreationProcess process, Player player, ItemStack item) {
		if(!shopManager().passesItemListCheck(item)){
			logger().debug("Item is not allowed to be set as a shop");
			langManager().request("interaction.issues.create.item-filter-deny").sendToAudience(player);
			return false;
		}
		
		logger.debug("Sending shop pre init event");
		PlayerPreInitializeShopEvent shopEvent = new PlayerPreInitializeShopEvent(player, process.toImmutableProgress(), item);
		Bukkit.getPluginManager().callEvent(shopEvent);
		if(shopEvent.isCancelled()){
			logger.debug("Event was cancelled by third party plugin");
			langManager().request("interaction.issues.create.cancel").sendToAudience(player);
			return false;
		}
		
		AbstractShop shop;
		if(process.getType() != ShopType.BARTER){
			process.setItemStack(item);
			shop = process.createShop();
			logger().debug("Setting item for shop: " + item);
		} else {
			if(process.getItemStack() == null){
				process.setItemStack(item);
				logger.debug("Setting first item for barter shop: " + item);
				langManager().request("interaction.success.barter.initialize-barter").sendToAudience(player);
				if(process instanceof SignCreationProcess signCreationProcess){
					signCreationProcess.updateSignText();
				}
				return false;
			} else {
				process.setSecondaryStack(item);
				shop = process.createShop();
				logger.debug("Setting second iem for barter shop " + item);
			}
		}
		
		if(shop != null){
			finishShopCreation(player, shop);
			logger.debug("Sending Post init shop event");
			Bukkit.getPluginManager().callEvent(new PlayerPostInitializeShopEvent(player, shop));
			LangRequest request = langManager().request("interaction.success." + shop.getType() + ".create");
			AbstractShop.shopPlaceholders(request, shop, false, player);
			request.sendToAudience(player);
			return true;
		}
		return true;
	}
	
	public boolean passesItemListCheck(ItemStack itemStack) {
		return plugin.getSettingsConfig().isValidItem(itemStack);
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
