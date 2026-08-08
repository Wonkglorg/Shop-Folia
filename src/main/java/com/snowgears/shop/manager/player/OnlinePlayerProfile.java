package com.snowgears.shop.manager.player;

import com.snowgears.shop.shop.ShopType;
import org.bukkit.entity.Player;

import java.util.List;

public class OnlinePlayerProfile extends OfflinePlayerProfile{
	private final Player player;
	
	public OnlinePlayerProfile(Player player) {
		super(player);
		this.player = player;
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
	
	/**
	 *
	 * @return all shop types the player is allowed to build
	 */
	private List<ShopType> getBuildableShopTypes() {
		return getBuildableShopTypes(player);
	}
	
}
