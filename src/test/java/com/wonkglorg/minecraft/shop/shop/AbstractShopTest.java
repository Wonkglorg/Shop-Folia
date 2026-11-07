package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.TestBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Testing static shop functions
 */
public class AbstractShopTest extends TestBase{
	
	@Test
	void condenseCurrencyAtExactAmount() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 9));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK), 1));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND)));
	}
	
	@Test
	void condenseCurrencyMultipleTimes() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 27));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK), 3));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND)));
	}
	
	@Test
	void doNotCondenseInsufficientCurrency() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 8));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND), 8));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK)));
	}
	
	@Test
	void condenseCurrencyWithRemainingItems() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 10));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK), 1));
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND), 1));
	}
	
	@Test
	void doNotCondenseUnrelatedItems() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.STONE, 64));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.STONE), 64));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK)));
	}
	
	@Test
	void condenseCurrencyWithExistingBlocks() {
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND_BLOCK, 2));
		inventory.addItem(new ItemStack(Material.DIAMOND, 9));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK), 3));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND)));
	}
	
	@Test
	void condenseCurrencyMultipleLevelsWithRemainder() {
		addCurrencyDenomination(Material.EMERALD_BLOCK, 81);
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 86));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.EMERALD_BLOCK), 1));
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND), 5));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK)));
		
		removerCurrencyDenomination(81);
	}
	
	@Test
	void condenseCurrencyPartialHigherDenomination() {
		addCurrencyDenomination(Material.EMERALD_BLOCK, 81);
		
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 100));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.EMERALD_BLOCK), 1));
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK), 2));
		assertTrue(inventory.contains(new ItemStack(Material.DIAMOND), 1));
		
		removerCurrencyDenomination(81);
	}
	
	@Test
	void condenseCurrencyThreeLevels() {
		addCurrencyDenomination(Material.EMERALD_BLOCK, 81);
		addCurrencyDenomination(Material.NETHERITE_BLOCK, 729);
		
		var inventory = server.addPlayer().getInventory();
		inventory.addItem(new ItemStack(Material.DIAMOND, 729));
		
		AbstractShop.condenseCurrency(inventory);
		
		assertTrue(inventory.contains(new ItemStack(Material.NETHERITE_BLOCK), 1));
		assertFalse(inventory.contains(new ItemStack(Material.EMERALD_BLOCK)));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND_BLOCK)));
		assertFalse(inventory.contains(new ItemStack(Material.DIAMOND)));
		
		removerCurrencyDenomination(81);
		removerCurrencyDenomination(729);
	}
	
	@Test
	void formatPrice() {
		assertEquals("0", AbstractShop.formatPrice(0.0));
		assertEquals("1", AbstractShop.formatPrice(1.0));
		assertEquals("1.01", AbstractShop.formatPrice(1.01));
		assertEquals("1.1", AbstractShop.formatPrice(1.10));
		assertEquals("1.12", AbstractShop.formatPrice(1.123));
		assertEquals("1.13", AbstractShop.formatPrice(1.126));
		assertEquals("10.99", AbstractShop.formatPrice(10.99));
		assertEquals("100", AbstractShop.formatPrice(100.0));
		assertEquals("0.01", AbstractShop.formatPrice(0.01));
		assertEquals("0.1", AbstractShop.formatPrice(0.1));
	}
	
}
