package com.wonkglorg.minecraft.shop.item;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.config.types.LangConfig;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.util.roman.ConverterRoman;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import static org.bukkit.inventory.ItemRarity.COMMON;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Formatting definition for custom items, this should be the only way used to evaluate an items name
 */
public enum ItemNameFormat{
	DISPLAY_NAME(ItemMeta::hasDisplayName, (format, locale, i) -> i.getItemMeta().displayName()),
	CUSTOM_NAME(ItemMeta::hasCustomName, (format, locale, i) -> i.getItemMeta().customName()),
	MATERIAL_NAME(i -> true, ItemNameFormat::fromItemData);
	
	private static final Registry<MusicInstrument> musicInstrumentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT);
	
	private final Predicate<ItemMeta> isValid;
	private final TriFunction<FormatUse, Locale, ItemStack, Component> toName;
	
	ItemNameFormat(Predicate<ItemMeta> isValid, TriFunction<FormatUse, Locale, ItemStack, Component> toName) {
		this.isValid = isValid;
		this.toName = toName;
	}
	
	/**
	 * Checks if the given formatting can be used on this itemMeta
	 */
	public boolean canBeUsedOn(@NotNull ItemMeta meta) {
		return isValid.test(meta);
	}
	
	public boolean canBeUsedOn(@NotNull ItemStack itemStack) {
		ItemMeta meta = itemStack.getItemMeta();
		//material name is true regardless
		if(meta == null && this != MATERIAL_NAME){
			return false;
		}
		return isValid.test(meta);
	}
	
	/**
	 * Evaluates the name for the given itemstack, check if the item is valid to be evaluated using {@link #canBeUsedOn}
	 *
	 * @param formatUse what format this itemName should be used for
	 * @param locale the locale of the person getting the information
	 * @param itemStack the stack to extract the name from
	 * @return the returned name
	 */
	public @NotNull Component itemName(@NotNull FormatUse formatUse, @NotNull Locale locale, @NotNull ItemStack itemStack) {
		return toName.apply(formatUse, locale, itemStack);
	}
	
	/**
	 * Evaluates the name based on the given items data
	 */
	private static Component fromItemData(FormatUse formatUse, Locale locale, ItemStack item) {
		if(item == null){
			return Component.text("");
		}
		
		ItemRarity rarity = COMMON;
		if(item.hasData(DataComponentTypes.RARITY)){
			ItemRarity foundRarity = item.getData(DataComponentTypes.RARITY);
			if(foundRarity != null){
				rarity = foundRarity;
			}
		}
		
		LangManager lang = ShopPlugin.langManager();
		Optional<LangConfig> configOptional = lang.getAnyValidLangConfig(locale);
		if(configOptional.isEmpty()){
			//no config present at all fallback to normal material name
			return formatMaterialName(item.getType()).color(rarity.color());
		}
		var config = configOptional.get();
		LangRequest request = null;
		String materialString = item.getType().toString().toLowerCase();
		
		switch(formatUse) {
			//fall through sign case if content is available in the override use it otherwise fall back on the text one
			case SIGN:
				String path = "item.name-format-sign-override." + materialString;
				if(config.contains(path)){
					request = lang.request(path);
					break;
				}
				path = "item.name-format-sign-override.base";
				if(config.contains(path)){
					request = lang.request(path);
					break;
				}
			case TEXT:
				path = "item.name-format." + materialString;
				if(config.contains(path)){
					request = lang.request(path);
					break;
				}
				path = "item.name-format.base";
				if(config.contains(path)){
					request = lang.request(path);
					break;
				}
				break;
		}
		
		if(request == null){
			return formatMaterialName(item.getType()).color(rarity.color());
		}
		
		ItemMeta meta = item.getItemMeta();
		applyMetaPlaceholders(meta, rarity, request);
		return request.toSingleComponent();
	}
	
	/**
	 * Applies placeholders based on the itemMeta used to the given lang request
	 */
	public static void applyMetaPlaceholders(ItemMeta baseMeta, ItemRarity rarity, LangRequest request) {
		request.replace("%item-rarity%", rarity);
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
				request.replace("%type%", meta.getBasePotionType());
				try{
					//todo add the option to show all effects in the future? not needed for signs or normal preview text so only take the dominant one
					PotionEffect effect = meta.getAllEffects().getFirst();
					
					request.replace("%type%", capitalizeAndReplaceUnderscores(effect.getType().getKey().getKey()));
					request.replace("%duration%", effect.getDuration());
					request.replace("%amplifier%", effect.getAmplifier());
					//fallback if no effect is present
				} catch(RuntimeException e){
					request.replace("%type%", "NONE");
					request.replace("%duration%", 0);
					request.replace("%amplifier%", 0);
				}
			}
			
			case FireworkMeta meta -> {
				request.replace("%duration%", meta.getPower());
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
				NamespacedKey key = musicInstrumentRegistry.getKey(instrument);
				if(key == null){
					request.replace("%instrument%", "unknown");
					return;
				}
				
				request.replace("%instrument%", capitalizeAndReplaceUnderscores(key.getKey()));
			}
			
			case EnchantmentStorageMeta meta -> {
				StringBuilder builder = new StringBuilder();
				int i = 0;
				for(Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()){
					builder.append(capitalizeAndReplaceUnderscores(entry.getKey().getKey().getKey()));
					builder.append(" ");
					builder.append(ConverterRoman.toRoman(entry.getValue()));
					i++;
					if(i != meta.getStoredEnchants().size()){
						builder.append(", ");
					}
					request.replace("%enchantments%", builder.toString());
				}
			}
			
			case OminousBottleMeta meta -> {
				request.replace("%amplifier%", meta.getAmplifier());
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
	
	public enum FormatUse{
		SIGN,
		TEXT;
	}
}