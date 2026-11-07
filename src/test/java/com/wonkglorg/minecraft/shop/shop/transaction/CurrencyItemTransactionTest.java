package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.TestBase;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class CurrencyItemTransactionTest extends TestBase{
	
	@Test
	void refuseTransactionIfBuyerHasNoItems() {
		var buyer = createTransactionParty();
		var seller = createTransactionParty();
		var transaction = new ItemCurrencyTransaction(buyer,
				seller,
				1,
				1.0,
				new ItemStack(Material.STONE),
				createCurrencyStack(1),
				itemConfigMock.getCurrencyDenominations());
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_BUYER, result);
	}
	
	@Test
	void refuseTransactionIfSellerHasNoItems() {
		var buyer = createTransactionParty();
		buyer.addItem(createCurrencyStack(1), 1);
		var seller = createTransactionParty();
		var transaction = new ItemCurrencyTransaction(buyer,
				seller,
				1,
				1.0,
				new ItemStack(Material.STONE),
				createCurrencyStack(1),
				itemConfigMock.getCurrencyDenominations());
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_SELLER, result);
	}
	
	@Test
	void allowTransactionIfSellerAndBuyerHaveItems() {
		var buyer = createTransactionParty();
		buyer.addItem(createCurrencyStack(1), 1);
		var seller = createTransactionParty();
		seller.addItem(new ItemStack(Material.STONE), 1);
		var transaction = new ItemCurrencyTransaction(buyer,
				seller,
				1,
				1.0,
				new ItemStack(Material.STONE),
				createCurrencyStack(1),
				itemConfigMock.getCurrencyDenominations());
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
			buyer.addItem(createCurrencyStack(1), scenario.buyerCurrency());
		}
		
		if(scenario.sellerStock() > 0){
			seller.addItem(new ItemStack(Material.STONE), scenario.sellerStock());
		}
		
		var transaction = new ItemCurrencyTransaction(buyer,
				seller,
				scenario.amount(),
				scenario.price(),
				new ItemStack(Material.STONE),
				createCurrencyStack(1),
				itemConfigMock.getCurrencyDenominations());
		assertSame(scenario.expected(), transaction.canFulfill());
		if(scenario.expected != TransactionResult.OK){
			return;
		}
		transaction.execute();
		assertEquals(scenario.buyerCurrency - scenario.price, transaction.getBuyerAvailableFunds());
		assertEquals(scenario.sellerStock - scenario.amount, transaction.getSellerAvailableItems());
	}
	
	private ItemCurrencyTransaction createTransaction(TransactionParty buyer,
													  TransactionParty seller,
													  int amount,
													  double price,
													  ItemStack itemStack) {
		return new ItemCurrencyTransaction(buyer,
				seller,
				amount,
				price,
				itemStack,
				createCurrencyStack(1),
				itemConfigMock.getCurrencyDenominations());
	}
	
	@Test
	void condensedCurrencyExactPayment() {
		var buyer = createTransactionParty();
		buyer.addItem(new ItemStack(Material.DIAMOND_BLOCK), 1);
		
		ItemStack itemStack = new ItemStack(Material.STONE);
		var seller = createTransactionParty();
		seller.addItem(itemStack, 1);
		
		var transaction = createTransaction(buyer, seller, 1, 9, itemStack);
		
		//allowing condensed currencies is off so this should fail as diamond blocks is not a base currency
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_BUYER, transaction.canFulfill());
		setAllowCondenseCurrencyTrades(true);
		assertSame(TransactionResult.OK, transaction.canFulfill());
		
		transaction.execute();
		
		assertEquals(0, transaction.getBuyerAvailableFunds());
		assertEquals(0, transaction.getSellerAvailableItems());
		setAllowCondenseCurrencyTrades(false);
	}
	
	@Test
	void condensedCurrencyExcessPayment() {
		var buyer = createTransactionParty();
		buyer.addItem(new ItemStack(Material.DIAMOND_BLOCK), 1);
		
		ItemStack itemStack = new ItemStack(Material.STONE);
		var seller = createTransactionParty();
		seller.addItem(itemStack, 6);
		
		var transaction = createTransaction(buyer, seller, 1, 5, itemStack);
		
		setAllowCondenseCurrencyTrades(true);
		assertSame(TransactionResult.OK, transaction.canFulfill());
		
		transaction.execute();
		
		assertEquals(4, transaction.getBuyerAvailableFunds());
		assertEquals(5, transaction.getSellerAvailableItems());
		
		setAllowCondenseCurrencyTrades(false);
	}
	
	@Test
	void mixedCurrencyPayment() {
		var buyer = createTransactionParty();
		buyer.addItem(createCurrencyStack(1), 4);
		buyer.addItem(new ItemStack(Material.DIAMOND_BLOCK), 1);
		
		ItemStack itemStack = new ItemStack(Material.STONE);
		var seller = createTransactionParty();
		seller.addItem(itemStack, 1);
		
		var transaction = createTransaction(buyer, seller, 1, 10, itemStack);
		
		setAllowCondenseCurrencyTrades(true);
		assertSame(TransactionResult.OK, transaction.canFulfill());
		
		transaction.execute();
		
		assertEquals(3, transaction.getBuyerAvailableFunds());
		assertEquals(0, transaction.getSellerAvailableItems());
		
		setAllowCondenseCurrencyTrades(false);
	}
	
	@Test
	void condensedCurrencyInsufficientFunds() {
		var buyer = createTransactionParty();
		buyer.addItem(new ItemStack(Material.DIAMOND_BLOCK), 1);
		
		ItemStack itemStack = new ItemStack(Material.STONE);
		var seller = createTransactionParty();
		seller.addItem(itemStack, 1);
		
		var transaction = createTransaction(buyer, seller, 1, 10, itemStack);
		
		setAllowCondenseCurrencyTrades(true);
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_BUYER, transaction.canFulfill());
		
		assertEquals(9, transaction.getBuyerAvailableFunds());
		
		setAllowCondenseCurrencyTrades(false);
	}
}
