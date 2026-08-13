package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.event.PlayerPreInitializeShopEvent;
import com.snowgears.shop.event.PlayerPostInitializeShopEvent;
import static com.snowgears.shop.manager.PlayerManager.getShopCreationProcess;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.config.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
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
			
			PlayerPreInitializeShopEvent e = new PlayerPreInitializeShopEvent(player, shop);
			Bukkit.getServer().getPluginManager().callEvent(e);
			
			if(e.isCancelled()){
				return false;
			}
			
			shop.setItemStack(item);
			
			ShopCreationProcessOld process = getShopCreationProcess(player.getUniqueId());
			if(shop.getType() == ShopType.BARTER && barterItem == null){
				ShopMessage.request(shop.getType() + ".initializeInfo", player, shop).sendToAudience(player);
				process.setStep(ShopCreationProcessOld.ChatCreationStep.SIGN_BARTER_ITEM);
				process.displayFloatingText(shop.getType() + ".initializeBarter");
			} else if(shop.getType() != ShopType.BARTER){
				Bukkit.getServer().getPluginManager().callEvent(new PlayerPostInitializeShopEvent(player, shop));
				return true;
			}
		}
		if(shop.getSecondaryItemStack() == null && barterItem != null){
			
			PlayerPreInitializeShopEvent e = new PlayerPreInitializeShopEvent(player, shop);
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
}
