package com.snowgears.shop.util;

import static com.wonkglorg.minecraft.util.Components.toComponent;
import static com.wonkglorg.minecraft.util.Components.toPlainText;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemNameUtil{
	
	public ItemNameUtil() { /* utility class */ }
	
	public static String getNameAsPlainText(ItemStack item) {
		return toPlainText(getName(item));
	}
	
	public static Component getName(ItemStack item) {
		if(item == null){
			return Component.text("");
		}
		ItemMeta meta = item.getItemMeta();
		if(meta == null){
			return getNameTranslatable(item.getType());
		}
		
		if(meta.hasCustomName()){
			return meta.customName();
		}
		
		// Add custom formatting for player heads
		if(meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null){
			return Component.text(skullMeta.getOwningPlayer().getName() + "'s Head");
		}
		
		// Add support for displaying smithing template types
		String itemType = item.getType().name();
		
		if(itemType.endsWith("_SMITHING_TEMPLATE")){
			String templateType = itemType.replace("_SMITHING_TEMPLATE", "");
			// Extract the template pattern name (e.g., "EYE" from "EYE_ARMOR_TRIM_SMITHING_TEMPLATE")
			if(templateType.endsWith("_ARMOR_TRIM")){
				String trimNameColor = "<yellow>";
				// Aqua: "Vex", "Spire", "Eye" and "Ward"
				if(templateType.contains("VEX") || templateType.contains("SPIRE") || templateType.contains("EYE") || templateType.contains("WARD")){
					trimNameColor = "<aqua>";
				} else if(templateType.contains("SILENCE")){
					trimNameColor = "<light-purple>";
				}
				String formattedName = UtilMethods.capitalize(templateType.toLowerCase().replace("_", " "));
				return toComponent(trimNameColor + formattedName);
			} else if(templateType.equals("NETHERITE_UPGRADE")){
				return toComponent("<yellow>Netherite Upgrade Template");
			} else {
				return toComponent("<yellow>" + UtilMethods.capitalize(templateType.toLowerCase().replace("_", " ")));
			}
		}
		
		// Add custom potion formatting
		if(item.getItemMeta() instanceof PotionMeta potionMeta && potionMeta.getBasePotionType() != null){
			String formattedName = UtilMethods.capitalize(item.getType().name().replace("_", " ").toLowerCase());
			formattedName += " of ";
			formattedName += UtilMethods.capitalize(potionMeta.getBasePotionType().toString().replace("_", " ").toLowerCase());
			return Component.text(formattedName);
		}
		
		// Ominous Bottle's are Yellow *shrug*
		if(item.getType() == Material.OMINOUS_BOTTLE){
			return toComponent("<yellow>").append(getNameTranslatable(item.getType()));
		}
		
		// Fallback to the material name
		return getNameTranslatable(item.getType());
	}
	
	public static Component getNameTranslatable(Material material) {
		return Component.translatable(material.translationKey());
	}
	
	public static Component getEnchantmentTranslatable(Enchantment enchantment) {
		return enchantment.description();
	}
}
