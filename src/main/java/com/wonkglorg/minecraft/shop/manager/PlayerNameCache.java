package com.wonkglorg.minecraft.shop.manager;

import com.wonkglorg.minecraft.shop.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight cache for player names to avoid expensive OfflinePlayer.getName() calls.
 * Thread-safe with lazy loading during initial cache build phase.
 */
public class PlayerNameCache{
	
	private static final Map<UUID, String> cache = new ConcurrentHashMap<>();
	
	/**
	 * Initialize cache on startup - checks if cache file exists
	 */
	public static void initialize() {
		cache.putAll(Main.getPlugin().getShopmanager().getDatabase().loadPlayerNames());
	}
	
	/**
	 * Gets a player name from cache, with lazy loading during initial build
	 *
	 * @param uuid Player UUID
	 * @return Player name or formatted UUID fallback
	 */
	public static String getName(UUID uuid) {
		String cachedName = cache.get(uuid);
		if(cachedName != null){
			return cachedName;
		}
		
		// If the player is not in the cache, add a placeholder name before attempting to load from OfflinePlayer
		// This is to avoid issues with Bukkit.getOfflinePlayer(uuid).getName() causing a recursive error loop in 1.21.5
		// if we run into the recursive loop issue, then we'll just return the placeholder name in the future instead of lagging the server
		String shortId = uuid.toString();
		String unknownPlayerString = "Unknown Player (" + shortId.substring(0, 3) + "..." + shortId.substring(shortId.length() - 3) + ")";
		cache.put(uuid, unknownPlayerString);
		
		// Try loading from OfflinePlayer once, otherwise we'll just return the placeholder name
		try{
			OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
			if(player.hasPlayedBefore()){
				String name = player.getName();
				if(name != null){
					cache.put(uuid, name);
					return name;
				}
			} else {
				Main.getPlugin().getLogger().warning("Player " +
				                                     uuid +
				                                     " has not played on this server and/or their player data file does not exist! Unable to load the player name from OfflinePlayer!");
			}
		} catch(Exception e){
			Main.getPlugin().getLogger().warning("Error while getting player name for " + uuid + " from OfflinePlayer.getName()! " + e.getMessage());
		}
		
		// Return placeholder name as fallback
		return unknownPlayerString;
	}
	
	/**
	 * Caches a player name and switches off initial build mode
	 *
	 * @param uuid Player UUID
	 * @param name Player name
	 */
	public static void cacheName(UUID uuid, String name) {
		if(cache.containsKey(uuid)){
			return;
		}
		if(uuid != null && name != null && !name.trim().isEmpty()){
			cache.put(uuid, name);
			Main.getPlugin().getShopmanager().getDatabase().addPlayer(uuid, name);
		}
	}
	
	/**
	 * Removes a player from cache
	 *
	 * @param uuid Player UUID
	 */
	public static void removeName(UUID uuid) {
		cache.remove(uuid);
	}
	
	/**
	 * Gets current cache size for monitoring
	 *
	 * @return Number of cached entries
	 */
	public static int getCacheSize() {
		return cache.size();
	}
} 