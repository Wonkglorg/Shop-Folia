package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.inventory.ItemStack;

/**
 * Transaction using an item as the currency
 */
public class ItemTransaction extends Transaction{
	private final ItemStack currency;
	
	public ItemTransaction(TransactionParty buyer, TransactionParty seller, int amount, double price, ItemStack tradedStack, ItemStack currency) {
		super(buyer, seller, amount, price, tradedStack);
		this.currency = currency;
	}
	
	@Override
	public double getBuyerAvailableFunds() {
		return buyer.getAvailableItemFunds(tradedStack);
	}
	
	@Override
	public boolean canBuyerAcceptPayment() {
		return buyer.canAcceptItemPayment(currency, (int) price);
	}
	
	@Override
	public double getSellerAvailableFunds() {
		return seller.getAvailableItemFunds(currency);
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		return seller.canAcceptItemPayment(tradedStack, amount);
	}
	
	@Override
	public void execute() {
		buyer.removeItem(currency, (int) price);
		buyer.addItem(tradedStack, amount);
		
		seller.removeItem(tradedStack, amount);
		seller.addItem(currency, (int) price);
	}
	
	@Override
	public String toString() {
		return "ItemTransaction{" +
		       "currency=" +
		       currency +
		       ", buyer=" +
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
