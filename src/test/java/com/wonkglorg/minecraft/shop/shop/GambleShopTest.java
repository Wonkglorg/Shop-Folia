package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.TestBase;
import com.wonkglorg.minecraft.shop.mocks.GambleShopMock;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemCurrencyTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class GambleShopTest extends TestBase implements ShopTestable{
	private final ShopType type = ShopType.GAMBLE;
	private final int amount = 1;
	private final double price = 1.0;
	
	@Test
	public void canCreateShop() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		var shop = createGambleShop(price, amount);
		shop.getInventory().addItem(mainStack);
		shop.updateStock();
		assertSame(ShopType.GAMBLE, shop.getType());
		assertEquals(1, shop.amount);
		assertEquals(1.0, shop.price);
		assertTrue(shop.isAdmin);
		assertEquals(mainStack, shop.getItemStack());
	}
	
	//both should behave the same
	@Test
	public void testShopStates() {
		testShopStatesAdmin();
	}
	
	@Test
	public void testShopStatesAdmin() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		var shop = createGambleShop(price, amount);
		shop.getInventory().addItem(mainStack);
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
		shop.getInventory().addItem(createCurrencyStack(1));
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
		shop.getInventory().clear();
		//admin shops cannot be overfilled as they void the item
		fillInventory(shop.getInventory(), Material.SADDLE);
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
		shop.getInventory().removeItem(new ItemStack(Material.SADDLE, 1));
		shop.getInventory().addItem(createCurrencyStack(64));
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
		shop.getInventory().removeItem(createCurrencyStack(63));
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
	}
	
	@Test
	public void testTransactionTypesBasedOnSetCurrency() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		var shop = createGambleShop(price, amount);
		shop.getInventory().addItem(mainStack);
		shop.updateStock();
		setCurrencyType(CurrencyType.VAULT);
		var party = createTransactionParty();
		var transaction = shop.startTransaction(party, 1);
		assertSame(VaultTransaction.class, transaction.getClass());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertTrue(transaction.isSellerIsAdminShop());
		
		setCurrencyType(CurrencyType.EXPERIENCE);
		transaction = shop.startTransaction(party, 1);
		assertSame(ExpirienceTransaction.class, transaction.getClass());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertTrue(transaction.isSellerIsAdminShop());
		
		setCurrencyType(CurrencyType.ITEM);
		transaction = shop.startTransaction(party, 1);
		assertSame(ItemCurrencyTransaction.class, transaction.getClass());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertTrue(transaction.isSellerIsAdminShop());
	}
	
	protected AbstractShop createGambleShop(double price, int amount) {
		PlayerMock playerMock = server.addPlayer();
		return new GambleShopMock(playerMock.getUniqueId(), shopSignLocation, price, amount, playerMock.getInventory());
	}
}
