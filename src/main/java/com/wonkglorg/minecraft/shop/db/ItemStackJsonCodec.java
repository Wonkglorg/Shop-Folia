package com.wonkglorg.minecraft.shop.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Helper class to convert itemstacks from and to json
 */
@SuppressWarnings("deprecation")
public final class ItemStackJsonCodec{
	
	private static final Gson GSON = new GsonBuilder().create();
	
	private ItemStackJsonCodec() {
	}
	
	/**
	 * Serializes the itemstack
	 *
	 * @param itemStack the itemstack
	 * @param normalizeAmount if the amount should be normalized to 1 before saving
	 */
	public static String serialize(ItemStack itemStack, boolean normalizeAmount) {
		if(itemStack == null){
			return null;
		}
		
		ItemStack item = itemStack.clone();
		if(normalizeAmount){
			item.setAmount(1);
		}
		
		JsonObject json = Bukkit.getUnsafe().serializeItemAsJson(item);
		return GSON.toJson(json);
	}
	
	public static ItemStack deserialize(String json) {
		if(json == null || json.isBlank()){
			return null;
		}
		
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		return Bukkit.getUnsafe().deserializeItemFromJson(object);
	}
}