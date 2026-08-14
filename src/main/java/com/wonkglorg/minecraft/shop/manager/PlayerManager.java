package com.wonkglorg.minecraft.shop.manager;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.manager.player.OfflinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.config.types.Config;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager{
	private static final Map<UUID, OnlinePlayerProfile> onlineProfiles = new ConcurrentHashMap<>();
	private static final Map<UUID, OfflinePlayerProfile> offlineProfiles = new ConcurrentHashMap<>();
	private static final Config PLAYER_DATA = new Config(Shop.getPlugin(), Path.of("data", "player-settings.yml"));
	/**
	 * timestamp when the player last teleported to a shop
	 */
	private static final Map<UUID, Long> PLAYER_LAST_SHOP_TP = new HashMap<>();
	
	private PlayerManager() {
	}
	
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
	
	/**
	 * Loads an online players profile. <br>
	 * When calle dif the economy is set to expirience sets the players exp to the amount stored in file
	 */
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
	
	/**
	 * Removes the cached profiles from the player, if an online profile existed saves it to file before removal
	 */
	public static void removeProfile(OfflinePlayer player) {
		OnlinePlayerProfile remove = onlineProfiles.remove(player.getUniqueId());
		if(remove != null){
			saveToFile(remove);
		}
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
		if(Shop.getPlugin().getSettingsConfig().getCurrencyType() == CurrencyType.EXPERIENCE){
			PLAYER_DATA.set(basePath + ".experience", profile.getExperience());
		} else {
			PLAYER_DATA.set(basePath + ".experience", null);
		}
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
	public static void loadFromFile(PlayerProfile profile) {
		String basePath = "player." + profile.getUuid();
		
		profile.setNotifyOwner(PLAYER_DATA.getBoolean(basePath + ".notify-owner", false));
		profile.setNotifyStock(PLAYER_DATA.getBoolean(basePath + ".notify-stock", false));
		profile.setNotifyUser(PLAYER_DATA.getBoolean(basePath + ".notify-user", false));
		profile.setExperience(PLAYER_DATA.getInt(basePath + ".experience", -1));
	}
	
	public static void reload() {
		onlineProfiles.clear();
		offlineProfiles.clear();
		//load all profiles of currently online players
		for(var player : Bukkit.getOnlinePlayers()){
			getOnlineProfile(player);
		}
	}
	
	public static Duration getTeleportCooldownRemaining(UUID uuid) {
		double cooldownSeconds = Shop.getPlugin().getSettingsConfig().getTeleportCooldown();
		
		if(cooldownSeconds <= 0){
			return Duration.ZERO;
		}
		
		long lastTeleport = PLAYER_LAST_SHOP_TP.getOrDefault(uuid, 0L);
		
		if(lastTeleport == 0){
			return Duration.ZERO;
		}
		
		long cooldownMillis = (long) (cooldownSeconds * 1000);
		long expiresAt = lastTeleport + cooldownMillis;
		long remainingMillis = expiresAt - System.currentTimeMillis();
		
		return Duration.ofMillis(Math.max(remainingMillis, 0));
	}
	
	public static boolean canTeleport(UUID uuid) {
		return getTeleportCooldownRemaining(uuid).isZero();
	}
	
	public static void addTeleportCooldown(UUID uuid) {
		PLAYER_LAST_SHOP_TP.put(uuid, System.currentTimeMillis());
	}
}
