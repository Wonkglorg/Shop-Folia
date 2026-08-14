package com.wonkglorg.minecraft.shop.shop.display;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class CreationTextDisplay extends AbstractDisplay{
	protected CreationTextDisplay(Location shopSignLocation) {
		super(shopSignLocation, DisplayType);
	}
	
	@Override
	public void spawn(@NonNull Player player) {
	
	}
}
