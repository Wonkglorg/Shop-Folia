package com.wonkglorg.minecraft.shop.util;

import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.util.Components.toPlainText;
import com.wonkglorg.minecraft.util.roman.ConverterRoman;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UtilMethods{
	
	public static String capitalize(String line) {
		String[] spaces = line.split("\\s+");
		String capped = "";
		for(String s : spaces){
			if(s.length() > 1){
				capped = capped + Character.toUpperCase(s.charAt(0)) + s.substring(1) + " ";
			} else {
				capped = capped + s.toUpperCase() + " ";
			}
		}
		return capped.substring(0, capped.length() - 1);
	}
	
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
