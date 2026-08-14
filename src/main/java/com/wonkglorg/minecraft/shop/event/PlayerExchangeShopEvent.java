package com.wonkglorg.minecraft.shop.event;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Called when a player exchanges an item at a shop
 */
public class PlayerExchangeShopEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private Player player;
	@Getter
	private AbstractShop shop;
	/**
	 * If fractional purchases are allowed this value specified how much of a fraction in the shop was bought 1 = 100%, 0 = 0%
	 */
	@Getter
	private double fractionExchange = 1; //todo:mjd show how much was bought
	private boolean cancelled;
	
	public PlayerExchangeShopEvent(Player p, AbstractShop s) {
		player = p;
		shop = s;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public ShopType getType() {
		return shop.getType();
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
