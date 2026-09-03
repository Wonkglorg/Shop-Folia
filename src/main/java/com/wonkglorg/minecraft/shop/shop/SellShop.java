package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
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
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class SellShop extends AbstractShop{
	
	public SellShop(UUID shopId,
					Location signLoc,
					UUID player,
					double pri,
					int amt,
					Boolean admin,
					BlockFace facing,
					long creationDate,
					DisplayType type) {
		super(shopId, signLoc, player, ShopType.SELL, pri, amt, admin, facing, creationDate, type);
	}
	
	@SuppressWarnings("DuplicatedCode")
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
		//starts a mock transaction to get accurate data on what the actual trade logic would do
		Transaction transaction = startTransaction(null, 1);
		double availableFunds = transaction.getSellerAvailableFunds();
		stock = (int) (availableFunds / amount);
		
		//if there is no stock and the shop is offering more than 0 items
		if(stock < 1 && amount > 0){
			setShopState(EMPTY, true);
			return;
		}
		
		if(price == 0){
			setShopState(OK, true);
			return;
		}
		//start a test transaction to see if the shop can accept payment
		if(transaction.canSellerAcceptPayment()){
			setShopState(OK, true);
			return;
		}
		
		setShopState(OVERFILLED, true);
	}
	
	public @NonNull Transaction startTransaction(TransactionParty party, int multiplier) {
		int calculatedAmount = amount * multiplier;
		double calculatedPrice = price * multiplier;
		return switch(getCurrencyType()) {
			case VAULT -> new VaultTransaction(party, getParty(), calculatedAmount, calculatedPrice, item);
			case ITEM -> new ItemTransaction(party, getParty(), calculatedAmount, calculatedPrice, item, getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(party, getParty(), calculatedAmount, calculatedPrice, item);
		};
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player) {
		var lang = langManager();
		switch(result) {
			case OK -> notifyTransaction(player,multiplier);
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.sell.shop-performing-transaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.sell.cancelled-external").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> lang.request("transaction.issue.sell.player-no-stock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> notifyNoStock(player, multiplier);
			case INVENTORY_FULL_BUYER -> lang.request("transaction.issue.sell.player-no-space").sendToAudience(player);
			case INVENTORY_FULL_SELLER -> notifyNoSpace(player, multiplier);
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.sell.use-own-shop").sendToAudience(player);
			case PURCHASE_COOLDOWN -> notifyCooldownReached(player,multiplier);
			case PURCHASE_LIMIT_REACHED -> lang.request("transaction.issue.sell.player-transaction-limit-reached").sendToAudience(player);
		}
		
	}
}
