package com.wonkglorg.minecraft.shop.dialogs;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.settings.Settings;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public final class BedrockShopSettingsDialog{
	
	private BedrockShopSettingsDialog() {
	}
	
	public static void openShopSettings(Player player, AbstractShop shop) {
		
		UUID uuid = player.getUniqueId();
		
		boolean notifyStock = shop.getSetting(Settings.OUT_OF_STOCK_NOTIFICATION);
		
		boolean notifyTransactions = shop.getSetting(Settings.TRANSACTION_NOTIFICATION);
		
		boolean itemUpdater = shop.getSetting(Settings.ITEM_UPDATER);
		
		int purchaseLimit = shop.getSetting(Settings.PURCHASE_LIMIT);
		
		long purchaseCooldownMillis = shop.getSetting(Settings.PURCHASE_COOLDOWN);
		
		long purchaseCooldownSeconds = purchaseCooldownMillis / 1000L;
		
		CustomForm.Builder form = CustomForm.builder().title("Shop Settings").label("Configure your shop settings below.");
		
		if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
			form.toggle("Notify me when shop can't transact", notifyStock);
		}
		
		if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
			form.toggle("Notify me about transactions", notifyTransactions);
		}
		//leave disabled until fully implemented
				/*
		if(Settings.ITEM_UPDATER.isEnabled()){
			form.toggle("Update custom items", itemUpdater);
		}
		
				 */
		
		if(Settings.PURCHASE_LIMIT.isEnabled()){
			form.input("Transaction limit (per Player)", "", String.valueOf(purchaseLimit));
		}
		
		if(Settings.PURCHASE_COOLDOWN.isEnabled()){
			form.input("Transaction cooldown (seconds) per Player", "", String.valueOf(purchaseCooldownSeconds));
		}
		
		form.validResultHandler(response -> {
			
			Boolean newNotifyStock = notifyStock;
			
			if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
				newNotifyStock = response.next();
				
				if(newNotifyStock == null){
					return;
				}
			}
			
			Boolean newNotifyTransactions = notifyTransactions;
			
			if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
				newNotifyTransactions = response.next();
				
				if(newNotifyTransactions == null){
					return;
				}
			}
			
			Boolean newItemUpdater = itemUpdater;
			//leave disabled until fully implemented
					/*
			if(Settings.ITEM_UPDATER.isEnabled()){
				newItemUpdater = response.next();
				
				if(newItemUpdater == null){
					return;
				}
			}
			
					 */
			
			String newPurchaseLimit = String.valueOf(purchaseLimit);
			
			if(Settings.PURCHASE_LIMIT.isEnabled()){
				newPurchaseLimit = response.next();
				
				if(newPurchaseLimit == null){
					return;
				}
			}
			
			String newPurchaseCooldown = String.valueOf(purchaseCooldownMillis / 1000L);
			
			if(Settings.PURCHASE_COOLDOWN.isEnabled()){
				newPurchaseCooldown = response.next();
				
				if(newPurchaseCooldown == null){
					return;
				}
			}
			
			ShopSettingsDialog.updateShopSettings(shop, newNotifyStock, newNotifyTransactions, newItemUpdater, newPurchaseLimit, newPurchaseCooldown);
		});
		
		form.closedOrInvalidResultHandler(() -> {
			// Player closed the form or sent an invalid response.
		});
		
		GeyserApi.api().sendForm(uuid, form.build());
	}
	
}