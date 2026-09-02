package com.wonkglorg.minecraft.shop.dialogs;

import static com.wonkglorg.minecraft.shop.ShopPlugin.shopClientManager;
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

import java.util.List;

public class ShopSettingsDialog{
	
	public static void openShopSettings(Player player, AbstractShop shop) {
		
		boolean notifyStock = shop.getSetting(Settings.OUT_OF_STOCK_NOTIFICATION);
		
		boolean notifyTransactions = shop.getSetting(Settings.TRANSACTION_NOTIFICATION);
		
		boolean itemUpdater = shop.getSetting(Settings.ITEM_UPDATER);
		
		int purchaseLimit = shop.getSetting(Settings.PURCHASE_LIMIT);
		
		long purchaseCooldownMillis = shop.getSetting(Settings.PURCHASE_COOLDOWN);
		
		long purchaseCooldownSeconds = purchaseCooldownMillis / 1000L;
		
		Dialog dialog = Dialog.create(builder -> {
			builder.empty().base(DialogBase.builder(Component.text("Shop Settings")).body(List.of(DialogBody.plainMessage(Component.text(
					"Configure your shop settings below.")))).inputs(List.of(
					
					DialogInput.bool("notify_stock", Component.text("Notify me when shop can't transact"), notifyStock, "On", "Off"),
					
					DialogInput.bool("notify_transactions", Component.text("Notify me about transactions"), notifyTransactions, "On", "Off"),
					
					DialogInput.bool("item_updater", Component.text("Update custom items"), itemUpdater, "On", "Off"),
					
					DialogInput.text("purchase_limit", 200, Component.text("Transaction limit (per Player)"), true, String.valueOf(purchaseLimit), 10, null),
					
					DialogInput.text("purchase_cooldown",
							200,
							Component.text("Transaction cooldown (seconds) per Player"),
							true,
							String.valueOf(purchaseCooldownSeconds),
							10,
							null))).build()).type(DialogType.notice(ActionButton.builder(Component.text("Save"))
																				.action(DialogAction.customClick((view, audience) -> {
																					
																					if(!(audience instanceof Player target)){
																						return;
																					}
																					
																					Boolean newNotifyStock = view.getBoolean("notify_stock");
																					
																					Boolean newNotifyTransactions = view.getBoolean(
																							"notify_transactions");
																					
																					Boolean newItemUpdater = view.getBoolean("item_updater");
																					
																					String newPurchaseLimit = view.getText("purchase_limit");
																					
																					String newPurchaseCooldown = view.getText("purchase_cooldown");
																					
																					if(newNotifyStock == null ||
																					   newNotifyTransactions == null ||
																					   newItemUpdater == null ||
																					   newPurchaseLimit == null ||
																					   newPurchaseCooldown == null){
																						return;
																					}
																					
																					int newLimit;
																					
																					try{
																						newLimit = Integer.parseInt(newPurchaseLimit);
																					} catch(NumberFormatException ignored){
																						return;
																					}
																					
																					long newCooldownSeconds;
																					
																					try{
																						newCooldownSeconds = Long.parseLong(newPurchaseCooldown);
																					} catch(NumberFormatException ignored){
																						return;
																					}
																					
																					if(newLimit < 0 || newCooldownSeconds < 0){
																						return;
																					}
																					boolean needsRefresh = false;
																					
																					if(newNotifyStock != notifyStock){
																						shop.setSetting(Settings.OUT_OF_STOCK_NOTIFICATION,
																								newNotifyStock);
																					}
																					
																					if(newNotifyTransactions != notifyTransactions){
																						shop.setSetting(Settings.TRANSACTION_NOTIFICATION,
																								newNotifyTransactions);
																					}
																					
																					if(newItemUpdater != itemUpdater){
																						shop.setSetting(Settings.ITEM_UPDATER, newItemUpdater);
																						needsRefresh = true;
																					}
																					
																					if(newLimit != purchaseLimit){
																						shop.setSetting(Settings.PURCHASE_LIMIT, newLimit);
																						needsRefresh = true;
																					}
																					
																					long newCooldownMillis = newCooldownSeconds * 1000L;
																					
																					if(newCooldownMillis != purchaseCooldownMillis){
																						shop.setSetting(Settings.PURCHASE_COOLDOWN,
																								newCooldownMillis);
																						needsRefresh = true;
																					}
																					if(needsRefresh){
																						shopClientManager().updateShop(shop);
																					}
																				}, ClickCallback.Options.builder().uses(1).build()))
																				.build()));
		});
		
		player.showDialog(dialog);
	}
}