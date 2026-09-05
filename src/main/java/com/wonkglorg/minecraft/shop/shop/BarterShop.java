package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.shop.ShopState.EMPTY;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OVERFILLED;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
	protected @NotNull Transaction startTransaction(TransactionParty party, ShopTransactionParty cachedParty, int multiplier) {
		int calculatedAmount = amount * multiplier;
		double calculatedPrice = price * multiplier;
		return switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT -> new VaultTransaction(party, cachedParty, calculatedAmount, calculatedPrice, item);
			case ITEM -> new ItemTransaction(party, cachedParty, calculatedAmount, calculatedPrice, item, secondaryItem);
			case EXPERIENCE -> new ExpirienceTransaction(party, cachedParty, calculatedAmount, calculatedPrice, item);
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
		
		double availableFunds = transaction.getSellerAvailableItems();
		stock = (int) (availableFunds / this.getAmount());
		
		if(stock < 1 && amount > 0){
			setShopState(EMPTY, true);
			return;
		}
		
		if(price == 0){
			setShopState(OK, true);
			return;
		}
		
		if(transaction.canSellerAcceptPayment()){
			setShopState(OK, true);
			return;
		}
		setShopState(OVERFILLED, true);
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player) {
		var lang = langManager();
		switch(result) {
			case OK -> notifyTransaction(player, multiplier);
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.barter.shop-performing-transaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.barter.cancelled-external").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> lang.request("transaction.issue.barter.player-no-stock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> notifyNoStock(player, multiplier);
			case INVENTORY_FULL_BUYER -> lang.request("transaction.issue.barter.player-no-space").sendToAudience(player);
			case INVENTORY_FULL_SELLER -> notifyNoSpace(player, multiplier);
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.barter.use-own-shop").sendToAudience(player);
			case PURCHASE_COOLDOWN -> notifyCooldownReached(player, multiplier);
			case PURCHASE_LIMIT_REACHED -> lang.request("transaction.issue.barter.player-transaction-limit-reached").sendToAudience(player);
		}
		
	}
	
}
