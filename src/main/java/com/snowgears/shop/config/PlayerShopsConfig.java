package com.snowgears.shop.config;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.ShopLogger;
import com.wonkglorg.minecraft.config.types.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents a players shops in form of a yml config.
 */
public class PlayerShopsConfig extends Config{
	private final ShopLogger logger = Shop.getPlugin().logger();
	
	public PlayerShopsConfig(@NotNull Path path) {
		super(path);
	}
	
	public List<AbstractShop> loadShops() {
		if(!contains("shops")){
			return null;
		}
		
		List<AbstractShop> shops = new ArrayList<>();
		
		int numShopsLoaded = 0;
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
					owner = Shop.getPlugin().getShopHandler().getAdminUUID();
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
				
				ShopType shopType = ShopType.valueOf(type.toUpperCase());
				
				ItemStack itemStack = section.getItemStack("item");
				if(shopType == ShopType.GAMBLE){
					itemStack = Shop.getPlugin().getGambleDisplayItem();
				}
				
				if(itemStack == null){
					Shop.getPlugin().logger().log(Level.WARNING,
							"Unable to load Shop #" + shopNumber + " for owner '" + shopOwner + "'! no valid item Skipping!");
					continue;
				}
				
				// This inits a new shop but won't have a chestLocation until load().
				AbstractShop shop = AbstractShop.create(signLoc, owner, price, priceSell, amount, isAdmin, shopType, facing);
				if(id != null){
					shop.setId(id);
				}
				
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
				
				// Load the GUI Icon so that it appears when players perform a search, even if the chunks haven't loaded yet.
				shop.refreshGuiIcon();

				// If we just added an ID to a shop for the first time, then it will need to be saved/updated as well
				if(idString == null || idString.isEmpty()){
					shop.setNeedsSave(true);
				}
				shops.add(shop);
				numShopsLoaded++;
				playerLoadedShops++;
			}
			String ownerName = shopOwner.equals("admin") ? "admin" : plugin.getServer().getOfflinePlayer(UUID.fromString(shopOwner)).getName();
			logger.helpful("Loaded (" + playerLoadedShops + ") shops for Player " + ownerName + " from: " + shopOwner + ".yml");
		}
		
		return shops;
	}
	
	public static int saveShops(final UUID player) {return saveShops(player, false);}
	
	public static int saveShops(final UUID player, boolean force) {
		// If the plugin is in immediate shutdown mode, skip saving any new files to protect against data loss
		if(this.immediateShutdown){
			return -5;
		}
		
		// Check if any of the players shops want to be saved
		String playerName = player == this.getAdminUUID() ? "admin" : plugin.getServer().getOfflinePlayer(player).getName();
		int numWantingToUpdate = numShopsNeedSave(player);
		if(!force && numWantingToUpdate == 0 && getNumberOfShops(player) > 0){
			logger.trace("save shops for player (" + playerName + ") was called, but no shops for player need updating! " + player.toString());
			return 0;
		}
		
		// There are shops that need to be saved, so go ahead and save the file!
		logger.debug("attempting to save shops for player " +
		             playerName +
		             " (" +
		             player.toString() +
		             ") isAdmin: " +
		             (player == Shop.getPlugin().getShopHandler().getAdminUUID()));
		File currentFile = null;
		try{
			
			File fileDirectory = new File(plugin.getDataFolder(), "Data");
			if(!fileDirectory.exists()){
				fileDirectory.mkdir();
			}
			
			String owner = null;
			if(player.equals(adminUUID)){
				owner = "admin";
				currentFile = new File(fileDirectory + "/admin.yml");
			} else {
				owner = player.toString();
				currentFile = new File(fileDirectory + "/" + player.toString() + ".yml");
			}
			
			logger.trace("    current file " + currentFile);
			
			// We will build the YAML in-memory and write via a temp file to avoid data loss.
			YamlConfiguration config = new YamlConfiguration();
			logger.trace("    preparing yaml for " + currentFile);
			
			List<AbstractShop> shopList = getShops(player);
			if(shopList.isEmpty()){
				currentFile.delete();
				logger.debug("    no shops exist for player (" + playerName + "), deleting file... " + currentFile);
				return -1;
			}
			
			int shopNumber = 0;
			for(AbstractShop shop : shopList){
				//this is to remove a bug that caused one shop to be saved to multiple files at one point
				if(!shop.getOwnerUUID().equals(player)){
					continue;
				}
				
				//don't save shops that are not initialized with items
				if(shop.isInitialized()){
					shopNumber++;
					config.set("shops." + owner + "." + shopNumber + ".id", shop.getId().toString());
					config.set("shops." + owner + "." + shopNumber + ".location", locationToString(shop.getSignLocation()));
					if(shop.getFacing() != null){
						config.set("shops." + owner + "." + shopNumber + ".facing", shop.getFacing().toString());
					}
					config.set("shops." + owner + "." + shopNumber + ".price", shop.getPrice());
					if(shop.getType() == ShopType.COMBO){
						config.set("shops." + owner + "." + shopNumber + ".priceSell", ((ComboShop) shop).getPriceSell());
					}
					config.set("shops." + owner + "." + shopNumber + ".amount", shop.getAmount());
					String type = "";
					if(shop.isAdmin()){
						type = "admin ";
					}
					type = type + shop.getType().toString();
					config.set("shops." + owner + "." + shopNumber + ".type", type);
					if(shop.getDisplay().getType() != null){
						config.set("shops." + owner + "." + shopNumber + ".displayType", shop.getDisplay().getType().toString());
					} else { //not sure why I have to do this but if I don't it will be set to LARGE_ITEM for some reason (I cannot find right now)
						config.set("shops." + owner + "." + shopNumber + ".displayType", null);
					}
					//only write the variable if true
					if(shop.isFakeSign()){
						config.set("shops." + owner + "." + shopNumber + ".fakeSign", shop.isFakeSign());
					}
					
					config.set("shops." + owner + "." + shopNumber + ".stock", shop.getStock());
					
					ItemStack itemStack = shop.getItemStack();
					itemStack.setAmount(1);
					if(shop.getType() == ShopType.GAMBLE){
						itemStack = new ItemStack(Material.AIR);
					}
					config.set("shops." + owner + "." + shopNumber + ".item", itemStack);
					
					if(shop.getType() == ShopType.BARTER){
						ItemStack barterItemStack = shop.getSecondaryItemStack();
						barterItemStack.setAmount(1);
						config.set("shops." + owner + "." + shopNumber + ".itemBarter", barterItemStack);
					}
					
					shop.setNeedsSave(false);
				} else {
					logger.debug("    shop " + shop + " is not initialized, skipping...");
				}
			}
			
			// Only generate the stringified config for logging if spam logging is enabled
			if(logger.isLevelEnabled(ShopLogger.SPAM)){
				logger.spam("    built config to save... \n" + config.saveToString());
			}
			
			// ---------- Safe file write ----------
			Path targetPath = currentFile.toPath();
			Path tempFile = Files.createTempFile(targetPath.getParent(), owner + "_", ".tmp");
			config.save(tempFile.toFile());
			try{
				// Atomic moves are very safe, so we use them if possible
				Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				logger.helpful("Saved " + shopNumber + " Shops for Player " + playerName + " to file: " + currentFile);
				return shopNumber;
			} catch(Error | Exception ex){
				logger.debug("Error during atomic move", ex);
				logger.debug("Filesystem does not support atomic move; using manual two-step replacement with backup...");
				// Filesystem does not support atomic move; use manual two-step replacement with backup
				Path backupPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".bak");
				try{
					if(Files.exists(targetPath)){
						logger.debug("Backing up existing shop file for " + playerName + " from (" + targetPath + ") to (" + backupPath + ")...");
						Files.move(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
						logger.debug("Successfully backed up existing shop file for " +
						             playerName +
						             " from (" +
						             targetPath +
						             ") to (" +
						             backupPath +
						             ")");
					}
					logger.debug("Moving new shop file for " + playerName + " from (" + tempFile + ") to (" + targetPath + ")");
					Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
					logger.debug("Successfully moved new shop file for " + playerName + " from (" + tempFile + ") to (" + targetPath + ")!");
					// New file written successfully – delete backup
					if(Files.exists(backupPath)){
						logger.debug("Deleting temporary backup of old shop file for " + playerName + " from (" + backupPath + ")");
						Files.deleteIfExists(backupPath);
						logger.debug("Successfully deleted temporary backup of old shop file for " + playerName + " from (" + backupPath + ")!");
					}
					
					logger.helpful("Saved " + shopNumber + " Shops for Player " + playerName + " to file: " + currentFile);
					return shopNumber;
				} catch(Error | Exception moveEx){
					// Attempt to restore from backup on failure
					logger.severe("Critical error writing updated shop file for (" +
					              playerName +
					              ") to (" +
					              targetPath +
					              ")! This issue should not be ignored! Error message: " +
					              moveEx.getMessage());
					try{
						if(Files.exists(targetPath)){
							logger.warning("Original file was left untouched. Player shop updates were not saved!");
						} else if(Files.exists(backupPath)){
							logger.warning("Restoring backup player shop file for " +
							               playerName +
							               " from (" +
							               backupPath +
							               ") to (" +
							               targetPath +
							               ")");
							Files.move(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
							logger.info("Successfully restored backup player shop file for " +
							            playerName +
							            " from (" +
							            backupPath +
							            ") to (" +
							            targetPath +
							            ")!");
						}
					} catch(Error | Exception restoreEx){
						logger.severe("Failed to restore backup player shop file for " +
						              playerName +
						              " from (" +
						              backupPath +
						              ") to (" +
						              targetPath +
						              ")! Exception: " +
						              restoreEx.getMessage());
					}
					// Double check that the file was restored successfully and/or the current state of the files
					if(Files.exists(targetPath)){
						logger.warning("Original file was left untouched. Player shop updates were not saved!");
						return -2;
					} else if(Files.exists(backupPath)){
						logger.severe("Failed to restore backup player shop file for " + playerName);
						logger.severe("You will need to manually restore this players backup file from (" +
						              backupPath +
						              ") to (" +
						              targetPath +
						              ")!");
						return -3;
					} else {
						// uh... no files exist somehow? Should never get here, but just in case since this is a critical failure
						logger.severe("Possible data loss detected! Original file does not exist and Backup file does not exist for player (" +
						              playerName +
						              ")! Original MISSING: (" +
						              targetPath +
						              "), Backup MISSING: (" +
						              backupPath +
						              ")!!!");
						logger.severe(
								"Do not startup the plugin again until you have traced and fixed the issue! You may delete a new player file with each startup if the issue is not fixed!");
						// Immediate shutdown of server. Something is very wrong.
						logger.severe("Shutting down plugin immediately to prevent Shop save data loss...");
						this.immediateShutdown = true;
						return -5;
					}
				}
			}
		} catch(Error | Exception e){
			// log severe: the player file failed to be generated/saved
			logger.severe("Unable to update/save player shop file for (" +
			              playerName +
			              ") at (" +
			              currentFile +
			              ")! Original file was left untouched. Error message: " +
			              e.getMessage());
			// log warning: Are these Shop files from an older version of the Minecraft?
			logger.warning(
					"Are these Shop player files from an older version of the Minecraft? You can run into issues with Item NBT data not migrating correctly if you jump forward/skip too many MC versions at a time. You might be able to fix this error by copying the affected player(s) file(s) to a new test server (you do not have to copy the world, but should if you are able to) and starting up the server in each 'skipped' version of Minecraft with the Shop plugin's `debug_forceResaveAll` config option set to `true`. This will force a resave of all Shop files and will update any NBT changes between the last run version of Minecraft and the new one you are trying to use.");
			// log about if they are unable to fix this error they might have to delete the Shop plugin data folder to start fresh
			logger.severe("If you are unable to fix this error, you will need to delete or manually fix the affected player shop file at (" +
			              currentFile +
			              ") in order to allow them to create new Shops and make this error go away. This will delete all Shops for the player and will require the player to re-add their shops.");
			// log stack trace at debug level
			logger.debug("Stacktrace: ", e);
			return -2;
		}
	}
	
	public static int saveAllShops() {
		HashMap<UUID, Boolean> allPlayersWithShops = new HashMap<>();
		for(AbstractShop shop : Shop.getPlugin().getShopHandler().getAllShops()){
			allPlayersWithShops.put(shop.getOwnerUUID(), true);
		}
		
		int numberUpdated = 0;
		int playersWithUpdate = 0;
		for(UUID player : allPlayersWithShops.keySet()){
			int shopsUpdated = saveShops(player);
			
			if(shopsUpdated > 0){
				numberUpdated += shopsUpdated;
				playersWithUpdate++;
			}
		}
		if(playersWithUpdate > 0){
			logger.info("Saved " + playersWithUpdate + " Player Shop file updates to disk");
		}
		return numberUpdated;
	}
	
	private String locationToString(Location loc) {
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
