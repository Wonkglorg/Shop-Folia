package com.wonkglorg.minecraft.shop.manager.client;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ShopClientListener{
	
	/**
	 * Called when a shop becomes visible to a player.
	 */
	void onShopEnter(Player player, AbstractShop shop);
	
	/**
	 * Called when a shop is no longer visible to a player.
	 */
	void onShopLeave(Player player, AbstractShop shop);
	
	/**
	 * Requests a data clear for a player, this does not undo packets sent to the client, use {@link #onShopCleanup(Player, AbstractShop)} to cleanup client data for a specific shop
	 */
	void clearData(UUID playerId);
	
	/**
	 * Requests a cleanup for client side data for this shop, this happens as a result of the shop reloading its shop data and in turn should cleanup any client side shop related data
	 *
	 * @param player the player to clean data for
	 * @param shop the shop to cleanup
	 */
	void onShopCleanup(Player player, AbstractShop shop);
}