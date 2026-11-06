package com.wonkglorg.minecraft.shop.shop.transaction.party;

import java.util.List;

/**
 * Represents a result of an item removal action for a given {@link TransactionParty}
 * @param remaining remaining items that could not be removed from the party
 * @param removedItems a list of all removed items
 */
public record ItemRemovalResult(int remaining, List<RemovedItem> removedItems){
	/**
	 * @return true if all items could be removed
	 */
	public boolean success() {
		return remaining == 0;
	}
	
}
