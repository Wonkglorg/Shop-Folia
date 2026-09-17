package com.wonkglorg.minecraft.shop.config;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopDatabase;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.serialization.wrap.PhysicalWrap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemConfig extends Config{
	
	/**
	 * Base currency item
	 */
	@Getter
	private ItemStack currencyItem;
	/**
	 * List of items considered as condensed currency versions (a currency item representing x times more than the base which can also be used for trades, sorted by highest to lowest value
	 */
	@Getter
	private List<CurrencyDenomination> currencyDenominations = new ArrayList<>();
	
	/**
	 * The smallest multiplier a condensed currency is a multiplier of in the {@link #currencyDenominations} list
	 */
	@Getter
	private int smallestCurrencyDenominationMultiplier = 1;
	
	/**
	 * The item displayed for gambling shops.
	 */
	@Getter
	private ItemStack gambleDisplayItem;
	
	/**
	 * Custom item definitions.
	 *
	 * The map key is the configured custom item ID.
	 * -- GETTER --
	 * Returns all custom item definitions.
	 */
	@Getter
	private final Map<String, ItemStack> customItems = new ConcurrentHashMap<>();
	
	private HMCWraps wraps;
	private boolean nexoEnabled;
	
	public ItemConfig() {
		super(ShopPlugin.getPlugin(), Path.of("item-config.yml"));
		reload();
		loadIntegrations();
		loadCustomItems();
	}
	
	public void reload() {
		silentLoad();
		currencyDenominations.clear();
		
		gambleDisplayItem = getItemStack("gamble-display-item", new ItemStack(Material.DIAMOND));
		
		ConfigurationSection section = getConfigurationSection("currency-items");
		if(section == null){
			logger().severe("No currency-items defined in item-config.yml!");
			return;
		}
		var baseCurrency = section.getItemStack("base");
		if(baseCurrency == null){
			logger().severe("No base currency item definition in item-config.yml!");
			return;
		}
		baseCurrency.setAmount(1);
		currencyItem = baseCurrency;
		
		section = section.getConfigurationSection("condensed");
		if(section == null){
			return;
		}
		for(var key : section.getKeys(false)){
			var item = section.getItemStack(key + ".item");
			if(item == null){
				continue;
			}
			int currencyMultiplier = section.getInt(key + ".multiplier");
			if(currencyMultiplier <= 1){
				logger().warning("Currency amount multiplier for " + key + " must be at least x2");
				continue;
			}
			item.setAmount(1);
			currencyDenominations.add(new CurrencyDenomination(item, currencyMultiplier));
		}
		
		if(currencyDenominations.isEmpty()){
			return;
		}
		
		validateDenominations();
		currencyDenominations.sort(Comparator.comparingInt(CurrencyDenomination::value).reversed());
		smallestCurrencyDenominationMultiplier = currencyDenominations.getLast().value;
	}
	
	/**
	 * Reject invalid or ambiguous denomination configurations.
	 */
	private void validateDenominations() {
		for(int i = 0; i < currencyDenominations.size(); i++){
			CurrencyDenomination denomination = currencyDenominations.get(i);
			
			if(denomination == null || denomination.item() == null){
				throw new IllegalArgumentException("Condensed currency and its item must not be null");
			}
			
			if(denomination.value() <= 1){
				throw new IllegalArgumentException("Condensed denomination value must be greater than 1");
			}
			
			if(currencyItem.isSimilar(denomination.item())){
				throw new IllegalArgumentException("Base currency cannot also be a condensed denomination");
			}
			
			for(int j = 0; j < i; j++){
				if(currencyDenominations.get(j).item().isSimilar(denomination.item())){
					throw new IllegalArgumentException("Duplicate condensed currency denomination");
				}
			}
		}
	}
	
	private void loadIntegrations() {
		if(Bukkit.getPluginManager().isPluginEnabled("HMCWraps")){
			wraps = (HMCWraps) Bukkit.getPluginManager().getPlugin("HMCWraps");
		} else {
			wraps = null;
		}
		
		nexoEnabled = Bukkit.getPluginManager().isPluginEnabled("nexo");
	}
	
	private void loadCustomItems() {
		customItems.clear();
		
		ConfigurationSection section = getConfigurationSection("custom-item-definitions");
		
		if(section == null){
			logger().warning("No custom-item-definitions section found skipping...");
			return;
		}
		
		Set<String> keys = section.getKeys(false);
		if(keys.isEmpty()){
			logger().warning("No values defined in custom-item-definitions section skipping...");
			return;
		}
		for(String key : keys){
			String id = key.toLowerCase(Locale.ROOT);
			
			ItemStack itemStack = section.getItemStack(key);
			
			if(itemStack != null){
				customItems.put(id, itemStack);
				continue;
			}
			
			String definition = section.getString(key);
			
			if(definition == null || definition.isBlank()){
				logger().warning("Custom item '" + key + "' has no definition.");
				continue;
			}
			
			ItemStack customItem = loadCustomItem(id, definition);
			
			if(customItem == null){
				logger().severe("Unable to load custom item '" + key + "' from definition '" + definition + "'.");
				continue;
			}
			
			customItems.put(id, customItem);
		}
	}
	
	private ItemStack loadCustomItem(String id, String definition) {
		String[] parts = definition.split(":", 2);
		
		if(parts.length != 2){
			logger().severe("Invalid custom item definition for '" + id + "': " + definition + ". Expected namespace:itemId.");
			return null;
		}
		
		String namespace = parts[0].toLowerCase(Locale.ROOT);
		
		String itemId = parts[1];
		
		return switch(namespace) {
			case "nexo" -> loadNexoItem(id, itemId);
			case "hmcwraps" -> loadHmcWrapsItem(id, itemId);
			default -> {
				logger().severe("Unknown custom item namespace '" + namespace + "' for item '" + id + "'.");
				yield null;
			}
		};
	}
	
	private ItemStack loadNexoItem(String id, String itemId) {
		if(!nexoEnabled){
			logger().severe("Custom item '" + id + "' uses Nexo, but Nexo is not enabled.");
			return null;
		}
		
		ItemBuilder itemBuilder = NexoItems.itemFromId(itemId);
		
		if(itemBuilder == null){
			logger().warning(itemId + " is not a valid Nexo item.");
			return null;
		}
		
		return itemBuilder.build();
	}
	
	private ItemStack loadHmcWrapsItem(String id, String itemId) {
		if(wraps == null){
			logger().severe("Custom item '" + id + "' uses HMCWraps, but HMCWraps is not enabled.");
			return null;
		}
		
		var wrap = wraps.getWrapsLoader().getWraps().get(itemId);
		
		if(wrap == null){
			logger().severe("HMCWraps has no registered wrap with ID '" + itemId + "'.");
			return null;
		}
		
		PhysicalWrap physical = wrap.getPhysical();
		
		if(physical == null){
			logger().severe("HMCWraps wrap '" + itemId + "' has no physical representation.");
			return null;
		}
		
		return physical.toItem(wraps, null);
	}
	
	public void setCurrencyItem(ItemStack currencyItem) {
		this.currencyItem = currencyItem.clone();
		this.currencyItem.setAmount(1);
		set("currency-items.base", this.currencyItem);
		shopDatabase().logCurrencyChange(CurrencyType.ITEM, this.currencyItem);
		silentSave();
	}
	
	public void setGambleDisplayItem(ItemStack gambleDisplayItem) {
		this.gambleDisplayItem = gambleDisplayItem.clone();
		set("gamble-display-item", this.gambleDisplayItem);
		silentSave();
	}
	
	/**
	 * Represents a currency item and it's worth
	 */
	public record CurrencyDenomination(ItemStack item, int value){
		public CurrencyDenomination {
			if(item == null || item.getType().isAir()){
				throw new IllegalArgumentException("Currency item cannot be empty");
			}
			
			if(value <= 0){
				throw new IllegalArgumentException("Currency value must be positive");
			}
			
			item = item.clone();
			item.setAmount(1);
		}
	}
}