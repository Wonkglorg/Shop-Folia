package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.TestBase;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemCurrencyTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

public class BarterShopTest extends TestBase implements ShopTestable{
	private final ShopType type = ShopType.BARTER;
	private final int amount = 1;
	private final double price = 1.0;
	
	@Test
	public void canCreateShop() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		ItemStack secondaryStack = new ItemStack(Material.GRASS_BLOCK);
		var shop = createShop(type, price, amount, false, mainStack, secondaryStack);
		assertSame(ShopType.BARTER, shop.getType());
		assertEquals(1, shop.amount);
		assertEquals(1.0, shop.price);
		assertFalse(shop.isAdmin);
		assertEquals(mainStack, shop.getItemStack());
	}
	
	@Test
	public void testShopStates() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		ItemStack secondaryStack = new ItemStack(Material.GRASS_BLOCK);
		var shop = createShop(type, price, amount, false, mainStack, secondaryStack);
		shop.updateStock();
		assertSame(ShopState.EMPTY, shop.getShopState());
		shop.getInventory().addItem(mainStack);
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
		shop.getInventory().clear();
		//empty has priority over overfilled
		fillInventory(shop.getInventory(), Material.SADDLE);
		shop.updateStock();
		assertSame(ShopState.EMPTY, shop.getShopState());
		shop.getInventory().removeItem(new ItemStack(Material.SADDLE, 1));
		shop.getInventory().addItem(new ItemStack(Material.STONE, 64));
		shop.updateStock();
		assertSame(ShopState.OVERFILLED, shop.getShopState());
		shop.getInventory().removeItem(new ItemStack(Material.STONE, 63));
		shop.updateStock();
		assertSame(ShopState.OK, shop.getShopState());
	}
	
	@Test
	public void testShopStatesAdmin() {
		ItemStack mainStack = new ItemStack(Material.STONE);
		ItemStack secondaryStack = new ItemStack(Material.GRASS_BLOCK);
		var shop = createShop(type, price, amount, true, mainStack, secondaryStack);
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
		ItemStack barterStack = new ItemStack(Material.GRASS_BLOCK);
		var shop = createShop(type, price, amount, false, mainStack, barterStack);
		setCurrencyType(CurrencyType.VAULT);
		var party = createTransactionParty();
		var transaction = shop.startTransaction(party, 1);
		assertSame(VaultTransaction.class, transaction.getClass());
		assertEquals(transaction.getSeller().getInventory(), shop.getInventory());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertFalse(transaction.isSellerIsAdminShop());
		
		setCurrencyType(CurrencyType.EXPERIENCE);
		transaction = shop.startTransaction(party, 1);
		assertSame(ExpirienceTransaction.class, transaction.getClass());
		assertEquals(transaction.getSeller().getInventory(), shop.getInventory());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertFalse(transaction.isSellerIsAdminShop());
		
		setCurrencyType(CurrencyType.ITEM);
		transaction = shop.startTransaction(party, 1);
		assertSame(ItemTransaction.class, transaction.getClass());
		assertEquals(transaction.getSeller().getInventory(), shop.getInventory());
		assertEquals(amount, transaction.getAmount());
		assertEquals(price, transaction.getPrice());
		assertEquals(party, transaction.getBuyer());
		assertFalse(transaction.isBuyerIsAdminShop());
		assertFalse(transaction.isSellerIsAdminShop());
	}
}
