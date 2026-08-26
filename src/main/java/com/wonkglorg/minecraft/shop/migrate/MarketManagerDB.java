package com.wonkglorg.minecraft.shop.migrate;

import com.wonkglorg.database.DatabaseType;
import com.wonkglorg.database.databases.SqliteDatabase;
import com.wonkglorg.database.datasources.FileDataSource;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.util.PluginLogger;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MarketManagerDB extends SqliteDatabase<FileDataSource>{
	
	public static final Path DBPATH = Path.of("migration", "marketmanager", "shopStats.db");
	
	public static final String SHOP_TRANSACTIONS_SQL = """
			SELECT shopUuid, timestamp, purchaserUuid
			FROM transactions
			""";
	
	public static final String SHOP_SELECT_SQL = """
			SELECT 	shopUuid, ownerUuid, itemBase64, price, amount, active, shopType, barterItemBase64, timestamp, itemType, itemBarterType, location
			FROM shops
			""";
	
	public MarketManagerDB(Plugin plugin) {
		super(new FileDataSource(DatabaseType.SQLITE, plugin.getDataPath().resolve(DBPATH)));
	}
	
	public static boolean containsDb(Main plugin) {
		return Files.exists(plugin.getDataPath().resolve(DBPATH));
	}
	
	public List<ShopHistoryEntry> getTransactions() {
		try(var ps = getConnection().prepareStatement(SHOP_TRANSACTIONS_SQL)){
			List<ShopHistoryEntry> entries = new java.util.ArrayList<>();
			try(var rs = ps.executeQuery()){
				while(rs.next()){
					entries.add(new ShopHistoryEntry(UUID.fromString(rs.getString(1)), rs.getLong(2), UUID.fromString(rs.getString(3))));
				}
			}
			return entries;
		} catch(SQLException e){
			PluginLogger.error("Error while fetching transactions for shop", e);
			return new ArrayList<>();
		}
	}
	
	public Map<AbstractShop, Boolean> getShops() {
		try(var ps = getConnection().prepareStatement(SHOP_SELECT_SQL)){
			
			Map<AbstractShop, Boolean> entries = new HashMap<>();
			
			try(var rs = ps.executeQuery()){
				while(rs.next()){
					UUID shopUuid = UUID.fromString(rs.getString("shopUuid"));
					UUID ownerUuid = UUID.fromString(rs.getString("ownerUuid"));
					
					String itemBase64 = rs.getString("itemBase64");
					double price = rs.getDouble("price");
					int amount = rs.getInt("amount");
					
					boolean active = rs.getBoolean("active");
					
					ShopType shopType = ShopType.from(rs.getString("shopType"));
					String barterItemBase64 = rs.getString("barterItemBase64");
					
					long timestamp = rs.getLong("timestamp");
					String location = rs.getString("location");
					
					if(location == null){
						log.warn("Skipping shop: '%s' with owner: '%s' missing location value!".formatted(shopUuid, ownerUuid));
						continue;
					}
					
					AbstractShop shop = AbstractShop.create(shopUuid,
							deserializeLegacyLocation(location),
							ownerUuid,
							price,
							amount,
							false,
							shopType == null ? ShopType.SELL : shopType,
							BlockFace.EAST,
							timestamp,
							DisplayType.NONE);
					
					shop.setItemStack(ItemStack.deserializeBytes(Base64.getMimeDecoder().decode(itemBase64)));
					if(barterItemBase64 != null){
						shop.setSecondaryItemStack(ItemStack.deserializeBytes(Base64.getMimeDecoder().decode(barterItemBase64)));
					}
					entries.put(shop, active);
				}
			}
			
			return entries;
			
		} catch(SQLException e){
			PluginLogger.error("Error while fetching shops", e);
			return new HashMap<>();
		}
	}
	
	public static Location deserializeLegacyLocation(String value) {
		Pattern pattern = Pattern.compile(
				"Location\\{world=CraftWorld\\{(?:key=minecraft:|name=)([^}]+)},x=([^,]+),y=([^,]+),z=([^,]+),pitch=([^,]+),yaw=([^}]+)}");
		
		Matcher matcher = pattern.matcher(value);
		
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid legacy location: " + value);
		}
		
		String worldName = matcher.group(1);
		
		// Legacy Bukkit/Minecraft key mapping
		if(worldName.equals("overworld")){
			worldName = "world";
		}
		
		double x = Double.parseDouble(matcher.group(2));
		double y = Double.parseDouble(matcher.group(3));
		double z = Double.parseDouble(matcher.group(4));
		float pitch = Float.parseFloat(matcher.group(5));
		float yaw = Float.parseFloat(matcher.group(6));
		
		World world = Bukkit.getWorld(worldName);
		
		if(world == null){
			throw new IllegalStateException("World not loaded: " + worldName);
		}
		
		return new Location(world, x, y, z, yaw, pitch);
	}
	
	public record ShopHistoryEntry(UUID shopUuid, long timestamp, UUID purchaserUuid){
	
	}
	
	public record TransactionStats(long day1, long day7, long day30, long allTime){}
	
}
