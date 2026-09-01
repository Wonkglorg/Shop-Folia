package com.wonkglorg.minecraft.shop.shop.settings;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;

/**
 * Settings each {@link com.wonkglorg.minecraft.shop.shop.AbstractShop} can define
 */
public final class Settings{
	
	/**
	 * Cooldown applied after each purchase (per player)
	 */
	public static final Setting<Long> PURCHASE_COOLDOWN = new Setting<>("purchase-cooldown",
			Long.class,
			Long::parseLong,
			shopSettings()::getTransactionCooldownDefault,
			shopSettings()::isTransactionCooldownEnabled);
	/**
	 * Limit on how often a player can buy from this shop before permanently being unavailable
	 */
	public static final Setting<Integer> PURCHASE_LIMIT = new Setting<>("purchase-limit",
			Integer.class,
			Integer::parseInt,
			shopSettings()::getTransactionLimitDefault,
			shopSettings()::isTransactionLimitEnabled);
	/**
	 * If the shop should inform the shop owner if it is out of stock
	 */
	public static final Setting<Boolean> OUT_OF_STOCK_NOTIFICATION = new Setting<>("out-of-stock-notification",
			Boolean.class,
			Boolean::parseBoolean,
			shopSettings()::isOutOfStockNotificationDefault,
			shopSettings()::isOutOfStockNotificationEnabled);
	/**
	 * If the shop owner should be notified about this shop doing transaction
	 */
	public static final Setting<Boolean> TRANSACTION_NOTIFICATION = new Setting<>("transaction-notification",
			Boolean.class,
			Boolean::parseBoolean,
			shopSettings()::isTransactionNotificationDefault,
			shopSettings()::isTransactionNotificationEnabled);
	
	private static SettingsConfig.ShopSettings shopSettings() {
		return Main.getPlugin().getSettingsConfig().getShopSettings();
	}
}