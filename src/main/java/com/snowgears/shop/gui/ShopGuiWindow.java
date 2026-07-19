package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopGuiWindow {

    public enum GUIArea{
        TOP_BAR, BOTTOM_BAR, BODY;
    }

    protected int INV_SIZE = 54;
    protected String title;
    protected Inventory page;
    protected ShopGuiWindow prevWindow;
    protected UUID player;
    private int currentSlot;
    protected int pageIndex;

    public ShopGuiWindow(UUID player){
        this.player = player;
        page = null;
        prevWindow = null;
        currentSlot = 9;
    }

    public boolean scrollPageNext(){
        ItemStack nextPageIcon = page.getItem(53);

        if(nextPageIcon != null){
            //set the previous scroll page
            page.setItem(45, this.getPrevPageIcon());

            this.pageIndex++;

            initInvContents();

            return true;
        }
        return false;
    }

    public boolean scrollPagePrev(){
        ItemStack nextPageIcon = page.getItem(45);

        if(nextPageIcon != null){
            //set the next scroll page
            page.setItem(53, this.getNextPageIcon());

            this.pageIndex--;

            if(pageIndex == 0){
                page.setItem(45, null);
            }

            initInvContents();

            return true;
        }
        return false;
    }

    public void setPrevWindow(ShopGuiWindow prevWindow){
        this.prevWindow = prevWindow;
        page.setItem(0, this.getBackIcon());
    }

    public boolean hasPrevWindow(){
        if(prevWindow == null)
            return false;
        return true;
    }

    //this method will add to the GUI with taking into account top and bottom menu bars
    protected boolean addIcon(ItemStack icon){

        //this page has been filled with icons
        if(currentSlot == 45)
            return false;

        page.setItem(currentSlot, icon);
        currentSlot++;

        return true;
    }

    public boolean open(){
        Player p = this.getPlayer();
        if(p != null){
            p.openInventory(this.page);
            return true;
        }
        return false;
    }

    //override in subclasses
    protected void initInvContents(){
        currentSlot = 9;
    }

    protected void clearInvBody(){
        for(int i=9; i<INV_SIZE-9; i++){
            page.setItem(i, null);
        }
    }

    protected void makeMenuBarUpper(){/* override in subclasses */}
    protected void makeMenuBarLower(){/* override in subclasses */}

    public Player getPlayer(){
        return Bukkit.getPlayer(player);
    }

    public Inventory getInventory(){
        return this.page;
    }

    public String getTitle(){
        return this.title;
    }

    protected ItemStack getPrevPageIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_LAST_PAGE, null, null);
    }

    protected ItemStack getNextPageIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_NEXT_PAGE, null, null);
    }

    protected ItemStack getBackIcon(){
        return Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.MENUBAR_BACK, null, null);
    }
}
