package com.wonkglorg.minecraft.shop.dialogs;

import static com.wonkglorg.minecraft.shop.ShopPlugin.isBedrockPlayer;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopClientManager;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.settings.Settings;
import org.bukkit.entity.Player;

import java.util.Objects;

//todo: rework this later down the line to not be shit
public class ShopSettingsDialog{
	public static void openDialog(Player player, AbstractShop shop) {
		if(isBedrockPlayer(player.getUniqueId())){
			BedrockShopSettingsDialog.openShopSettings(player, shop);
		} else {
			JavaShopSettingsDialog.openShopSettings(player, shop);
		}
	}
	
	protected static void updateShopSettings(AbstractShop shop,
											 Boolean notifyStock,
											 Boolean notifyTransactions,
											 Boolean itemUpdater,
											 String purchaseLimit,
											 String purchaseCooldown) {
		boolean needsRefresh = false;
		
		if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
			if(!Objects.equals(shop.getSetting(Settings.OUT_OF_STOCK_NOTIFICATION), notifyStock)){
				shop.setSetting(Settings.OUT_OF_STOCK_NOTIFICATION, notifyStock);
			}
		}
		if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
			if(!Objects.equals(shop.getSetting(Settings.TRANSACTION_NOTIFICATION), notifyTransactions)){
				shop.setSetting(Settings.TRANSACTION_NOTIFICATION, notifyTransactions);
			}
		}
		if(Settings.ITEM_UPDATER.isEnabled()){
			if(!Objects.equals(shop.getSetting(Settings.ITEM_UPDATER), itemUpdater)){
				shop.setSetting(Settings.ITEM_UPDATER, itemUpdater);
				needsRefresh = true;
			}
		}
		if(Settings.PURCHASE_LIMIT.isEnabled()){
			int newPurchaseLimit;
			
			try{
				newPurchaseLimit = Integer.parseInt(purchaseLimit);
			} catch(NumberFormatException _){
				return;
			}
			if(newPurchaseLimit >= 0){
				if(shop.getSetting(Settings.PURCHASE_LIMIT) != newPurchaseLimit){
					shop.setSetting(Settings.PURCHASE_LIMIT, newPurchaseLimit);
					needsRefresh = true;
				}
			}
		}
		if(Settings.PURCHASE_COOLDOWN.isEnabled()){
			int newCooldownSeconds;
			
			try{
				newCooldownSeconds = Integer.parseInt(purchaseCooldown);
			} catch(NumberFormatException _){
				return;
			}
			long newCooldownMillis = newCooldownSeconds * 1000L;
			if(newCooldownMillis >= 0){
				if(shop.getSetting(Settings.PURCHASE_COOLDOWN) != newCooldownMillis){
					shop.setSetting(Settings.PURCHASE_COOLDOWN, newCooldownMillis);
					needsRefresh = true;
				}
			}
		}
		if(needsRefresh){
			shopClientManager().updateShop(shop);
		}
	}
}
