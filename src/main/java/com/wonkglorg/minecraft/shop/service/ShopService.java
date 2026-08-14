package com.wonkglorg.minecraft.shop.service;

import com.wonkglorg.minecraft.shop.db.ShopDatabase;
import com.wonkglorg.minecraft.shop.manager.player.OfflinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.Consumer;

public interface ShopService{
	/**
	 * Gets the shop database
	 */
	ShopDatabase getDatabase();
	
	/**
	 * Hook to execute once all shops have been loaded (also gets called on plugin reload)
	 *
	 * @param consumer the action to run on a copy of available shops
	 */
	void onShopsLoaded(Consumer<Collection<AbstractShop>> consumer);
	
	OfflinePlayerProfile getProfile(OfflinePlayer player);
	
	OnlinePlayerProfile getProfile(Player player);
}