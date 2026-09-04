package com.wonkglorg.minecraft.shop.shop;

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
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class BuyShop extends AbstractShop{
	
	public BuyShop(UUID shopId,
				   Location signLoc,
				   UUID player,
				   double pri,
				   int amt,
				   Boolean admin,
				   BlockFace facing,
				   long creationDate,
				   DisplayType type) {
		super(shopId, signLoc, player, ShopType.BUY, pri, amt, admin, facing, creationDate, type);
	}
	
	@Override
	protected void calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			setShopState(OK, true);
			return;
		}
		if(item == null){
			//leave cached value
			return;
		}
		
		assert amount > 0;
		
		Transaction transaction = startTransaction(null, 1);
		
		//if its free we can do infinite trades as long as space is available
		boolean canBuyerAcceptItems = transaction.canBuyerAcceptItems();
		if(price == 0){
			stock = Integer.MAX_VALUE;
			if(canBuyerAcceptItems){
				setShopState(OK, true);
			} else {
				setShopState(OVERFILLED, true);
			}
			return;
		} else {
			double availableFunds = transaction.getBuyerAvailableFunds();
			stock = (int) (availableFunds / this.getPrice());
		}
		
		if(stock == 0){
			setShopState(EMPTY, true);
			return;
		}
		
		if(canBuyerAcceptItems){
			setShopState(OK, true);
			return;
		}
		
		setShopState(OVERFILLED, true);
	}
	
	@Override
	protected @NotNull Transaction startTransaction(TransactionParty party, ShopTransactionParty cachedParty, int multiplier) {
		int calculatedAmount = amount * multiplier;
		double calculatedPrice = price * multiplier;
		return switch(getCurrencyType()) {
			case VAULT -> new VaultTransaction(cachedParty, party, calculatedAmount, calculatedPrice, item);
			case ITEM -> new ItemTransaction(cachedParty, party, calculatedAmount, calculatedPrice, item, getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(cachedParty, party, calculatedAmount, calculatedPrice, item);
		};
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player) {
		var lang = langManager();
		switch(result) {
			case OK -> notifyTransaction(player, multiplier);
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.buy.shop-performing-transaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.buy.cancelled-external").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> lang.request("transaction.issue.buy.player-no-stock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> notifyNoStock(player, multiplier);
			case INVENTORY_FULL_SELLER -> lang.request("transaction.issue.buy.player-no-space").sendToAudience(player);
			case INVENTORY_FULL_BUYER -> notifyNoSpace(player, multiplier);
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.buy.use-own-shop").sendToAudience(player);
			case PURCHASE_COOLDOWN -> notifyCooldownReached(player, multiplier);
			case PURCHASE_LIMIT_REACHED -> lang.request("transaction.issue.buy.player-transaction-limit-reached").sendToAudience(player);
		}
		
	}
}
