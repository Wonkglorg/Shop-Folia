package com.wonkglorg.minecraft.shop.item;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.config.types.LangConfig;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.getPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import static com.wonkglorg.minecraft.util.roman.ConverterRoman.toRoman;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import static org.bukkit.inventory.ItemRarity.COMMON;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import static org.bukkit.potion.PotionType.LONG_TURTLE_MASTER;
import static org.bukkit.potion.PotionType.STRONG_TURTLE_MASTER;
import static org.bukkit.potion.PotionType.TURTLE_MASTER;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Formatting definition for custom items, this should be the only way used to evaluate an items name
 */
public enum ItemNameFormat{
	ITEM_NAME(ItemNameFormat::isValidItemName, (_, _, i) -> i.getItemMeta().itemName()),
	DISPLAY_NAME(ItemNameFormat::isValidDisplayName, (_, _, i) -> i.getItemMeta().displayName()),
	CUSTOM_NAME(ItemNameFormat::isValidCustomName, (_, _, i) -> i.getItemMeta().customName()),
	MATERIAL_NAME((_, _) -> true, ItemNameFormat::fromItemData);
	
	private final BiPredicate<FormatUse, ItemMeta> isValid;
	private final TriFunction<FormatUse, Locale, ItemStack, Component> toName;
	
	ItemNameFormat(BiPredicate<FormatUse, ItemMeta> isValid, TriFunction<FormatUse, Locale, ItemStack, Component> toName) {
		this.isValid = isValid;
		this.toName = toName;
	}
	
	/**
	 * Checks if the given formatting can be used on this itemMeta
	 */
	public boolean canBeUsedOn(FormatUse use, @NotNull ItemMeta meta) {
		return isValid.test(use, meta);
	}
	
	/**
	 * Evaluates the name for the given itemstack, check if the item is valid to be evaluated using {@link #canBeUsedOn}
	 *
	 * @param use what format this itemName should be used for
	 * @param locale the locale of the person getting the information
	 * @param itemStack the stack to extract the name from
	 * @return the returned name
	 */
	public @NotNull Component itemName(@NotNull FormatUse use, @NotNull Locale locale, @Nullable ItemStack itemStack) {
		Component component = toName.apply(use, locale, itemStack);
		if(ShopPlugin.getPlugin().getSettingsConfig().getItemNameFormatSettings().shouldBeStripped(use, this)){
			return Component.text(PlainTextComponentSerializer.plainText().serialize(component));
		}
		return component;
	}
	
	/**
	 * returns the matching formatter to use for the given item as defined in {@link SettingsConfig#getItemNameFormatSettings()}
	 *
	 * @return the formatter or the default {@link ItemNameFormat#MATERIAL_NAME} formatter
	 */
	public static Component itemName(ItemStack itemStack, FormatUse use) {
		return ShopPlugin.getPlugin().getSettingsConfig().getItemNameFormatSettings().getMatchingFormatter(itemStack, use).itemName(use,
				Locale.ENGLISH,
				itemStack);
	}
	
	private static boolean isValidItemName(FormatUse use, ItemMeta meta) {
		if(meta == null){
			return false;
		}
		if(meta.hasItemName()){
			if(getPlugin().getSettingsConfig().getItemNameFormatSettings().getIgnorePlayerNamedFormatting(use)){
				Component component = meta.itemName();
				//translatable is what every default item has
				return !(component instanceof TranslatableComponent);
			}
			
			return true;
		}
		return false;
	}
	
	private static boolean isValidCustomName(FormatUse use, ItemMeta meta) {
		if(meta == null){
			return false;
		}
		if(!meta.hasCustomName()){
			return false;
		}
		return isValidComponentName(use, meta.customName());
	}
	
	private static boolean isValidDisplayName(FormatUse use, ItemMeta meta) {
		if(meta == null){
			return false;
		}
		if(!meta.hasDisplayName()){
			return false;
		}
		return isValidComponentName(use, meta.displayName());
	}
	
	private static boolean isValidComponentName(FormatUse use, Component component) {
		if(getPlugin().getSettingsConfig().getItemNameFormatSettings().getIgnorePlayerNamedFormatting(use)){
			return !isLikelyPlayerRenamed(component);
		}
		
		return true;
	}
	
	/**
	 * best guess if the given component is a player named one or not.
	 */
	private static boolean isLikelyPlayerRenamed(Component component) {
		if(component == null){
			return false;
		}
		if(component instanceof TextComponent textComponent){
			return textComponent.children().isEmpty() && textComponent.color() == null;
		}
		return false;
	}
	
	/**
	 * Evaluates the name based on the given items data
	 */
	private static Component fromItemData(FormatUse formatUse, Locale locale, ItemStack item) {
		if(item == null){
			return Component.text("");
		}
		
		ItemRarity rarity = getRarity(item);
		LangManager lang = langManager();
		Optional<LangConfig> configOptional = lang.getAnyValidLangConfig(locale);
		if(configOptional.isEmpty()){
			//no config present at all fallback to normal material name
			return formatMaterialName(item.getType()).color(rarity.color());
		}
		var config = configOptional.get();
		String materialString = item.getType().toString().toLowerCase();
		
		LangRequest request = getRequest(formatUse, materialString, config);
		
		if(request == null){
			return formatMaterialName(item.getType()).color(rarity.color());
		}
		
		applyMetaPlaceholders(item, rarity, request);
		return request.toSingleComponent();
	}
	
	private static ItemRarity getRarity(ItemStack item) {
		if(item.hasData(DataComponentTypes.RARITY)){
			ItemRarity foundRarity = item.getData(DataComponentTypes.RARITY);
			if(foundRarity != null){
				return foundRarity;
			}
		}
		return COMMON;
	}
	
	/**
	 * Finds the right request in the lang file for the given format and material
	 *
	 * @return the request or null
	 */
	private static LangRequest getRequest(FormatUse formatUse, String materialString, LangConfig config) {
		switch(formatUse) {
			//fall through sign case if content is available in the override use it otherwise fall back on the text one
			case SIGN:
				String path = "item.name-format-sign-override.material." + materialString;
				if(config.contains(path)){
					return langManager().request(path);
				}
				path = "item.name-format-sign-override.base";
				if(config.contains(path)){
					return langManager().request(path);
				}
			case TEXT:
				path = "item.name-format.material." + materialString;
				if(config.contains(path)){
					return langManager().request(path);
				}
				path = "item.name-format.base";
				if(config.contains(path)){
					return langManager().request(path);
				}
				break;
		}
		return null;
	}
	
	/**
	 * Applies placeholders based on the itemMeta used to the given lang request
	 */
	public static void applyMetaPlaceholders(ItemStack itemStack, ItemRarity rarity, LangRequest request) {
		request.replace("%item-rarity%", rarity);
		if(itemStack == null){
			return;
		}
		request.replace("%material-name%", formatMaterialName(itemStack.getType()).color(rarity.color()));
		ItemMeta baseMeta = itemStack.getItemMeta();
		if(baseMeta == null){
			return;
		}
		switch(baseMeta) {
			case SkullMeta meta -> {
				if(meta.hasOwner()){
					request.replace("%player-name%", meta.getOwningPlayer().getName());
				}
			}
			case PotionMeta meta -> {
				List<PotionEffect> allEffects = meta.getAllEffects();
				request.replace("%duration%", "");
				request.replace("%duration-number%", 0);
				request.replace("%amplifier%", "");
				request.replace("%amplifier-number%", 0);
				PotionType basePotionType = meta.getBasePotionType();
				if(basePotionType != null){
					//replace strong and long so we are left with the base name.
					request.replace("%type%",
							capitalizeAndReplaceUnderscores(basePotionType.getKey()
																		  .getKey()
																		  .toLowerCase()
																		  .replace("strong_", "")
																		  .replace("long_", "")));
				}
				
				if(allEffects.isEmpty()){
					return;
				}
				
				PotionEffect effect = allEffects.getFirst();
				if(basePotionType == null){
					request.replace("%type%", capitalizeAndReplaceUnderscores(effect.getType().getKey().getKey()));
				}
				if(effect.getDuration() == -1){
					request.replace("%duration%", "99:99");
					request.replace("%duration-number%", 9999);
				} else {
					request.replace("%duration-number%", effect.getDuration());
					request.replace("%duration%", formatTickTime(effect.getDuration()));
				}
				
				//show II as the strength for turtle master as it has 2 effects
				if(basePotionType == STRONG_TURTLE_MASTER){
					request.replace("%amplifier%", toRoman(2));
					request.replace("%amplifier-number%", 2);
					return;
				}
				
				if(basePotionType == LONG_TURTLE_MASTER || basePotionType == TURTLE_MASTER){
					//do not get amplifier for turtle master always use 0 unless it's a strong one then use 2
					return;
				}
				
				if(effect.getAmplifier() > 0){
					//add 1 because amplifier 0 is potion effect level 1
					request.replace("%amplifier%", toRoman(effect.getAmplifier() + 1));
					request.replace("%amplifier-number%", effect.getAmplifier());
				}
			}
			
			case FireworkMeta meta -> {
				if(meta.hasPower()){
					request.replace("%duration%", meta.getPower());
				} else {
					//if no power is present the actual fly time is 1
					request.replace("%duration%", 1);
				}
				//todo add effects here?
			}
			
			case FireworkEffectMeta meta -> {
				//todo add effects here?
			}
			
			case MusicInstrumentMeta meta -> {
				MusicInstrument instrument = meta.getInstrument();
				if(instrument == null){
					request.replace("%instrument%", "unknown");
					return;
				}
				NamespacedKey key = RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT).getKey(instrument);
				if(key == null){
					request.replace("%instrument%", "unknown");
					return;
				}
				
				request.replace("%instrument%", capitalizeAndReplaceUnderscores(key.getKey()));
			}
			
			case EnchantmentStorageMeta meta -> {
				//only append the first enchantment otherwise too long for any text box / sign
				for(Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()){
					request.replace("%enchantment%", capitalizeAndReplaceUnderscores(entry.getKey().getKey().getKey()));
					request.replace("%enchantment-level%", toRoman(entry.getValue()));
					return;
				}
			}
			
			case OminousBottleMeta meta -> {
				if(meta.hasAmplifier()){
					request.replace("%amplifier%", toRoman(meta.getAmplifier()));
				} else {
					request.replace("%amplifier%", "I");
				}
			}
			
			default -> {
			}
		}
	}
	
	public static Component formatMaterialName(Material material) {
		if(ShopPlugin.getPlugin().getSettingsConfig().isUseLocalizedMaterials()){
			return Component.translatable(material.translationKey());
		}
		return Component.text(capitalizeAndReplaceUnderscores(material.name().toLowerCase()));
	}
	
	public static HoverEvent<ShowItem> getItemHover(ItemStack item) {
		if(item == null){
			return null;
		}
		return Bukkit.getItemFactory().asHoverEvent(item, hover -> hover);
	}
	
	private static String formatTickTime(int ticks) {
		int totalSeconds = ticks / 20;
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if(hours > 0){
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}
	
	public static String capitalizeAndReplaceUnderscores(String input) {
		String[] parts = input.split("_");
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
		return sb.toString().trim();
	}
	
	public static String toPlainTextMaterial(ItemStack stack) {
		if(stack == null){
			return Material.AIR.toString();
		}
		return stack.getType().toString();
	}
	
	public enum FormatUse{
		SIGN,
		TEXT;
	}
}