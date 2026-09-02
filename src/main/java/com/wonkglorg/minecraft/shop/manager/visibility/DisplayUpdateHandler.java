package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles display visibility
 */
public class DisplayUpdateHandler implements ShopVisibilityListener{
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		shop.getDisplay().spawn(player);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		onShopCleanup(player, shop);
	}
	
	@Override
	public void clearData(UUID player) {
		//nothing
	}
	
	@Override
	public void onShopCleanup(Player player, AbstractShop shop) {
		shop.getDisplay().remove(player);
	}
}