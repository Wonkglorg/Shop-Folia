package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * Called when a player gambles at a shop
 */
public class PlayerGambleShopEvent extends PlayerExchangeShopEvent{
	
	private static final HandlerList handlers = new HandlerList();
	/**
	 * The gamble item the player will receive
	 */
	@Getter
	private ItemStack gambleItem;
	
	public PlayerGambleShopEvent(Player p, AbstractShop s, ItemStack gambleItem) {
		super(p, s);
		this.gambleItem = gambleItem;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public @NonNull HandlerList getHandlers() {
		return handlers;
	}
}
