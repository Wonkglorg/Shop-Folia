package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.Collection;
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
	
	/**
	 * Called when the shop plugin requests a complete refresh of all shops, this method runs once for every shop that is currently visible to anyone and for each person that can see it
	 *
	 * @param player player on the server that can currently see the shop
	 * @param shop the shop
	 */
	void onShopRefresh(Player player, AbstractShop shop);
	
	/**
	 * Requests a data clear for a player
	 */
	void clearData(UUID playerId);
	
	default Collection<Player> getPlayersSeeingShop(AbstractShop shop) {
		return shop.getSignLocation().getNearbyPlayers(ShopPlugin.getPlugin().getSettingsConfig().getMaxShopProcessingDistanceChunks());
	}
}