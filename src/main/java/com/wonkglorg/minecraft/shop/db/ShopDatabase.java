package com.wonkglorg.minecraft.shop.db;

import com.tcoded.folialib.impl.PlatformScheduler;
import com.wonkglorg.database.DatabaseType;
import com.wonkglorg.database.databases.SqliteDatabase;
import com.wonkglorg.database.datasources.FileDataSource;
import com.wonkglorg.minecraft.shop.AdminOfflinePlayer;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.migrate.MarketManagerDB.ShopHistoryEntry;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopActionType;
import com.wonkglorg.minecraft.shop.shop.ShopState;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.settings.Setting;
import static com.wonkglorg.minecraft.shop.shop.settings.Settings.ALL_SETTINGS;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import com.wonkglorg.minecraft.util.PluginLogger;
import static net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ShopDatabase extends SqliteDatabase<FileDataSource>{
	private static final String SHOP_INSERT_SQL = """
			INSERT INTO shops
			(shop_uuid, owner_uuid, item, price, amount,last_known_stock_count, last_known_stock_status, shop_type,sign_facing, display_type,fake_sign, secondary_item, creation_time, item_type, secondary_item_type, shop_world, shop_x, shop_y, shop_z)
			VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			ON CONFLICT(shop_uuid) DO NOTHING;
			""";
	
	private static final String SHOP_SELECT_SQL = """
			SELECT shop_uuid, owner_uuid, item, price, amount, last_known_stock_count,last_known_stock_status, destroy_time, shop_type,sign_facing, display_type, fake_sign, secondary_item, creation_time, item_type, secondary_item_type, shop_world, shop_x, shop_y, shop_z
			  FROM shops
			 WHERE destroy_time = 0 OR 1 = ?;
			""";
	
	private static final String SHOP_CACHE_STOCK_SQL = """
			UPDATE shops SET last_known_stock_count = ?, last_known_stock_status = ? WHERE shop_uuid = ?
			""";
	
	private static final String SHOP_UPDATE_SQL = """
			UPDATE shops SET owner_uuid = ?, item = ?, price = ?, amount = ?, last_known_stock_count = ?, shop_type = ?, sign_facing = ?, display_type = ?, fake_sign = ?, secondary_item = ?, item_type = ?, secondary_item_type = ?, shop_world = ?, shop_x = ?, shop_y = ?, shop_z = ? WHERE shop_uuid = ?
			""";
	
	private static final String LEGACY_SHOP_INSERT_SQL = """
			INSERT INTO shops
			(shop_uuid, owner_uuid, item, price, amount,last_known_stock_count, last_known_stock_status, shop_type,sign_facing, display_type,fake_sign, secondary_item, creation_time,destroy_time, item_type, secondary_item_type, shop_world, shop_x, shop_y, shop_z)
			VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			ON CONFLICT(shop_uuid) DO NOTHING;
			""";
	
	private static final String PURCHASE_STATS_SQL = """
			SELECT
			    shop_uuid,
			    SUM(transaction_count) AS total_purchases,
			    MAX(timestamp) AS last_purchase
			FROM transactions
			WHERE purchaser_uuid = ?
			GROUP BY shop_uuid
			""";
	
	private static final String INSERT_SHOP_SETTING_SQL = """
			INSERT INTO shop_settings(shop_uuid,setting_key,value) VALUES(?,?,?) ON CONFLICT DO UPDATE SET value = EXCLUDED.value
			""";
	
	private static final String SELECT_SHOP_SETTING_SQL = """
			SELECT shop_uuid, setting_key, value FROm shop_settings
			WHERE shop_uuid = ?
			""";
	
	public static final String TRANSACTION_STATS_SQL = """
			SELECT
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day1,
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day7,
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day30,
			    COUNT(timestamp) AS all_time
			    FROM transactions t
			    LEFT JOIN players
			    ON t.purchaser_uuid = players.uuid
			    WHERE shop_uuid = ?
			""";
	
	public static final String TRANSACTION_STATS_SHOPS_SQL = """
			SELECT
			    s.shopUuid,
			    s.ownerUuid,
			    s.shop_type
			    s.itemBase64,
			    s.barterItemBase64,
			    s.active,
			    s.amount,
			    s.timestamp,
			    s.shopType,
			    s.price,
			
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day1,
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day7,
			    COUNT(CASE WHEN t.timestamp >= ? THEN 1 END) AS day30,
			    COUNT(t.timestamp) AS all_time
			
			FROM shops s
			
			LEFT JOIN transactions t
			    ON t.shopUuid = s.shopUuid
			
			WHERE s.shopUuid IN (%s)
			
			GROUP BY
			    s.shopUuid,
			    s.ownerUuid,
			    s.itemBase64,
			    s.barterItemBase64,
			    s.active,
			    s.amount,
			    s.timestamp,
			    s.shopType,
			    s.price
			""";
	
	private final ShopPlugin plugin;
	private final PlatformScheduler scheduler;
	
	public ShopDatabase(ShopPlugin plugin) throws SQLException, IOException {
		super(new FileDataSource(DatabaseType.SQLITE, plugin.getDataPath().resolve("data", "shop.db")));
		this.plugin = plugin;
		this.scheduler = plugin.getFoliaLib().getScheduler();
		initDB();
		addPlayer(AdminOfflinePlayer.getAdminUUID(), "admin");
	}
	
	public void initDB() throws SQLException, IOException {
		loadSqlStatements(plugin.getResource("db-setup.sql"), "");
	}
	
	/**
	 * Adds a new shop to the database, if a shop with this uuid already exists do nothing
	 *
	 * @param shop the shop to insert
	 */
	public void addShop(AbstractShop shop) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement(SHOP_INSERT_SQL)){
				insertShopValues(shop, ps);
				ps.executeUpdate();
			} catch(SQLException e){
				logger().error("Error while creating shop chest", e);
			}
		});
	}
	
	/**
	 * Method to import legacy shops, should not be used by any other caller outside of migration purposes
	 *
	 * @param shops the shops to add
	 * @param deletionTimes shops and their deletion time
	 */
	@Internal
	public void addLegacyShops(Map<UUID, AbstractShop> shops, Map<UUID, Long> deletionTimes) {
		if(shops == null || shops.isEmpty()){
			return;
		}
		try(var connection = getConnection(); var ps = connection.prepareStatement(LEGACY_SHOP_INSERT_SQL)){
			connection.setAutoCommit(false);
			
			try{
				for(AbstractShop shop : shops.values()){
					ItemStack mainStack = shop.getItemStack().clone();
					mainStack.setAmount(1);
					ItemStack barterStack = shop.getSecondaryItemStack();
					if(barterStack != null){
						barterStack = barterStack.clone();
						barterStack.setAmount(1);
					}
					Location signLocation = shop.getSignLocation();
					ps.setString(1, shop.getId().toString());
					ps.setString(2, shop.getOwnerUUID().toString());
					ps.setString(3, ItemStackJsonCodec.serialize(mainStack, true));
					ps.setDouble(4, shop.getPrice());
					ps.setInt(5, shop.getAmount());
					ps.setInt(6, shop.getStock());
					ps.setString(7, shop.getShopState().toString());
					ps.setString(8, shop.getType().toString().toUpperCase());
					BlockFace facing = shop.getFacing();
					if(facing == null){
						facing = BlockFace.EAST;
					}
					ps.setString(9, facing.toString().toUpperCase());
					ps.setString(10,
							(shop.getDisplay() != null && shop.getDisplay().getType() != null)
							? shop.getDisplay().getType().toString()
							: DisplayType.NONE.toString());
					ps.setInt(11, shop.isFakeSign() ? 1 : 0);
					ps.setString(12, barterStack != null ? ItemStackJsonCodec.serialize(barterStack, true) : null);
					ps.setLong(13, shop.getCreationDate());
					ps.setLong(14, deletionTimes.getOrDefault(shop.getId(), 0L));
					ps.setString(15, mainStack.getType().toString());
					ps.setString(16, barterStack != null ? barterStack.getType().toString() : null);
					ps.setString(17, signLocation.getWorld().getName());
					ps.setInt(18, signLocation.getBlockX());
					ps.setInt(19, signLocation.getBlockY());
					ps.setInt(20, signLocation.getBlockZ());
					ps.addBatch();
				}
				
				ps.executeBatch();
				connection.commit();
				
			} catch(SQLException e){
				connection.rollback();
				throw e;
				
			} finally{
				connection.setAutoCommit(true);
			}
			
		} catch(SQLException e){
			logger().error("Error while creating shops", e);
		}
	}
	
	/**
	 * All shops known to the shops plugin
	 *
	 * @param includeDeleted if already deleted shops should be included
	 */
	public CompletableFuture<List<AbstractShop>> getShops(boolean includeDeleted) {
		CompletableFuture<List<AbstractShop>> future = new CompletableFuture<>();
		scheduler.runAsync(t -> {
			List<AbstractShop> shops = new ArrayList<>();
			try(var ps = getConnection().prepareStatement(SHOP_SELECT_SQL)){
				ps.setInt(1, includeDeleted ? 1 : 0);
				var resultSet = ps.executeQuery();
				while(resultSet.next()){
					shops.add(buildShop(resultSet));
				}
			} catch(SQLException e){
				logger().error("Error while creating shop chest", e);
			}
			future.complete(shops);
		});
		
		return future;
	}
	
	/**
	 * Caches the shops current stock state and stock amount, used when shutting down the plugin to have a somwhat up to date initial value for shop stock before the chunk is loaded
	 */
	public void cacheStockValues(Collection<AbstractShop> shops) {
		if(shops == null || shops.isEmpty()){
			return;
		}
		try(Connection connection = getConnection(); PreparedStatement ps = connection.prepareStatement(SHOP_CACHE_STOCK_SQL)){
			connection.setAutoCommit(false);
			try{
				for(AbstractShop shop : shops){
					if(shop == null || !shop.isInitialized()){
						continue;
					}
					ps.setInt(1, shop.getStock());
					ps.setString(2, shop.getShopState().toString());
					ps.setString(3, shop.getId().toString());
					ps.addBatch();
				}
				ps.executeBatch();
				connection.commit();
			} catch(SQLException e){
				connection.rollback();
				throw e;
			} finally{
				connection.setAutoCommit(true);
			}
		} catch(SQLException e){
			logger().error("Error while caching shop stock values", e);
		}
	}
	
	private AbstractShop buildShop(ResultSet set) throws SQLException {
		UUID id = UUID.fromString(set.getString("shop_uuid"));
		UUID ownerId = UUID.fromString(set.getString("owner_uuid"));
		Location signLocation = new Location(Bukkit.getWorld(set.getString("shop_world")),
				set.getDouble("shop_x"),
				set.getDouble("shop_y"),
				set.getDouble("shop_z"));
		double price = set.getDouble("price");
		int amount = set.getInt("amount");
		long creationTime = set.getLong("creation_time");
		boolean isAdmin = AdminOfflinePlayer.adminUUID.equals(ownerId);
		ShopType shopType = ShopType.from(set.getString("shop_type"));
		if(shopType == null){
			throw new IllegalArgumentException("Shoptype is invalid provided: " + set.getString("shop_type"));
		}
		BlockFace facing = BlockFace.valueOf(set.getString("sign_facing"));
		DisplayType displayType = DisplayType.valueOf(set.getString("display_type"));
		// This inits a new shop but won't have a chestLocation until load().
		AbstractShop shop = AbstractShop.create(id, signLocation, ownerId, price, amount, isAdmin, shopType, facing, creationTime, displayType);
		ItemStack stack = ItemStackJsonCodec.deserialize(set.getString("item"));
		
		int stock = set.getInt("last_known_stock_count");
		shop.setStockOnLoad(stock);
		ShopState state = ShopState.from(set.getString("last_known_stock_status"));
		shop.setShopState(state, false);
		shop.setItemStack(stack);
		if(shop.getType() == ShopType.BARTER){
			ItemStack barterItem = ItemStackJsonCodec.deserialize(set.getString("secondary_item"));
			shop.setSecondaryItemStack(barterItem);
		}
		
		boolean isFakeSign = set.getBoolean("fake_sign");
		if(isFakeSign){
			shop.setFakeSign(true);
		}
		
		try(var ps = getConnection().prepareStatement(SELECT_SHOP_SETTING_SQL)){
			ps.setString(1, id.toString());
			ResultSet resultSet = ps.executeQuery();
			while(resultSet.next()){
				String settingKey = resultSet.getString("setting_key");
				Setting<?> setting = ALL_SETTINGS.get(settingKey);
				if(settingKey == null){
					logger.warning("Shop setting with key: " + settingKey + " not valid!");
					continue;
				}
				shop.initializeSetting(setting, setting.parse(resultSet.getString("value")));
			}
		}
		
		return shop;
	}
	
	private void insertShopValues(AbstractShop shop, PreparedStatement ps) throws SQLException {
		ItemStack mainStack = shop.getItemStack().clone();
		mainStack.setAmount(1);
		ItemStack barterStack = shop.getSecondaryItemStack();
		if(barterStack != null){
			barterStack = barterStack.clone();
			barterStack.setAmount(1);
		}
		Location signLocation = shop.getSignLocation();
		ps.setString(1, shop.getId().toString());
		ps.setString(2, shop.getOwnerUUID().toString());
		ps.setString(3, ItemStackJsonCodec.serialize(mainStack, true));
		ps.setDouble(4, shop.getPrice());
		ps.setInt(5, shop.getAmount());
		ps.setInt(6, shop.getStock());
		ps.setString(7, shop.getShopState().toString());
		ps.setString(8, shop.getType().toString().toUpperCase());
		BlockFace facing = shop.getFacing();
		if(facing == null){
			facing = BlockFace.EAST;
		}
		ps.setString(9, facing.toString().toUpperCase());
		ps.setString(10,
				(shop.getDisplay() != null && shop.getDisplay().getType() != null)
				? shop.getDisplay().getType().toString()
				: DisplayType.NONE.toString());
		ps.setInt(11, shop.isFakeSign() ? 1 : 0);
		ps.setString(12, barterStack != null ? ItemStackJsonCodec.serialize(barterStack, true) : null);
		ps.setLong(13, shop.getCreationDate());
		ps.setString(14, mainStack.getType().toString());
		ps.setString(15, barterStack != null ? barterStack.getType().toString() : null);
		ps.setString(16, signLocation.getWorld().getName());
		ps.setInt(17, signLocation.getBlockX());
		ps.setInt(18, signLocation.getBlockY());
		ps.setInt(19, signLocation.getBlockZ());
	}
	
	/**
	 * Adds a player to the player name cache
	 *
	 * @param uuid the uuid of the player
	 * @param name the name of the player
	 */
	public void addPlayer(UUID uuid, String name) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement("""
					INSERT INTO players (uuid, name)
					VALUES (?, ?)
					ON CONFLICT(uuid) DO UPDATE SET name = excluded.name;
					""")){
				
				ps.setString(1, uuid.toString());
				ps.setString(2, name);
				ps.execute();
				
			} catch(SQLException e){
				logger().error("Error while adding user to db", e);
			}
		});
	}
	
	/**
	 * Data migration helper method to add players to the known names
	 *
	 * @param players
	 */
	public void addLegacyPlayers(Map<UUID, String> players) {
		if(players == null || players.isEmpty()){
			return;
		}
		
		Connection connection = getConnection();
		
		try{
			connection.setAutoCommit(false);
			
			try(var ps = connection.prepareStatement("""
					INSERT INTO players (uuid, name)
					VALUES (?, ?)
					ON CONFLICT(uuid) DO NOTHING
					""")){
				
				for(var player : players.entrySet()){
					ps.setString(1, player.getKey().toString());
					ps.setString(2, player.getValue());
					
					ps.addBatch();
				}
				
				ps.executeBatch();
			}
			
			connection.commit();
			
		} catch(SQLException e){
			try{
				connection.rollback();
			} catch(SQLException rollbackException){
				logger().error("Error while rolling back legacy players", rollbackException);
			}
			
			logger().error("Error while adding legacy players to db", e);
			
		} finally{
			try{
				connection.setAutoCommit(true);
			} catch(SQLException e){
				logger().error("Error while restoring database auto-commit", e);
			}
		}
	}
	
	/**
	 * @return all cached player names
	 */
	public Map<UUID, String> loadPlayerNames() {
		Map<UUID, String> names = new HashMap<>();
		try(var ps = getConnection().prepareStatement("SELECT uuid,name FROM  players")){
			var result = ps.executeQuery();
			while(result.next()){
				names.put(UUID.fromString(result.getString(1)), result.getString(2));
			}
			
		} catch(SQLException e){
			logger().error("Error while adding user to db", e);
		}
		return names;
	}
	
	/**
	 * Marks a shop as being destroyed in the database
	 */
	public void removeShop(AbstractShop shop) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement("""
					UPDATE shops
					SET destroy_time = ?
					WHERE shop_uuid = ?
					""")){
				
				ps.setLong(1, System.currentTimeMillis());
				ps.setString(2, shop.getId().toString());
				ps.execute();
				
			} catch(SQLException e){
				logger().error("Error while deactivating shop", e);
			}
		});
	}
	
	/**
	 * Logs a transaction to the database and records the purchase to the player profile if they are online
	 *
	 * @param shopId the id of the shop
	 * @param timestamp when the transaction happened
	 * @param purchaserId the id of the purchaser
	 * @param gambleReward the reward given out from gambling
	 * @param multiplier how often the transaction happened
	 */
	public void logTransaction(UUID shopId, long timestamp, UUID purchaserId, @Nullable ItemStack gambleReward, int multiplier) {
		scheduler.runAsync(_ -> {
			try(var preparedStatement = getConnection().prepareStatement("""
					INSERT INTO transactions(shop_uuid, timestamp, purchaser_uuid,cache_offline,gamble_reward, transaction_count) VALUES (?,?,?,?,?,?)
					""");){//SQLITE auto increments primary keys, message here is false!
				preparedStatement.setString(1, shopId.toString());
				preparedStatement.setLong(2, timestamp);
				preparedStatement.setString(3, purchaserId.toString());
				preparedStatement.setInt(4, 0); //todo:mjd impplement offline caching for players to get stats when they login next time.
				preparedStatement.setString(5, gambleReward != null ? ItemStackJsonCodec.serialize(gambleReward, false) : null);
				preparedStatement.setInt(6, multiplier);
				preparedStatement.execute();
			} catch(SQLException e){
				logger().error("Error while adding transaction to shop", e);
			}
		});
		
	}
	
	/**
	 * Logs a transaction for migration tasks to the database and records the purchase to the player profile if they are online
	 */
	public void logLegacyTransactions(List<ShopHistoryEntry> entries) {
		if(entries == null || entries.isEmpty()){
			return;
		}
		
		Connection connection = getConnection();
		
		try{
			connection.setAutoCommit(false);
			
			try(var preparedStatement = connection.prepareStatement("""
					INSERT INTO transactions(
					    shop_uuid,
					    timestamp,
					    purchaser_uuid,
					    cache_offline,
					    gamble_reward,
					    transaction_count
					) VALUES (?, ?, ?, ?, ?, ?)
					""")){
				
				for(ShopHistoryEntry entry : entries){
					preparedStatement.setString(1, entry.shopUuid().toString());
					preparedStatement.setLong(2, entry.timestamp());
					preparedStatement.setString(3, entry.purchaserUuid().toString());
					preparedStatement.setInt(4, 0);
					preparedStatement.setNull(5, java.sql.Types.VARCHAR);
					preparedStatement.setInt(6, 1);
					
					preparedStatement.addBatch();
				}
				
				preparedStatement.executeBatch();
			}
			
			connection.commit();
			
		} catch(SQLException e){
			try{
				connection.rollback();
			} catch(SQLException rollbackException){
				logger().error("Error while rolling back legacy transactions", rollbackException);
			}
			
			logger().error("Error while adding legacy transactions to shop", e);
			
		} finally{
			try{
				connection.setAutoCommit(true);
			} catch(SQLException e){
				logger().error("Error while restoring database auto-commit", e);
			}
		}
	}
	
	/**
	 * Logs a change in currency
	 *
	 * @param type the type of the currency
	 * @param currency if its an itemstack the stack
	 */
	public void logCurrencyChange(CurrencyType type, @Nullable ItemStack currency) {
		scheduler.runAsync(_ -> {
			try(var preparedStatement = getConnection().prepareStatement("""
					INSERT INTO currency_history(timestamp, currency_type, item) VALUES (?,?,?)
					""");){//SQLITE auto increments primary keys, message here is false!
				preparedStatement.setLong(1, System.currentTimeMillis());
				preparedStatement.setString(2, type.toString());
				preparedStatement.setString(3, currency != null ? ItemStackJsonCodec.serialize(currency, true) : null);
				preparedStatement.execute();
			} catch(SQLException e){
				logger().error("Error while adding transaction to shop", e);
			}
		});
	}
	
	/**
	 * Compares the given parameters with the last known currency state and updates the database if it differs
	 */
	public void updateCurrencyIfChanged(CurrencyType currencyType, @Nullable ItemStack currencyItem) {
		scheduler.runAsync(_ -> {
			try(var preparedStatement = getConnection().prepareStatement("""
					SELECT item, currency_type
					FROM currency_history
					ORDER BY timestamp DESC
					LIMIT 1
					""")){
				try(var resultSet = preparedStatement.executeQuery()){
					if(!resultSet.next()){
						// No previous state exists, so record the current state.
						if(currencyType == CurrencyType.ITEM){
							logCurrencyChange(currencyType, currencyItem);
						} else {
							logCurrencyChange(currencyType, null);
						}
						return;
					}
					
					String previousItem = resultSet.getString("item");
					String currentItem = currencyItem != null ? ItemStackJsonCodec.serialize(currencyItem, true) : null;
					CurrencyType type = CurrencyType.fromValue(resultSet.getString("currency_type"));
					
					if(currencyType == type){
						if(Objects.equals(previousItem, currentItem)){
							return;
						}
					}
					
					if(currencyType == CurrencyType.ITEM){
						logCurrencyChange(currencyType, currencyItem);
					} else {
						logCurrencyChange(currencyType, null);
					}
				}
				
			} catch(SQLException e){
				logger().error("Error while checking currency state for {}", currencyType, e);
			}
		});
	}
	
	/**
	 * Logs a shop action
	 *
	 * @param player the player doing the action
	 * @param shopUuid the shop id
	 * @param actionType the action
	 */
	public void logAction(OfflinePlayer player, UUID shopUuid, ShopActionType actionType) {
		scheduler.runAsync(_ -> {
			// Connect to datasource & create statement in "try" to handle automatically closing the connection!
			try(Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO shop_actions(timestamp, player_uuid, shop_uuid, player_action) VALUES(?,  ?, ?, ?);");){
				stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				stmt.setString(2, player.getUniqueId().toString());
				stmt.setString(3, shopUuid.toString());
				stmt.setString(4, actionType.toString());
				stmt.execute();
			} catch(SQLException e){
				plugin.logger().log(Level.WARNING, "SQL error occurred while trying to log player action.");
				e.printStackTrace();
			}
		});
	}
	
	public void logAction(OfflinePlayer player, AbstractShop shop, ShopActionType actionType) {
		if(actionType == ShopActionType.INIT){
			logger().debug(player.getName() +
						   " created a " +
						   shop.getType().name().toUpperCase() +
						   " shop at (" +
						   "x: " +
						   shop.getContainerLocation().getBlockX() +
						   " y: " +
						   shop.getContainerLocation().getBlockY() +
						   " z: " +
						   shop.getContainerLocation().getBlockZ() +
						   ") item: " +
						   ItemNameUtil.getNameAsPlainText(shop.getItemStack()) +
						   (shop.getSecondaryItemStack() != null
							? " barterItem: " + ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack())
							: ""));
		}
		if(actionType == ShopActionType.DESTROY){
			logger().debug(player.getName() +
						   " destroyed a " +
						   shop.getType().name().toUpperCase() +
						   " shop at (" +
						   "x: " +
						   shop.getContainerLocation().getBlockX() +
						   " y: " +
						   shop.getContainerLocation().getBlockY() +
						   " z: " +
						   shop.getContainerLocation().getBlockZ() +
						   ") item: " +
						   ItemNameUtil.getNameAsPlainText(shop.getItemStack()) +
						   (shop.getSecondaryItemStack() != null
							? " barterItem: " + ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack())
							: ""));
		}
		logAction(player, shop.getId(), actionType);
	}
	
	/**
	 * update all shops
	 *
	 * @param shops
	 */
	public void updateShops(Collection<? extends AbstractShop> shops) {
		if(shops == null || shops.isEmpty() || plugin.isImmediateShutdown()){
			return;
		}
		
		try(Connection connection = getConnection(); PreparedStatement ps = connection.prepareStatement(SHOP_UPDATE_SQL)){
			
			connection.setAutoCommit(false);
			
			try{
				List<AbstractShop> updatedShops = new ArrayList<>();
				
				for(AbstractShop shop : shops){
					if(plugin.isImmediateShutdown()){
						connection.rollback();
						return;
					}
					
					if(shop == null || !shop.isInitialized()){
						continue;
					}
					
					updateShopValues(shop, ps);
					ps.addBatch();
					updatedShops.add(shop);
				}
				
				if(updatedShops.isEmpty()){
					connection.rollback();
					return;
				}
				
				ps.executeBatch();
				connection.commit();
				
				for(AbstractShop shop : updatedShops){
					shop.setNeedsSave(false);
				}
				
			} catch(SQLException e){
				connection.rollback();
				throw e;
			} finally{
				connection.setAutoCommit(true);
			}
			
		} catch(SQLException e){
			logger().error("Error while batch updating shops", e);
		}
	}
	
	private void updateShopValues(AbstractShop shop, PreparedStatement ps) throws SQLException {
		ItemStack mainStack = shop.getItemStack().clone();
		mainStack.setAmount(1);
		
		ItemStack barterStack = shop.getSecondaryItemStack();
		if(barterStack != null){
			barterStack = barterStack.clone();
			barterStack.setAmount(1);
		}
		
		Location signLocation = shop.getSignLocation();
		
		ps.setString(1, shop.getOwnerUUID().toString());
		ps.setString(2, ItemStackJsonCodec.serialize(mainStack, true));
		ps.setDouble(3, shop.getPrice());
		ps.setInt(4, shop.getAmount());
		ps.setInt(5, shop.getStock());
		ps.setString(6, shop.getType().toString().toUpperCase());
		ps.setString(7, shop.getFacing().toString().toUpperCase());
		ps.setString(8,
				shop.getDisplay() != null && shop.getDisplay().getType() != null
				? shop.getDisplay().getType().toString()
				: DisplayType.NONE.toString());
		ps.setInt(9, shop.isFakeSign() ? 1 : 0);
		ps.setString(10, barterStack != null ? ItemStackJsonCodec.serialize(barterStack, true) : null);
		ps.setString(11, mainStack.getType().toString());
		ps.setString(12, barterStack != null ? barterStack.getType().toString() : null);
		ps.setString(13, signLocation.getWorld().getName());
		ps.setInt(14, signLocation.getBlockX());
		ps.setInt(15, signLocation.getBlockY());
		ps.setInt(16, signLocation.getBlockZ());
		ps.setString(17, shop.getId().toString());
	}
	
	/**
	 * Loads player specific shop data
	 *
	 * @param playerUuid
	 * @param totalPurchasesPerShop
	 * @param lastPurchaseTimePerShop
	 * @return
	 */
	public CompletableFuture<Void> loadShopPurchaseStats(UUID playerUuid,
														 Map<UUID, Integer> totalPurchasesPerShop,
														 Map<UUID, Long> lastPurchaseTimePerShop) {
		return CompletableFuture.runAsync(() -> {
			try(var ps = getConnection().prepareStatement(PURCHASE_STATS_SQL)){
				ps.setString(1, playerUuid.toString());
				
				try(var resultSet = ps.executeQuery()){
					while(resultSet.next()){
						UUID shopUuid = UUID.fromString(resultSet.getString("shop_uuid"));
						
						totalPurchasesPerShop.put(shopUuid, resultSet.getInt("total_purchases"));
						
						lastPurchaseTimePerShop.put(shopUuid, resultSet.getLong("last_purchase"));
					}
				}
			} catch(SQLException e){
				logger().error("Error while loading player shop purchase statistics", e);
			}
		});
	}
	
	public <T> void addSetting(AbstractShop abstractShop, Setting<T> setting, T value) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement(INSERT_SHOP_SETTING_SQL)){
				ps.setString(1, abstractShop.getId().toString());
				ps.setString(2, setting.getKey());
				ps.setString(3, value == null ? null : value.toString());
				ps.execute();
			} catch(SQLException e){
				logger().error("Error while saving shop settings", e);
				
			}
		});
	}
	
	/**
	 * Performance Statistics about a shops transactions.
	 *
	 * @param shop the shop to query
	 * @return
	 */
	public CompletableFuture<TransactionStats> getTransactionStats(AbstractShop shop) {
		CompletableFuture<TransactionStats> stats = new CompletableFuture<>();
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement(TRANSACTION_STATS_SQL)){
				long now = System.currentTimeMillis();
				
				ps.setLong(1, now - TimeUnit.DAYS.toMillis(1));
				ps.setLong(2, now - TimeUnit.DAYS.toMillis(7));
				ps.setLong(3, now - TimeUnit.DAYS.toMillis(30));
				ps.setString(4, shop.getId().toString());
				
				try(var rs = ps.executeQuery()){
					if(rs.next()){
						stats.complete(new TransactionStats(rs.getLong("day1"), rs.getLong("day7"), rs.getLong("day30"), rs.getLong("all_time")));
					}
					stats.complete(new TransactionStats(0, 0, 0, 0));
				}
			} catch(SQLException e){
				logger().error("Error while fetching transactions for shop", e);
				stats.complete(new TransactionStats(0, 0, 0, 0));
			}
		});
		return stats;
	}
	
	/**
	 * Returns a collection of stats on what transaction the user has made and with what shops
	 */
	public CompletableFuture<Map<ShopEntry, TransactionStats>> getTransactionStatsForShops(Collection<UUID> shopIds) {
		if(shopIds.isEmpty()){
			return CompletableFuture.completedFuture(Collections.emptyMap());
		}
		CompletableFuture<Map<ShopEntry, TransactionStats>> stats = new CompletableFuture<>();
		
		scheduler.runAsync(_ -> {
			Map<ShopEntry, TransactionStats> shopEntries = new HashMap<>();
			
			String placeholders = String.join(", ", Collections.nCopies(shopIds.size(), "?"));
			try(var ps = getConnection().prepareStatement(TRANSACTION_STATS_SHOPS_SQL.formatted(placeholders))){
				long now = System.currentTimeMillis();
				
				int index = 1;
				
				ps.setLong(index++, now - TimeUnit.DAYS.toMillis(1));
				ps.setLong(index++, now - TimeUnit.DAYS.toMillis(7));
				ps.setLong(index++, now - TimeUnit.DAYS.toMillis(30));
				
				for(UUID shopId : shopIds){
					ps.setString(index++, shopId.toString());
				}
				
				try(var rs = ps.executeQuery()){
					while(rs.next()){
						UUID shopUuid = UUID.fromString(rs.getString("shopuuid"));
						UUID owner = UUID.fromString(rs.getString("ownerUuid"));
						ShopType type = ShopType.typeFromString((rs.getString("shop_type")));
						
						ItemStack stack = ItemStackJsonCodec.deserialize(rs.getString("itemBase64"));
						
						ItemStack barterStack = null;
						
						if(type == ShopType.BARTER){
							barterStack = ItemStackJsonCodec.deserialize(rs.getString("barterItemBase64"));
						}
						
						int amount = rs.getInt("amount");
						int price = rs.getInt("price");
						long creationDate = rs.getLong("timestamp");
						boolean isActive = rs.getBoolean("active");
						
						shopEntries.put(new ShopEntry(shopUuid, type, owner, stack, amount, price, creationDate, isActive, type, barterStack),
								new TransactionStats(rs.getLong("day1"), rs.getLong("day7"), rs.getLong("day30"), rs.getLong("all_time")));
					}
				}
				
			} catch(SQLException e){
				PluginLogger.error("Error while fetching transactions for shops", e);
				stats.complete(Collections.emptyMap());
			}
			
			stats.complete(shopEntries);
		});
		
		return stats;
	}
	
	public record ShopEntry(UUID shopUuid, ShopType type, UUID ownerUuid, ItemStack itemStack, int amount, int price, long creationDate,
							boolean isActive, ShopType shopType, ItemStack barterItem){
		
	}
	
	public record TransactionStats(long day1, long day7, long day30, long allTime){}
}
