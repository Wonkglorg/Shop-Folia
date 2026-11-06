package com.wonkglorg.minecraft.shop.shop.creation;

public enum CreationValidationResult{
	/**
	 * If the shop creation process cna be completed
	 */
	SUCCESS,
	/**
	 * If no data was provided to the shop creation process, this is a developer error
	 */
	NO_DATA,
	/**
	 * Shops are blacklisted in this dimension
	 */
	DIMENSION_BLACKLIST,
	/**
	 * Player does not have the required creation permission to create this shop
	 */
	LACKS_CREATION_PERMISSION,
	/**
	 * Player does not have permissions to create an admin shop
	 */
	LACKS_ADMIN_CREATION_PERMISSION,
	/**
	 * Player reached their maximum allowed shops
	 */
	SHOP_LIMIT_REACHED,
	/**
	 * If a creation cost is set and the player lacks the funds
	 */
	INSUFFICIENT_FUNDS
	
}
