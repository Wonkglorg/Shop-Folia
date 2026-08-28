package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

public abstract class Transaction{
	/**
	 * The buyer of the stack in the transaction
	 */
	protected final TransactionParty buyer;
	/**
	 * The seller of the stack in the transaction
	 */
	protected final TransactionParty seller;
	/**
	 * The currency price to pay for the transacted stack
	 */
	@Getter
	protected final double price;
	/**
	 * The amount of the stack being traded
	 */
	@Getter
	protected final int amount;
	/**
	 * The stack being traded
	 */
	protected final ItemStack tradedStack;
	
	/**
	 * The last evaluated result from {@link #canFulfill()}
	 */
	@Getter
	private TransactionResult result;
	
	/**
	 * Represents a transaction between 2 parties
	 *
	 * @param buyer the party buying an itemstack for the servers provided currency
	 * @param seller the party selling the itemstack
	 * @param amount how much of the itemstack to buy
	 * @param price the price of the transaction
	 * @param tradedStack the item being traded
	 */
	protected Transaction(TransactionParty buyer, TransactionParty seller, int amount, double price, ItemStack tradedStack) {
		this.buyer = buyer;
		this.seller = seller;
		this.amount = amount;
		this.price = price;
		this.tradedStack = tradedStack;
	}
	
	/**
	 * Get the funds the buyer has access to in this transaction
	 */
	public abstract double getBuyerAvailableItems();
	
	/**
	 * If the buyer can accept the items in this transaction
	 */
	public boolean canBuyerAcceptItems() {
		if(amount == 0){
			return true;
		}
		var inventory = buyer.createVirtualInventory();
		var tradeStackClone = tradedStack.clone();
		tradeStackClone.setAmount(amount);
		return inventory.addItem(tradedStack).isEmpty();
	}
	
	/**
	 * Get the funds the seller has access to in this transaction
	 */
	public abstract double getSellerAvailableFunds();
	
	/**
	 * If the seller can accept payments in this transaction
	 */
	public abstract boolean canSellerAcceptPayment();
	
	/**
	 * Executes the transaction, this does not confirm if the transaction would be valid and does not return feedback use {@link #canFulfill()} to verify this transaction can be executed
	 */
	public abstract void execute();
	
	/**
	 * Verifies if the transaction can be made
	 *
	 * @return the result of the possible transaction
	 */
	public TransactionResult canFulfill() {
		if(getBuyerAvailableItems() < price){
			return result = TransactionResult.INSUFFICIENT_FUNDS_BUYER;
		}
		
		if(getSellerAvailableFunds() < amount){
			return result = TransactionResult.INSUFFICIENT_FUNDS_SELLER;
		}
		
		if(!canBuyerAcceptItems()){
			return result = TransactionResult.INVENTORY_FULL_BUYER;
		}
		
		if(!canSellerAcceptPayment()){
			return result = TransactionResult.INVENTORY_FULL_SELLER;
			
		}
		
		return result = TransactionResult.OK;
	}
	
	@Override
	public String toString() {
		return "Transaction{" +
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
