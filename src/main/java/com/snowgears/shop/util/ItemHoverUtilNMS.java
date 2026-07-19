package com.snowgears.shop.util;
import com.snowgears.shop.Shop;

/**
 * This class is used to create hover events for items.
 * It uses direct NMS calls to serialize the item to a JSON object,
 * and then creates a hover event from that JSON object.
 * 
 * This is a workaround to the fact that the ItemStack class in Bukkit
 * does give easy access to the new Item Components Data system.
 */
public class ItemHoverUtilNMS {

    private static net.minecraft.core.RegistryAccess.Frozen registryAccess;
    private static net.minecraft.resources.RegistryOps<com.google.gson.JsonElement> itemSerializationCodec;

    // 1.20.5+ create a hover event using the new Item Components Data system 
    // (which replaces NBT tags, which replaces... which replaces......... comeon mojang :c )
    public static net.md_5.bungee.api.chat.HoverEvent getHoverEventNMS(org.bukkit.inventory.ItemStack bukkitItem) {
        String id = null;
        int count = -1;
        com.google.gson.JsonElement components = null;

        if (registryAccess == null) {
            registryAccess = net.minecraft.server.MinecraftServer.getServer().registryAccess();
        }
        if (itemSerializationCodec == null) {
            itemSerializationCodec = registryAccess.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        }

        net.minecraft.world.item.ItemStack mcItem = Shop.getPlugin().getNmsBullshitHandler().getMCItemStack(bukkitItem);
        final java.util.Optional<com.google.gson.JsonElement> encoded = net.minecraft.world.item.ItemStack.CODEC.encodeStart(itemSerializationCodec, mcItem).result();
        
        if (encoded.isPresent()) {
            final com.google.gson.JsonObject itemJson = encoded.get().getAsJsonObject();
            id = itemJson.get("id").getAsString();
            count = itemJson.get("count").getAsInt();
            components = itemJson.get("components");
        }

        return new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM, new HoverItem(id, count, components));
    }

    /** Minimal show_item content carrying components for modern clients. */
    private static final class HoverItem extends net.md_5.bungee.api.chat.hover.content.Content {
        private final String id;
        private final int count;
        private final com.google.gson.JsonElement components;

        private HoverItem(final String id, final int count, final com.google.gson.JsonElement components) {
            this.id = id;
            this.count = count;
            this.components = components;
        }

        @Override
        public net.md_5.bungee.api.chat.HoverEvent.Action requiredAction() {
            return net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM;
        }
    }
}
