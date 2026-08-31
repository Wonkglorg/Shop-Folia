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
	
	public ItemConfig() {
		super(Main.getPlugin(), Path.of("item-config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		currencyItem = getItemStack("currency-item", new ItemStack(Material.DIAMOND));
		currencyItem.setAmount(1);
		gambleDisplayItem = getItemStack("gamble-display-item", new ItemStack(Material.DIAMOND));
		
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
