package com.snowgears.shop.event;

import com.snowgears.shop.shop.creation.ImmutableShopCreationProcess;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a shop is created in order
 * <br><br>====ORDER====<br>
 * Sign is Placed and creation word entered-> {@link PlayerCreateShopEvent} <br>
 * Item is defined for shop -> {@link PlayerInitializeShopEvent}<br>
 * Item was defined and shop is ready -> {@link PlayerPostInitializeShopEvent}<br>
 */
public class PlayerCreateShopEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	@Getter
	private final Player player;
	@Getter
	private final ImmutableShopCreationProcess process;
	private boolean cancelled;
	
	public PlayerCreateShopEvent(Player p, ImmutableShopCreationProcess s) {
		player = p;
		process = s;
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
