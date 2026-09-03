package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SignCreationLayoutParser{
	
	private static final String AMOUNT = "%amount%";
	private static final String PRICE = "%price%";
	private static Map<String, List<Layout>> layouts = Map.of();
	
	private SignCreationLayoutParser() {
	}
	
	/**
	 * Rebuilds the compiled layout index.
	 *
	 * Call this once during startup and again whenever the configuration
	 * containing creation-layout is reloaded.
	 */
	public static void reload(ConfigurationSection section) {
		Map<String, List<Layout>> newLayouts = new HashMap<>();
		
		for(ShopType type : ShopType.values()){
			
			String typePath = type.name().toLowerCase(Locale.ROOT);
			
			parseDeclarations(type, section.getList(typePath + ".normal"), false, newLayouts);
			
			parseDeclarations(type, section.getList(typePath + ".admin"), true, newLayouts);
		}
		
		layouts = freeze(newLayouts);
	}
	
	private static void parseDeclarations(ShopType type, List<?> declarations, boolean admin, Map<String, List<Layout>> result) {
		if(declarations == null){
			return;
		}
		
		for(Object declaration : declarations){
			
			if(!(declaration instanceof Map<?, ?> lines)){
				throw new IllegalArgumentException("Invalid creation layout declaration for " + type + "/" + (admin ? "admin" : "normal"));
			}
			
			Layout layout = parseLayout(type, admin, lines);
			
			for(String firstLine : layout.lines[0]){
				
				if(isDynamic(firstLine)){
					throw new IllegalArgumentException("First line of a creation layout must be static: " + firstLine);
				}
				
				result.computeIfAbsent(normalize(firstLine), _ -> new ArrayList<>()).add(layout);
			}
		}
	}
	
	private static Layout parseLayout(ShopType type, boolean admin, Map<?, ?> lines) {
		String[][] parsedLines = new String[4][];
		
		for(int line = 1; line <= 4; line++){
			Object raw = lines.get(line);
			
			if(raw == null){
				raw = lines.get(String.valueOf(line));
			}
			
			if(raw instanceof String string){
				parsedLines[line - 1] = new String[]{string};
			} else if(raw instanceof List<?> list){
				String[] options = new String[list.size()];
				
				for(int i = 0; i < list.size(); i++){
					Object value = list.get(i);
					
					if(!(value instanceof String string)){
						throw new IllegalArgumentException("Creation layout line " + line + " contains a non-string value");
					}
					
					options[i] = string;
				}
				
				parsedLines[line - 1] = options;
			} else {
				throw new IllegalArgumentException("Missing/invalid creation layout line " + line + " for " + type);
			}
			
			if(parsedLines[line - 1].length == 0){
				throw new IllegalArgumentException("Creation layout line " + line + " cannot be empty");
			}
		}
		
		// Explicitly enforce your invariant.
		for(String option : parsedLines[0]){
			if(isDynamic(option)){
				throw new IllegalArgumentException("Creation layout first line must be static: " + option);
			}
		}
		
		return new Layout(type, admin, parsedLines);
	}
	
	/**
	 * Very cheap lookup used before constructing SignCreationProcess.
	 */
	public static boolean hasFirstLine(String firstLine) {
		return layouts.containsKey(normalize(firstLine));
	}
	
	/**
	 * Matches a sign and extracts all creation data.
	 *
	 * Returns null when the sign does not match.
	 */
	public static CreationMatch match(String[] lines) {
		if(lines == null || lines.length < 4){
			logger().debug("Not enough lines defined!");
			return null;
		}
		
		/*
		 * First-line lookup is the primary fast path.
		 */
		List<Layout> candidates = layouts.get(normalize(lines[0]));
		
		if(candidates == null){
			logger().debug("No possible candidates found for first line lookup!");
			return null;
		}
		
		for(Layout layout : candidates){
			CreationMatch match = layout.match(lines);
			
			if(match != null){
				logger().debug("Found valid match for lines! " + match);
				return match;
			}
		}
		
		return null;
	}
	
	private static boolean isDynamic(String value) {
		return AMOUNT.equalsIgnoreCase(value) || PRICE.equalsIgnoreCase(value);
	}
	
	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
	
	private static Map<String, List<Layout>> freeze(Map<String, List<Layout>> source) {
		Map<String, List<Layout>> result = new ConcurrentHashMap<>(source.size());
		
		for(Map.Entry<String, List<Layout>> entry : source.entrySet()){
			result.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		
		return Map.copyOf(result);
	}
	
	public record CreationMatch(ShopType shopType, boolean admin, int amount, double price){}
	
	private static final class Layout{
		
		private final ShopType type;
		private final boolean admin;
		private final String[][] lines;
		
		private final int amountLine;
		private final int priceLine;
		
		private Layout(ShopType type, boolean admin, String[][] lines) {
			this.type = type;
			this.admin = admin;
			this.lines = lines;
			
			this.amountLine = findToken(lines, AMOUNT);
			this.priceLine = findToken(lines, PRICE);
		}
		
		private CreationMatch match(String[] input) {
			
			int amount = -1;
			double price = -1;
			
			for(int line = 0; line < 4; line++){
				
				String value = input[line];
				
				boolean matched = false;
				
				for(String option : lines[line]){
					
					if(AMOUNT.equalsIgnoreCase(option)){
						int parsed = parseAmount(value);
						
						if(parsed > 0){
							amount = parsed;
							matched = true;
							break;
						}
						
						continue;
					}
					
					if(PRICE.equalsIgnoreCase(option)){
						double parsed = parsePrice(value);
						
						if(parsed >= 0){
							price = parsed;
							matched = true;
							break;
						}
						
						continue;
					}
					
					if(option.equalsIgnoreCase(value)){
						matched = true;
						break;
					}
				}
				
				if(!matched){
					return null;
				}
			}
			
			/*
			 * A valid declaration must provide both values.
			 *
			 * Gamble has no amount token, so its amount is handled
			 * separately by the shop creation code.
			 */
			if(amountLine >= 0 && amount < 1){
				return null;
			}
			
			if(priceLine >= 0 && price < 0){
				return null;
			}
			
			return new CreationMatch(type, admin, amount, price);
		}
		
		private static int findToken(String[][] lines, String token) {
			for(int line = 0; line < lines.length; line++){
				for(String option : lines[line]){
					if(token.equalsIgnoreCase(option)){
						return line;
					}
				}
			}
			
			return -1;
		}
	}
	
	private static int parseAmount(String input) {
		try{
			return Integer.parseInt(UtilMethods.cleanNumberText(input));
		} catch(NumberFormatException ignored){
			return -1;
		}
	}
	
	private static double parsePrice(String input) {
		try{
			String cleaned = UtilMethods.cleanNumberText(input);
			
			if(cleaned.isEmpty()){
				return -1;
			}
			
			String[] parts = cleaned.split(" ");
			
			return Double.parseDouble(parts[0]);
			
		} catch(NumberFormatException ignored){
			return -1;
		}
	}
}