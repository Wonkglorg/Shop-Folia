package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import static com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty.removeItemSmallestStacksFirst;
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
	public double getBuyerAvailableItems() {
		return buyer.getAvailableItemFunds(currency);
	}
	
	@Override
	public boolean canBuyerAcceptItems() {
		if(amount == 0){
			return true;
		}
		var inventory = buyer.createVirtualInventory();
		//only check if the price fits in the buyers inv if there is a price
		if(price != 0){
			if(removeItemSmallestStacksFirst(inventory, currency, (int) price) != 0){
				return false;
			}
		}
		var tradeStackClone = tradedStack.clone();
		tradeStackClone.setAmount(amount);
		
		return inventory.addItem(tradeStackClone).isEmpty();
	}
	
	@Override
	public double getSellerAvailableFunds() {
		return seller.getAvailableItemFunds(tradedStack);
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		if(price == 0){
			return true;
		}
		var inventory = seller.createVirtualInventory();
		if(amount != 0){
			if(removeItemSmallestStacksFirst(inventory, tradedStack, amount) != 0){
				return false;
			}
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
