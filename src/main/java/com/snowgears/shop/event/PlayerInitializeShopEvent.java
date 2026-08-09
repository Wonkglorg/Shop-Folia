package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Called during shop initialisation with items. currently initializing item will be null at the time of calling use {@link PlayerPostInitializeShopEvent} if access to the item being set for the shop is required
 * <br><br>====ORDER====<br>
 * Sign is Placed and creation word entered-> {@link PlayerCreateShopEvent} <br>
 * Item is defined for shop -> {@link PlayerInitializeShopEvent}<br>
 * Item was defined and shop is ready -> {@link PlayerPostInitializeShopEvent}<br>
 */
public class PlayerInitializeShopEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private Player player;
    @Getter
    private AbstractShop shop;
    private boolean cancelled;

    public PlayerInitializeShopEvent(Player p, AbstractShop s) {
        player = p;
        shop = s;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
	
	public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean set) {
        cancelled = set;
    }
}
