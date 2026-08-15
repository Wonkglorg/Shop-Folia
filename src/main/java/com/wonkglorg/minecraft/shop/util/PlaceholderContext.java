package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceholderContext{
	@Getter
	private AbstractShop shop;
	@Getter
	private Player player;
	@Getter
	private OfflinePlayer offlinePlayer;
	@Getter
	private boolean forSign = false;
	@Getter
	private ItemStack item = null;
	@Getter
	private ItemStack barterItem = null;
	@Getter
	private OfflineTransactions offlineTransactions;
	@Getter
	private Location location;
	@Getter
	private ShopCreationProcess process;
	
	// Create empty Placeholder Context
	public PlaceholderContext() {}
	
	public static PlaceholderContext of(AbstractShop shop) {
		return new PlaceholderContext().setShop(shop);
	}
	
	public PlaceholderContext setShop(AbstractShop shop) {
		this.shop = shop;
		return this;
	}
	
	public PlaceholderContext setPlayer(Player player) {
		this.player = player;
		return this;
	}
	
	public PlaceholderContext setOfflinePlayer(OfflinePlayer offlinePlayer) {
		this.offlinePlayer = offlinePlayer;
		return this;
	}
	
	public PlaceholderContext setItem(ItemStack item) {
		this.item = item;
		return this;
	}
	
	public PlaceholderContext setBarterItem(ItemStack barterItem) {
		this.barterItem = barterItem;
		return this;
	}
	
	public PlaceholderContext setOfflineTransactions(OfflineTransactions offlineTransactions) {
		this.offlineTransactions = offlineTransactions;
		return this;
	}
	
	public PlaceholderContext setLocation(Location location) {
		this.location = location;
		return this;
	}
	
	public PlaceholderContext setForSign(boolean forSign) {
		this.forSign = forSign;
		return this;
	}
	
	public PlaceholderContext setProcess(ShopCreationProcess process) {
		this.process = process;
		return this;
	}
}


