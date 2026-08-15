package com.wonkglorg.minecraft.shop.manager.player;

import com.wonkglorg.minecraft.shop.Main;
import static com.wonkglorg.minecraft.shop.manager.PlayerManager.saveToFile;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.EconomyUtils;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class OnlinePlayerProfile extends OfflinePlayerProfile{
	private final Player player;
	
	public OnlinePlayerProfile(Player player) {
		super(player);
		this.player = player;
		//if the currency is experience and a value was saved set it to the current players exp
		if(Main.getPlugin().getSettingsConfig().getCurrencyType() == CurrencyType.EXPERIENCE && getExperience() != -1){
			EconomyUtils.setTotalExperience(player, getExperience());
		}
	}
	
	public boolean isOperator() {
		return isOperator(player);
	}
	
	public boolean isAllowedToCreateShopType(ShopType type) {
		return isAllowedToCreateShop(player, type);
	}
	
	public boolean isAllowedToCreateShop() {
		return isAllowedToCreateShop(player);
	}
	
	public boolean isAllowedToUseShop() {
		return isAllowedToUseShop(player);
	}
	
	public boolean isAllowedToUseShop(ShopType type) {
		return isAllowedToUseShop(player, type);
	}
	
	public boolean isAllowedToDestroyShop() {
		return isAllowedToDestroyShop(player);
	}
	
	public boolean isAllowedToDestroyShop(ShopType type) {
		return isAllowedToDestroyShop(player, type);
	}
	
	public boolean isAllowedToDestroyShopOther() {
		return isAllowedToDestroyShopOther(player);
	}
	
	public int getShopBuildLimit() {
		return getShopBuildLimit(player);
	}
	
	public void removeExperienceAmount(int amount) {
		EconomyUtils.setTotalExperience(player, EconomyUtils.getTotalExperience(player) - amount);
		experience = EconomyUtils.getTotalExperience(player);
		saveToFile(this);
	}
	
	public void addExperienceAmount(int amount) {
		EconomyUtils.setTotalExperience(player, EconomyUtils.getTotalExperience(player) + amount);
		experience = EconomyUtils.getTotalExperience(player);
		saveToFile(this);
	}
	
	/**
	 *
	 * @return all shop types the player is allowed to build
	 */
	private List<ShopType> getBuildableShopTypes() {
		return getBuildableShopTypes(player);
	}
	
	public Duration getTeleportCooldownRemaining() {
		return getTeleportCooldownRemaining(offlinePlayer.getUniqueId());
	}
	
	public boolean canTeleport() {
		return canTeleport(offlinePlayer.getUniqueId());
	}
	
	public void addTeleportCooldown() {
		addTeleportCooldown(offlinePlayer.getUniqueId());
	}
}
