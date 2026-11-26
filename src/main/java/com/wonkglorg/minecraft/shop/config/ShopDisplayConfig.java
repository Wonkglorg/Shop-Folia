package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

public class ShopDisplayConfig extends Config{
	
	/**
	 * The default display new shops spawn with
	 */
	@Getter
	private DisplayType displayTypeDefault;
	
	/**
	 * The cycling order of displays
	 */
	@Getter
	private DisplayType[] displayCycle;
	
	/**
	 * If the itemframe should glow on shop displays
	 */
	@Getter
	private boolean displayGlowingItemFrame;
	
	public ShopDisplayConfig() {
		super(ShopPlugin.getPlugin(), Path.of("shop-display-config.yml"));
		reload();
	}
	
	public void reload() {
		displayTypeDefault = DisplayType.fromValue(getString("display.type"));
		List<String> cycle = getStringList("display.cycle");
		if(cycle.isEmpty()){
			for(DisplayType dt : DisplayType.values()){
				cycle.add(dt.name());
			}
		}
		
		displayCycle = new DisplayType[cycle.size()];
		for(int i = 0; i < cycle.size(); i++){
			displayCycle[i] = DisplayType.fromValue(cycle.get(i));
		}
		
		displayGlowingItemFrame = getBoolean("display.glowing-item-frame");
	}
}
