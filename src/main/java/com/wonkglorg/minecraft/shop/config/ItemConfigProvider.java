package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.shop.config.ItemConfig.CurrencyDenomination;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mockito can't mock ItemConfig directly so we use this interface instead
 */
public interface ItemConfigProvider{
	
	ItemStack getCurrencyItem();
	
	List<CurrencyDenomination> getCurrencyDenominations();
	
	int getSmallestCurrencyDenominationMultiplier();
	
	ItemStack getGambleDisplayItem();
	
	Map<String, ItemStack> getCustomItems();
	
	Set<DataComponentType> getExcludedDataComponentsForMaterial(Material material);
	
	void setGambleDisplayItem(ItemStack itemStack);
	
	void reload();
	
	void silentLoad();
	
	void setCurrencyItem(ItemStack heldItem);
}