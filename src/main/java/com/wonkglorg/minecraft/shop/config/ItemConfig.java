package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.config.types.Config;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemConfig extends Config{
	/**
	 * The item used as currency (if set to item in {@link SettingsConfig#getCurrencyType()}
	 */
	@Getter
	private ItemStack currencyItem;
	/**
	 * The item displayed for gambling shops
	 */
	@Getter
	private ItemStack gambleDisplayItem;
	/**
	 * No shop can be created if it is present here
	 */
	@Getter
	private List<ItemStack> blacklistItems = new ArrayList<>();
	/**
	 * Shop needs to be made of this material to be allowed
	 */
	@Getter
	private List<ItemStack> whitelistItems = new ArrayList<>();
	/**
	 * No shop can be created with this material
	 */
	@Getter
	private List<Material> blacklistMaterials = new ArrayList<>();
	/**
	 * Only shops with this material can be created
	 */
	@Getter
	private List<Material> whitelistMaterials = new ArrayList<>();
	
	public ItemConfig() {
		super(Shop.getPlugin().getDataPath().resolve("item-config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		currencyItem = getItemStack("currency-item", new ItemStack(Material.EMERALD));
		currencyItem.setAmount(1);
		gambleDisplayItem = getItemStack("gamble-display-item", null);
		blacklistMaterials.clear();
		whitelistMaterials.clear();
		
		blacklistItems = getItemStackList("blacklist.items");
		
		if(contains("blacklist.materials")){
			for(var material : getStringList("blacklist.materials")){
				blacklistMaterials.add(Material.valueOf(material));
			}
		}
		
		whitelistItems = getItemStackList("whitelist.items");
		
		if(contains("whitelist.materials")){
			for(var material : getStringList("whitelist.materials")){
				whitelistMaterials.add(Material.valueOf(material));
			}
		}
	}
	
	public boolean isValidItem(ItemStack itemStack) {
		if(itemStack == null || itemStack.getType().isAir()){
			return false;
		}
		
		if(blacklistMaterials.contains(itemStack.getType())){
			return false;
		}
		
		if(blacklistItems.stream().anyMatch(item -> item.isSimilar(itemStack))){
			return false;
		}
		
		boolean hasMaterialWhitelist = !whitelistMaterials.isEmpty();
		boolean hasItemWhitelist = !whitelistItems.isEmpty();
		
		// No whitelist means all non-blacklisted items are allowed.
		if(!hasMaterialWhitelist && !hasItemWhitelist){
			return true;
		}
		
		// An item is valid if it matches either active whitelist.
		return (hasMaterialWhitelist && whitelistMaterials.contains(itemStack.getType())) || (hasItemWhitelist && whitelistItems.stream().anyMatch(
				item -> item.isSimilar(itemStack)));
	}
	
	public List<ItemStack> getItemStackList(String path) {
		List<?> list = getList(path);
		if(list == null){
			return new ArrayList<>();
		}
		
		return list.stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public void setCurrencyItem(ItemStack currencyItem) {
		this.currencyItem = currencyItem;
		set("currency-item", currencyItem);
		silentSave();
	}
	
	public void setGambleDisplayItem(ItemStack gambleDisplayItem) {
		this.gambleDisplayItem = gambleDisplayItem;
		set("gamble-display-item", gambleDisplayItem);
		silentSave();
	}
}
