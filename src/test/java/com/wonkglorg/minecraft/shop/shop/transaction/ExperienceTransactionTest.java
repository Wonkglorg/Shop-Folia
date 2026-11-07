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

public class ExperienceTransactionTest extends TestBase{
	
	@Test
	void refuseTransactionIfBuyerHasNoItems() {
		var buyer = createTransactionParty();
		var seller = createTransactionParty();
		var transaction = new ExpirienceTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_BUYER, result);
	}
	
	@Test
	void refuseTransactionIfSellerHasNoItems() {
		var buyer = createTransactionParty();
		buyer.addExperience(1);
		var seller = createTransactionParty();
		var transaction = new ExpirienceTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.INSUFFICIENT_FUNDS_SELLER, result);
		buyer.removeExperience(1);
	}
	
	@Test
	void allowTransactionIfSellerAndBuyerHaveItems() {
		var buyer = createTransactionParty();
		buyer.addExperience(1);
		var seller = createTransactionParty();
		seller.addItem(new ItemStack(Material.STONE), 1);
		var transaction = new ExpirienceTransaction(buyer, seller, 1, 1.0, new ItemStack(Material.STONE));
		TransactionResult result = transaction.canFulfill();
		assertSame(TransactionResult.OK, result);
		buyer.removeExperience(1);
	}
	
	record VaultFulfillmentScenario(int buyerExperience, int sellerStock, int amount, double price, TransactionResult expected){}
	
	static Stream<Arguments> vaultFulfillmentScenarios() {
		return Stream.of(
				// BUYER FUNDS
				Arguments.of(new VaultFulfillmentScenario(0, 10, 1, 1, TransactionResult.INSUFFICIENT_FUNDS_BUYER)),
				Arguments.of(new VaultFulfillmentScenario(1, 10, 1, 1, TransactionResult.OK)),
				Arguments.of(new VaultFulfillmentScenario(10, 10, 1, 1, TransactionResult.OK)),
				
				// SELLER STOCK
				Arguments.of(new VaultFulfillmentScenario(10, 0, 1, 1, TransactionResult.INSUFFICIENT_FUNDS_SELLER)),
				Arguments.of(new VaultFulfillmentScenario(10, 1, 1, 1, TransactionResult.OK)),
				Arguments.of(new VaultFulfillmentScenario(10, 10, 10, 1, TransactionResult.OK)),
				Arguments.of(new VaultFulfillmentScenario(10, 1, 2, 1, TransactionResult.INSUFFICIENT_FUNDS_SELLER)),
				
				// INDEPENDENT FAILURE CONDITIONS
				Arguments.of(new VaultFulfillmentScenario(0, 0, 1, 1, TransactionResult.INSUFFICIENT_FUNDS_BUYER)));
	}
	
	@ParameterizedTest
	@MethodSource("vaultFulfillmentScenarios")
	void canFulfillScenario(VaultFulfillmentScenario scenario) {
		var buyer = createTransactionParty();
		var seller = createTransactionParty();
		buyer.addExperience(scenario.buyerExperience());
		
		if(scenario.sellerStock() > 0){
			seller.addItem(new ItemStack(Material.STONE), scenario.sellerStock());
		}
		
		var transaction = new ExpirienceTransaction(buyer, seller, scenario.amount(), scenario.price(), new ItemStack(Material.STONE));
		
		assertSame(scenario.expected(), transaction.canFulfill());
		
		if(scenario.expected() != TransactionResult.OK){
			return;
		}
		
		transaction.execute();
		
		assertEquals(scenario.buyerExperience - scenario.price, transaction.getBuyerAvailableFunds());
		assertEquals(scenario.sellerStock - scenario.amount, transaction.getSellerAvailableItems());
	}
}
