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
		return buyer.getAvailableItemFunds(currency);
	}
	
	@Override
	public boolean canBuyerAcceptPayment() {
		var inventory = buyer.createVirtualInventory();
		
		var cloneCurrency = currency.clone();
		cloneCurrency.setAmount((int) price);
		
		if(!inventory.removeItemAnySlot(cloneCurrency).isEmpty()){
			return false;
		}
		var tradeStackClone = tradedStack.clone();
		tradeStackClone.setAmount(amount);
		
		return inventory.addItem(tradedStack).isEmpty();
	}
	
	@Override
	public double getSellerAvailableFunds() {
		return seller.getAvailableItemFunds(tradedStack);
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		var inventory = seller.createVirtualInventory();
		
		var tradeStackClone = tradedStack.clone();
		tradeStackClone.setAmount(amount);
		
		if(!inventory.removeItemAnySlot(tradeStackClone).isEmpty()){
			return false;
		}
		
		var cloneCurrency = currency.clone();
		cloneCurrency.setAmount((int) price);
		
		return inventory.addItem(cloneCurrency).isEmpty();
	}
	
	@Override
	public void execute() {
		buyer.removeItem(currency, (int) price);
		seller.removeItem(tradedStack, amount);
		
		buyer.addItem(tradedStack, amount);
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
