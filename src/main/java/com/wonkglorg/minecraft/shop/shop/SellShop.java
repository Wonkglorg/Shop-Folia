package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class SellShop extends AbstractShop{
	
	public SellShop(UUID shopId,
	                Location signLoc,
	                UUID player,
	                double pri,
	                int amt,
	                Boolean admin,
	                BlockFace facing,
	                long creationDate,
	                DisplayType type) {
		super(shopId, signLoc, player, ShopType.SELL, pri, amt, admin, facing, creationDate, type);
	}
	
	@Override
	public Transaction startTransaction(TransactionParty party) {
		return switch(getCurrencyType()) {
			case VAULT -> new VaultTransaction(party, getParty(), amount, price, item);
			case ITEM -> new ItemTransaction(party, getParty(), amount, price, item, getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(party, getParty(), amount, price, item);
		};
	}
}
