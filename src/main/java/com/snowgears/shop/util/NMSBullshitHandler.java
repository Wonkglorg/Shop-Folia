package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NMSBullshitHandler {

    private Shop plugin;
    @Getter
    private double serverVersion;

    @Getter
    private Class<?> craftItemStackClass;
    @Getter
    private Class<?> craftWorldClass;
    @Getter
    private Class<?> craftPlayerClass;
    private Class<?> craftChatMessageClass;
    
    // Cached reflection Method objects
    private Method chatMessageFromStringMethod;
    private Method asNMSCopyMethod;
    private Method getHandleWorldMethod;
    private Method getHandlePlayerMethod;

    public NMSBullshitHandler(Shop plugin){
        this.plugin = plugin;
        init();
    }

    public void init() {
        String mcVersion = plugin.getServer().getClass().getPackage().getName();
        Shop.getPlugin().logger().debug("mcVersion: " + mcVersion);

        // MockBukkit testing does not support NMS, so we need to just return early
        if (plugin.isMockBukkit()) { 
            return;
        }

        // Check if we are on Paper 1.20.5 or later, it will not include the CB relocation version (i.e. "1_20_R3")
        if (!mcVersion.equals("org.bukkit.craftbukkit")) {
            Shop.getPlugin().logger().warning("Minecraft version is old or Spigot, loaded version is: " + mcVersion);

            String[] mcVersionSplit = mcVersion.replace(".", ",").split(",");
            // Convert mcVersion into a number like 120.4 (1_20_R4) or 121.1 (1_21_R1) so that we can use it later
            String versionNumberString = mcVersionSplit[mcVersionSplit.length-1].replace("_R", ".").replaceAll("[rvV_]*", "");
            serverVersion = Double.parseDouble(versionNumberString);
        }

        // log the server version we are on, it will be 0 when we are on a Paper server
        Shop.getPlugin().logger().debug("Server Version: " + this.getServerVersion());
        Shop.getPlugin().logger().debug("Is Server Version over 117.0D: " + (Math.floor(this.getServerVersion()) >= 117.0D));

        try {
            this.craftItemStackClass = Class.forName(mcVersion + ".inventory.CraftItemStack");
            this.craftChatMessageClass = Class.forName(mcVersion + ".util.CraftChatMessage");
            // Server Version will be 0 for Paper
            if (Math.floor(this.getServerVersion()) >= 117.0D || this.getServerVersion() == 0) {
                this.craftWorldClass = Class.forName(mcVersion + ".CraftWorld");
                this.craftPlayerClass = Class.forName(mcVersion + ".entity.CraftPlayer");

                // java.lang.ClassNotFoundException: net.minecraft.server.v1_17_R1.ItemStack

                Shop.getPlugin().logger().debug("CraftItemStack: " + this.craftItemStackClass.toString());
                Shop.getPlugin().logger().debug("CraftWorld: " + this.craftWorldClass.toString());
                Shop.getPlugin().logger().debug("CraftPlayer: " + this.craftPlayerClass.toString());
                
                // Cache the commonly used methods
                try {
                    chatMessageFromStringMethod = craftChatMessageClass.getMethod("fromStringOrNull", String.class);
                    asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
                    getHandleWorldMethod = craftWorldClass.getMethod("getHandle");
                    getHandlePlayerMethod = craftPlayerClass.getMethod("getHandle");
                    
                    Shop.getPlugin().logger().debug("Successfully cached reflection methods");
                } catch (NoSuchMethodException e) {
                    Shop.getPlugin().logger().warning("Failed to cache some reflection methods: " + e.getMessage());
                }
            }
        } catch (Error | Exception e) {
            Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to retrieve a NMS class used for NBT data. Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
            Shop.getPlugin().getShopHandler().disableDisplayClass();
        }
    }
	
	public net.minecraft.network.chat.Component getFormattedChatMessage(String text) {
        try {
            if (chatMessageFromStringMethod == null) {
                chatMessageFromStringMethod = craftChatMessageClass.getMethod("fromStringOrNull", String.class);
            }
            return (net.minecraft.network.chat.Component) chatMessageFromStringMethod.invoke(null, text);
        } catch (Error | Exception e) { /** Suppress errors */ }

        Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to load CraftChatMessage class! Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
        Shop.getPlugin().getShopHandler().disableDisplayClass();
        return null;
    }

    public net.minecraft.world.item.ItemStack getMCItemStack(ItemStack is) {
        try {
            if (asNMSCopyMethod == null) {
                asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            }
            return (net.minecraft.world.item.ItemStack) asNMSCopyMethod.invoke(null, is);
        } catch (Error | Exception e) { /** Suppress errors */ }

        Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to get MCItemStack! Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
        Shop.getPlugin().getShopHandler().disableDisplayClass();
        return null;
    }

    public net.minecraft.world.level.Level getMCLevel(Location location) {
        try {
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            if (craftWorld != null) {
                if (getHandleWorldMethod == null) {
                    getHandleWorldMethod = craftWorldClass.getMethod("getHandle");
                }
                return (net.minecraft.world.level.Level) getHandleWorldMethod.invoke(craftWorld);
            }
        } catch (Error | Exception e) { /** Suppress errors */ }

        Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to get ServerLevel! Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
        Shop.getPlugin().getShopHandler().disableDisplayClass();
        return null;
    }

    public ServerLevel getMCServerLevel(Location location) {
        try {
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            if (craftWorld != null) {
                if (getHandleWorldMethod == null) {
                    getHandleWorldMethod = craftWorldClass.getMethod("getHandle");
                }
                return (ServerLevel) getHandleWorldMethod.invoke(craftWorld);
            }
        } catch (Error | Exception e) { /** Suppress errors */ }

        Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to get ServerLevel! Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
        Shop.getPlugin().getShopHandler().disableDisplayClass();
        return null;
    }

    public ServerPlayerConnection getPlayerConnection(Player player) {
        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            if (craftPlayer != null) {
                if (getHandlePlayerMethod == null) {
                    getHandlePlayerMethod = craftPlayerClass.getMethod("getHandle");
                }
                Object entityPlayer = getHandlePlayerMethod.invoke(craftPlayer);
                if (entityPlayer != null) {
                    try {
                        Field playerConnection = entityPlayer.getClass().getDeclaredField("connection");
                        return (ServerPlayerConnection) playerConnection.get(entityPlayer);
                    } catch (Error | Exception e) {
                        // Try to access the obfuscated field directly on CraftBukkit (for Spigot support)
                        try {
                            Field playerConnection = entityPlayer.getClass().getField("c");
                            return (ServerPlayerConnection) playerConnection.get(entityPlayer);
                        } catch (Error | Exception err) { /** Suppress errors */ }
                    }
                }
            }
        } catch(Error | Exception e){ /** Suppress errors */ }
        Shop.getPlugin().logger().log(java.util.logging.Level.SEVERE, "Unable to hook into internal ServerPlayerConnection to send Display packets to users! Are you using a supported server type/version? We suggest you use PaperMC for running Shop! Visual displays will now be disabled.");
        // Disable the display class so that we don't try to use it anymore since we can't send packets to users.
        Shop.getPlugin().getShopHandler().disableDisplayClass();

        return null;
    }
}
