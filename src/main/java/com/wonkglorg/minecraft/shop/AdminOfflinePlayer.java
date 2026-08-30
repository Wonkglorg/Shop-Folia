package com.wonkglorg.minecraft.shop;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.persistence.PersistentDataContainerView;
import lombok.Getter;
import org.bukkit.BanEntry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * A virtual offline player used for admin shops
 */
public class AdminOfflinePlayer implements OfflinePlayer{
	@Getter
	public static final UUID adminUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	
	@Override
	public boolean isOnline() {
		return false;
	}
	
	@Override
	public boolean isConnected() {
		return false;
	}
	
	@Override
	public String getName() {
		return "SERVER";
	}
	
	@Override
	public @NonNull UUID getUniqueId() {
		return adminUUID;
	}
	
	@Override
	public PlayerProfile getPlayerProfile() {
		return null;
	}
	
	@Override
	public boolean isBanned() {
		return false;
	}
	
	@Override
	public @Nullable <E extends BanEntry<? super PlayerProfile>> E ban(@Nullable String reason, @Nullable Date expires, @Nullable String source) {
		return null;
	}
	
	@Override
	public @Nullable <E extends BanEntry<? super PlayerProfile>> E ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source) {
		return null;
	}
	
	@Override
	public @Nullable <E extends BanEntry<? super PlayerProfile>> E ban(@Nullable String reason,
	                                                                   @Nullable Duration duration,
	                                                                   @Nullable String source) {
		return null;
	}
	
	@Override
	public boolean isWhitelisted() {
		return false;
	}
	
	@Override
	public void setWhitelisted(boolean value) {
		//do nothing
	}
	
	@Override
	public @Nullable Player getPlayer() {
		return null;
	}
	
	@Override
	public long getFirstPlayed() {
		return 0;
	}
	
	@Override
	public long getLastPlayed() {
		return 0;
	}
	
	@Override
	public boolean hasPlayedBefore() {
		return true;
	}
	
	@Override
	public long getLastLogin() {
		return 0;
	}
	
	@Override
	public long getLastSeen() {
		return 0;
	}
	
	@Override
	public @Nullable Location getRespawnLocation(boolean loadLocationAndValidate) {
		return null;
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic, int amount) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic, int amount) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void setStatistic(@NonNull Statistic statistic, int newValue) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public int getStatistic(@NonNull Statistic statistic) throws IllegalArgumentException {
		return 0;
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic, @NonNull Material material) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic, @NonNull Material material) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public int getStatistic(@NonNull Statistic statistic, @NonNull Material material) throws IllegalArgumentException {
		return 0;
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic, @NonNull Material material, int amount) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic, @NonNull Material material, int amount) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void setStatistic(@NonNull Statistic statistic, @NonNull Material material, int newValue) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public int getStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType) throws IllegalArgumentException {
		return 0;
	}
	
	@Override
	public void incrementStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType, int amount) throws IllegalArgumentException {
		//do nothing
	}
	
	@Override
	public void decrementStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType, int amount) {
		//do nothing
	}
	
	@Override
	public void setStatistic(@NonNull Statistic statistic, @NonNull EntityType entityType, int newValue) {
		//do nothing
	}
	
	@Override
	public @Nullable Location getLastDeathLocation() {
		return null;
	}
	
	@Override
	public @Nullable Location getLocation() {
		return null;
	}
	
	@SuppressWarnings("NullableProblems")
	@Override
	public @Nullable PersistentDataContainerView getPersistentDataContainer() { //NOSONAR
		return null;
	}
	
	@Override
	public @NotNull Map<String, Object> serialize() {
		return Map.of();
	}
	
	@Override
	public boolean isOp() {
		return true;
	}
	
	@Override
	public void setOp(boolean value) {
		//do nothing
	}
}
