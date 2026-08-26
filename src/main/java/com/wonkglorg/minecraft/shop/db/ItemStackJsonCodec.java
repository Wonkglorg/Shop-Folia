package com.wonkglorg.minecraft.shop.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class ItemStackJsonCodec{
	
	private static final Gson GSON = new GsonBuilder().create();
	
	private ItemStackJsonCodec() {
	}
	
	public static String serialize(ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}
		
		ItemStack item = itemStack.clone();
		item.setAmount(1);
		
		JsonObject json = Bukkit.getUnsafe().serializeItemAsJson(item);
		return GSON.toJson(json);
	}
	
	public static ItemStack deserialize(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		return Bukkit.getUnsafe().deserializeItemFromJson(object);
	}
}