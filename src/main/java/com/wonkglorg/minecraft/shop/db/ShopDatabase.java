package com.wonkglorg.minecraft.shop.db;

import com.tcoded.folialib.impl.PlatformScheduler;
import com.wonkglorg.database.DatabaseType;
import com.wonkglorg.database.databases.SqliteDatabase;
import com.wonkglorg.database.datasources.FileDataSource;
import com.wonkglorg.minecraft.shop.Constants;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ComboShop;
import com.wonkglorg.minecraft.shop.shop.ShopState;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import com.wonkglorg.minecraft.shop.util.OfflineTransactions;
import com.wonkglorg.minecraft.shop.shop.ShopActionType;
import com.wonkglorg.minecraft.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ShopDatabase extends SqliteDatabase<FileDataSource>{
	private static final String SHOP_INSERT_SQL = """
			INSERT INTO shops
			(shop_uuid, owner_uuid, item, price,price_combo_sell, amount,last_known_stock_count, last_known_stock_status, shop_type,sign_facing, display_type,fake_sign, barter_item, creation_time, item_type, item_barter_type, shop_world, shop_x, shop_y, shop_z)
			VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			ON CONFLICT(shop_uuid, owner_uuid) DO NOTHING;
			""";
	private static final String SHOP_SELECT_SQL = """
			SELECT shop_uuid, owner_uuid, item, price,price_combo_sell, amount, last_known_stock_count,last_known_stock_status, destroyed_time, shop_type,sign_facing, display_type, fake_sign, barter_item, creation_time, item_type, item_barter_type, shop_world, shop_x, shop_y, shop_z
			  FROM shops
			 WHERE destroyed_time = 0 OR 1 = ?;
			""";
	
	private static final String SHOP_CACHE_STOCK_SQL = """
			UPDATE shops SET last_known_stock_count = ?, last_known_stock_status = ? WHERE shop_uuid = ?
			""";
	
	private static final String SHOP_UPDATE_SQL = """
			UPDATE shops SET owner_uuid = ?, item = ?, price = ?, price_combo_sell = ?, amount = ?, last_known_stock_count = ?, shop_type = ?, sign_facing = ?, display_type = ?, fake_sign = ?, barter_item = ?, item_type = ?, item_barter_type = ?, shop_world = ?, shop_x = ?, shop_y = ?, shop_z = ? WHERE shop_uuid = ?
			""";
	
	private final Main plugin;
	private final PlatformScheduler scheduler;
	
	public ShopDatabase(Main plugin) throws SQLException, IOException {
		super(new FileDataSource(DatabaseType.SQLITE, plugin.getDataPath().resolve("data", "shop.db")));
		this.plugin = plugin;
		this.scheduler = plugin.getFoliaLib().getScheduler();
		initDB();
		addPlayer(Constants.getAdminUUID(), "admin");
	}
	
	public void initDB() throws SQLException, IOException {
		loadSqlStatements(plugin.getResource("db-setup.sql"), "");
	}
	
	public void addShop(AbstractShop shop) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement(SHOP_INSERT_SQL)){
				if(shop.getFacing() == null){
					PluginLogger.error("Shop " + shop + "is missing a facing direction!");
					return;
				}
				insertShopValues(shop, ps);
				ps.executeUpdate();
			} catch(SQLException e){
				PluginLogger.error("Error while creating shop chest", e);
			}
		});
	}
	
	public void addShops(Collection<? extends AbstractShop> shops) {
		if(shops == null || shops.isEmpty()){
			return;
		}
		
		scheduler.runAsync(_ -> {
			try(var connection = getConnection(); var ps = connection.prepareStatement(SHOP_INSERT_SQL)){
				
				connection.setAutoCommit(false);
				
				try{
					for(AbstractShop shop : shops){
						if(shop.getFacing() == null){
							PluginLogger.error("Shop " + shop + "is missing a facing direction!");
							continue;
						}
						insertShopValues(shop, ps);
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
				PluginLogger.error("Error while creating shops", e);
			}
		});
	}
	
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
				PluginLogger.error("Error while creating shop chest", e);
			}
			future.complete(shops);
		});
		
		return future;
	}
	
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
			PluginLogger.error("Error while caching shop stock values", e);
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
		double priceSell = set.getDouble("price_combo_sell");
		int amount = set.getInt("amount");
		long creationTime = set.getLong("creation_time");
		boolean isAdmin = Constants.adminUUID.equals(ownerId);
		ShopType shopType = ShopType.from(set.getString("shop_type"));
		if(shopType == null){
			throw new IllegalArgumentException("Shoptype is invalid provided: " + set.getString("shop_type"));
		}
		BlockFace facing = BlockFace.valueOf(set.getString("sign_facing"));
		DisplayType displayType = DisplayType.valueOf(set.getString("display_type"));
		// This inits a new shop but won't have a chestLocation until load().
		AbstractShop shop = AbstractShop.create(id,
				signLocation,
				ownerId,
				price,
				priceSell,
				amount,
				isAdmin,
				shopType,
				facing,
				creationTime,
				displayType);
		ItemStack stack = ItemStackJsonCodec.deserialize(set.getString("item"));
		
		int stock = set.getInt("last_known_stock_count");
		shop.setStockOnLoad(stock);
		ShopState state = ShopState.from(set.getString("last_known_stock_status"));
		shop.setShopState(state);
		shop.setItemStack(stack);
		if(shop.getType() == ShopType.BARTER){
			ItemStack barterItem = ItemStackJsonCodec.deserialize(set.getString("barter_item"));
			shop.setSecondaryItemStack(barterItem);
		}
		
		boolean isFakeSign = set.getBoolean("fake_sign");
		if(isFakeSign){
			shop.setFakeSign(true);
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
		ps.setString(3, ItemStackJsonCodec.serialize(mainStack));
		ps.setDouble(4, shop.getPrice());
		ps.setDouble(5, shop.getType() == ShopType.COMBO ? ((ComboShop) shop).getPriceSell() : 0);
		ps.setInt(6, shop.getAmount());
		ps.setInt(7, shop.getStock());
		ps.setString(8, shop.getShopState().toString());
		ps.setString(9, shop.getType().toString().toUpperCase());
		ps.setString(10, shop.getFacing().toString().toUpperCase());
		ps.setString(11,
				(shop.getDisplay() != null && shop.getDisplay().getType() != null)
				? shop.getDisplay().getType().toString()
				: DisplayType.NONE.toString());
		ps.setInt(12, shop.isFakeSign() ? 1 : 0);
		ps.setString(13, barterStack != null ? ItemStackJsonCodec.serialize(barterStack) : null);
		ps.setLong(14, System.currentTimeMillis());
		ps.setString(15, mainStack.getType().toString());
		ps.setString(16, barterStack != null ? barterStack.getType().toString() : null);
		ps.setString(17, signLocation.getWorld().getName());
		ps.setInt(18, signLocation.getBlockX());
		ps.setInt(19, signLocation.getBlockY());
		ps.setInt(20, signLocation.getBlockZ());
	}
	
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
				PluginLogger.error("Error while adding user to db", e);
			}
		});
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
			PluginLogger.error("Error while adding user to db", e);
		}
		return names;
	}
	
	public void removeShop(AbstractShop shop) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement("""
					UPDATE shops
					SET destroyed_time = ?
					WHERE shop_uuid = ?
					""")){
				
				ps.setLong(1, System.currentTimeMillis());
				ps.setString(2, shop.getId().toString());
				ps.execute();
				
			} catch(SQLException e){
				PluginLogger.error("Error while deactivating shop", e);
			}
		});
	}
	
	public void logTransaction(UUID shopId, long timestamp, UUID purchaserId, double fraction, @Nullable ItemStack gambleReward) {
		scheduler.runAsync(_ -> {
			try(var preparedStatement = getConnection().prepareStatement("""
					INSERT INTO transactions(shop_uuid, timestamp, purchaser_uuid,cache_offline,fraction,gamble_reward) VALUES (?,?,?,?,?,?)
					""");){//SQLITE auto increments primary keys, message here is false!
				preparedStatement.setString(1, shopId.toString());
				preparedStatement.setLong(2, timestamp);
				preparedStatement.setString(3, purchaserId.toString());
				preparedStatement.setInt(4, 0); //todo:mjd impplement offline caching for players to get stats when they login next time.
				preparedStatement.setDouble(5, fraction);
				preparedStatement.setString(6, gambleReward != null ? ItemStackJsonCodec.serialize(gambleReward) : null);
				preparedStatement.execute();
			} catch(SQLException e){
				PluginLogger.error("Error while adding transaction to shop", e);
			}
		});
		
	}
	
	public void logCurrencyChange(CurrencyType type, @Nullable ItemStack currency) {
		scheduler.runAsync(_ -> {
			try(var preparedStatement = getConnection().prepareStatement("""
					INSERT INTO currency_history(timestamp, currency_type, item) VALUES (?,?,?)
					""");){//SQLITE auto increments primary keys, message here is false!
				preparedStatement.setLong(1, System.currentTimeMillis());
				preparedStatement.setString(2, type.toString());
				preparedStatement.setString(3, currency != null ? ItemStackJsonCodec.serialize(currency) : null);
				preparedStatement.execute();
			} catch(SQLException e){
				PluginLogger.error("Error while adding transaction to shop", e);
			}
		});
		
	}
	
	public void logAction(OfflinePlayer player, UUID shopOwner, UUID shopUuid, ShopActionType actionType) {
		scheduler.runAsync(_ -> {
			// Connect to datasource & create statement in "try" to handle automatically closing the connection!
			try(Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO shop_actions(timestamp, player_uuid, owner_uuid, shop_uuid, player_action) VALUES(?, ?, ?, ?, ?);");){
				stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				stmt.setString(2, player.getUniqueId().toString());
				if(Constants.getAdminUUID().equals(shopOwner)){
					stmt.setString(3, "admin");
				} else {
					stmt.setString(3, shopOwner.toString());
				}
				stmt.setString(4, shopUuid.toString());
				stmt.setString(5, actionType.toString());
				stmt.execute();
			} catch(SQLException e){
				plugin.logger().log(Level.WARNING, "SQL error occurred while trying to log player action.");
				e.printStackTrace();
			}
		});
	}
	
	public void logAction(OfflinePlayer player, AbstractShop shop, ShopActionType actionType) {
		if(actionType == ShopActionType.INIT){
			plugin.logger().debug(player.getName() +
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
			                       (shop.getSecondaryItemStack() != null ? " barterItem: " +
			                                                               ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack()) : ""));
		}
		if(actionType == ShopActionType.DESTROY){
			plugin.logger().debug(player.getName() +
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
			                       (shop.getSecondaryItemStack() != null ? " barterItem: " +
			                                                               ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack()) : ""));
		}
		logAction(player, shop.getOwnerUUID(), shop.getId(), actionType);
	}
	
	public void calculateOfflineTransactions(OfflineTransactions offlineTransactions) {
	
	}
	
	public void updateShops(Collection<? extends AbstractShop> shops) {
		if(shops == null || shops.isEmpty() || plugin.isImmediateShutdown()){
			return;
		}
		
		if(plugin.isImmediateShutdown()){
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
			PluginLogger.error("Error while batch updating shops", e);
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
		ps.setString(2, ItemStackJsonCodec.serialize(mainStack));
		ps.setDouble(3, shop.getPrice());
		ps.setDouble(4, shop.getType() == ShopType.COMBO ? ((ComboShop) shop).getPriceSell() : 0);
		ps.setInt(5, shop.getAmount());
		ps.setInt(6, shop.getStock());
		ps.setString(7, shop.getType().toString().toUpperCase());
		ps.setString(8, shop.getFacing().toString().toUpperCase());
		ps.setString(9,
				shop.getDisplay() != null && shop.getDisplay().getType() != null
				? shop.getDisplay().getType().toString()
				: DisplayType.NONE.toString());
		ps.setInt(10, shop.isFakeSign() ? 1 : 0);
		ps.setString(11, barterStack != null ? ItemStackJsonCodec.serialize(barterStack) : null);
		ps.setString(12, mainStack.getType().toString());
		ps.setString(13, barterStack != null ? barterStack.getType().toString() : null);
		ps.setString(14, signLocation.getWorld().getName());
		ps.setInt(15, signLocation.getBlockX());
		ps.setInt(16, signLocation.getBlockY());
		ps.setInt(17, signLocation.getBlockZ());
		ps.setString(18, shop.getId().toString());
	}
}
