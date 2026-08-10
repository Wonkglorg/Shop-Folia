package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceholderContext{
	private AbstractShop shop;
	private Player player;
	private OfflinePlayer offlinePlayer;
	private boolean forSign = false;
	private ItemStack item = null;
	private ItemStack barterItem = null;
	private ShopCreationProcessOld process;
	private OfflineTransactions offlineTransactions;
	private Location location;
	
	// Create empty Placeholder Context
	public PlaceholderContext() {}
	
	public static PlaceholderContext of(AbstractShop shop) {
		return new PlaceholderContext().setShop(shop);
	}
	
	public PlaceholderContext setShop(AbstractShop shop) {
		this.shop = shop;
		return this;
	}
	
	public AbstractShop getShop() {
		return shop;
	}
	
	public PlaceholderContext setPlayer(Player player) {
		this.player = player;
		return this;
	}
	
	public PlaceholderContext setOfflinePlayer(OfflinePlayer offlinePlayer) {
		this.offlinePlayer = offlinePlayer;
		return this;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return offlinePlayer;
	}
	
	public PlaceholderContext setItem(ItemStack item) {
		this.item = item;
		return this;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public PlaceholderContext setBarterItem(ItemStack barterItem) {
		this.barterItem = barterItem;
		return this;
	}
	
	public ItemStack getBarterItem() {return barterItem;}
	
	public PlaceholderContext setProcess(ShopCreationProcessOld process) {
		this.process = process;
		return this;
	}
	
	public ShopCreationProcessOld getProcess() {
		return process;
	}
	
	public PlaceholderContext setOfflineTransactions(OfflineTransactions offlineTransactions) {
		this.offlineTransactions = offlineTransactions;
		return this;
	}
	
	public OfflineTransactions getOfflineTransactions() {
		return offlineTransactions;
	}
	
	public Location getLocation() {return location;}
	
	public PlaceholderContext setLocation(Location location) {
		this.location = location;
		return this;
	}
	
	public PlaceholderContext setForSign(boolean forSign) {
		this.forSign = forSign;
		return this;
	}
	
	public boolean isForSign() {
		return forSign;
	}
}


