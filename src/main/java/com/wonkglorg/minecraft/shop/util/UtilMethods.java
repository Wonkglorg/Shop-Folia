package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.Main;
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
	private static final Registry<MusicInstrument> musicInstrumentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.INSTRUMENT);
	
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
	
	public static int getDurabilityPercent(ItemStack item) {
		if(item instanceof Damageable damageable){
			double dur = ((double) (damageable.getMaxDamage() - damageable.getDamage()) / (double) damageable.getMaxDamage());
			return (int) (dur * 100);
		}
		return 100;
	}
	
	public static String formatTickTime(int ticks) {
		// Convert ticks to seconds (20 ticks = 1 second)
		int totalSeconds = ticks / 20;
		
		// Calculate hours, minutes, and seconds
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		
		// Format the time string
		if(hours > 0){
			return " " + String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return " " + String.format("%d:%02d", minutes, seconds);
		}
	}
	
	public static Component getEnchantmentsComponent(ItemStack item) {
		Component message = Component.text("");
		
		if(item.getItemMeta() instanceof EnchantmentStorageMeta || !item.getEnchantments().isEmpty()){
			Map<Enchantment, Integer> enchantsMap;
			if(item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentStorageMeta){
				enchantsMap = enchantmentStorageMeta.getStoredEnchants();
			} else {
				enchantsMap = item.getEnchantments();
			}
			
			if(enchantsMap.isEmpty()){
				return message;
			}
			
			message = message.append(Component.text(" ["));
			int i = 0;
			for(Map.Entry<Enchantment, Integer> entry : enchantsMap.entrySet()){
				message = message.append(ItemNameUtil.getEnchantmentTranslatable(entry.getKey()));
				message = message.append(Component.text(ConverterRoman.toRoman(entry.getValue())));
				i++;
				if(i != enchantsMap.size()){
					message = message.append(Component.text(", "));
				} else {
					message = message.append(Component.text("]"));
				}
			}
		}
		
		if(item.getItemMeta() != null && item.getItemMeta() instanceof ArmorMeta){
			ArmorMeta armorMeta = (ArmorMeta) item.getItemMeta();
			if(armorMeta.getTrim() != null){
				String material = toPlainText(armorMeta.getTrim().getMaterial().description());
				String pattern = toPlainText(armorMeta.getTrim().getPattern().description());
				// Since we want to remove the "Armor Trim" and "Material" from the string, we have to translate it first
				// causing translatable components to not work clientside.
				message = message.append(Component.text(" [" + pattern.replace(" Armor Trim", "")));
				message = message.append(Component.text(" (" + material.replace(" Material", "") + ")]"));
			}
		}
		
		// Add support for displaying music disc information and goat horn sounds
		if(item.getItemMeta() != null){
			String itemType = item.getType().name();
			
			// Add support for displaying music disc information
			if(itemType.startsWith("MUSIC_DISC_")){
				String trackName = itemType.replace("MUSIC_DISC_", "");
				String formattedName = capitalize(trackName.toLowerCase().replace("_", " "));
				message = message.append(Component.text(" [Song: " + formattedName + "]"));
			}
			// Add support for displaying goat horn sounds
			else if(itemType.equals("GOAT_HORN")){
				// Try to get the instrument type from item data if available
				MusicInstrumentMeta instrumentMeta = (MusicInstrumentMeta) item.getItemMeta();
				if(instrumentMeta != null && instrumentMeta.getInstrument() != null){
					
					NamespacedKey key = musicInstrumentRegistry.getKey(instrumentMeta.getInstrument());
					String instrumentKey = "NON";
					if(key != null){
						instrumentKey = key.getKey();
					}
					// Format the instrument key properly (e.g., "ponder_goat_horn" -> "Ponder")
					String soundType = instrumentKey.replace("_goat_horn", "");
					message = message.append(Component.text(" [Sound: " + capitalize(soundType) + "]"));
				} else {
					message = message.append(Component.text(" [Sound: Unknown]"));
				}
			}
			
			// Add support for displaying bee hive/nest information
			else if(itemType.equals("BEE_NEST") || itemType.equals("BEEHIVE")){
				if(item.getItemMeta() instanceof BlockStateMeta blockStateMeta){
					if(blockStateMeta.hasBlockState() && blockStateMeta.getBlockState() instanceof org.bukkit.block.Beehive beehive){
						int honeyLevel = 0;
						int beeCount = 0;
						
						// Get honey level (this is from BlockData)
						var beehiveData = (org.bukkit.block.data.type.Beehive) beehive.getBlockData();
						honeyLevel = beehiveData.getHoneyLevel();
						// Get bee count (this is from the entity storage)
						beeCount = beehive.getEntityCount();
						// Format the message
						if(honeyLevel > 0 || beeCount > 0){
							StringBuilder beeInfo = new StringBuilder(" [");
							if(honeyLevel > 0){
								beeInfo.append("Honey: ").append(honeyLevel).append("/5");
								if(beeCount > 0){
									beeInfo.append(", ");
								}
							}
							if(beeCount > 0){
								beeInfo.append("Bees: ").append(beeCount);
							}
							beeInfo.append("]");
							message = message.append(Component.text(beeInfo.toString()));
						}
					}
				}
			}
		}
		
		// Add Ominous Bottle support (Bad Omen level)
		if(item.getItemMeta() != null && item.getItemMeta() instanceof org.bukkit.inventory.meta.OminousBottleMeta){
			org.bukkit.inventory.meta.OminousBottleMeta ominousMeta = (org.bukkit.inventory.meta.OminousBottleMeta) item.getItemMeta();
			int level = ominousMeta.hasAmplifier() ? ominousMeta.getAmplifier() + 1 : 1; // zero based
			message = message.append(Component.text(" [Bad Omen" + ConverterRoman.toRoman(level) + "]"));
		}
		
		// Add custom potion formatting
		if(item.getItemMeta() != null && item.getItemMeta() instanceof PotionMeta){
			PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
			if(potionMeta.getBasePotionType() != null){
				message = message.append(getPotionEffects(potionMeta.getBasePotionType().getPotionEffects()));
			}
			
			// Check for custom effects
			List<PotionEffect> customEffects = potionMeta.getCustomEffects();
			if(!customEffects.isEmpty()){
				message = message.append(getPotionEffects(customEffects));
			}
		}
		
		// Add detailed firework effect information
		if(item.getItemMeta() != null){
			// Handle Firework Stars
			if(item.getItemMeta() instanceof FireworkEffectMeta fireworkMeta){
				if(fireworkMeta.hasEffect()){
					message = message.append(getFormattedFireworkEffect(fireworkMeta.getEffect(), true));
				}
			}
			// Handle Fireworks
			else if(item.getItemMeta() instanceof FireworkMeta fireworkMeta){
				int power = fireworkMeta.getPower();
				
				// Display duration
				if(power == 0){
					power = 1;
				}
				message = message.append(Component.text(" [Duration " + power + "]"));
				
				// Display effects
				List<FireworkEffect> effects = fireworkMeta.getEffects();
				if(!effects.isEmpty()){
					int effectCount = effects.size();
					if(effectCount <= 2){
						// If there's only one-two effects, show their details
						for(FireworkEffect effect : effects){
							message = message.append(getFormattedFireworkEffect(effect, false));
						}
					} else {
						// If there are multiple effects, just show the count
						message = message.append(Component.text(" [" + effectCount + " Effects]"));
					}
				}
			}
		}
		
		return message;
	}
	
	private static Component getPotionEffects(List<PotionEffect> effects) {
		Component formattedEffects = Component.text("");
		int numEffects = effects.size();
		if(numEffects == 0){
			return formattedEffects;
		}
		formattedEffects = formattedEffects.append(Component.text(" ("));
		for(int i = 0; i < numEffects; i++){
			PotionEffect effect = effects.get(i);
			formattedEffects = formattedEffects.append(Component.translatable(effect.getType().translationKey()));
			
			// Show level for all potions, not just those with amplifier > 0
			// For potions with amplifier 0, we don't add any suffix (it's the base level)
			if(effect.getAmplifier() > 0){
				formattedEffects = formattedEffects.append(Component.text(ConverterRoman.toRoman(effect.getAmplifier() +
																							  1))); // +1 because amplifier is 0-based
			}
			
			// Only add duration for non-instant effects
			// Instant effects like Instant Health and Instant Damage shouldn't show duration
			boolean isInstantEffect = effect.getType().equals(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH) ||
									  effect.getType().equals(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE);
			
			if(effect.getDuration() > 0 && !isInstantEffect){
				formattedEffects = formattedEffects.append(Component.text(formatTickTime(effect.getDuration())));
			}
			
			// if we have more than one effect, add a comma, dont add a comma after the last effect
			if(i < numEffects - 1){
				formattedEffects = formattedEffects.append(Component.text(", "));
			}
		}
		return formattedEffects.append(Component.text(")"));
	}
	
	/**
	 * Formats a firework effect into a readable string
	 *
	 * @param effect The firework effect to format
	 * @param isFireworkStar Whether this is for a firework star (true) or a firework (false)
	 * @return Formatted text component with firework effect information
	 */
	private static Component getFormattedFireworkEffect(FireworkEffect effect, boolean isFireworkStar) {
		Component formattedEffect = Component.text("");
		
		if(effect == null){
			return formattedEffect;
		}
		
		StringBuilder sb = new StringBuilder();
		
		// Start the formatted string
		sb.append(" [");
		
		// Add the shape
		String shapeName = formatFireworkShape(effect.getType());
		sb.append(shapeName);
		
		// Add special effects
		List<String> specialEffects = new ArrayList<>();
		if(effect.hasTrail()){
			specialEffects.add("Trail");
		}
		if(effect.hasFlicker()){
			specialEffects.add("Twinkle");
		}
		
		if(!specialEffects.isEmpty()){
			sb.append(" (");
			sb.append(String.join(", ", specialEffects));
			sb.append(")");
		}
		
		// Add color information if we have it
		List<Color> colors = effect.getColors();
		if(colors != null && !colors.isEmpty()){
			if(colors.size() == 1){
				// If there's just one color, add it directly
				sb.append(" ").append(formatFireworkColor(colors.get(0)));
			} else if(colors.size() <= 3){
				// If there are 2-3 colors, list them
				sb.append(" ");
				for(int i = 0; i < colors.size(); i++){
					sb.append(formatFireworkColor(colors.get(i)));
					if(i < colors.size() - 1){
						sb.append(", ");
					}
				}
			} else {
				// If there are many colors, just show the count
				sb.append(" ").append(colors.size()).append(" Colors");
			}
		}
		
		// Add fade information if available
		List<Color> fadeColors = effect.getFadeColors();
		if(fadeColors != null && !fadeColors.isEmpty()){
			if(fadeColors.size() == 1){
				// If there's just one fade color, add it directly
				sb.append("→").append(formatFireworkColor(fadeColors.get(0)));
			} else if(fadeColors.size() <= 2){
				// If there are 2 fade colors, list them
				sb.append("→");
				for(int i = 0; i < fadeColors.size(); i++){
					sb.append(formatFireworkColor(fadeColors.get(i)));
					if(i < fadeColors.size() - 1){
						sb.append(", ");
					}
				}
			} else {
				// If there are many fade colors, just show the count
				sb.append(" → ").append(fadeColors.size()).append(" Fade Colors");
			}
		}
		
		sb.append("]");
		
		return formattedEffect.append(Component.text(sb.toString()));
	}
	
	/**
	 * Formats a firework shape into a readable string
	 *
	 * @param type The firework effect type
	 * @return Formatted shape name
	 */
	private static String formatFireworkShape(FireworkEffect.Type type) {
		switch(type) {
			case BALL:
				return "Small";
			case BALL_LARGE:
				return "Large";
			case STAR:
				return "Star";
			case BURST:
				return "Burst";
			case CREEPER:
				return "Creeper";
			default:
				return capitalize(type.toString().toLowerCase().replace("_", " "));
		}
	}
	
	/**
	 * Formats a color into a readable string
	 *
	 * @param color The color to format
	 * @return Formatted color name
	 */
	private static String formatFireworkColor(Color color) {
		Main.getPlugin().logger().debug("[formatFireworkColor]     color: " + color.toString());
		
		// Map common RGB values to color names
		if(color.equals(Color.WHITE)){
			return "White";
		}
		if(color.equals(Color.SILVER)){
			return "Silver";
		}
		if(color.equals(Color.GRAY)){
			return "Gray";
		}
		if(color.equals(Color.BLACK)){
			return "Black";
		}
		if(color.equals(Color.RED)){
			return "Red";
		}
		if(color.equals(Color.MAROON)){
			return "Maroon";
		}
		if(color.equals(Color.YELLOW)){
			return "Yellow";
		}
		if(color.equals(Color.OLIVE)){
			return "Olive";
		}
		if(color.equals(Color.LIME)){
			return "Lime";
		}
		if(color.equals(Color.GREEN)){
			return "Green";
		}
		if(color.equals(Color.AQUA)){
			return "Aqua";
		}
		if(color.equals(Color.TEAL)){
			return "Teal";
		}
		if(color.equals(Color.BLUE)){
			return "Blue";
		}
		if(color.equals(Color.NAVY)){
			return "Navy";
		}
		if(color.equals(Color.FUCHSIA)){
			return "Fuchsia";
		}
		if(color.equals(Color.PURPLE)){
			return "Purple";
		}
		if(color.equals(Color.ORANGE)){
			return "Orange";
		}
		
		for(DyeColor dyeColor : DyeColor.values()){
			if(dyeColor.getColor().equals(color)){
				return capitalize(dyeColor.toString().toLowerCase().replace("_", " "));
			}
			if(dyeColor.getFireworkColor().equals(color)){
				return capitalize(dyeColor.toString().toLowerCase().replace("_", " "));
			}
		}
		
		// If no match is found, return a generic "Custom"
		return "Custom";
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
