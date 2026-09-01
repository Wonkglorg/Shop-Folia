package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.manager.visibility.SignUpdateHandler;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import com.wonkglorg.minecraft.util.Components;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;

public class SignCreationProcess extends ShopCreationProcess{
	
	private static final String AMOUNT = "%amount%";
	private static final String PRICE = "%price%";
	
	/*
	 * Replaced whenever the configuration is reloaded.
	 *
	 * The map is indexed by literal first-line values:
	 *
	 * "shop"      -> layouts beginning with "shop"
	 * "[shop]"    -> layouts beginning with "[shop]"
	 * "sell shop" -> layouts beginning with "sell shop"
	 */
	private static volatile LayoutIndex LAYOUT_INDEX = LayoutIndex.EMPTY;
	
	public SignCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		super(player, sign, container, signDirection);
		isFakeSign = false;
	}
	
	/**
	 * Reads and validates the four sign lines.
	 */
	public boolean readSignLines(List<Component> components) {
		if(components.size() < 4){
			return false;
		}
		
		/*
		 * Convert components to plain text exactly once.
		 */
		String[] lines = new String[4];
		
		for(int i = 0; i < 4; i++){
			lines[i] = Components.toPlainText(components.get(i)).trim();
		}
		
		/*
		 * First-line lookup.
		 *
		 * This avoids checking every configured layout.
		 */
		List<Layout> layouts = LAYOUT_INDEX.byFirstLine.get(lines[0].toLowerCase(Locale.ROOT));
		
		if(layouts == null){
			layouts = LAYOUT_INDEX.dynamicFirstLineLayouts;
		} else if(!LAYOUT_INDEX.dynamicFirstLineLayouts.isEmpty()){
			for(Layout layout : layouts){
				if(layout.matches(lines)){
					return applyMatch(layout, lines);
				}
			}
			
			for(Layout layout : LAYOUT_INDEX.dynamicFirstLineLayouts){
				if(layout.matches(lines)){
					return applyMatch(layout, lines);
				}
			}
			
			return false;
		}
		
		for(Layout layout : layouts){
			if(layout.matches(lines)){
				return applyMatch(layout, lines);
			}
		}
		
		return false;
	}
	
	/**
	 * Applies the values extracted from a matched layout.
	 */
	private boolean applyMatch(Layout layout, String[] lines) {
		this.type = layout.type;
		this.adminShop = layout.admin;
		
		this.amount = parseAmount(lines[layout.amountLine]);
		
		if(this.amount < 1){
			Main.getPlugin().logger().debug("Invalid shop amount: " + lines[layout.amountLine]);
			return false;
		}
		
		Double parsedPrice = parsePrice(lines[layout.priceLine], type);
		
		if(parsedPrice == null){
			Main.getPlugin().logger().debug("Invalid shop price: " + lines[layout.priceLine]);
			return false;
		}
		
		this.price = parsedPrice;
		
		Main.getPlugin().logger().debug("Matched shop creation layout: type=" +
										type +
										", admin=" +
										adminShop +
										", amount=" +
										amount +
										", price=" +
										price);
		
		if(!isAllowedToCreateShop()){
			Main.getPlugin().logger().debug("Player is not allowed to build shop of type " + type);
			return false;
		}
		
		return true;
	}
	
	private int parseAmount(String input) {
		try{
			return Integer.parseInt(UtilMethods.cleanNumberText(input));
		} catch(NumberFormatException ignored){
			return -1;
		}
	}
	
	private Double parsePrice(String input, ShopType shopType) {
		String cleaned = UtilMethods.cleanNumberText(input);
		
		try{
			if(Main.getPlugin().getSettingsConfig().getCurrencyType() == CurrencyType.VAULT){
				
				double multiplier = getMultiplyValue(cleaned);
				
				String[] parts = cleaned.split(" ");
				
				if(parts.length == 0 || parts[0].isEmpty()){
					return null;
				}
				
				double price = Double.parseDouble(parts[0]);
				price *= multiplier;
				
				if(price < 0){
					return null;
				}
				
				if(price == 0 && shopType == ShopType.BARTER){
					return null;
				}
				
				return price;
			}
			
			String[] parts = cleaned.split(" ");
			
			if(parts.length == 0 || parts[0].isEmpty()){
				return null;
			}
			
			long price = Long.parseLong(parts[0]);
			
			if(price < 0){
				return null;
			}
			
			if(price == 0 && shopType == ShopType.BARTER){
				return null;
			}
			
			return (double) price;
			
		} catch(NumberFormatException ignored){
			return null;
		}
	}
	
	private double getMultiplyValue(String text) {
		String priceString = text.replace(" ", "").toLowerCase(Locale.ROOT);
		
		String priceSuffix = priceString.replaceAll("[0-9.]", "");
		
		NavigableMap<Double, String> suffixes = Main.getPlugin().getSettingsConfig().getPriceSuffixes();
		
		for(Map.Entry<Double, String> entry : suffixes.entrySet()){
			if(priceSuffix.equals(entry.getValue().toLowerCase(Locale.ROOT))){
				return entry.getKey();
			}
		}
		
		return 1;
	}
	
	/**
	 * Reloads all creation layouts from the current configuration.
	 *
	 * Call this after the settings/configuration has been reloaded.
	 */
	public static void reloadLayouts() {
		Map<String, List<Layout>> byFirstLine = new HashMap<>();
		List<Layout> dynamicFirstLine = new ArrayList<>();
		
		Map<String, Object> config = Main.getPlugin().getSettingsConfig().getCreationLayout();
		
		for(Map.Entry<String, Object> shopTypeEntry : config.entrySet()){
			
			ShopType shopType;
			
			try{
				shopType = ShopType.valueOf(shopTypeEntry.getKey().toUpperCase(Locale.ROOT));
			} catch(IllegalArgumentException ignored){
				Main.getPlugin().logger().warning("Unknown shop type in creation-layout: " + shopTypeEntry.getKey());
				continue;
			}
			
			if(!(shopTypeEntry.getValue() instanceof Map<?, ?> categoryMap)){
				Main.getPlugin().logger().warning("Creation layout for " + shopType + " must contain normal/admin categories.");
				continue;
			}
			
			parseCategory(shopType, categoryMap, "normal", false, byFirstLine, dynamicFirstLine);
			
			/*
			 * Gamble is always admin, but using the config category
			 * keeps the parser generic.
			 */
			parseCategory(shopType, categoryMap, "admin", true, byFirstLine, dynamicFirstLine);
		}
		
		LAYOUT_INDEX = new LayoutIndex(byFirstLine, dynamicFirstLine);
	}
	
	/**
	 * Parses either the normal or admin section.
	 */
	private static void parseCategory(ShopType shopType,
									  Map<?, ?> categoryMap,
									  String categoryName,
									  boolean admin,
									  Map<String, List<Layout>> byFirstLine,
									  List<Layout> dynamicFirstLine) {
		Object raw = categoryMap.get(categoryName);
		
		if(raw == null){
			return;
		}
		
		if(!(raw instanceof List<?> declarations)){
			Main.getPlugin().logger().warning("Creation layout " + shopType + "." + categoryName + " must be a list.");
			return;
		}
		
		for(Object rawDeclaration : declarations){
			
			if(!(rawDeclaration instanceof Map<?, ?> declaration)){
				Main.getPlugin().logger().warning("Invalid creation layout declaration for " + shopType + "." + categoryName);
				continue;
			}
			
			String[][] lines = new String[4][];
			
			boolean valid = true;
			
			for(int line = 1; line <= 4; line++){
				Object rawLine = declaration.get(line);
				
				if(rawLine == null){
					rawLine = declaration.get(String.valueOf(line));
				}
				
				String[] options = parseOptions(rawLine, shopType, categoryName, line);
				
				if(options == null){
					valid = false;
					break;
				}
				
				lines[line - 1] = options;
			}
			
			if(!valid){
				continue;
			}
			
			Layout layout = new Layout(shopType, admin, lines);
			
			/*
			 * Index the declaration using literal first-line options.
			 */
			boolean hasLiteralFirstLine = false;
			
			for(String option : lines[0]){
				if(!isDynamic(option)){
					hasLiteralFirstLine = true;
					
					byFirstLine.computeIfAbsent(option.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(layout);
				}
			}
			
			/*
			 * If the first line only contains %amount%/%price%,
			 * it cannot use the literal lookup index.
			 */
			if(!hasLiteralFirstLine){
				dynamicFirstLine.add(layout);
			}
		}
	}
	
	/**
	 * Converts one YAML line into its list of alternatives.
	 *
	 * Supports both:
	 *
	 * 1: "shop"
	 *
	 * and:
	 *
	 * 1: [ "shop", "[shop]" ]
	 */
	private static String[] parseOptions(Object raw, ShopType shopType, String category, int line) {
		if(raw instanceof String string){
			return new String[]{string.trim()};
		}
		
		if(raw instanceof List<?> list){
			if(list.isEmpty()){
				Main.getPlugin().getLogger().warning("Empty creation layout options for " + shopType + "." + category + " line " + line);
				return null;
			}
			
			String[] options = new String[list.size()];
			
			for(int i = 0; i < list.size(); i++){
				Object value = list.get(i);
				
				if(!(value instanceof String string)){
					Main.getPlugin().getLogger().warning("Non-string creation layout option for " + shopType + "." + category + " line " + line);
					return null;
				}
				
				options[i] = string.trim();
			}
			
			return options;
		}
		
		Main.getPlugin().getLogger().warning("Invalid creation layout value for " + shopType + "." + category + " line " + line);
		
		return null;
	}
	
	private static boolean isDynamic(String value) {
		return AMOUNT.equalsIgnoreCase(value) || PRICE.equalsIgnoreCase(value);
	}
	
	/**
	 * One compiled creation declaration.
	 */
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
			
			this.amountLine = findLine(lines, AMOUNT);
			this.priceLine = findLine(lines, PRICE);
			
			if(amountLine < 0){
				throw new IllegalArgumentException("Creation layout for " + type + " does not contain %amount%");
			}
			
			if(priceLine < 0){
				throw new IllegalArgumentException("Creation layout for " + type + " does not contain %price%");
			}
		}
		
		private boolean matches(String[] input) {
			for(int line = 0; line < 4; line++){
				
				if(!matchesLine(lines[line], input[line])){
					return false;
				}
			}
			
			return true;
		}
		
		private boolean matchesLine(String[] options, String input) {
			for(String option : options){
				
				if(AMOUNT.equalsIgnoreCase(option)){
					if(isAmount(input)){
						return true;
					}
					
					continue;
				}
				
				if(PRICE.equalsIgnoreCase(option)){
					if(isPrice(input)){
						return true;
					}
					
					continue;
				}
				
				if(option.equalsIgnoreCase(input)){
					return true;
				}
			}
			
			return false;
		}
		
		private static int findLine(String[][] lines, String value) {
			for(int line = 0; line < lines.length; line++){
				for(String option : lines[line]){
					if(value.equalsIgnoreCase(option)){
						return line;
					}
				}
			}
			
			return -1;
		}
		
		private static boolean isAmount(String value) {
			try{
				return Integer.parseInt(UtilMethods.cleanNumberText(value)) > 0;
			} catch(NumberFormatException ignored){
				return false;
			}
		}
		
		private static boolean isPrice(String value) {
			try{
				return Double.parseDouble(UtilMethods.cleanNumberText(value)) >= 0;
			} catch(NumberFormatException ignored){
				return false;
			}
		}
	}
	
	/**
	 * Runtime lookup structure.
	 */
	private static final class LayoutIndex{
		
		private static final LayoutIndex EMPTY = new LayoutIndex(Map.of(), List.of());
		
		private final Map<String, List<Layout>> byFirstLine;
		private final List<Layout> dynamicFirstLineLayouts;
		
		private LayoutIndex(Map<String, List<Layout>> byFirstLine, List<Layout> dynamicFirstLineLayouts) {
			this.byFirstLine = Map.copyOf(byFirstLine);
			this.dynamicFirstLineLayouts = List.copyOf(dynamicFirstLineLayouts);
		}
	}
	
	public void updateSignText() {
		Main.getPlugin().getFoliaLib().getScheduler().runAtLocation(sign.getLocation(), _ -> {
			
			if(sign.getBlockData() instanceof WallSign){
				
				List<Component> signLines = SignUpdateHandler.getSignLines(this);
				
				SignSide signSide = sign.getSide(Side.FRONT);
				
				for(int i = 0; i < 4; i++){
					signSide.line(i, signLines.get(i));
				}
				
				sign.update(true);
			}
		});
	}
	
	@Override
	public String toString() {
		return "SignCreationProcess{" +
			   "player=" +
			   player +
			   ", playerIsOperator=" +
			   playerIsOperator +
			   ", playerUUID=" +
			   playerUUID +
			   ", shopId=" +
			   shopId +
			   ", sign=" +
			   sign +
			   ", container=" +
			   container +
			   ", signDirection=" +
			   signDirection +
			   ", type=" +
			   type +
			   ", amount=" +
			   amount +
			   ", price=" +
			   price +
			   ", adminShop=" +
			   adminShop +
			   ", isFakeSign=" +
			   isFakeSign +
			   ", itemStack=" +
			   itemStack +
			   ", secondaryStack=" +
			   secondaryStack +
			   ", finishedInitialisation=" +
			   finishedInitialisation +
			   ", isCancelled=" +
			   isCancelled +
			   '}';
	}
}