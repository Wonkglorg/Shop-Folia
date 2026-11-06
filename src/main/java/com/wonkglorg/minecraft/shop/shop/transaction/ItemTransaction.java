package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.ItemRemovalResult;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import static com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty.removeItem;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiPredicate;

/**
 * Transaction using an item as the currency
 */
public class ItemTransaction extends Transaction{
	/**
	 * The currency being used for this item transaction
	 */
	protected final ItemStack currency;
	/**
	 * The matcher to use when comparing items equality
	 */
	protected final BiPredicate<ItemStack, ItemStack> currencyMatcher;
	
	public ItemTransaction(TransactionParty buyer, TransactionParty seller, int amount, double price, ItemStack tradedStack, ItemStack currency) {
		super(buyer, seller, amount, price, tradedStack);
		this.currency = currency;
		currencyMatcher = getMatcherForItem(currency.getType());
	}
	
	public ItemTransaction(TransactionParty buyer,
						   TransactionParty seller,
						   int amount,
						   double price,
						   ItemStack tradedStack,
						   ItemStack currency,
						   BiPredicate<ItemStack, ItemStack> currencyMatcher) {
		super(buyer, seller, amount, price, tradedStack);
		this.currency = currency;
		this.currencyMatcher = currencyMatcher;
	}
	
	@Override
	public double getBuyerAvailableFunds() {
		return buyer.getAvailableItemFunds(currency, currencyMatcher);
	}
	
	@Override
	public boolean canBuyerAcceptItems() {
		if(amount == 0){
			return true;
		}
		if(buyerIsAdminShop){
			return true;
		}
		var inventory = buyer.createVirtualInventory();
		//only check if the price fits in the buyers inv if there is a price
		if(price != 0){
			if(!removeItem(inventory, currency, (int) price, currencyMatcher).success()){
				return false;
			}
		}
		var tradeStackClone = tradedStack.clone();
		tradeStackClone.setAmount(amount);
		
		return inventory.addItem(tradeStackClone).isEmpty();
	}
	
	@Override
	public double getSellerAvailableItems() {
		return seller.getAvailableItemFunds(tradedStack, itemMatcher);
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		if(price == 0){
			return true;
		}
		//admin shops do not care about any constraints
		if(sellerIsAdminShop){
			return true;
		}
		var inventory = seller.createVirtualInventory();
		if(amount != 0){
			if(!removeItem(inventory, tradedStack, amount, itemMatcher).success()){
				return false;
			}
		}
		
		var cloneCurrency = currency.clone();
		cloneCurrency.setAmount((int) price);
		
		return inventory.addItem(cloneCurrency).isEmpty();
	}
	
	@Override
	public void execute() {
		ItemRemovalResult currencyRemoveResult = buyer.removeItem(currency, (int) price, currencyMatcher);
		ItemRemovalResult itemRemovalResult = seller.removeItem(tradedStack, amount, itemMatcher);
		
		buyer.addItems(itemRemovalResult);
		seller.addItems(currencyRemoveResult);
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
