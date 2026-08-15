package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.EconomyUtils;
import com.wonkglorg.minecraft.shop.util.InventoryUtils;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

@Getter
public class ComboShop extends AbstractShop{
	
	private final double priceBuy;
	private final double priceSell;
	
	public ComboShop(UUID shopId, Location signLoc, UUID player, double pri, double priSell, int amt, Boolean admin, BlockFace facing, long creationDate,
	                 DisplayType type) {
		super(shopId,signLoc, player, pri, amt, admin, facing, creationDate,type);
		this.creationWord = CreationWord.COMBO;
		this.type = ShopType.COMBO;
		this.signLines = ShopMessage.getSignLines(this);
		this.priceBuy = pri;
		this.priceSell = priSell;
	}
	
	public String getPriceSellString() {
		return Main.getPlugin().getPriceString(this.priceSell, false);
	}
	
	public String getPriceSellPerItemString() {
		double pricePer = this.getPriceSell() / this.getAmount();
		return Main.getPlugin().getPriceString(pricePer, true);
	}
	
	public String getPriceComboString() {
		return Main.getPlugin().getPriceComboString(this.price, this.priceSell, false);
	}
	
	@Override
	protected void calculateStock() {
		if(this.isAdmin){
			stock = Integer.MAX_VALUE;
		} else {
			double funds = EconomyUtils.getFunds(this.getOwner(), this.getInventory());
			if(this.getPrice() == 0){
				stock = Integer.MAX_VALUE;
				return;
			} else {
				stock = (int) Math.floor(funds / this.getPrice());
				// Check if we should show partial stock
				if(stock == 0 && Main.getPlugin().getSettingsConfig().isAllowPartialSales()){
					if(funds >= this.getPricePerItem()){
						stock = 1;
						return;
					}
				}
			}
			
			// Check if we have stock to sell items still, even if we don't have funds to buy items anymore
			if(stock == 0){
				int itemsToSell = InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
				stock = itemsToSell / this.getAmount();
				// Check if we should show partial stock
				if(stock == 0 && Main.getPlugin().getSettingsConfig().isAllowPartialSales()){
					// Calculate the minimum items required to show as in stock
					int minItemAmountRequired = (int) Math.ceil(1 / this.getPricePerItem());
					int itemsInShop = InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
					
					if(itemsInShop >= minItemAmountRequired){
						stock = 1;
					}
				}
			}
		}
	}
}
