package com.wonkglorg.minecraft.shop.shop;

import static com.wonkglorg.minecraft.shop.shop.ShopState.EMPTY;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OVERFILLED;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

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
			shopState = OK;
			return;
		}
		if(item == null){
			//leave cached value
			return;
		}
		ShopTransactionParty party = getParty();
		
		double availableFunds = party.getAvailableFunds(getCurrencyItem());
		stock = (int) (availableFunds / this.getAmount());
		
		if(stock < 1){
			shopState = EMPTY;
			return;
		}
		
		if(party.canAcceptPayment(item, price)){
			shopState = OK;
			return;
		}
		
		shopState = OVERFILLED;
	}
	
	@Override
	public Transaction startTransaction(TransactionParty party) {
		return switch(getCurrencyType()) {
			case VAULT -> new VaultTransaction(getParty(), party, amount, price, item);
			case ITEM -> new ItemTransaction(getParty(), party, amount, price, item, getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(getParty(), party, amount, price, item);
		};
	}
}
