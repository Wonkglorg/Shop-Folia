package com.snowgears.shop.gui;

public class ListSearchResultsWindow extends ShopGuiWindow{
/*
    private ItemStack searchItem;

    public ListSearchResultsWindow(UUID player, ItemStack searchItem){
        super(player);

        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.LIST_SEARCH_RESULTS);

        this.page = Bukkit.createInventory(null, INV_SIZE, this.title);
        this.searchItem = searchItem;
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        this.clearInvBody();

        makeMenuBarUpper();
        makeMenuBarLower();

        List<AbstractShop> shops = Shop.getPlugin().getShopHandler().getShopsByItem(this.searchItem);
        Collections.sort(shops, new ComparatorShopType());

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
    
 */
}

