package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.shop.ShopState.EMPTY;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OVERFILLED;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BarterShop extends AbstractShop{
	
	public BarterShop(UUID shopId,
	                  Location signLoc,
	                  UUID player,
	                  double pri,
	                  int amt,
	                  Boolean admin,
	                  BlockFace facing,
	                  long creationDate,
	                  DisplayType type) {
		super(shopId, signLoc, player, ShopType.BARTER, pri, amt, admin, facing, creationDate, type);
	}
	
	@Override
	public boolean isInitialized() {
		return (item != null && secondaryItem != null);
	}
	
	@Override
	public Transaction startTransaction(TransactionParty party, int multiplier) {
		int calculatedAmount = amount * multiplier;
		double calculatedPrice = price * multiplier;
		return switch(Main.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT -> new VaultTransaction(party, getParty(), calculatedAmount, calculatedPrice, item);
			case ITEM -> new ItemTransaction(party, getParty(), calculatedAmount, calculatedPrice, item, secondaryItem);
			case EXPERIENCE -> new ExpirienceTransaction(getParty(), party, calculatedAmount, calculatedPrice, item);
		};
	}
	
	@Override
	protected void calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			setShopState(OK, true);
			return;
		}
		if(item == null || secondaryItem == null){
			//leave cached value
			return;
		}
		Transaction transaction = startTransaction(null, 1);
		
		double availableFunds = transaction.getSellerAvailableFunds();
		stock = (int) (availableFunds / this.getAmount());
		
		if(stock < 1){
			setShopState(EMPTY, true);
			return;
		}
		
		if(transaction.canSellerAcceptPayment()){
			setShopState(OK, true);
			return;
		}
		setShopState(OVERFILLED, true);
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player, PlayerProfile owner) {
		var lang = Main.getPlugin().getLangManager();
		switch(result) {
			case OK -> {
				LangRequest userRequest = lang.request("transaction.success.BARTER.user");
				shopPlaceholders(userRequest, this, false);
				userRequest.replace("%price%", formatPrice(price * multiplier));
				userRequest.replace("%amount%", amount * multiplier);
				userRequest.sendToAudience(player);
				if(owner.isNotifyOwner() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.success.BARTER.owner").replace("%user%", player.getName());
					shopPlaceholders(ownerRequest, this, false);
					ownerRequest.replace("%price%", formatPrice(price * multiplier));
					ownerRequest.replace("%amount%", amount * multiplier);
					ownerRequest.sendToAudience(online.getPlayer());
				}
			}
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.BARTER.shopPerformingTransaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.BARTER.cancelledExternal").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> lang.request("transaction.issue.BARTER.playerNoStock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> {
				lang.request("transaction.issue.BARTER.shopNoStock").sendToAudience(player);
				if(owner.isNotifyStock() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.issue.BARTER.ownerNoStock");
					shopPlaceholders(ownerRequest, this, false);
					ownerRequest.replace("%user%", player.getName()).sendToAudience(online.getPlayer());
				}
			}
			case INVENTORY_FULL_BUYER -> lang.request("transaction.issue.BARTER.playerNoSpace").sendToAudience(player);
			case INVENTORY_FULL_SELLER -> {
				lang.request("transaction.issue.BARTER.shopNoSpace").sendToAudience(player);
				if(owner.isNotifyStock() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.issue.BARTER.ownerNoSpace");
					shopPlaceholders(ownerRequest, this, false);
					ownerRequest.replace("%user%", player.getName()).sendToAudience(online.getPlayer());
				}
			}
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.BARTER.useOwnShop").sendToAudience(player);
		}
		
	}
	
}
