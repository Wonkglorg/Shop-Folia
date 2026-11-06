package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.ItemRemovalResult;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.inventory.ItemStack;

/**
 * Transaction using vault as the currency
 */
public class VaultTransaction extends Transaction{
	public VaultTransaction(TransactionParty buyer, TransactionParty seller, int amount, double price, ItemStack tradedStack) {
		super(buyer, seller, amount, price, tradedStack);
	}
	
	@Override
	public double getSellerAvailableItems() {
		return seller.getAvailableItemFunds(tradedStack, itemMatcher);
	}
	
	@Override
	public double getBuyerAvailableFunds() {
		return buyer.getAvailableVaultFunds();
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		if(price == 0){
			return true;
		}
		return seller.canAcceptVaultPayment(price);
	}
	
	@Override
	public void execute() {
		ItemRemovalResult result = seller.removeItem(tradedStack, amount, itemMatcher);
		buyer.removeCurrency(price);
		
		buyer.addItems(result);
		seller.addCurrency(price);
	}
	
	@Override
	public String toString() {
		return "VaultTransaction{" +
			   "buyer=" +
			   buyer +
			   ", seller=" +
			   seller +
			   ", price=" +
			   price +
			   ", amount=" +
			   amount +
			   ", tradedStack=" +
			   tradedStack +
			   '}';
	}
}
