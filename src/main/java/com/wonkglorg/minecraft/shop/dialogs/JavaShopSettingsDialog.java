package com.wonkglorg.minecraft.shop.dialogs;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.settings.Settings;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JavaShopSettingsDialog{
	
	public static void openShopSettings(Player player, AbstractShop shop) {
		
		boolean notifyStock = shop.getSetting(Settings.OUT_OF_STOCK_NOTIFICATION);
		
		boolean notifyTransactions = shop.getSetting(Settings.TRANSACTION_NOTIFICATION);
		
		boolean itemUpdater = shop.getSetting(Settings.ITEM_UPDATER);
		
		int purchaseLimit = shop.getSetting(Settings.PURCHASE_LIMIT);
		
		long purchaseCooldownMillis = shop.getSetting(Settings.PURCHASE_COOLDOWN);
		
		long purchaseCooldownSeconds = purchaseCooldownMillis / 1000L;
		
		List<DialogInput> inputs = new ArrayList<>();
		
		if(Settings.OUT_OF_STOCK_NOTIFICATION.isEnabled()){
			inputs.add(DialogInput.bool("notify_stock", Component.text("Notify me when shop can't transact"), notifyStock, "On", "Off"));
		}
		
		if(Settings.TRANSACTION_NOTIFICATION.isEnabled()){
			inputs.add(DialogInput.bool("notify_transactions", Component.text("Notify me about transactions"), notifyTransactions, "On", "Off"));
		}
		//leave disabled until fully implemented
		/*
		if(Settings.ITEM_UPDATER.isEnabled()){
			inputs.add(DialogInput.bool("item_updater", Component.text("Update custom items"), itemUpdater, "On", "Off"));
		}
		
		 */
		
		if(Settings.PURCHASE_LIMIT.isEnabled()){
			inputs.add(DialogInput.text("purchase_limit",
					200,
					Component.text("Transaction limit (per Player)"),
					true,
					String.valueOf(purchaseLimit),
					10,
					null));
		}
		
		if(Settings.PURCHASE_COOLDOWN.isEnabled()){
			inputs.add(DialogInput.text("purchase_cooldown",
					200,
					Component.text("Transaction cooldown (seconds) per Player"),
					true,
					String.valueOf(purchaseCooldownSeconds),
					10,
					null));
		}
		
		Dialog dialog = Dialog.create(builder -> {
			builder.empty().base(DialogBase.builder(Component.text("Shop Settings")).body(List.of(DialogBody.plainMessage(Component.text(
					"Configure your shop settings below.")))).inputs(inputs).build()).type(DialogType.notice(ActionButton.builder(Component.text(
					"Save")).action(DialogAction.customClick((view, audience) -> {
				if(!(audience instanceof Player target)){
					return;
				}
				
				Boolean newNotifyStock = view.getBoolean("notify_stock");
				
				Boolean newNotifyTransactions = view.getBoolean("notify_transactions");
				//leave disabled until fully implemented
						/*
				Boolean newItemUpdater = view.getBoolean("item_updater");
						 */
				String newPurchaseLimit = view.getText("purchase_limit");
				
				String newPurchaseCooldown = view.getText("purchase_cooldown");
				
				ShopSettingsDialog.updateShopSettings(shop,
						newNotifyStock,
						newNotifyTransactions,
						itemUpdater,
						newPurchaseLimit,
						newPurchaseCooldown);
			}, ClickCallback.Options.builder().uses(1).build())).build()));
		});
		player.showDialog(dialog);
	}
}