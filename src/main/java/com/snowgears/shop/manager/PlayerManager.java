package com.snowgears.shop.manager;

import com.snowgears.shop.Shop;
import com.snowgears.shop.manager.player.OfflinePlayerProfile;
import com.snowgears.shop.manager.player.OnlinePlayerProfile;
import com.snowgears.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.config.types.Config;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager{
	private static final Map<UUID, OnlinePlayerProfile> onlineProfiles = new ConcurrentHashMap<>();
	private static final Map<UUID, OfflinePlayerProfile> offlineProfiles = new ConcurrentHashMap<>();
	private static final Config PLAYER_DATA = new Config(Shop.getPlugin(), Path.of("data", "player-settings.yml"));
	
	/**
	 * Gets the players profile if it has been loaded null otherwise
	 */
	public static @Nullable OnlinePlayerProfile getOnlineProfileIfCached(UUID uuid) {
		return onlineProfiles.get(uuid);
	}
	
	public static OnlinePlayerProfile getOnlineProfile(Player player) {
		UUID uniqueId = player.getUniqueId();
		return onlineProfiles.computeIfAbsent(uniqueId, uuid -> new OnlinePlayerProfile(player));
	}
	
	public static void loadProfile(Player player) {
		onlineProfiles.put(player.getUniqueId(), new OnlinePlayerProfile(player));
	}
	
	public static void loadProfile(OfflinePlayer player) {
		offlineProfiles.put(player.getUniqueId(), new OfflinePlayerProfile(player));
	}
	
	public static OfflinePlayerProfile getOfflineProfile(OfflinePlayer player) {
		UUID uniqueId = player.getUniqueId();
		if(onlineProfiles.containsKey(uniqueId)){
			return onlineProfiles.get(uniqueId);
		}
		return offlineProfiles.computeIfAbsent(uniqueId, uuid -> new OfflinePlayerProfile(player));
	}
	
	public static void removeProfile(OfflinePlayer player) {
		onlineProfiles.remove(player.getUniqueId());
		offlineProfiles.remove(player.getUniqueId());
	}
	
	/**
	 * Saves a profile to file
	 *
	 * @param profile the profile to save
	 */
	public static void saveToFile(PlayerProfile profile) {
		String basePath = "player." + profile.getUuid();
		setOrRemove(basePath + ".notify-owner", profile.isNotifyOwner());
		setOrRemove(basePath + ".notify-stock", profile.isNotifyStock());
		setOrRemove(basePath + ".notify-user", profile.isNotifyUser());
	}
	
	private static void setOrRemove(String path, boolean value) {
		if(value){
			PLAYER_DATA.set(path, true);
		} else {
			PLAYER_DATA.set(path, null);
		}
	}
	
	/**
	 * Fills a profiles saved data from file. This happens automatically when calling any of the load profile methods this manager provides
	 *
	 * @param profile the profile to save the loaded data into
	 */
	public static void loadfromFile(PlayerProfile profile) {
		String basePath = "player." + profile.getUuid();
		
		profile.setNotifyOwner(PLAYER_DATA.getBoolean(basePath + ".notify-owner", false));
		profile.setNotifyStock(PLAYER_DATA.getBoolean(basePath + ".notify-stock", false));
		profile.setNotifyUser(PLAYER_DATA.getBoolean(basePath + ".notify-user", false));
	}
	
	public static void reload() {
		onlineProfiles.clear();
		offlineProfiles.clear();
		//load all profiles of currently online players
		for(var player : Bukkit.getOnlinePlayers()){
			getOnlineProfile(player);
		}
	}
}
