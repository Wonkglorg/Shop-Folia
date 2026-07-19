package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight cache for player names to avoid expensive OfflinePlayer.getName() calls.
 * Thread-safe with lazy loading during initial cache build phase.
 */
public class PlayerNameCache {
    
    private static final ConcurrentHashMap<UUID, String> cache = new ConcurrentHashMap<>();
    private static final String CACHE_FILENAME = "Data/playerNameCache.yml";
    
    /**
     * Initialize cache on startup - checks if cache file exists
     */
    public static void initialize() {
        File cacheFile = new File(Shop.getPlugin().getDataFolder(), CACHE_FILENAME);
        
        if (cacheFile.exists()) {
            loadFromFile(cacheFile);
        }
    }
    
    /**
     * Gets a player name from cache, with lazy loading during initial build
     * @param uuid Player UUID
     * @return Player name or formatted UUID fallback
     */
    public static String getName(UUID uuid) {
        String cachedName = cache.get(uuid);
        if (cachedName != null) {
            return cachedName;
        }

        // If the player is not in the cache, add a placeholder name before attempting to load from OfflinePlayer
        // This is to avoid issues with Bukkit.getOfflinePlayer(uuid).getName() causing a recursive error loop in 1.21.5
        // if we run into the recursive loop issue, then we'll just return the placeholder name in the future instead of lagging the server
        String shortId = uuid.toString();
        String unknownPlayerString = "Unknown Player (" + shortId.substring(0, 3) + "..." + shortId.substring(shortId.length() - 3) + ")";
        cache.put(uuid, unknownPlayerString);

        // Try loading from OfflinePlayer once, otherwise we'll just return the placeholder name
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.hasPlayedBefore()) {
                String name = player.getName();
                if (name != null) {
                    cache.put(uuid, name);
                    return name;
                }
            } else {
                Shop.getPlugin().getLogger().warning("Player " + uuid + " has not played on this server and/or their player data file does not exist! Unable to load the player name from OfflinePlayer!");
            }
        } catch (Error | Exception e) {
            Shop.getPlugin().getLogger().warning("Error while getting player name for " + uuid + " from OfflinePlayer.getName()! " + e.getMessage());
        }

        // Return placeholder name as fallback
        return unknownPlayerString;
    }
    
    /**
     * Caches a player name and switches off initial build mode
     * @param uuid Player UUID  
     * @param name Player name
     */
    public static void cacheName(UUID uuid, String name) {
        if (uuid != null && name != null && !name.trim().isEmpty()) {
            cache.put(uuid, name);
        }
    }
    
    /**
     * Removes a player from cache
     * @param uuid Player UUID
     */
    public static void removeName(UUID uuid) {
        cache.remove(uuid);
    }
    
    /**
     * Saves cache to file
     * @param dataFolder Plugin data folder
     */
    public static void saveToFile() {
        File cacheFile = new File(Shop.getPlugin().getDataFolder(), CACHE_FILENAME);
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (ConcurrentHashMap.Entry<UUID, String> entry : cache.entrySet()) {
                config.set(entry.getKey().toString(), entry.getValue());
            }
            config.save(cacheFile);
        } catch (IOException e) {
            Shop.getPlugin().getLogger().warning("Error while saving player name cache to file! " + e.getMessage());
        }
    }
    
    /**
     * Loads cache from file
     */
    private static void loadFromFile(File cacheFile) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);
            for (String key : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = config.getString(key);
                    if (name != null) {
                        cache.put(uuid, name);
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUID entries
                }
            }
        } catch (Exception e) {
            Shop.getPlugin().getLogger().warning("Error while loading player name cache from file! " + e.getMessage());
        }
    }
    
    /**
     * Gets current cache size for monitoring
     * @return Number of cached entries
     */
    public static int getCacheSize() {
        return cache.size();
    }
} 