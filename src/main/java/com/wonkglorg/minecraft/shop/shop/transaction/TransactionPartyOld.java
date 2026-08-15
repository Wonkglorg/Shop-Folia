package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.util.EconomyUtils;
import com.wonkglorg.minecraft.shop.util.InventoryUtils;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TransactionPartyOld{
	// Party - The player that is a party in the transaction, could be the shop owner, or the player clicking the sign
	private OfflinePlayer party;
	
	// Inventory - the party inventory to access for currency/items
	private Inventory inventory;
	
	// The amount the party has to spend, could be vault currency, or from an item currency in their inventory
	private double availableFunds;
	
	// The item being used for currency in a trade (i.e. barter shop)
	private ItemStack currencyItem;
	
	// Are we the player who created the transaction (used for error handling)
	@Getter
	private boolean isPlayer;
	
	// Are we an admin shop
	private boolean isAdmin;
	
	public TransactionPartyOld(boolean isPlayer, boolean isAdmin, OfflinePlayer party, Inventory inventory) {
		this.isPlayer = isPlayer;
		this.isAdmin = isAdmin;
		this.party = party;
		this.inventory = inventory;
	}
	
	// Allow creating a party that uses an item for it's currency/available funds.
	public TransactionPartyOld(boolean isPlayer, boolean isAdmin, OfflinePlayer party, Inventory inventory, ItemStack currencyItem) {
		this.isPlayer = isPlayer;
		this.isAdmin = isAdmin;
		this.party = party;
		this.inventory = inventory;
		this.currencyItem = currencyItem;
	}
	

	
	// Update the amount of currency the player has available (vault/currency item) and return it.

	
	// Check if we have enough space to receive a payment, we might not have the inventory space for it!
	public boolean canAcceptPayment(double paymentAmount) {
		// If we are an admin, then we can accept the payment no matter what
		if(this.isAdmin){
			return true;
		}
		
		if(this.currencyItem != null){
			// We are being paid with an item
			ItemStack payment = this.currencyItem.clone();
			payment.setAmount((int) paymentAmount);
			return InventoryUtils.hasRoom(this.inventory, payment);
		}
		
		// We are being paid through the normal economy
		return EconomyUtils.canAcceptFunds(this.inventory, paymentAmount);
	}
	
	// Receive a payment and add it to the players wallet/inventory
	public void depositFunds(double paymentAmount) {
		// If we are an admin, then we don't deposit any funds
		if(this.isAdmin){
			return;
		}
		
		if(this.currencyItem != null){
			// We are being paid with an item
			ItemStack payment = this.currencyItem.clone();
			payment.setAmount((int) paymentAmount);
			InventoryUtils.addItem(this.inventory, payment);
		} else {
			// We are being paid using our normal currency
			EconomyUtils.addFunds(party, this.inventory, paymentAmount);
		}
	}
	
	// Make a payment for a purchase
	public boolean deductFunds(double paymentAmount) {
		// If we are an admin, then we don't deduct any funds
		if(this.isAdmin){
			return true;
		}
		
		// Check if we have enough funds to make the payment
		if(this.getAvailableFunds() < paymentAmount){
			return false;
		}
		
		// Check if we are being paid using an item instead of currency
		if(this.currencyItem != null){
			ItemStack payment = this.currencyItem.clone();
			payment.setAmount((int) paymentAmount);
			InventoryUtils.removeItem(inventory, payment);
			return true;
		}
		
		// We are being paid using our normal currency
		return EconomyUtils.removeFunds(party, this.inventory, paymentAmount);
	}
	
	// Check if there is space in the inventory to recieve an item
	public boolean hasRoomForItem(ItemStack item) {
		// If we are an admin, then we always have room for the item (since we don't deposit it)
		if(this.isAdmin){
			return true;
		}
		
		return InventoryUtils.hasRoom(this.inventory, item);
	}
	
	public boolean depositItem(ItemStack item) {
		// If we are an admin, then we always have room for the item (since we don't deposit it)
		if(this.isAdmin){
			return true;
		}
		
		// Check if we have room for the item in our inventory
		if(!this.hasRoomForItem(item)){
			return false;
		}
		
		// We have the space, so add the item to our inventory!
		// @TODO: Maybe check how many items were unable to be added to the inv to make sure we actually deposited the item
		InventoryUtils.addItem(inventory, item);
		return true;
	}
	
	public boolean deductItem(ItemStack item) {
		// If we are an admin, then we don't remove the item
		if(this.isAdmin){
			return true;
		}
		
		// @TODO: Maybe check how many items were unable to be removed from the inv to verify tx occured successfully
		InventoryUtils.removeItem(inventory, item);
		return true;
	}
	
	@Override
	public String toString() {
		return "TransactionParty{" + "isPlayer=" + isPlayer + ", funds=" + availableFunds + '}';
	}
}
