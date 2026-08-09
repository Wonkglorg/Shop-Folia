package com.snowgears.shop.service;

import com.snowgears.shop.Shop;
import com.snowgears.shop.db.ShopDatabase;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.manager.player.OfflinePlayerProfile;
import com.snowgears.shop.manager.player.OnlinePlayerProfile;
import com.snowgears.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShopServiceProvider implements ShopService{
	
	private final Shop shop;
	@Getter
	private final List<Consumer<List<AbstractShop>>> shopLoadHooks = new ArrayList<>();
	
	public ShopServiceProvider(Shop shop) {
		this.shop = shop;
	}
	
	@Override
	public ShopDatabase getDatabase() {
		return shop.getDatabase();
	}
	
	@Override
	public void onShopsLoaded(Consumer<List<AbstractShop>> consumer) {
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
