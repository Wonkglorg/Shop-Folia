package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface ShopVisibilityListener{
	
	/**
	 * Called when a shop becomes visible to a player.
	 */
	void onShopEnter(Player player, AbstractShop shop);
	
	/**
	 * Called when a shop is no longer visible to a player.
	 */
	void onShopLeave(Player player, AbstractShop shop);
	
	void onShopRefresh(Player player, AbstractShop shop);
	
	default Set<UUID> getPlayersSeeingShop(AbstractShop shop) {
		return Main.getPlugin().getShopmanager().getVisibilityManager().getPlayersSeeingShop(shop);
	}
}