package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TextDisplay extends AbstractDisplay{
	protected TextDisplay(AbstractShop shop) {
		super(shop, DisplayType.TEXT);
	}
	
	@Override
	public List<Integer> spawn(Player player) {
		//todo add text display functionality
		return Collections.emptyList();
	}
}
