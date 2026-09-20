package com.wonkglorg.minecraft.shop.dialogs;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.settings.Settings;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public final class BedrockShopSettingsDialog{
	
	private BedrockShopSettingsDialog() {}
	
	public static void openShopSettings(Player player, AbstractShop shop) {
		UUID uuid = player.getUniqueId();
		
		boolean notifyStock = shop.getSetting(Settings.OUT_OF_STOCK_NOTIFICATION);
		boolean notifyTransactions = shop.getSetting(Settings.TRANSACTION_NOTIFICATION);
		boolean itemUpdater = shop.getSetting(Settings.ITEM_UPDATER);
		boolean condenseCurrency = shop.getSetting(Settings.CONDENSE_CURRENCY);
		
		int purchaseLimit = shop.getSetting(Settings.PURCHASE_LIMIT);
		long purchaseCooldownSeconds = shop.<Long>getSetting(Settings.PURCHASE_COOLDOWN) / 1000L;
		
		CustomForm.Builder form = CustomForm.builder().title("Shop Settings").label("Configure your shop settings below.");
		
		if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
			form.toggle("Notify me when shop can't transact", notifyStock);
		}
		
		if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
			form.toggle("Notify me about transactions", notifyTransactions);
		}
		
		boolean showCondense = Settings.CONDENSE_CURRENCY.isEnabled() && shop.getType() != ShopType.GAMBLE && shop.getType() != ShopType.BARTER;
		
		if(showCondense){
			form.toggle("Condense Currency inside shop chest", condenseCurrency);
		}
		
		if(Settings.PURCHASE_LIMIT.isEnabled()){
			form.input("Transaction limit (per Player)", "10", String.valueOf(purchaseLimit));
		}
		
		if(Settings.PURCHASE_COOLDOWN.isEnabled()){
			form.input("Transaction cooldown (seconds) per Player", "10", String.valueOf(purchaseCooldownSeconds));
		}
		
		form.validResultHandler(response -> {
			Boolean newNotifyStock = notifyStock;
			if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
				newNotifyStock = response.next();
			}
			
			Boolean newNotifyTransactions = notifyTransactions;
			if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
				newNotifyTransactions = response.next();
			}
			
			Boolean newCondenseCurrency = condenseCurrency;
			if(showCondense){
				newCondenseCurrency = response.next();
			}
			
			String newPurchaseLimit = String.valueOf(purchaseLimit);
			if(Settings.PURCHASE_LIMIT.isEnabled()){
				newPurchaseLimit = response.next();
			}
			
			String newPurchaseCooldown = String.valueOf(purchaseCooldownSeconds);
			if(Settings.PURCHASE_COOLDOWN.isEnabled()){
				newPurchaseCooldown = response.next();
			}
			
			ShopSettingsDialog.updateShopSettings(shop,
					newNotifyStock,
					newNotifyTransactions,
					itemUpdater,
					newPurchaseLimit,
					newPurchaseCooldown,
					newCondenseCurrency);
		});
		
		GeyserApi.api().sendForm(uuid, form.build());
	}
}