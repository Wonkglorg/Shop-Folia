package com.wonkglorg.minecraft.shop.manager.player;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.manager.PlayerManager.saveToFile;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ExperienceUtils;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.List;

public class OnlinePlayerProfile extends OfflinePlayerProfile{
	@Getter
	private final Player player;
	
	public OnlinePlayerProfile(Player player) {
		super(player);
		this.player = player;
		//if the currency is experience and a value was saved set it to the current players exp
		if(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType() == CurrencyType.EXPERIENCE && getExperience() != -1){
			ExperienceUtils.setTotalExperience(player, getExperience());
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
	
	/**
	 * See {@link #isAllowedToCycleDisplay(Permissible)}
	 */
	public boolean isAllowedToCycleDisplay() {return isAllowedToCycleDisplay(player);}
	
	/**
	 * See {@link #isAllowedToCycleDisplayOther(Permissible)}
	 */
	public boolean isAllowedToCycleDisplayOther() {return isAllowedToCycleDisplayOther(player);}
	
	public int getShopBuildLimit() {
		return getShopBuildLimit(player);
	}
	
	@Override
	public void removeExperienceAmount(int amount) {
		ExperienceUtils.setTotalExperience(player, ExperienceUtils.getTotalExperience(player) - amount);
		experience = ExperienceUtils.getTotalExperience(player);
		saveToFile(this);
	}
	
	@Override
	public void addExperienceAmount(int amount) {
		ExperienceUtils.setTotalExperience(player, ExperienceUtils.getTotalExperience(player) + amount);
		experience = ExperienceUtils.getTotalExperience(player);
		saveToFile(this);
	}
	
	/**
	 *
	 * @return all shop types the player is allowed to build
	 */
	public List<ShopType> getBuildableShopTypes() {
		return getBuildableShopTypes(player);
	}
	
	@Override
	public int getExperience() {
		return ExperienceUtils.getTotalExperience(player);
	}
}
