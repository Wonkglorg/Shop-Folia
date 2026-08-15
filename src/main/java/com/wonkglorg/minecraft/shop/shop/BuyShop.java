package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.EconomyUtils;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class BuyShop extends AbstractShop{
	
	public BuyShop(UUID shopId, Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing, long creationDate,
	               DisplayType type) {
		super(shopId,signLoc, player, pri, amt, admin, facing,creationDate,type);
		this.creationWord = CreationWord.BUY;
		this.type = ShopType.BUY;
		this.signLines = ShopMessage.getSignLines(this);
	}
	
	@Override
	protected void calculateStock() {
		if(this.isAdmin){
			stock = Integer.MAX_VALUE;
		} else {
			double funds = EconomyUtils.getFunds(this.getOwner(), this.getInventory());
			if(this.getPrice() == 0){
				stock = Integer.MAX_VALUE;
			} else {
				// Check if the player has enough funds to cover a full transaction
				stock = (int) Math.floor(funds / this.getPrice());
				// If the player doesn't have enough funds for a full transaction, see if they can accept a partial one
				if(stock == 0 && Main.getPlugin().getSettingsConfig().isAllowPartialSales()){
					if(this.getItemStack() == null){
						stock = 0;
					} else {
						double pricePer = this.getPricePerItem();
						if(funds >= pricePer){
							stock = 1;
						}
					}
				}
			}
		}
	}
}
