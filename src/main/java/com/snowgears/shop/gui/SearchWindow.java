package com.snowgears.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;

import java.util.UUID;

public class SearchWindow extends ShopGuiWindow {

    public SearchWindow(UUID player){
        super(player);
        this.page = Bukkit.createInventory(null, InventoryType.ANVIL, "Search");
        initInvContents();
    }

    @Override
    protected void initInvContents(){/* no-op */}

    @Override
    protected void makeMenuBarUpper(){ /* no-op */}

    @Override
    protected void makeMenuBarLower(){ /* no-op */}
}

