package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.inventory.ItemStack;

/**
 * Transaction using experience as the currency
 */
public class ExpirienceTransaction extends Transaction{
	public ExpirienceTransaction(TransactionParty buyer, TransactionParty seller, int amount, double price, ItemStack tradedStack) {
		super(buyer, seller, amount, price, tradedStack);
	}
	
	@Override
	public double getBuyerAvailableFunds() {
		return buyer.getAvailableExperienceFunds();
	}
	
	@Override
	public boolean canBuyerAcceptPayment() {
		return buyer.canAcceptExperiencePayment(amount);
	}
	
	@Override
	public double getSellerAvailableFunds() {
		return seller.getAvailableExperienceFunds();
	}
	
	@Override
	public boolean canSellerAcceptPayment() {
		return seller.canAcceptExperiencePayment(price);
	}
	
	@Override
	public void execute() {
		buyer.removeExperience((int) price);
		buyer.addItem(tradedStack, amount);
		
		seller.addExperience((int) price);
		seller.removeItem(tradedStack, amount);
	}
	
	@Override
	public String toString() {
		return "ExpirienceTransaction{" +
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
