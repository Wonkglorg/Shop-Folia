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

@SuppressWarnings("unused")
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
	
	/**
	 * If the user either has the operator permission or is op, giving them full access to all features of the plugin
	 */
	public boolean isOperator() {
		return isOperator(player);
	}
	
	/**
	 * If the user is allowed to create a shop of this type, this does NOT enforce shop build limit
	 */
	public boolean isAllowedToCreateShopType(ShopType type) {
		return isAllowedToCreateShop(player, type);
	}
	
	/**
	 * If the user is allowed to create a shop of any type, to find out what specific type they can create use {@link #isAllowedToCreateShop(Permissible, ShopType)} instead
	 */
	public boolean isAllowedToCreateShop() {
		return isAllowedToCreateShop(player);
	}
	
	/**
	 * If the user is allowed to use any of the shop types, to find out what specific type they can use, use {@link #isAllowedToUseShop(Permissible, ShopType)} instead
	 */
	public boolean isAllowedToUseShop() {
		return isAllowedToUseShop(player);
	}
	
	/**
	 * If the user is allowed to use a shop of this type
	 */
	public boolean isAllowedToUseShop(ShopType type) {
		return isAllowedToUseShop(player, type);
	}
	
	/**
	 * If the user is allowed to destroy any of the shop types, to find out what specific type they can destroy, use {@link #isAllowedToDestroyShop(Permissible, ShopType)} instead
	 */
	public boolean isAllowedToDestroyShop() {
		return isAllowedToDestroyShop(player);
	}
	
	/**
	 * If the user is allowed to use a shop of this type
	 */
	public boolean isAllowedToDestroyShop(ShopType type) {
		return isAllowedToDestroyShop(player, type);
	}
	
	/**
	 * If the user is allowed to destroy someone else's shop
	 */
	public boolean isAllowedToDestroyShopOther() {
		return isAllowedToDestroyShopOther(player);
	}
	
	/**
	 * If the user is allowed to cycle shop displays of their own shops
	 */
	public boolean isAllowedToCycleDisplay() {return isAllowedToCycleDisplay(player);}
	
	/**
	 * If the user is allowed to cycle shop displays of other players shops
	 */
	public boolean isAllowedToCycleDisplayOther() {return isAllowedToCycleDisplayOther(player);}
	
	/**
	 * If the user is allowed to view their own shop transaction history
	 */
	public boolean isAllowedToViewShopTransactions() {return isAllowedToViewShopTransactions(player);}
	
	/**
	 * If the user is allowed to view another players shop transaction history
	 */
	public boolean isAllowedToViewShopTransactionsOther() {return isAllowedToViewShopTransactionsOther(player);}
	
	/**
	 * If the user is allowed to change the settings of their own shop
	 */
	public boolean isAllowedToChangeSettings() {return isAllowedToChangeSettings(player);}
	
	/**
	 * If the user is allowed to change the settings of another players shop
	 */
	public boolean isAllowedToChangeSettingsOther() {return isAllowedToChangeSettingsOther(player);}
	
	/**
	 * If the user is allowed to change the list of banned players for their own shop
	 */
	public boolean isAllowedToChangeShopBans() {return isAllowedToChangeShopBans(player);}
	
	/**
	 * If the user is allowed to change the list of banned players for another player's shop
	 */
	public boolean isAllowedToChangeShopBansOther() {return isAllowedToChangeShopBansOther(player);}
	
	/**
	 * if the user can bypasses a shop ban
	 */
	public boolean canBypassShopBan() {return canBypassBan(player);}
	
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
