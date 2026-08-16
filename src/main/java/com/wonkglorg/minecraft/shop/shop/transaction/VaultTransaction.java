package com.wonkglorg.minecraft.shop.shop.transaction;

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
	public double getBuyerAvailableFunds() {
		return buyer.getAvailableVaultFunds();
	}
	
	@Override
	public boolean canBuyerAcceptPayment() {
		return buyer.canAcceptVaultPayment(amount);
	}
	
	@Override
	public double getSellerAvailableFunds() {
		return seller.getAvailableVaultFunds();
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		return seller.canAcceptVaultPayment(price);
	}
	
	@Override
	public void execute() {
		buyer.removeCurrency(price);
		buyer.addItem(tradedStack, amount);
		
		seller.addCurrency(price);
		seller.removeItem(tradedStack, amount);
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
