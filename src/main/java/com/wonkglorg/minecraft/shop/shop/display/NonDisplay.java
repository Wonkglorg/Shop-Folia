package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class NonDisplay extends AbstractDisplay{
	protected NonDisplay(AbstractShop shop) {
		super(shop, DisplayType.NONE);
	}
	
	@Override
	public List<Integer> spawn(Player player) {
		//nothing
		return Collections.emptyList();
	}
}
