package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.Main;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
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
		super(Main.getPlugin(), Path.of("item-config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		currencyItem = getItemStack("currency-item", new ItemStack(Material.DIAMOND));
		currencyItem.setAmount(1);
		gambleDisplayItem = getItemStack("gamble-display-item", new ItemStack(Material.DIAMOND));
		blacklistMaterials.clear();
		whitelistMaterials.clear();
		
		if(contains("blacklist.materials")){
			for(var material : getStringList("blacklist.materials")){
				try{
					blacklistMaterials.add(Material.valueOf(material));
				} catch(IllegalArgumentException e){
					logger.warning("Invalid blacklist material:" + material);
				}
			}
		}
		
		if(contains("whitelist.materials")){
			for(var material : getStringList("whitelist.materials")){
				try{
					whitelistMaterials.add(Material.valueOf(material));
				} catch(IllegalArgumentException e){
					logger.warning("Invalid whitelist material:" + material);
				}
			}
		}
	}
	
	/**
	 * If the item is allowed by the black / whitelist
	 */
	public boolean isValidItem(ItemStack itemStack) {
		if(itemStack == null || itemStack.getType().isAir()){
			return false;
		}
		
		if(!blacklistMaterials.isEmpty() && blacklistMaterials.contains(itemStack.getType())){
			return false;
		}
		
		if(!whitelistMaterials.isEmpty() && !whitelistMaterials.contains(itemStack.getType())){
			return false;
		}
		
		return true;
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
