package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.event.PlayerPostInitializeShopEvent;
import static com.snowgears.shop.manager.PlayerManager.cleanupShopCreationProcess;
import static com.snowgears.shop.manager.PlayerManager.getShopCreationProcess;
import com.snowgears.shop.manager.player.PlayerProfile;
import static com.snowgears.shop.manager.player.PlayerProfile.getShopBuildLimit;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToCreateShop;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.CreationWord;
import com.snowgears.shop.shop.ShopType;
import com.wonkglorg.minecraft.config.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Light;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopCreationUtil{
	
	private final Shop plugin;
	private final SettingsConfig settingsConfig;
	private LangManager lang;
	private BlockFace[] wallFaces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	
	public ShopCreationUtil(Shop plugin) {
		this.plugin = plugin;
		settingsConfig = plugin.getSettingsConfig();
		lang = plugin.getLangManager();
	}
	
	public BlockFace calculateBlockFaceForSign(Player player, Block chest, BlockFace facePreference) {
		if(facePreference == BlockFace.UP || facePreference == BlockFace.DOWN){
			facePreference = BlockFace.NORTH;
		}
		Block futureSign = chest.getRelative(facePreference);
		if(UtilMethods.materialIsNonIntrusive(futureSign.getType())){
			return facePreference;
		}
		for(BlockFace face : wallFaces){
			futureSign = chest.getRelative(face);
			if(UtilMethods.materialIsNonIntrusive(futureSign.getType())){
				return face;
			}
		}
		lang.request("interaction_issue.signRoom").sendToAudience(player);
		return null;
	}
	
	public boolean shopCanBeCreated(Player player, Block chest) {
		if(player.isOp()){
			return true;
		}
		
		boolean isOperator = isOperator(player);
		
		//operators ignore world blacklist
		if(!isOperator && settingsConfig.getWorldBlackList().contains(chest.getWorld().getName())){
			lang.request("interaction_issue.worldBlacklist").sendToAudience(player);
			return false;
		}
		
		if(!isAllowedToCreateShop(player)){
		
		}
		
		int numberOfShops = plugin.getShopmanager().getNumberOfShops(player.getUniqueId());
		int buildPermissionNumber = getShopBuildLimit(player);
		if(numberOfShops >= buildPermissionNumber){
			lang.request("permission.error.buildLimit").sendToAudience(player);
			return false;
		}
		
		return true;
	}
	
	public AbstractShop createShop(Player player,
	                               Block chestBlock,
	                               Block signBlock,
	                               PricePair pricePair,
	                               int amount,
	                               boolean isAdmin,
	                               ShopType type,
	                               BlockFace signDirection,
	                               boolean isFakeSign) {
		String playerMessage = null;
		if(type == null){
			type = ShopType.SELL;
		}
		
		boolean hasOperatorPermission = isOperator(player);
		
		if(!PlayerProfile.isAllowedToCreateShop(player, type)){
			lang.request("permission.error.create").replace("%shop-type%", type).sendToAudience(player);
			return null;
		}
		
		final AbstractShop shop = AbstractShop.create(signBlock.getLocation(),
				player.getUniqueId(),
				pricePair.getPrice(),
				pricePair.getPriceCombo(),
				amount,
				isAdmin,
				type,
				signDirection,
				System.currentTimeMillis());
		shop.setFakeSign(isFakeSign);
		
		if(type == ShopType.GAMBLE){
			isAdmin = true;
			shop.setAdmin(true);
			//todo:jmd why was this in the original? why is gamble always admin?
			if(!hasOperatorPermission){
				lang.request("permission.error.create").sendToAudience(player);
				return null;
			}
		}
		
		//if players must pay to create shops, check that they have enough money first
		if(!hasOperatorPermission){
			double cost = settingsConfig.getCreationCost();
			if(cost > 0 && !EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)){
				lang.request("interaction_issue.createInsufficientFunds").sendToAudience(player);
				return null;
			}
		}
		
		//prevent players (even if they are OP) from creating a shop on a double chest with another player
		AbstractShop existingShop = plugin.getShopmanager().getShopByContainer(chestBlock);
		if(existingShop != null && !existingShop.isAdmin()){
			if(!existingShop.getOwnerUUID().equals(player.getUniqueId())){
				playerMessage = ShopMessage.formatPlainTextSingle("interactionIssue.createOtherPlayer", new PlaceholderContext());
			}
		}
		
		if(playerMessage != null){
			if(!playerMessage.isEmpty()){
				ShopMessage.request(playerMessage, player, shop).sendToAudience(player);
			}
			return null;
		}
		
		//removed all the direction checking code. just make sure its a container
		//make sure that the sign is in front of the chest, unless it is a shulker box
		if(chestBlock.getState() instanceof Container){
			existingShop = plugin.getShopmanager().getShopByContainer(chestBlock);
			if(existingShop != null){
				//if the block they are adding a sign to is already a shop, do not let them
				if(chestBlock.getLocation().equals(existingShop.getContainerLocation())){
					ShopMessage.request("interactionIssue.createOtherPlayer", player, shop).sendToAudience(player);
					return null;
				}
			}
			
			if(!(signBlock.getBlockData() instanceof WallSign)){
				if(!signBlock.getType().toString().contains("_SIGN")){
					return null;
				}
				String wallSignString = signBlock.getType().toString().replace("_SIGN", "_WALL_SIGN");
				signBlock.setType(Material.valueOf(wallSignString));
				
				Directional wallSignData = (Directional) signBlock.getBlockData();
				wallSignData.setFacing(signDirection);
				signBlock.setBlockData(wallSignData);
			}
			Sign signBlockState = (Sign) signBlock.getState();
			signBlockState.update();
			
			shop.setAdmin(isAdmin);
			boolean loaded = shop.load();
			if(!loaded){
				plugin.getLogger()
				      .warning("Shop creation failed, unable to load the shop. Aborting shop creation."); // only seen this happen in tests
				return null;
			}
			
			PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
			plugin.getServer().getPluginManager().callEvent(e);
			
			plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.CREATE);
			
			if(e.isCancelled()){
				return null;
			}
			
			if(plugin.getSettingsConfig().getDisplayLightLevel() > 0){
				Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
				if(UtilMethods.materialIsNonIntrusive(displayBlock.getType())){
					displayBlock.setType(Material.LIGHT);
					Light data = (Light) displayBlock.getBlockData();
					data.setLevel(plugin.getSettingsConfig().getDisplayLightLevel());
					displayBlock.setBlockData(data);
				}
			}
			
			if(type == ShopType.GAMBLE){
				shop.setItemStack(plugin.getItemConfig().getGambleDisplayItem());
				shop.setAmount(1);
				plugin.getShopmanager().registerShop(shop);
				shop.getDisplay().setType(DisplayType.LARGE_ITEM, false);
				
				plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
				plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.INIT);
				return null;
			}
			
			plugin.getShopmanager().registerShop(shop);
			Shop.getPlugin().logger().trace("[ShopCreationUtil.createShop] updateSign");
			shop.updateSign();
		}
		return shop;
	}
	
	public void sendCreationSuccess(Player player, AbstractShop shop) {
		if(shop.getDisplay() != null){
			shop.getDisplay().spawn(player);
		}
		Shop.getPlugin().logger().trace("[ShopCreationUtil.sendCreationSuccess] updateSign");
		shop.updateSign(true);
		shop.setNeedsSave(true);
		ShopMessage.request(shop.getType() + ".create", player, shop).sendToAudience(player);
		shop.sendEffects(true, player);
		// Save the shop to disk
		// TODO: We should move this save trigger elsewhere, it doesn't belong in `sendCreationSuccess`,
		//       it is currently non-intuitive that this is the method to save a shop when it is created.
		//       We should move it elsewhere.
		plugin.getShopmanager().getDatabase().addShop(shop);
		// Cleanup the shop creation process
		cleanupShopCreationProcess(player);
	}
	
	public boolean itemsCanBeInitialized(Player player, ItemStack itemStack, ItemStack barterItemStack) {
		boolean isAdmin = isOperator(player);
		
		//if the item is on the DENY LIST or the item is not on the ALLOW LIST, don't let player initialize with it
		// Only perform this check for non admins
		if(!isAdmin){
			boolean passesItemList = plugin.getShopmanager().passesItemListCheck(itemStack);
			if(!passesItemList){
				lang.request("interaction_issue.itemListDeny").sendToAudience(player);
				return false;
			}
		}
		
		// Always perform this check, even if admin!
		if(InventoryUtils.itemstacksAreSimilar(itemStack, barterItemStack)){
			lang.request("interaction_issue.createSameItem").sendToAudience(player);
			return false;
		}
		return true;
	}
	
	public boolean initializeShop(AbstractShop shop, Player player, ItemStack item, ItemStack barterItem) {
		if(!player.getUniqueId().equals(shop.getOwnerUUID())){
			//do not allow non operators to initialize other player's shops
			if(!isOperator(player)){
				ShopMessage.request("interactionIssue.initialize", player, shop).sendToAudience(player);
				shop.sendEffects(false, player);
				return false;
			}
		}
		
		if(item.getType() == Material.AIR){
			return false;
		}
		
		if(plugin.getSettingsConfig().getDisplayTypeDefault() != DisplayType.NONE){
			//make sure there is room above the shop for the display
			Block aboveShop = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
			if(!UtilMethods.materialIsNonIntrusive(aboveShop.getType())){
				if(settingsConfig.isForceDisplayToNoneIfBlocked()){
					shop.getDisplay().setType(DisplayType.NONE, false);
				} else {
					ShopMessage.request("interactionIssue.displayRoom", player, shop).sendToAudience(player);
					shop.sendEffects(false, player);
					return false;
				}
			}
		}
		
		//if players must pay to create shops, remove money first
		double cost = settingsConfig.getCreationCost();
		// Check if the shop is not an admin shop and if the shop is not a barter shop or the barter item is not null
		// When creating a barter shop with a sign, initializeShop is called twice, we only want to charge them once both items are selected
		if(cost > 0 && !shop.isAdmin() && !(shop.getType() == ShopType.BARTER && barterItem == null)){
			boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
			if(!removed){
				ShopMessage.request("interactionIssue.createInsufficientFunds", player, shop).sendToAudience(player);
				shop.sendEffects(false, player);
				return false;
			}
		}
		
		//stop the edge case of shulker boxes being able to be used in shulker chests
		if(Tag.SHULKER_BOXES.isTagged(item.getType())){
			if(shop.getContainerLocation().getBlock().getState() instanceof ShulkerBox){
				return false;
			}
		}
		
		if(!itemsCanBeInitialized(player, item, barterItem)){
			shop.sendEffects(false, player);
			return false;
		}
		
		if(shop.getItemStack() == null){
			
			PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
			Bukkit.getServer().getPluginManager().callEvent(e);
			
			if(e.isCancelled()){
				return false;
			}
			
			shop.setItemStack(item);
			
			ShopCreationProcess process = getShopCreationProcess(player.getUniqueId());
			if(shop.getType() == ShopType.BARTER && barterItem == null){
				ShopMessage.request(shop.getType() + ".initializeInfo", player, shop).sendToAudience(player);
				process.setStep(ShopCreationProcess.ChatCreationStep.SIGN_BARTER_ITEM);
				process.displayFloatingText(shop.getType() + ".initializeBarter");
			} else if(shop.getType() != ShopType.BARTER){
				Bukkit.getServer().getPluginManager().callEvent(new PlayerPostInitializeShopEvent(player, shop));
				return true;
			}
		}
		if(shop.getSecondaryItemStack() == null && barterItem != null){
			
			PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
			Bukkit.getServer().getPluginManager().callEvent(e);
			
			if(e.isCancelled()){
				return false;
			}
			
			shop.setSecondaryItemStack(barterItem);
			Bukkit.getServer().getPluginManager().callEvent(new PlayerPostInitializeShopEvent(player, shop));
			return true;
		}
		return false;
	}
	
	public ShopType getShopType(String input) {
		ShopType type = null;
		SettingsConfig config = Shop.getPlugin().getSettingsConfig();
		input = input.toLowerCase();
		if(input.contains(config.getCreationWord(CreationWord.BUY))){
			type = ShopType.BUY;
		} else if(input.contains(config.getCreationWord(CreationWord.BARTER))){
			type = ShopType.BARTER;
		} else if(input.contains(config.getCreationWord(CreationWord.GAMBLE))){
			type = ShopType.GAMBLE;
		} else if(input.contains(config.getCreationWord(CreationWord.COMBO))){
			type = ShopType.COMBO;
		} else if(input.contains(config.getCreationWord(CreationWord.SELL))){
			type = ShopType.SELL;
		}
		return type;
	}
	
	public double getShopPrice(Player player, String input, ShopType shopType) {
		double price = 0;
		if(settingsConfig.getCurrencyType() == CurrencyType.VAULT){
			try{
				double multiplyValue = UtilMethods.getMultiplyValue(input);
				String line3 = UtilMethods.cleanNumberText(input);
				
				if(line3.contains(".")){
					price = Double.parseDouble(line3);
				} else {
					price = Long.parseLong(line3);
				}
				
				price *= multiplyValue;
			} catch(NumberFormatException e){
				lang.request("interaction_issue.line3").sendToAudience(player);
				return -1;
			}
		} else {
			try{
				String line3 = UtilMethods.cleanNumberText(input);
				price = Long.parseLong(line3);
				
			} catch(NumberFormatException e){
				lang.request("interactionIssue.line3").sendToAudience(player);
				return -1;
			}
		}
		//only allow price to be zero if the type is selling
		if(price < 0 || (price == 0 && shopType == ShopType.BARTER)){
			lang.request("interactionIssue.line3").sendToAudience(player);
			return -1;
		}
		return price;
	}
	
	public double getShopPriceCombo(Player player, String input, ShopType shopType) {
		double priceCombo = 0;
		if(settingsConfig.getCurrencyType() == CurrencyType.VAULT){
			try{
				double multiplyValue = UtilMethods.getMultiplyValue(input);
				String line3 = UtilMethods.cleanNumberText(input);
				
				if(line3.contains(".")){
					priceCombo = Double.parseDouble(line3);
				} else {
					priceCombo = Long.parseLong(line3);
				}
				
				priceCombo *= multiplyValue;
				
			} catch(NumberFormatException e){
				lang.request("interactionIssue.line3").sendToAudience(player);
				return -1;
			}
		} else {
			try{
				String line3 = UtilMethods.cleanNumberText(input);
				priceCombo = Long.parseLong(line3);
			} catch(NumberFormatException e){
				lang.request("interactionIssue.line3").sendToAudience(player);
				return -1;
			}
		}
		return priceCombo;
	}
	
	public PricePair getShopPricePair(Player player, String input, ShopType shopType) {
		double price = 0;
		double priceCombo = 0;
		if(settingsConfig.getCurrencyType() == CurrencyType.VAULT){
			try{
				double multiplyValue = UtilMethods.getMultiplyValue(input);
				String line3 = UtilMethods.cleanNumberText(input);
				
				String[] multiplePrices = line3.split(" ");
				if(multiplePrices.length > 1){
					if(multiplePrices[0].contains(".")){
						price = Double.parseDouble(multiplePrices[0]);
					} else {
						price = Long.parseLong(multiplePrices[0]);
					}
					
					if(multiplePrices[1].contains(".")){
						priceCombo = Double.parseDouble(multiplePrices[1]);
					} else {
						priceCombo = Long.parseLong(multiplePrices[1]);
					}
				} else {
					if(line3.contains(".")){
						price = Double.parseDouble(line3);
					} else {
						price = Long.parseLong(line3);
					}
				}
				
				price *= multiplyValue;
				priceCombo *= multiplyValue;
				
			} catch(NumberFormatException e){
				lang.request("interactionIssue.line3").sendToAudience(player);
				return null;
			}
		} else {
			try{
				String line3 = UtilMethods.cleanNumberText(input);
				
				String[] multiplePrices = line3.split(" ");
				if(multiplePrices.length > 1){
					price = Long.parseLong(multiplePrices[0]);
					priceCombo = Long.parseLong(multiplePrices[1]);
				} else {
					price = Long.parseLong(line3);
				}
			} catch(NumberFormatException e){
				lang.request("interactionIssue.line3").sendToAudience(player);
				return null;
			}
		}
		//only allow price to be zero if the type is selling
		if(price < 0 || (price == 0 && shopType == ShopType.BARTER)){
			lang.request("interactionIssue.line3").sendToAudience(player);
			return null;
		}
		return new PricePair(price, priceCombo);
	}
	
	public boolean getShopIsAdmin(String input) {
		return input.toLowerCase().contains(Shop.getPlugin().getSettingsConfig().getCreationWord(CreationWord.ADMIN));
	}
}
