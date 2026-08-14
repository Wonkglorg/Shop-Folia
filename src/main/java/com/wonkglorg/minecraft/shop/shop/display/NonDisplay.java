package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NonDisplay extends AbstractDisplay{
	protected NonDisplay(AbstractShop shop) {
		super(shop, DisplayType.NONE);
	}
	
	@Override
	public void spawn(@NotNull Player player) {
		//nothing
	}
	
	@Override
	public void spawn() {
		//nothing
	}
	
	@Override
	public void remove() {
		//nothing
	}
	
	@Override
	public void remove(@NotNull Player player) {
		//nothing
	}
}
