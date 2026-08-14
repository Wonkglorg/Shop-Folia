package com.snowgears.shop.migrate;

import com.snowgears.shop.Constants;
import com.snowgears.shop.Shop;
import com.snowgears.shop.manager.player.PlayerProfile;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import static com.snowgears.shop.shop.ShopType.typeFromString;
import com.snowgears.shop.shop.display.DisplayType;
import com.snowgears.shop.util.ShopLogger;
import com.wonkglorg.minecraft.config.types.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Represents a players shops in form of a yml config.
 */
public class PlayerShopsConfig extends Config{
	private static final ShopLogger logger = Shop.getPlugin().logger();
	/**
	 * The folder player shops get saved to
	 */
	public static final Path SHOPS_DATA_FOLDER = Shop.getPlugin().getDataPath().resolve("data", "shops");
	
	/**
	 * The shop file name
	 */
	public PlayerShopsConfig(@NotNull Path path) {
		super(path);
	}
	
	public static List<AbstractShop> loadLegacyShops() {
		List<AbstractShop> shops = new ArrayList<>();
		if(!Files.exists(SHOPS_DATA_FOLDER)){
			try{
				Files.createDirectories(SHOPS_DATA_FOLDER);
			} catch(IOException e){
				Shop.getPlugin().logger().severe("Unable to create shop directory." + e.getMessage());
				return shops;
			}
		}
		
		AtomicInteger numShopsLoaded = new AtomicInteger(0);
		
		try(Stream<Path> walk = Files.walk(SHOPS_DATA_FOLDER)){
			walk.forEach(path -> {
				if(!Files.isRegularFile(path)){
					return;
				}
				if(!path.toString().endsWith(".yml")){
					return;
				}
				String fileName = path.getFileName().toString().replace(".yml", "");
				try{
					PlayerShopsConfig config = new PlayerShopsConfig(SHOPS_DATA_FOLDER.resolve(fileName + ".yml"));
					for(var shop : config.loadShops()){
						numShopsLoaded.incrementAndGet();
						shops.add(shop);
					}
				} catch(IllegalArgumentException iae){
					Shop.getPlugin().logger().severe("Unable to load file: '" + path + "' '" + path.getFileName() + "' is not a valid uuid!");
				}
			});
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		Shop.getPlugin().logger().log(Level.INFO, "Loaded " + numShopsLoaded.get() + " Shops!");
		return shops;
	}
	
	private List<AbstractShop> loadShops() {
		if(!contains("shops")){
			return new ArrayList<>();
		}
		
		List<AbstractShop> shops = new ArrayList<>();
		Set<String> allShopOwners = getKeys("shops", false);
		
		for(String shopOwner : allShopOwners){
			UUID owner = null;
			
			Set<String> allShopNumbers = getKeys("shops." + shopOwner, false);
			int playerLoadedShops = 0;
			for(String shopNumber : allShopNumbers){
				ConfigurationSection section = getConfigurationSection("shops." + shopOwner + "." + shopNumber);
				if(section == null){
					//should not be possible
					continue;
				}
				Location signLoc = locationFromString(section.getString("location"));
				String idString = section.getString("id");
				UUID id = null;
				if(idString != null && !idString.isEmpty()){
					id = UUID.fromString(idString);
				}
				boolean isAdmin;
				if(shopOwner.equals("admin")){
					owner = Constants.getAdminUUID();
					isAdmin = true;
				} else {
					owner = UUID.fromString(shopOwner);
					isAdmin = false;
				}
				
				BlockFace facing = null;
				String facingStr = section.getString("facing");
				if(facingStr != null){
					facing = BlockFace.valueOf(facingStr);
				}
				
				String type = section.getString("type");
				double price = section.getDouble("price");
				double priceSell = 0;
				if(section.contains("priceSell")){
					priceSell = section.getDouble("priceSell", 0);
				}
				int amount = section.getInt("amount");
				
				ShopType shopType = typeFromString(type);
				
				ItemStack itemStack = section.getItemStack("item");
				if(shopType == ShopType.GAMBLE){
					itemStack = Shop.getPlugin().getItemConfig().getGambleDisplayItem();
				}
				
				if(itemStack == null){
					Shop.getPlugin().logger().log(Level.WARNING,
							"Unable to load Shop #" + shopNumber + " for owner '" + shopOwner + "'! no valid item Skipping!");
					continue;
				}
				
				AbstractShop shop = AbstractShop.create(id == null ? UUID.randomUUID() : id,
						signLoc,
						owner,
						price,
						priceSell,
						amount,
						isAdmin,
						shopType,
						facing,
						System.currentTimeMillis());
				
				// Important: apply saved stock BEFORE setting item stacks, since setItemStack()
				// may calculate stock (inventory may be null pre-load) and may also render sign text.
				int stock = section.getInt("stock");
				shop.setStockOnLoad(stock);
				
				shop.setItemStack(itemStack);
				if(shop.getType() == ShopType.BARTER){
					ItemStack barterItemStack = section.getItemStack("itemBarter");
					if(barterItemStack == null){
						Shop.getPlugin().logger().log(Level.WARNING,
								"Unable to load Shop #" + shopNumber + " for owner '" + shopOwner + "'! no valid barter item Skipping!");
						continue;
					}
					shop.setSecondaryItemStack(barterItemStack);
				}
				String displayType = section.getString("displayType");
				if(displayType != null){
					shop.getDisplay().setType(DisplayType.valueOf(displayType), false);
				}
				
				boolean isFakeSign = section.getBoolean("fakeSign");
				if(isFakeSign){
					shop.setFakeSign(true);
				}
				
				// If we just added an ID to a shop for the first time, then it will need to be saved/updated as well
				if(idString == null || idString.isEmpty()){
					shop.setNeedsSave(true);
				}
				shops.add(shop);
				playerLoadedShops++;
			}
			String ownerName = shopOwner.equals("admin")
			                   ? "admin"
			                   : Shop.getPlugin().getServer().getOfflinePlayer(UUID.fromString(shopOwner)).getName();
			logger.helpful("Loaded (" + playerLoadedShops + ") shops for Player " + ownerName + " from: " + shopOwner + ".yml");
		}
		return shops;
	}
	
	public int saveShops(final UUID uuid) {return saveShops(uuid, false);}
	
	public int saveShops(final UUID uuid, boolean force) {
		// Check if any of the players shops want to be saved
		Shop plugin = Shop.getPlugin();
		if(plugin.isImmediateShutdown()){
			return 0;
		}
		boolean isAdminShops = uuid.equals(Constants.getAdminUUID());
		String playerName = isAdminShops ? "admin" : plugin.getServer().getOfflinePlayer(uuid).getName();
		List<AbstractShop> shops = PlayerProfile.getShops(uuid);
		
		int needToBeSaved = 0;
		for(AbstractShop shop : shops){
			if(shop.needsSave()){
				needToBeSaved++;
			}
		}
		
		if(!force && needToBeSaved == 0 && !shops.isEmpty()){
			logger.trace("save shops for player (" + playerName + ") was called, but no shops for player need updating! " + uuid);
			return 0;
		}
		
		// There are shops that need to be saved, so go ahead and save the file!
		logger.debug("attempting to save shops for player " + playerName + " (" + uuid + ") isAdmin: " + (uuid.equals(Constants.getAdminUUID())));
		
		Path tempFile = SHOPS_DATA_FOLDER.resolve(isAdminShops ? playerName : uuid + ".tmp");
		Path file = SHOPS_DATA_FOLDER.resolve(isAdminShops ? playerName : uuid + ".yml");
		if(shops.isEmpty()){
			try{
				Files.deleteIfExists(tempFile);
			} catch(IOException e){
				logger.severe("Unable to delete file " + tempFile);
				return 0;
			}
		}
		
		PlayerShopsConfig config = new PlayerShopsConfig(tempFile);
		int shopNumber = 0;
		for(var shop : shops){
			if(plugin.isImmediateShutdown()){
				return 0;
			}
			
			//this is to remove a bug that caused one shop to be saved to multiple files at one point
			if(!shop.getOwnerUUID().equals(uuid)){
				continue;
			}
			if(!shop.isInitialized()){
				continue;
			}
			shopNumber++;
			var section = config.createSection("shops." + uuid + "." + shopNumber);
			
			section.set("id", shop.getId().toString());
			section.set("location", locationToString(shop.getSignLocation()));
			if(shop.getFacing() != null){
				section.set("facing", shop.getFacing().toString());
			}
			if(shop.getType() == ShopType.COMBO){
				section.set("priceSell", ((ComboShop) shop).getPriceSell());
			}
			section.set("amount", shop.getAmount());
			/*
			 * Why was this:
			 * 					String type = "";
			 * 					if(shop.isAdmin()){
			 * 						type = "admin ";
			 * 					                    }
			 * 					type = type + shop.getType().toString();
			 *
			 */
			section.set("type", shop.getType().toString());
			if(shop.getDisplay().getType() != null){
				section.set("displayType", shop.getDisplay().getType().toString());
			} else {
				section.set("displayType", null);
			}
			if(shop.isFakeSign()){
				section.set("fakeSign", true);
			} else {
				section.set("fakeSign", null);
			}
			
			section.set("stock", shop.getStock());
			
			ItemStack itemStack = shop.getItemStack();
			itemStack.setAmount(1);
			if(shop.getType() == ShopType.GAMBLE){
				itemStack = new ItemStack(Material.AIR);
			}
			section.set("item", itemStack);
			
			if(shop.getType() == ShopType.BARTER){
				ItemStack barterItemStack = shop.getSecondaryItemStack();
				barterItemStack.setAmount(1);
				section.set("itemBarter", barterItemStack);
			}
			
			shop.setNeedsSave(false);
		}
		config.silentSave();
		
		try{
			Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch(Error | Exception ex){
			try{
				logger.debug("Error during atomic move", ex);
				logger.debug("Filesystem does not support atomic move; using manual two-step replacement with backup...");
				//try using non atomic move (filesystem might not support it)
				Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
			} catch(Error | Exception moveEx){
				// Attempt to restore from backup on failure
				logger.severe("Critical error writing updated shop file for (" +
				              playerName +
				              ") to (" +
				              file +
				              ")! This issue should not be ignored! Error message: " +
				              moveEx.getMessage());
				Path backupFile = tempFile.resolveSibling(tempFile.getFileName() + ".bak");
				try{
					if(Files.exists(file)){
						logger.warning("Original file was left untouched. Player shop updates were not saved!");
					} else if(Files.exists(backupFile)){
						logger.warning("Restoring backup player shop file for " + playerName + " from (" + backupFile + ") to (" + file + ")");
						Files.move(backupFile, file, StandardCopyOption.REPLACE_EXISTING);
						logger.info("Successfully restored backup player shop file for " +
						            playerName +
						            " from (" +
						            backupFile +
						            ") to (" +
						            file +
						            ")!");
					}
				} catch(Error | Exception restoreEx){
					logger.severe("Failed to restore backup player shop file for " +
					              playerName +
					              " from (" +
					              backupFile +
					              ") to (" +
					              file +
					              ")! Exception: " +
					              restoreEx.getMessage());
				}
				// Double check that the file was restored successfully and/or the current state of the files
				if(Files.exists(file)){
					logger.warning("Original file was left untouched. Player shop updates were not saved!");
				} else if(Files.exists(backupFile)){
					logger.severe("Failed to restore backup player shop file for " + playerName);
					logger.severe("You will need to manually restore this players backup file from (" + backupFile + ") to (" + file + ")!");
				} else {
					// uh... no files exist somehow? Should never get here, but just in case since this is a critical failure
					logger.severe("Possible data loss detected! Original file does not exist and Backup file does not exist for player (" +
					              playerName +
					              ")! Original MISSING: (" +
					              file +
					              "), Backup MISSING: (" +
					              backupFile +
					              ")!!!");
					logger.severe(
							"Do not startup the plugin again until you have traced and fixed the issue! You may delete a new player file with each startup if the issue is not fixed!");
					// Immediate shutdown of server. Something is very wrong.
					logger.severe("Shutting down plugin immediately to prevent Shop save data loss...");
					com.snowgears.shop.Shop.getPlugin().immediateShutdown();
				}
			}
		}
		return shopNumber;
	}
	
	private static String locationToString(Location loc) {
		String worldName = loc.getWorld() == null ? "unknown_world" : loc.getWorld().getName();
		return worldName + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	private Location locationFromString(String locString) {
		String[] parts = locString.split(",");
		return new Location(Shop.getPlugin().getServer().getWorld(parts[0]),
				Double.parseDouble(parts[1]),
				Double.parseDouble(parts[2]),
				Double.parseDouble(parts[3]));
	}
	
}
