package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.Main;
import static com.wonkglorg.minecraft.util.Components.toPlainText;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import static net.kyori.adventure.text.event.HoverEvent.showItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import static org.bukkit.inventory.ItemRarity.COMMON;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemNameUtil{
	
	public ItemNameUtil() { /* utility class */ }
	
	public static String getNameAsPlainText(ItemStack item) {
		return toPlainText(getName(item));
	}
	
	public static Component formatMaterialName(Material material) {
		if(Main.getPlugin().getSettingsConfig().isUseLocalizedMaterials()){
			return Component.translatable(material.translationKey());
		}
		
		String[] parts = material.name().toLowerCase().split("_");
		StringBuilder sb = new StringBuilder();
		for(String part : parts){
			if(part.isEmpty()){
				continue;
			}
			sb.append(Character.toUpperCase(part.charAt(0)));
			if(part.length() > 1){
				sb.append(part.substring(1));
			}
			sb.append(" ");
		}
		return Component.text(sb.toString().trim());
	}
	
	public static Component getName(ItemStack item) {
		if(item == null){
			return Component.text("");
		}
		ItemRarity rarity = COMMON;
		if(item.hasData(DataComponentTypes.RARITY)){
			rarity = item.getData(DataComponentTypes.RARITY);
			assert rarity != null;
		}
		
		ItemMeta meta = item.getItemMeta();
		if(meta == null){
			return formatMaterialName(item.getType()).color(rarity.color());
		}
		
		if(meta.hasCustomName()){
			Component component = meta.customName();
			assert component != null;
			if(component.color() == null){
				return component.color(rarity.color());
			}
			return component;
		}
		
		// Add custom formatting for player heads
		if(meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null){
			return Component.text(skullMeta.getOwningPlayer().getName() + "'s Head").color(rarity.color());
		}
		// Add custom potion formatting
		if(item.getItemMeta() instanceof PotionMeta potionMeta && potionMeta.getBasePotionType() != null){
			String formattedName = UtilMethods.capitalize(item.getType().name().replace("_", " ").toLowerCase());
			formattedName += " of ";
			formattedName += UtilMethods.capitalize(potionMeta.getBasePotionType().toString().replace("_", " ").toLowerCase());
			return Component.text(formattedName).color(rarity.color());
		}
		
		// Fallback to the material name
		return formatMaterialName(item.getType()).color(rarity.color());
	}
	
	public static HoverEvent<ShowItem> getItemHover(ItemStack item) {
		if(item == null){
			return null;
		}
		var dataComponents = ItemNBTUtils.getNMSItemStackDataComponents(item);
		return showItem(item.getType().getKey(), 1, dataComponents);
	}
	
	public static Component getEnchantmentTranslatable(Enchantment enchantment) {
		return enchantment.description();
	}
}
