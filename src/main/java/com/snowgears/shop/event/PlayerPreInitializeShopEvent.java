package com.snowgears.shop.event;

import com.snowgears.shop.shop.creation.ImmutableShopCreationProcess;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * Called during shop initialization of a shop with an item, currently initializing item will be null in the shop process context at the time of calling. {@link PlayerPostInitializeShopEvent} if access to the finished shop before registration is required
 * <br><br>====ORDER====<br>
 * Sign is Placed and creation word entered-> {@link PlayerCreateShopEvent} <br>
 * Item is defined for shop -> {@link PlayerPreInitializeShopEvent}<br>
 * Item was defined and shop is ready -> {@link PlayerPostInitializeShopEvent}<br>
 */
public class PlayerPreInitializeShopEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private Player player;
	@Getter
	private ImmutableShopCreationProcess process;
	@Getter
	private ItemStack initialisingItem;
	private boolean cancelled;
	
	public PlayerPreInitializeShopEvent(Player player, ImmutableShopCreationProcess process, ItemStack itemStack) {
		this.player = player;
		this.process = process;
		this.initialisingItem = itemStack;
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
