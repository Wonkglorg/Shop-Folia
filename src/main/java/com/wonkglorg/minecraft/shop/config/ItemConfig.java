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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemConfig extends Config{
	
	/**
	 * The item used as currency when the currency type is ITEM.
	 */
	@Getter
	private ItemStack currencyItem;
	
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
		
		currencyItem = getItemStack("currency-item", new ItemStack(Material.DIAMOND));
		currencyItem.setAmount(1);
		
		gambleDisplayItem = getItemStack("gamble-display-item", new ItemStack(Material.DIAMOND));
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
		set("currency-item", this.currencyItem);
		shopDatabase().logCurrencyChange(CurrencyType.ITEM, this.currencyItem);
		silentSave();
	}
	
	public void setGambleDisplayItem(ItemStack gambleDisplayItem) {
		this.gambleDisplayItem = gambleDisplayItem.clone();
		set("gamble-display-item", this.gambleDisplayItem);
		silentSave();
	}
	
	/**
	 * Represents a configured custom item.
	 *
	 * @param id configured ID
	 * @param namespace external namespace, or null for a serialized Bukkit item
	 * @param itemId external item ID, or null for a serialized Bukkit item
	 * @param item resolved ItemStack
	 */
	public record CustomItem(String id, String namespace, String itemId, ItemStack item){}
}