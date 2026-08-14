package com.wonkglorg.minecraft.shop.handler;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.PlaceholderContext;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import static com.wonkglorg.minecraft.shop.util.ShopMessage.request;
import com.wonkglorg.minecraft.shop.util.Transaction;
import com.wonkglorg.minecraft.shop.util.TransactionError;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class TransactionHandler{
	
	private final Shop plugin;
	private final LangManager lang;
	private HashMap<Location, UUID> shopMessageCooldown = new HashMap<>(); //shop location, shop owner
	
	public TransactionHandler(Shop instance) {
		plugin = instance;
		lang = plugin.getLangManager();
	}
	
	public void executeTransactionFromEvent(PlayerInteractEvent event, AbstractShop shop, boolean fullStackOrder) {
		Player player = event.getPlayer();
		
		if(shop.isPerformingTransaction()){
			LangRequest request = lang.request("interaction_issue.useShopAlreadyInUse");
			AbstractShop.shopPlaceholders(request, shop);
			request.sendToAudience(player);
			event.setCancelled(true);
			return;
		}
		
		//delete shop if it does not have a chest attached to it
		if(!(plugin.getShopmanager().isAllowedContainer(shop.getContainerLocation().getBlock()))){
			plugin.getLogger().warning("Deleting Shop because chest does not exist! " + shop);
			plugin.getShopmanager().unregisterShop(shop);
			return;
		}
		
		//do not allow gamble shops to have full stack orders
		if(shop.getType() == ShopType.GAMBLE){
			fullStackOrder = false;
		}
		
		//player did not click their own shop
		if(!shop.getOwnerUUID().equals(player.getUniqueId()) || Shop.getPlugin().getSettingsConfig().isDebugAllowUseOwnShop()){
			if(!PlayerProfile.isAllowedToUseShop(player, shop.getType())){
				LangRequest request = lang.request("permission.error.use");
				AbstractShop.shopPlaceholders(request, shop);
				request.sendToAudience(player);
				return;
			}
			//for COMBO shops, shops can execute either a BUY or a SELL depending on the side of sign that was clicked
			if(shop.getType() == ShopType.COMBO){
				int clickedSide = UtilMethods.calculateSideFromClickedSign(player, event.getClickedBlock());
				//clicked left side of sign
				if(clickedSide >= 0){
					if(plugin.getSettingsConfig().isInverseComboShops()){
						executeTransactionSequence(player, shop, ShopType.SELL, fullStackOrder);
					} else {
						executeTransactionSequence(player, shop, ShopType.BUY, fullStackOrder);
					}
				}
				//clicked right side of sign
				else {
					if(plugin.getSettingsConfig().isInverseComboShops()){
						executeTransactionSequence(player, shop, ShopType.BUY, fullStackOrder);
					} else {
						executeTransactionSequence(player, shop, ShopType.SELL, fullStackOrder);
					}
				}
			} else {
				executeTransactionSequence(player, shop, shop.getType(), fullStackOrder);
			}
		} else {
			LangRequest request = lang.request("interaction_issue.useOwnShop");
			AbstractShop.shopPlaceholders(request, shop);
			request.sendToAudience(player);
			shop.sendEffects(false, player);
		}
		event.setCancelled(true);
	}
	
	private void executeTransactionSequence(Player player, AbstractShop shop, ShopType actionType, boolean fullStackOrder) {
		Transaction transaction = new Transaction(player, shop, actionType);
		
		// Set the desired purchase amount if we are a full stack order
		if(fullStackOrder){
			// Use either 64 (Minecraft's standard "full stack") or the shop's amount, whichever is larger
			// This ensures players get 64 items when buying from shops with smaller default amounts
			// But still get the full amount from shops configured to sell larger quantities
			transaction.negotiatePurchase(Math.max(shop.getAmount(), 64));
		} else {
			transaction.negotiatePurchase();
		}
		
		// Verify the transaction is possible
		TransactionError issue = transaction.verify();
		// If it is possible, go ahead and execute it, extra check just in case there is an issue (shouldn't ever happen, but who knows)
		if(issue == TransactionError.NONE){
			issue = transaction.execute();
		}
		
		// If there was an issue with the transaction, send the error message and bail out early
		if(issue != TransactionError.NONE){
			switch(transaction.getError()) {
				case INSUFFICIENT_FUNDS_SHOP:
					if(!shop.isAdmin()){
						Player owner = shop.getOwner().getPlayer();
						//the shop owner is online
						if(owner != null && notifyOwner(shop) && PlayerManager.getOnlineProfile(player).isNotifyStock()){
							request("transaction_issue." + actionType.toString() + ".ownerNoStock", shop).sendToAudience(player);
						}
						
					}
					request("transaction_issue." + actionType.toString() + ".ownerNoStock", shop).sendToAudience(player);
					return;
				case INSUFFICIENT_FUNDS_PLAYER:
					request("transaction_issue." + actionType.toString() + ".playerNoStock", shop).sendToAudience(player);
					return;
				case INVENTORY_FULL_SHOP:
					if(!shop.isAdmin()){
						Player owner = shop.getOwner().getPlayer();
						//the shop owner is online
						if(owner != null && notifyOwner(shop) && PlayerManager.getOnlineProfile(player).isNotifyStock()){
							request("transaction_issue." + actionType.toString() + ".ownerNoSpace", owner, shop).sendToAudience(player);
						}
						
					}
					request("transaction_issue." + actionType.toString() + ".shopNoSpace", shop).sendToAudience(player);
					return;
				case INVENTORY_FULL_PLAYER:
					request("transaction_issue." + actionType.toString() + ".playerNoSpace", shop).sendToAudience(player);
					return;
			}
			shop.sendEffects(false, player);
			return;
		}
		
		//the transaction has finished and the exchange event has not been cancelled
		sendExchangeMessagesAndLog(shop, player, actionType, transaction);
		shop.sendEffects(true, player);
	}
	
	private void sendExchangeMessagesAndLog(AbstractShop shop, Player player, ShopType transactionType, Transaction transaction) {
		
		double price = transaction.getPrice();
		String message = ShopMessage.getMessageFromOrders(transactionType, "user", price, transaction.getAmount());
		
		if(PlayerManager.getOnlineProfile(player).isNotifyUser() && message != null && !message.isEmpty()){
			ShopMessage.request(message, player, shop).sendToAudience(player);
		}
		
		Player owner = Bukkit.getPlayer(shop.getOwnerUUID());
		if((owner != null) && (!shop.isAdmin())){
			message = ShopMessage.getMessageFromOrders(transactionType, "owner", price, transaction.getAmount());
			if(PlayerManager.getOnlineProfile(player).isNotifyOwner() && message != null && !message.isEmpty()){
				ShopMessage.request(message, PlaceholderContext.of(shop).setPlayer(player)).sendToAudience(owner);
			}
		}
		//todo:mjd is that right or is the transaction already accumulated here? how about partial sales?
		plugin.getShopmanager().getDatabase().logTransaction(shop.getId(), System.currentTimeMillis(), player.getUniqueId(), 1, null);
	}
	
	private boolean notifyOwner(final AbstractShop shop) {
		if(shop.isAdmin()){
			return false;
		}
		if(shopMessageCooldown.containsKey(shop.getSignLocation())){
			return false;
		} else {
			shopMessageCooldown.put(shop.getSignLocation(), shop.getOwnerUUID());
			
			plugin.getFoliaLib().getScheduler().runLater(new BukkitRunnable(){
				@Override
				public void run() {
					if(shop != null){
						if(shopMessageCooldown.containsKey(shop.getSignLocation())){
							shopMessageCooldown.remove(shop.getSignLocation());
						}
					}
					//TODO if shop is null, should you clear the entire cooldown list so that that location isn't messed up?
				}
			}, 2400); //make cooldown 2 minutes
		}
		return true;
	}
}