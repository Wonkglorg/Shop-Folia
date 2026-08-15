package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.shop.CreationWord;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import com.wonkglorg.minecraft.util.Components;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class SignCreationProcess extends ShopCreationProcess{
	
	public SignCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		super(player, sign, container, signDirection);
		isFakeSign = false; //its a sign shop creation a real sign already exists
	}
	
	/**
	 * Read sign lines and populates process,
	 */
	public boolean readSignLines(List<Component> lines) {
		
		String line3 = Components.toPlainText(lines.get(3));
		type = getShopType(line3);
		if(!isAllowedToCreateShop()){
			Main.getPlugin().logger().debug("Player is not allowed to build shop of type " + type);
			return false;
		}
		adminShop = readShopAdmin(line3);
		Main.getPlugin().logger().debug("Is Admin shop: " + adminShop);
		
		Integer amountRead = readAmount(lines.get(1));
		if(amountRead == null){
			Main.getPlugin().logger().debug("Malformed shop line 2");
			lang.request("interaction_issue.createLine2").sendToAudience(player);
			lang.request("interaction_issue.createCancel").sendToAudience(player);
			return false;
		} else {
			amount = amountRead;
		}
		
		if(!readPrice(Components.toPlainText(lines.get(2)), type)){
			Main.getPlugin().logger().debug("Malformed shop line 3");
			lang.request("interaction_issue.createLine3").sendToAudience(player);
			return false;
		}
		
		if(type == null){
			type = ShopType.SELL;
			Main.getPlugin().logger().debug("No Type specified defaulting to " + type);
		} else {
			Main.getPlugin().logger().debug("Shop type: " + type);
		}
		
		finishedInitialisation = true;
		return true;
	}
	
	private Integer readAmount(Component component) {
		try{
			String line2 = UtilMethods.cleanNumberText(Components.toPlainText(component));
			amount = Integer.parseInt(line2);
			if(amount < 1){
				Main.getPlugin().logger().debug("Amount can't be 0");
				return null;
			}
			Main.getPlugin().logger().debug("Amount:" + amount);
			return amount;
		} catch(NumberFormatException _){
			Main.getPlugin().logger().debug("Not a valid integer");
			return null;
		}
	}
	
	private boolean readShopAdmin(String input) {
		return input.toLowerCase().contains(Main.getPlugin().getSettingsConfig().getCreationWord(CreationWord.ADMIN));
	}
	
	/**
	 * Reads in the price values
	 */
	public boolean readPrice(String input, ShopType shopType) {
		double price = 0;
		double priceCombo = 0;
		if(Main.getPlugin().getSettingsConfig().getCurrencyType() == CurrencyType.VAULT){
			Main.getPlugin().logger().debug("Reading Vault currency");
			try{
				double multiplyValue = getMultiplyValue(input);
				Main.getPlugin().logger().debug("Multiplier: " + multiplyValue);
				String line3 = UtilMethods.cleanNumberText(input);
				
				String[] multiplePrices = line3.split(" ");
				if(multiplePrices.length > 1){
					if(multiplePrices[0].contains(".")){
						price = Double.parseDouble(multiplePrices[0]);
					} else {
						price = Long.parseLong(multiplePrices[0]);
					}
					
					if(multiplePrices[1].contains(".")){
						priceCombo = Double.parseDouble(multiplePrices[1]);
					} else {
						priceCombo = Long.parseLong(multiplePrices[1]);
					}
				} else {
					if(line3.contains(".")){
						price = Double.parseDouble(line3);
					} else {
						price = Long.parseLong(line3);
					}
				}
				
				price *= multiplyValue;
				priceCombo *= multiplyValue;
				Main.getPlugin().logger().debug("Price: " + price);
				Main.getPlugin().logger().debug("Price Combo: " + priceCombo);
				
			} catch(NumberFormatException _){
				return false;
			}
		} else {
			Main.getPlugin().logger().debug("Reading non fractional currency ITEM OR EXP");
			try{
				String line3 = UtilMethods.cleanNumberText(input);
				
				String[] multiplePrices = line3.split(" ");
				if(multiplePrices.length > 1){
					price = Long.parseLong(multiplePrices[0]);
					priceCombo = Long.parseLong(multiplePrices[1]);
					Main.getPlugin().logger().debug("Price: " + price);
					Main.getPlugin().logger().debug("Price Combo: " + priceCombo);
				} else {
					price = Long.parseLong(line3);
					Main.getPlugin().logger().debug("Price: " + price);
				}
			} catch(NumberFormatException _){
				Main.getPlugin().logger().debug("Malformed Price Number");
				return false;
			}
		}
		//only allow price to be zero if the type is selling
		if(price < 0 || (price == 0 && shopType == ShopType.BARTER)){
			return false;
		}
		super.price = price;
		super.priceCombo = priceCombo;
		return true;
	}
	
	private ShopType getShopType(String input) {
		ShopType type = null;
		SettingsConfig config = Main.getPlugin().getSettingsConfig();
		input = input.toLowerCase();
		if(input.contains(config.getCreationWord(CreationWord.BUY))){
			type = ShopType.BUY;
		} else if(input.contains(config.getCreationWord(CreationWord.BARTER))){
			type = ShopType.BARTER;
		} else if(input.contains(config.getCreationWord(CreationWord.GAMBLE))){
			type = ShopType.GAMBLE;
		} else if(input.contains(config.getCreationWord(CreationWord.COMBO))){
			type = ShopType.COMBO;
		} else if(input.contains(config.getCreationWord(CreationWord.SELL))){
			type = ShopType.SELL;
		}
		return type;
	}
	
	//this takes a dirty (pre-cleaned) string and finds how much to multiply the final by
	//this utility allows the input of numbers like 1.2k (1200)
	private double getMultiplyValue(String text) {
		// Remove color formatting, whitespace, and make sure the string is lowercase for matching our suffixes below
		String priceString = text.replaceAll("\\s", "").toLowerCase();
		// Get just the suffix from the price string, remove all numbers and decimals
		String priceSuffix = priceString.replaceAll("[0-9.]", "");
		
		// Load the suffixes from the config values
		NavigableMap<Double, String> configPriceSuffixes = Main.getPlugin().getSettingsConfig().getPriceSuffixes();
		
		// Search for a suffix match
		for(Map.Entry<Double, String> entry : configPriceSuffixes.entrySet()){
			Double configPriceValue = entry.getKey();
			String configSuffix = entry.getValue().toLowerCase();
			
			if(priceSuffix.equals(configSuffix)){
				// Return the value for the suffix from the config
				return configPriceValue;
			}
		}
		
		// No match so our multiplier is just 1
		return 1;
	}
	
}
