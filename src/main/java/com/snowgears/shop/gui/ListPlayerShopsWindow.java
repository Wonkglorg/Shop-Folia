package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ComparatorShopItemNameLow;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ListPlayerShopsWindow extends ShopGuiWindow {

    private UUID playerToList;

    public ListPlayerShopsWindow(UUID player, UUID playerToList){
        super(player);

        if(Shop.getPlugin().getShopHandler().getAdminUUID().equals(playerToList)) {
            ItemStack is = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_PLAYER_ADMIN, Shop.getPlugin().getShopHandler().getAdminUUID(), null);
            this.title = is.getItemMeta().getDisplayName();
        }
        else {
            this.title = Bukkit.getOfflinePlayer(playerToList).getName();
            if (this.title == null){
                String shortId = playerToList.toString();
                shortId = shortId.substring(0,3) + "..." + shortId.substring(shortId.length()-3);
                this.title = "Unknown Player (" + shortId + ")";
            }
        }

        this.page = Bukkit.createInventory(null, INV_SIZE, this.title);
        this.playerToList = playerToList;
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        List<AbstractShop> shops = Shop.getPlugin().getShopHandler().getShops(playerToList);
        Collections.sort(shops, new ComparatorShopItemNameLow());

        int startIndex = pageIndex * 36; //36 items is a full page in the inventory
        ItemStack icon;
        boolean added = true;

        for (int i=startIndex; i< shops.size(); i++) {
            AbstractShop shop = shops.get(i);
            icon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.LIST_SHOP, null, shop);

            if(!this.addIcon(icon)){
                added = false;
                break;
            }
        }

        if(added){
            page.setItem(53, null);
        }
        else{
            page.setItem(53, this.getNextPageIcon());
        }
    }
}

