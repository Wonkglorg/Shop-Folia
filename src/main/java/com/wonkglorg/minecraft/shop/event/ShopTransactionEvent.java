package com.wonkglorg.minecraft.shop.event;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Called when a transaction between 2 parties is being executed
 */
public class ShopTransactionEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private final AbstractShop shop;
	@Getter
	private final OfflinePlayer player;
	private boolean cancelled;
	
	public ShopTransactionEvent(AbstractShop shop, OfflinePlayer player) {
		this.shop = shop;
		this.player = player;
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
