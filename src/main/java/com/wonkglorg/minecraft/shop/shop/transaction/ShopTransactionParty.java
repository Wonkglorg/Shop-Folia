package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import lombok.Getter;

public abstract class ShopTransactionParty extends TransactionParty{
	@Getter
	private final AbstractShop shop;
	
	protected ShopTransactionParty(AbstractShop shop) {
		super(shop.getOwner(), shop.getInventory());
		this.shop = shop;
	}
	
	public double getAvailableFunds() {
		// If we are an admin, don't check for funds
		if(shop.isAdmin()){
			return Double.MAX_VALUE;
		}
		return getFunds(player, shop.getInventory());
	}
}
