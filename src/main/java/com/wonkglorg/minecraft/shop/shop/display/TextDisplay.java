package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;

public class TextDisplay extends AbstractDisplay{
	protected TextDisplay(AbstractShop shop) {
		super(shop, DisplayType.TEXT);
	}
	
	@Override
	protected void onSpawn(Player player) {
		//todo:mjd add text display functionality
	}
}
