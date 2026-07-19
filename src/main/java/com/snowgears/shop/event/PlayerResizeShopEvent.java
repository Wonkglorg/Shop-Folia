package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class PlayerResizeShopEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private Player player;
	@Getter
	private AbstractShop shop;
	@Getter
	private Location location;
	@Getter
	private boolean isExpansion;
	private boolean cancelled;
	
	public PlayerResizeShopEvent(Player p, AbstractShop s, Location location, boolean isExpansion) {
		player = p;
		shop = s;
		this.location = location;
		this.isExpansion = isExpansion;
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
