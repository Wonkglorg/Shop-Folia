package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.display.DisplayType;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

//todo:mjd make gamble shops not admin only but also player ones with items inside that need to be refilled.
public class GambleShop extends AbstractShop{
	
	private ItemStack gambleItem;
	
	public GambleShop(UUID shopId,Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing, long creationDate) {
		super(shopId,signLoc, player, pri, amt, admin, facing,creationDate);
		
		this.isAdmin = true;
		this.creationWord = CreationWord.GAMBLE;
		this.type = ShopType.GAMBLE;
		this.signLines = ShopMessage.getSignLines(this);
		setGambleItem();
		this.setAmount(this.gambleItem.getAmount());
	}
	
	@Override
	protected void calculateStock() {
		stock = Integer.MAX_VALUE;
	}
	
	// Called upon a successful gamble transaction
	public void shuffleGambleItem(Player player) {
		isPerformingTransaction = true;
		this.setItemStack(gambleItem.clone());
		this.setAmount(gambleItem.getAmount());
		final DisplayType initialDisplayType = this.getDisplay().getType();
		this.getDisplay().setType(DisplayType.ITEM, false);
		setGambleItem();
		this.getDisplay().spawn(player);
		
		Shop.getPlugin().getFoliaLib().getScheduler().runLater(() -> {
			setItemStack(Shop.getPlugin().getItemConfig().getGambleDisplayItem());
			if(initialDisplayType == null){
				display.setType(Shop.getPlugin().getSettingsConfig().getDisplayTypeDefault(), false);
				getDisplay().spawn(player);
			} else {
				display.setType(initialDisplayType, false);
				getDisplay().spawn(player);
			}
			isPerformingTransaction = false;
		}, 20);
	}
	
	public void setGambleItem() {
		this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
	}
	
	public ItemStack getGambleItem() {
		return gambleItem;
	}
}
