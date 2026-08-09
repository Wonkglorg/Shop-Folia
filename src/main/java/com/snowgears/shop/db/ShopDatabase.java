package com.snowgears.shop.db;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.OfflineTransactions;
import com.snowgears.shop.util.ShopActionType;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.wonkglorg.database.DatabaseType;
import com.wonkglorg.database.databases.SqliteDatabase;
import com.wonkglorg.database.datasources.FileDataSource;
import com.wonkglorg.minecraft.util.PluginLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;

public class ShopDatabase extends SqliteDatabase<FileDataSource>{
	private static final String SHOP_INSERT_SQL = """
			INSERT INTO shops
			(shop_uuid, owner_uuid, item, price, amount, active, shop_type, barter_item, timestamp, item_type, item_barter_type, shop_world, shop_x, shop_y, shop_z)
			VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			ON CONFLICT(shop_uuid, owner_uuid) DO NOTHING;
			""";
	
	private final Shop plugin;
	private final PlatformScheduler scheduler;
	
	public ShopDatabase(Shop plugin) throws SQLException, IOException {
		super(new FileDataSource(DatabaseType.SQLITE, plugin.getDataPath().resolve("data", "shop.db")));
		this.plugin = plugin;
		this.scheduler = plugin.getFoliaLib().getScheduler();
		initDB();
	}
	
	public void initDB() throws SQLException, IOException {
		loadSqlStatements(plugin.getResource("db-setup.sql"), "");
	}
	
	public void createShop(AbstractShop shop) {
		scheduler.runAsync(_ -> {
			try(var ps = getConnection().prepareStatement(SHOP_INSERT_SQL)){
				insertShopValues(shop, ps);
				ps.executeUpdate();
			} catch(SQLException e){
				PluginLogger.error("Error while creating shop chest", e);
			}
		});
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
		ps.setInt(4, (int) shop.getPrice());
		ps.setInt(5, shop.getAmount());
		ps.setInt(6, 1);
		ps.setString(7, shop.getType().toString());
		ps.setString(8, barterStack != null ? ItemStackJsonCodec.serialize(barterStack) : null);
		ps.setLong(9, System.currentTimeMillis());
		ps.setString(10, mainStack.getType().toString());
		ps.setString(11, barterStack != null ? barterStack.getType().toString() : null);
		ps.setString(12, signLocation.getWorld().getName());
		ps.setInt(13, signLocation.getBlockX());
		ps.setInt(14, signLocation.getBlockY());
		ps.setInt(15, signLocation.getBlockZ());
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
				if(ShopHandler.getAdminUUID().equals(shop.getOwnerUUID())){
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
