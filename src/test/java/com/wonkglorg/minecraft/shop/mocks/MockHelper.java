package com.wonkglorg.minecraft.shop.mocks;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MockHelper{
	
	public static AbstractShop createShop(ShopType type,
										  Location location,
										  UUID owner,
										  double price,
										  int amount,
										  boolean admin,
										  Inventory inventory,
										  ItemStack mainStack) {
		AbstractShop shop = switch(type) {
			case SELL -> new SellShopMock(owner,location,price, amount, admin, inventory);
			case BUY -> new BuyShopMock(owner, location,price, amount, admin, inventory);
			case BARTER -> new BarterShopMock(owner, location,price, amount, admin, inventory);
			case GAMBLE -> new GambleShopMock(owner, location,price, amount, inventory);
		};
		shop.setItemStack(mainStack);
		shop.updateStock();
		return shop;
	}
	
}
