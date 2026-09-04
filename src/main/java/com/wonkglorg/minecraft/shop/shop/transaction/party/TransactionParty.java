package com.wonkglorg.minecraft.shop.shop.transaction.party;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import static com.wonkglorg.minecraft.shop.util.ExperienceUtils.getTotalExperience;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A party in a transaction
 */
public class TransactionParty{
	/**
	 * The player representing this party
	 */
	@Getter
	protected final OfflinePlayer player;
	/**
	 * The inventory of the party
	 */
	protected final Inventory inventory;
	
	public TransactionParty(OfflinePlayer player, Inventory inventory) {
		this.player = player;
		this.inventory = inventory;
	}
	
	/**
	 * Gets the parties funds based on the servers specified currency
	 *
	 * @param itemStack the currency item if {@link CurrencyType#ITEM}
	 */
	public double getAvailableFunds(ItemStack itemStack) {
		return switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case ITEM -> getAvailableItemFunds(itemStack);
			case VAULT -> getAvailableVaultFunds();
			case EXPERIENCE -> getAvailableExperienceFunds();
		};
	}
	
	/**
	 * The funds available to the party when using {@link CurrencyType#EXPERIENCE} {@link CurrencyType#VAULT}
	 */
	public double getAvailableExperienceFunds() {
		logger().debug("Checking available experience for party");
		double experience = 0;
		if(player.getPlayer() != null){
			logger().debug("Party is online taking their experience directly");
			experience = getTotalExperience(player.getPlayer());
		} else {
			PlayerProfile data = PlayerProfile.offline(player);
			logger().debug("Party is not online taking their experience from exp cache");
			experience = data.getExperience();
		}
		return experience;
	}
	
	/**
	 * The funds available to the party when using {@link CurrencyType#VAULT}
	 */
	public double getAvailableVaultFunds() {
		return ShopPlugin.getPlugin().getEconomy().getBalance(player);
	}
	
	/**
	 * The item funds available to the party when using {@link CurrencyType#ITEM}
	 *
	 * @param itemStack the item used as funds
	 */
	public int getAvailableItemFunds(ItemStack itemStack) {
		logger().debug("Checking available items for party");
		int amount = 0;
		for(var item : inventory){
			if(item == null){
				continue;
			}
			if(itemStack.isSimilar(item)){
				amount += item.getAmount();
			}
		}
		return amount;
	}
	
	/**
	 * @param itemStack the currency item if {@link CurrencyType#ITEM}
	 * @param amount how much the payment will be
	 * @return if the party can accept this payment
	 */
	public boolean canAcceptPayment(ItemStack itemStack, double amount) {
		return switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case ITEM -> canAcceptItemPayment(itemStack, (int) amount);
			case VAULT -> canAcceptVaultPayment(amount);
			case EXPERIENCE -> canAcceptExperiencePayment(amount);
		};
	}
	
	/**
	 * If the party can accept payment when using {@link CurrencyType#EXPERIENCE}
	 */
	public boolean canAcceptExperiencePayment(double amount) {
		logger().debug("Party can accept experience payment");
		return true; // exp can always be accepted
	}
	
	/**
	 * If the party can accept payment when using {@link CurrencyType#VAULT}
	 */
	public boolean canAcceptVaultPayment(double amount) {
		logger().debug("Party can accept vault payment");
		return true; //vault can always accept payment
	}
	
	/**
	 * If the party can accept item based payment when using {@link CurrencyType#ITEM}
	 *
	 * @param itemStack the item stack thats used as payment
	 * @param amount how much of this items stack will be used as payment
	 */
	public boolean canAcceptItemPayment(ItemStack itemStack, int amount) {
		logger().debug("Checking if party can accept item payment");
		if(amount <= 0){
			logger().debug("Payment is 0 no check needed");
			return true;
		}
		
		ItemStack currency = itemStack.clone();
		currency.setAmount(amount);
		
		//let minecraft handle the checking and confirming, if it has room the returned map will be empty
		boolean empty = createVirtualInventory().addItem(itemStack).isEmpty();
		if(empty){
			logger().debug("Party can accept payment");
		} else {
			logger().debug("Party cannot accept payment");
		}
		return empty;
	}
	
	/**
	 * Adds currency to the party based on the servers defined {@link CurrencyType}
	 *
	 * @param stack the itemstack if item is defined
	 * @param amount the amount to add
	 */
	public void add(ItemStack stack, double amount) {
		switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case ITEM -> addItem(stack, (int) amount);
			case VAULT -> addCurrency(amount);
			case EXPERIENCE -> addExperience((int) amount);
		}
	}
	
	/**
	 * Adds experience to the party
	 *
	 * @param amount the amount to add
	 */
	public void addExperience(int amount) {
		PlayerProfile profile;
		if(player.getPlayer() != null){
			logger().debug("Party is online, granting experience directly");
			profile = PlayerProfile.online(player.getPlayer());
		} else {
			logger().debug("Party is offline, writing experience to cache");
			profile = PlayerProfile.offline(player);
		}
		logger().debug("Adding " + amount + " experience points to " + this);
		profile.addExperienceAmount(amount);
	}
	
	/**
	 * Removes experience from the party
	 *
	 * @param amount the amount to remove
	 */
	public void removeExperience(int amount) {
		PlayerProfile profile;
		if(player.getPlayer() != null){
			logger().debug("Party is online, removing experience directly");
			profile = PlayerProfile.online(player.getPlayer());
		} else {
			logger().debug("Party is offline, removing experience from cache");
			profile = PlayerProfile.offline(player);
		}
		logger().debug("Removing " + amount + " experience points from " + this);
		profile.removeExperienceAmount(amount);
	}
	
	/**
	 * Adds currency to the party
	 *
	 * @param amount the amount to add
	 */
	public void addCurrency(double amount) {
		logger().debug("Adding " + amount + " vault currency to " + this);
		ShopPlugin.getPlugin().getEconomy().depositPlayer(player, amount);
	}
	
	/**
	 * Removes currency from the party based on the servers defined {@link CurrencyType}
	 *
	 * @param stack the itemstack if item is defined
	 * @param amount the amount to remove
	 */
	public void remove(ItemStack stack, double amount) {
		switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case ITEM -> removeItem(stack, (int) amount);
			case VAULT -> removeCurrency(amount);
			case EXPERIENCE -> removeExperience((int) amount);
		}
	}
	
	/**
	 * Removes currency from the party
	 *
	 * @param amount the amount to remove
	 */
	public void removeCurrency(double amount) {
		ShopPlugin.getPlugin().getEconomy().withdrawPlayer(player, amount);
	}
	
	/**
	 * Adds items to the party
	 *
	 * @param itemStack the item to add
	 * @param amount how much of the item to add
	 */
	public void addItem(ItemStack itemStack, int amount) {
		ItemStack clone = itemStack.clone();
		clone.setAmount(amount);
		inventory.addItem(clone);
	}
	
	/**
	 * Removes items from the party
	 *
	 * @param itemStack the item to remove
	 * @param amount how much of the item to remove
	 */
	public void removeItem(ItemStack itemStack, int amount) {
		ItemStack clone = itemStack.clone();
		clone.setAmount(amount);
		removeItemSmallestStacksFirst(inventory, clone, amount);
	}
	
	/**
	 * Removes items from the inventory, consuming the smallest matching
	 * stacks first.
	 *
	 * @param item the item to remove
	 * @param amount the amount to remove
	 * @return the amount of items that could not be removed
	 */
	public static int removeItemSmallestStacksFirst(@NotNull Inventory inventory, @NotNull ItemStack item, int amount) {
		if(amount <= 0){
			return 0;
		}
		
		List<Integer> matchingSlots = new ArrayList<>();
		
		for(int slot = 0; slot < inventory.getSize(); slot++){
			ItemStack stack = inventory.getItem(slot);
			
			if(stack == null || stack.getAmount() <= 0){
				continue;
			}
			
			if(stack.isSimilar(item)){
				matchingSlots.add(slot);
			}
		}
		
		// Smallest stacks first.
		matchingSlots.sort(Comparator.comparingInt(slot -> inventory.getItem(slot).getAmount()));
		
		int remaining = amount;
		
		for(int slot : matchingSlots){
			if(remaining <= 0){
				break;
			}
			
			ItemStack stack = inventory.getItem(slot);
			
			int removeAmount = Math.min(stack.getAmount(), remaining);
			int newAmount = stack.getAmount() - removeAmount;
			
			if(newAmount <= 0){
				inventory.setItem(slot, null);
			} else {
				stack.setAmount(newAmount);
			}
			
			remaining -= removeAmount;
		}
		
		return remaining;
	}
	
	public Inventory createVirtualInventory() {
		return createVirtualInventory(inventory);
	}
	
	/**
	 * Gets a virtual copy of the inventory to modify without changing the real inventory yet
	 */
	public static Inventory createVirtualInventory(Inventory inventory) {
		// Check a cloned inventory instead of manipulating the original inventory
		Inventory clonedInv = Bukkit.createInventory(null, inventory.getStorageContents().length);
		clonedInv.setContents(inventory.getStorageContents());
		
		return clonedInv;
	}
	
	@Override
	public String toString() {
		return "TransactionParty{uuid=" + player.getUniqueId() + ", name=" + player.getName() + "}";
	}
}
