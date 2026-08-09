package com.snowgears.shop.manager;

import com.snowgears.shop.Shop;
import com.snowgears.shop.manager.player.OfflinePlayerProfile;
import com.snowgears.shop.manager.player.OnlinePlayerProfile;
import com.snowgears.shop.manager.player.PlayerProfile;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopCreationProcess;
import com.wonkglorg.minecraft.config.types.Config;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
	/**
	 * What step in the creation process the player is currently at
	 */
	@Getter
	private static final Map<UUID, ShopCreationProcess> PLAYER_SHOP_CREATION_STEP = new HashMap<>();
	/**
	 * Players currently in the creative shop creation
	 */
	private static final Map<UUID, PlayerData> PLAYER_CREATIVE_SHOP_CREATION = new HashMap<>();
	
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
	
	public static void addShopCreationProcess(UUID uuid, ShopCreationProcess process) {
		PLAYER_SHOP_CREATION_STEP.put(uuid, process);
	}
	
	public static ShopCreationProcess getShopCreationProcess(UUID uuid) {
		return PLAYER_SHOP_CREATION_STEP.get(uuid);
	}
	
	/**
	 * Cancels the shop creation process for the player
	 */
	public static void cancelShopCreationProcess(Player player) {
		ShopCreationProcess process = getShopCreationProcess(player.getUniqueId());
		if(process != null){
			process.display.removeDisplayEntities(player, true);
			removeShopCreationProcess(player.getUniqueId());
			Shop.getPlugin().getLangManager().request("interaction_issue.createCancel").sendToAudience(player);
		}
		
		// Remove player from creative selection if they are in it!
		Shop.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, _ -> removePlayerFromCreativeSelection(player));
	}
	
	public static void cleanupShopCreationProcess(Player player) {
		ShopCreationProcess process = getShopCreationProcess(player.getUniqueId());
		if(process != null){
			process.cleanup();
			removeShopCreationProcess(player.getUniqueId());
		}
	}
	
	private static void removeShopCreationProcess(UUID uuid) {
		PLAYER_SHOP_CREATION_STEP.remove(uuid);
	}
	
	public static boolean isInShopCreationProcess(UUID uuid) {
		return PLAYER_CREATIVE_SHOP_CREATION.containsKey(uuid);
	}
	
	public static boolean removePlayerFromCreativeSelection(Player player) {
		PlayerData data = PLAYER_CREATIVE_SHOP_CREATION.get(player.getUniqueId());
		if(data != null){
			PLAYER_CREATIVE_SHOP_CREATION.remove(player.getUniqueId());
			data.apply();
			player.closeInventory();
			return true;
		}
		return false;
	}
	
	public static PlayerData getCreativeShopCreationData(UUID uuid) {
		return PLAYER_CREATIVE_SHOP_CREATION.get(uuid);
	}
	
	public static boolean isInCreativeShopCreation(UUID uuid) {
		return PLAYER_CREATIVE_SHOP_CREATION.containsKey(uuid);
	}
	
	public static void putPlayerInCreativeSelection(Player player, Location shopSignLocation, boolean guiSearch) {
		// Sanity check, make sure players don't somehow go into creative mode when it's disabled!
		if(!Shop.getPlugin().getSettingsConfig().isAllowCreativeSelection()){
			Shop.getPlugin().getLangManager().request("creativeSelection.disabled").sendToAudience(player);
			return;
		}
		// Don't put them in creative if they are already in creative.
		UUID id = player.getUniqueId();
		if(isInCreativeShopCreation(id)){
			return;
		}
		PlayerData data = new PlayerData(player, shopSignLocation, guiSearch);
		PLAYER_CREATIVE_SHOP_CREATION.put(id, data);
		
		sendPlayerLockedMessages(player, data);
		player.setGameMode(GameMode.CREATIVE);
	}
	
}
