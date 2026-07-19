package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceholderContext {
    private AbstractShop shop;
    private Player player;
    private OfflinePlayer offlinePlayer;
    private boolean forSign = false;
    private ItemStack item = null;
    private ItemStack barterItem = null;
    private ShopCreationProcess process;
    private OfflineTransactions offlineTransactions;
    private Location location;

    // Create empty Placeholder Context
    public PlaceholderContext() { }

    public void setShop(AbstractShop shop) {
        this.shop = shop;
    }

    public AbstractShop getShop() {
        return shop;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
    public void setOfflinePlayer(OfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
    }

    public Player getPlayer() {
        return player;
    }
    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setBarterItem(ItemStack barterItem) { this.barterItem = barterItem; }

    public ItemStack getBarterItem() { return barterItem; }

    public void setProcess(ShopCreationProcess process) {
        this.process = process;
    }

    public ShopCreationProcess getProcess() {
        return process;
    }

    public void setOfflineTransactions(OfflineTransactions offlineTransactions) {
        this.offlineTransactions = offlineTransactions;
    }

    public OfflineTransactions getOfflineTransactions() {
        return offlineTransactions;
    }

    public Location getLocation() { return location; }

    public void setLocation(Location location) { this.location = location; }

    public void setForSign(boolean forSign) {
        this.forSign = forSign;
    }

    public boolean isForSign() {
        return forSign;
    }
}


