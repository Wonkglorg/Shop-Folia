package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import static com.snowgears.shop.shop.ShopState.OK;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GambleShop extends AbstractShop{
	
	private ItemStack gambleItem;
	
	public GambleShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
		super(signLoc, player, pri, amt, admin, facing);
		
		this.isAdmin = true;
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
			setItemStack(Shop.getPlugin().getGambleDisplayItem());
			if(initialDisplayType == null){
				display.setType(Shop.getPlugin().getDisplayType(), false);
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
