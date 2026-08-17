package com.wonkglorg.minecraft.shop.shop.transaction;

/**
 * The result of a transaction
 */
public enum TransactionResult{
	OK,
	SHOP_IS_PERFORMING_TRANSACTION,
	CANCELLED,
	INSUFFICIENT_FUNDS_BUYER,
	INSUFFICIENT_FUNDS_SELLER,
	INVENTORY_FULL_BUYER,
	INVENTORY_FULL_SELLER,
	OWNER_CANT_TRANSACT_OWN_SHOP
}
