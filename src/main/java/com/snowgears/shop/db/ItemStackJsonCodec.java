package com.snowgears.shop.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class ItemStackJsonCodec {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private ItemStackJsonCodec() {
    }
    
    public static String serialize(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        
        ItemStack item = itemStack.clone();
        item.setAmount(1);
        
        Map<String, Object> data = item.serialize();
        
        //those fields are unnecessary in the database, no need to store them
        data.remove("DataVersion");
        data.remove("count");
        data.remove("schema_version");
        
        return GSON.toJson(data);
    }

    @SuppressWarnings("unchecked")
    public static ItemStack deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        JsonElement element = JsonParser.parseString(json);

        Map<String, Object> data = GSON.fromJson(
                element,
                Map.class
        );
        

        //todo:mjd check if this is needed or not to deserialize
        //data.put("count", 1);

        return ItemStack.deserialize(data);
    }
}