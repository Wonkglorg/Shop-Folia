package com.wonkglorg.minecraft.shop.shop.transaction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PlayerTransactionParty extends TransactionParty{
	
	protected PlayerTransactionParty(Player player, Inventory inventory) {
		super(player, inventory);
	}
	
	@Override
	public double getAvailableFunds() {
		return getFunds(player, inventory);
	}
	
	@Override
	public boolean canAcceptPayment(double amount) {
		return canAcceptFunds(inventory, amount);
	}
	
}
