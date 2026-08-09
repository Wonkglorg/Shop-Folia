package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Called after the shop has been initialized with the correct items
 */
public class PlayerPostInitializeShopEvent extends Event{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private Player player;
	@Getter
	private AbstractShop shop;
	
	public PlayerPostInitializeShopEvent(Player p, AbstractShop s) {
		player = p;
		shop = s;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public @NonNull HandlerList getHandlers() {
		return handlers;
	}
}
