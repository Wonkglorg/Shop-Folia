package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.Set;

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
		shop.getDisplay().remove(player);
	}
	
	@Override
	public void onShopRefresh(Player player, AbstractShop shop) {
		shop.getDisplay().remove(player);
		shop.getDisplay().spawn(player);
	}
	
	public void clearDisplaysForPlayer(Player player) {
		Set<AbstractShop> shops = Main.getPlugin().getShopmanager().getVisibilityManager().getVisibleShops(player);
		for(var shop : shops){
			shop.getDisplay().remove(player);
		}
	}
}