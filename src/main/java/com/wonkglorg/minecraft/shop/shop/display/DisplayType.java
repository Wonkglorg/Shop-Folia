package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.Material;

import java.util.function.Predicate;

public enum DisplayType{
	NONE(s -> true),
	ITEM(s -> s.getAboveContainer().getBlock().getType() == Material.AIR),
	LARGE_ITEM(s -> s.getAboveContainer().getBlock().getType() == Material.AIR),
	GLASS_CASE(s -> s.getAboveContainer().getBlock().getType() == Material.AIR),
	ITEM_FRAME(s -> s.getAboveContainer().getBlock().getType() == Material.AIR ||
	                s.getAboveContainer().getBlock().getRelative(s.getFacing()).getType() == Material.AIR),
	;
	
	/**
	 * If the display can be spawned this calls region code needs to be run on a region thread
	 */
	private final Predicate<AbstractShop> canSpawn;
	
	DisplayType(Predicate<AbstractShop> canSpawn) {
		this.canSpawn = canSpawn;
	}
	
	public boolean canSpawn(AbstractShop shop) {
		return canSpawn.test(shop);
	}
}