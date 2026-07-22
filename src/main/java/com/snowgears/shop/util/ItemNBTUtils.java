package com.snowgears.shop.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"PatternValidation", "DataFlowIssue", "unchecked", "rawtypes"})
public class ItemNBTUtils{
	
	public static Map<Key, DataComponentValue> getNMSItemStackDataComponents(ItemStack itemStack) {
		if(itemStack.getType().isAir()){
			return Collections.emptyMap();
		}
		RegistryAccess minecraftRegistry = CraftRegistry.getMinecraftRegistry();
		net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
		DataComponentPatch dataComponentPatch = nmsItemStack.getComponentsPatch();
		Map<Key, DataComponentValue> convertedComponents = new HashMap<>();
		for(Map.Entry<DataComponentType<?>, Optional<?>> entry : dataComponentPatch.entrySet()){
			DataComponentType<?> type = entry.getKey();
			Optional<?> optValue = entry.getValue();
			Identifier identifier = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
			Key key = Key.key(identifier.getNamespace(), identifier.getPath());
			if(optValue.isPresent()){
				Codec codec = type.codec();
				if(codec != null){
					Object nativeJsonElement = codec.encodeStart(minecraftRegistry.createSerializationContext(JsonOps.INSTANCE), optValue.get())
					                                .getOrThrow();
					JsonElement jsonElement = NativeJsonConverter.fromNative(nativeJsonElement);
					DataComponentValue value = GsonDataComponentValue.gsonDataComponentValue(jsonElement);
					convertedComponents.put(key, value);
				}
			} else {
				convertedComponents.put(key, DataComponentValue.removed());
			}
		}
		return convertedComponents;
	}
}