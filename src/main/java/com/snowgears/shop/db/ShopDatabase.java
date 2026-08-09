package com.snowgears.shop.db;

import com.snowgears.shop.Constants;
import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.manager.player.PlayerProfile;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.OfflineTransactions;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopActionType;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.wonkglorg.database.DatabaseType;
import com.wonkglorg.database.databases.SqliteDatabase;
import com.wonkglorg.database.datasources.FileDataSource;
import com.wonkglorg.minecraft.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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
			(shop_uuid, owner_uuid, item, price,price_combo_sell, amount,last_known_stock_count, active, shop_type,sign_facing, display_type,fake_sign, barter_item, timestamp, item_type, item_barter_type, shop_world, shop_x, shop_y, shop_z)
			VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			ON CONFLICT(shop_uuid, owner_uuid) DO NOTHING;
			""";
	private static final String SHOP_SELECT_SQL = """
			SELECT shop_uuid, owner_uuid, item, price,price_combo_sell, amount, last_known_stock_count, active, shop_type,sign_facing, display_type, fake_sign, barter_item, timestamp, item_type, item_barter_type, shop_world, shop_x, shop_y, shop_z FROM shops;
			""";
	
	private static final String SHOP_UPDATE_SQL = """
			UPDATE shops SET owner_uuid = ?, item = ?, price = ?, price_combo_sell = ?, amount = ?, last_known_stock_count = ?, active = ?, shop_type = ?, sign_facing = ?, display_type = ?, fake_sign = ?, barter_item = ?, timestamp = ?, item_type = ?, item_barter_type = ?, shop_world = ?, shop_x = ?, shop_y = ?, shop_z = ? WHERE shop_uuid = ?
			""";
	
	private final Shop plugin;
	private final PlatformScheduler scheduler;
	
	public ShopDatabase(Shop plugin) throws SQLException, IOException {
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
	
	/**
	 * Saves the shops of a specific player
	 *
	 * @param uuid the player
	 * @param force if all shops should be force saved.
	 */
	public void saveShops(UUID uuid, boolean force) {
		if(plugin.isImmediateShutdown()){
			return;
		}
		boolean isAdminShops = Constants.adminUUID.equals(uuid);
		String playerName = isAdminShops ? "admin" : PlayerNameCache.getName(uuid);
		
		List<AbstractShop> shops = PlayerProfile.getShops(uuid);
		
		int needToBeSaved = 0;
		for(AbstractShop shop : shops){
			if(shop.needsSave()){
				needToBeSaved++;
			}
		}
		
		if(!force && needToBeSaved == 0 && !shops.isEmpty()){
			plugin.logger().trace("save shops for player (" + playerName + ") was called, but no shops for player need updating! " + uuid);
			return;
		}
		
		scheduler.runAsync(_ -> {
			for(var shop : shops){
				if(plugin.isImmediateShutdown()){
					return;
				}
				
				if(!shop.needsSave() && !force){
					continue;
				}
				if(!shop.isInitialized()){
					continue;
				}
				
				updateShop(shop);
				
				shop.setNeedsSave(false);
			}
			
		});
	}
	
	private void updateShop(AbstractShop shop) {
		try(var ps = getConnection().prepareStatement(SHOP_UPDATE_SQL)){
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
			ps.setInt(7, 1);
			ps.setString(8, shop.getType().toString());
			ps.setString(9, shop.getFacing().toString());
			ps.setString(10, shop.getDisplay() != null ? shop.getDisplay().getType().toString() : DisplayType.NONE.toString());
			ps.setInt(11, shop.isFakeSign() ? 1 : 0);
			ps.setString(12, barterStack != null ? ItemStackJsonCodec.serialize(barterStack) : null);
			ps.setLong(13, System.currentTimeMillis());
			ps.setString(14, mainStack.getType().toString());
			ps.setString(15, barterStack != null ? barterStack.getType().toString() : null);
			ps.setString(16, signLocation.getWorld().getName());
			ps.setInt(17, signLocation.getBlockX());
			ps.setInt(18, signLocation.getBlockY());
			ps.setInt(19, signLocation.getBlockZ()); // The shop UUID identifies the row to update.
			ps.setString(20, shop.getId().toString());
			ps.executeUpdate();
		} catch(SQLException e){
			PluginLogger.error("Error while updating shop " + shop.getId(), e);
		}
	}
	
	public CompletableFuture<List<AbstractShop>> getShops() {
		CompletableFuture<List<AbstractShop>> future = new CompletableFuture<>();
		scheduler.runAsync(t -> {
			List<AbstractShop> shops = new ArrayList<>();
			try(var ps = getConnection().prepareStatement(SHOP_SELECT_SQL)){
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
		boolean isAdmin = Constants.adminUUID.equals(ownerId);
		ShopType shopType = ShopType.valueOf(set.getString("shop_type"));
		BlockFace facing = BlockFace.valueOf(set.getString("sign_facing"));
		// This inits a new shop but won't have a chestLocation until load().
		AbstractShop shop = AbstractShop.create(signLocation, ownerId, price, priceSell, amount, isAdmin, shopType, facing);
		shop.setId(id);
		ItemStack stack = ItemStackJsonCodec.deserialize(set.getString("item"));
		
		int stock = set.getInt("last_known_stock_count");
		shop.setStockOnLoad(stock);
		
		shop.setItemStack(stack);
		if(shop.getType() == ShopType.BARTER){
			ItemStack barterItem = ItemStackJsonCodec.deserialize(set.getString("barter_item"));
			shop.setSecondaryItemStack(barterItem);
		}
		String displayType = set.getString("display_type");
		if(displayType != null){
			shop.getDisplay().setType(DisplayType.valueOf(displayType), false);
		}
		
		boolean isFakeSign = set.getBoolean("fake_sign");
		if(isFakeSign){
			shop.setFakeSign(true);
		}
		
		// Load the GUI Icon so that it appears when players perform a search, even if the chunks haven't loaded yet.
		shop.refreshGuiIcon();
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
		ps.setInt(8, 1);
		ps.setString(9, shop.getType().toString());
		ps.setString(10, shop.getFacing().toString());
		ps.setString(11, shop.getDisplay().getType().toString());
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
					SET active = 0
					WHERE shop_uuid = ?
					""")){
				
				ps.setString(1, shop.getId().toString());
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
	
	public void logAction(Player player, AbstractShop shop, ShopActionType actionType) {
		if(actionType == ShopActionType.INIT){
			plugin.logger().notice(player.getName() +
			                       " created a " +
			                       shop.getType().name().toUpperCase() +
			                       " shop at (" +
			                       "x: " +
			                       shop.getChestLocation().getBlockX() +
			                       " y: " +
			                       shop.getChestLocation().getBlockY() +
			                       " z: " +
			                       shop.getChestLocation().getBlockZ() +
			                       ") item: " +
			                       ItemNameUtil.getNameAsPlainText(shop.getItemStack()) +
			                       (shop.getSecondaryItemStack() != null ? " barterItem: " +
			                                                               ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack()) : ""));
		}
		if(actionType == ShopActionType.DESTROY){
			plugin.logger().notice(player.getName() +
			                       " destroyed a " +
			                       shop.getType().name().toUpperCase() +
			                       " shop at (" +
			                       "x: " +
			                       shop.getChestLocation().getBlockX() +
			                       " y: " +
			                       shop.getChestLocation().getBlockY() +
			                       " z: " +
			                       shop.getChestLocation().getBlockZ() +
			                       ") item: " +
			                       ItemNameUtil.getNameAsPlainText(shop.getItemStack()) +
			                       (shop.getSecondaryItemStack() != null ? " barterItem: " +
			                                                               ItemNameUtil.getNameAsPlainText(shop.getSecondaryItemStack()) : ""));
		}
		scheduler.runAsync(_ -> {
			// Connect to datasource & create statement in "try" to handle automatically closing the connection!
			try(Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO shop_actions(timestamp, player_uuid, owner_uuid, shop_uuid, player_action) VALUES(?, ?, ?, ?, ?);");){
				stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				stmt.setString(2, player.getUniqueId().toString());
				if(Constants.getAdminUUID().equals(shop.getOwnerUUID())){
					stmt.setString(3, "admin");
				} else {
					stmt.setString(3, shop.getOwnerUUID().toString());
				}
				stmt.setString(4, shop.getId().toString());
				stmt.setString(5, actionType.toString());
				stmt.execute();
			} catch(SQLException e){
				plugin.logger().log(Level.WARNING, "SQL error occurred while trying to log player action.");
				e.printStackTrace();
			}
		});
	}
	
	public void calculateOfflineTransactions(OfflineTransactions offlineTransactions) {
	
	}
}
