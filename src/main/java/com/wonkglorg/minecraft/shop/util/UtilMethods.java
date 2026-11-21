package com.wonkglorg.minecraft.shop.util;

import org.bukkit.Location;

public class UtilMethods{
	
	public static String getCleanLocation(Location loc, boolean includeWorld) {
		String text = "";
		if(loc == null){
			return text;
		}
		if(includeWorld && loc.getWorld() != null){
			text = loc.getWorld().getName() + " - ";
		}
		text = text + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
		return text;
	}
	
	
	public static String cleanNumberText(String text) {
		String cleaned = "";
		String toClean = text.trim();
		for(int i = 0; i < toClean.length(); i++){
			if(Character.isDigit(toClean.charAt(i))){
				cleaned += toClean.charAt(i);
			} else if(toClean.charAt(i) == '.'){
				cleaned += toClean.charAt(i);
			} else if(toClean.charAt(i) == ' '){
				cleaned += toClean.charAt(i);
			}
		}
		return cleaned;
	}
}
