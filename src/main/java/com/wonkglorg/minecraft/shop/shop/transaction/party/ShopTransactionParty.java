package com.wonkglorg.minecraft.shop.shop.transaction.party;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A transaction party represented by a shop
 */
public class ShopTransactionParty extends TransactionParty{
	@Getter
	private final AbstractShop shop;
	
	public ShopTransactionParty(AbstractShop shop) {
		super(shop.getOwner(), shop.getInventory());
		this.shop = shop;
	}
	
	/**
	 * Used for shops where a different inventory should be used instead of the shops internal one
	 *
	 * @param shop
	 * @param inventory
	 */
	public ShopTransactionParty(AbstractShop shop, Inventory inventory) {
		super(shop.getOwner(), inventory);
		this.shop = shop;
	}
	
	@Override
	public double getAvailableExperienceFunds() {
		if(shop.isAdmin()){
			return Double.MAX_VALUE;
		}
		return super.getAvailableExperienceFunds();
	}
	
	@Override
	public double getAvailableVaultFunds() {
		if(shop.isAdmin()){
			return Double.MAX_VALUE;
		}
		return super.getAvailableVaultFunds();
	}
	
	@Override
	public int getAvailableItemFunds(ItemStack itemStack) {
		if(shop.isAdmin()){
			return Integer.MAX_VALUE;
		}
		return super.getAvailableItemFunds(itemStack);
	}
	
	@Override
	public boolean canAcceptExperiencePayment(double amount) {
		if(shop.isAdmin()){
			return true;
		}
		return super.canAcceptExperiencePayment(amount);
	}
	
	@Override
	public boolean canAcceptVaultPayment(double amount) {
		if(shop.isAdmin()){
			return true;
		}
		return super.canAcceptVaultPayment(amount);
	}
	
	@Override
	public boolean canAcceptItemPayment(ItemStack itemStack, int amount) {
		if(shop.isAdmin()){
			return true;
		}
		return super.canAcceptItemPayment(itemStack, amount);
	}
	
	@Override
	public void addExperience(int amount) {
		if(shop.isAdmin()){
			return;
		}
		super.addExperience(amount);
	}
	
	@Override
	public void removeExperience(int amount) {
		if(shop.isAdmin()){
			return;
		}
		super.removeExperience(amount);
	}
	
	@Override
	public void addCurrency(double amount) {
		if(shop.isAdmin()){
			return;
		}
		super.addCurrency(amount);
	}
	
	@Override
	public void removeCurrency(double amount) {
		if(shop.isAdmin()){
			return;
		}
		super.removeCurrency(amount);
	}
	
	@Override
	public void addItem(ItemStack itemStack, int amount) {
		if(shop.isAdmin()){
			return;
		}
		super.addItem(itemStack, amount);
	}
	
	@Override
	public void removeItem(ItemStack itemStack, int amount) {
		if(shop.isAdmin()){
			return;
		}
		super.removeItem(itemStack, amount);
	}
}
