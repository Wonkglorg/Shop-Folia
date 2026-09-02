package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopDatabase;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;

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
		super(ShopPlugin.getPlugin(), Path.of("item-config.yml"));
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
		shopDatabase().logCurrencyChange(CurrencyType.ITEM, currencyItem);
		silentSave();
	}
	
	public void setGambleDisplayItem(ItemStack gambleDisplayItem) {
		this.gambleDisplayItem = gambleDisplayItem;
		set("gamble-display-item", gambleDisplayItem);
		silentSave();
	}
}
