package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.TestBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ItemTransactionTest extends TestBase{
	
	@Test
	void refuseTransactionIfBuyerHasNoItems() {
		var buyer = createTransactionParty();
		var seller = createTransactionParty();
		var transaction = new ItemTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE), createCurrencyStack(1));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_BUYER, result);
	}
	
	@Test
	void refuseTransactionIfSellerHasNoItems() {
		var buyer = createTransactionParty();
		buyer.addItem(createCurrencyStack(1), 1);
		var seller = createTransactionParty();
		var transaction = new ItemTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE), createCurrencyStack(1));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_SELLER, result);
	}
	
	@Test
	void allowTransactionIfSellerAndBuyerHaveItems() {
		var buyer = createTransactionParty();
		buyer.addItem(createCurrencyStack(1), 1);
		var seller = createTransactionParty();
		seller.addItem(new ItemStack(Material.STONE), 1);
		var transaction = new ItemTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE), createCurrencyStack(1));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.OK, result);
	}
	
	private static final int FULL_INVENTORY = 36 * 64;
	
	record FulfillmentScenario(int buyerCurrency, int sellerStock, int amount, double price, TransactionResult expected){}
	
	static Stream<Arguments> fulfillmentScenarios() {
		return Stream.of(
				// FUNDS
				Arguments.of(new FulfillmentScenario(0, 10, 1, 1, TransactionResult.INSUFFICIENT_FUNDS_BUYER)),
				Arguments.of(new FulfillmentScenario(1, 10, 1, 1, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(10, 10, 1, 5, TransactionResult.OK)),
				
				// STOCK
				Arguments.of(new FulfillmentScenario(10, 0, 1, 1, TransactionResult.INSUFFICIENT_FUNDS_SELLER)),
				Arguments.of(new FulfillmentScenario(10, 1, 1, 1, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(10, 1, 2, 1, TransactionResult.INSUFFICIENT_FUNDS_SELLER)),
				Arguments.of(new FulfillmentScenario(10, 64, 64, 1, TransactionResult.OK)),
				
				// BUYER INVENTORY CAPACITY
				Arguments.of(new FulfillmentScenario(FULL_INVENTORY, 1, 1, 1, TransactionResult.INVENTORY_FULL_BUYER)),
				Arguments.of(new FulfillmentScenario(FULL_INVENTORY, 1, 1, 64, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(FULL_INVENTORY, 1, 1, 63, TransactionResult.INVENTORY_FULL_BUYER)),
				Arguments.of(new FulfillmentScenario(FULL_INVENTORY - 63, 63, 63, 1, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(FULL_INVENTORY - 62, 64, 64, 1, TransactionResult.INVENTORY_FULL_BUYER)),
				
				// SELLER INVENTORY CAPACITY
				Arguments.of(new FulfillmentScenario(1, FULL_INVENTORY, 1, 1, TransactionResult.INVENTORY_FULL_SELLER)),
				Arguments.of(new FulfillmentScenario(64, FULL_INVENTORY, 64, 64, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(65, FULL_INVENTORY, 64, 65, TransactionResult.INVENTORY_FULL_SELLER)),
				Arguments.of(new FulfillmentScenario(64, FULL_INVENTORY - 63, 64, 64, TransactionResult.OK)),
				Arguments.of(new FulfillmentScenario(65, FULL_INVENTORY - 63, 64, 65, TransactionResult.INVENTORY_FULL_SELLER)));
	}
	
	@ParameterizedTest
	@MethodSource("fulfillmentScenarios")
	void canFulfillScenario(FulfillmentScenario scenario) {
		var buyer = createTransactionParty();
		var seller = createTransactionParty();
		
		if(scenario.buyerCurrency() > 0){
			buyer.addItem(new ItemStack(Material.GRASS_BLOCK), scenario.buyerCurrency());
		}
		
		if(scenario.sellerStock() > 0){
			seller.addItem(new ItemStack(Material.STONE), scenario.sellerStock());
		}
		
		var transaction = new ItemTransaction(buyer,
				seller,
				scenario.amount(),
				scenario.price(),
				new ItemStack(Material.STONE),
				new ItemStack(Material.GRASS_BLOCK));
		assertSame(scenario.expected(), transaction.canFulfill());
		if(scenario.expected != TransactionResult.OK){
			return;
		}
		transaction.execute();
		assertEquals(scenario.buyerCurrency - scenario.price, transaction.getBuyerAvailableFunds());
		assertEquals(scenario.sellerStock - scenario.amount, transaction.getSellerAvailableItems());
	}
}
