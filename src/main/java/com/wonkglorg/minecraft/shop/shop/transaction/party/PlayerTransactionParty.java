package com.wonkglorg.minecraft.shop.shop.transaction.party;

import org.bukkit.entity.Player;

/**
 * A party represented by a player directly
 */
public class PlayerTransactionParty extends TransactionParty{
	
	public PlayerTransactionParty(Player player) {
		super(player, player.getInventory());
	}
	
	@Override
	public String toString() {
		return "PlayerTransactionParty{uuid=" + player.getUniqueId() + ", name=" + player.getName() + '}';
	}
}
