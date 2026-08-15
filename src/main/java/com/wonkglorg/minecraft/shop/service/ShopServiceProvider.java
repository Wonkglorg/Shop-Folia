package com.wonkglorg.minecraft.shop.service;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.db.ShopDatabase;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.player.OfflinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ShopServiceProvider implements ShopService{
	
	private final Main main;
	@Getter
	private final List<Consumer<Collection<AbstractShop>>> shopLoadHooks = new ArrayList<>();
	
	public ShopServiceProvider(Main main) {
		this.main = main;
	}
	
	@Override
	public ShopDatabase getDatabase() {
		return main.getShopmanager().getDatabase();
	}
	
	@Override
	public void onShopsLoaded(Consumer<Collection<AbstractShop>> consumer) {
		shopLoadHooks.add(consumer);
	}
	
	@Override
	public OfflinePlayerProfile getProfile(OfflinePlayer player) {
		return PlayerManager.getOfflineProfile(player);
	}
	
	@Override
	public OnlinePlayerProfile getProfile(Player player) {
		return PlayerManager.getOnlineProfile(player);
	}
}
