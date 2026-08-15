package com.wonkglorg.minecraft.shop.shop.transaction;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.InventoryUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

public abstract class TransactionParty{
	protected final OfflinePlayer player;
	protected final Inventory inventory;
	protected final CurrencyType currencyType;
	protected final double availableFunds;
	
	protected TransactionParty(OfflinePlayer player, Inventory inventory) {
		this.player = player;
		this.inventory = inventory;
		this.currencyType = Main.getPlugin().getSettingsConfig().getCurrencyType();
		this.availableFunds = getAvailableFunds();
	}
	
	/**
	 * The funds available to the party
	 */
	protected abstract double getAvailableFunds();
	
	/**
	 * If the party can accept payment
	 */
	public abstract boolean canAcceptPayment(double amount);
	
	public static int getAmount(Inventory inventory, ItemStack itemStack) {
		if(inventory == null){
			return 0;
		}
		ItemStack[] contents = inventory.getStorageContents();
		int amount = 0;
		for(int i = 0; i < contents.length; i++){
			ItemStack is = contents[i];
			if(is != null){
				if(itemstacksAreSimilar(itemStack, is)){
					amount += is.getAmount();
				}
			}
		}
		return amount;
	}
	
	public static boolean itemstacksAreSimilar(ItemStack i1, ItemStack i2) {
		if(i1 == null || i2 == null){
			return false;
		}
		if(i1.getType() != i2.getType()){
			return false;
		}
		
		ItemStack itemStack1 = i1.clone();
		ItemStack itemStack2 = i2.clone();
		
		// Check if we are ignoring item durability, if so, reset the durability of both items and continue with later checks
		if(!Main.getPlugin().getSettingsConfig().isCheckItemDurability()){
			Damageable is1Damagable = (Damageable) itemStack1.getItemMeta();
			is1Damagable.setDamage(0);
			
			Damageable is2Damagable = (Damageable) itemStack2.getItemMeta();
			is2Damagable.setDamage(0);
			
			itemStack1.setItemMeta(is1Damagable);
			itemStack2.setItemMeta(is2Damagable);
		}
		
		// Check if we are ignoring item durability, if so, reset the durability of both items and continue with later checks
		if(Main.getPlugin().getSettingsConfig().isIgnoreItemRepairCost()){
			Repairable item1Cost = (Repairable) itemStack1.getItemMeta();
			item1Cost.setRepairCost(0);
			
			Repairable item2Cost = (Repairable) itemStack2.getItemMeta();
			item2Cost.setRepairCost(0);
			
			itemStack1.setItemMeta(item1Cost);
			itemStack2.setItemMeta(item2Cost);
		}
		
		ItemMeta i1Meta = itemStack1.getItemMeta();
		ItemMeta i2Meta = itemStack2.getItemMeta();
		
		// Check if shulker box contents are identical
		if(itemStack1.getType().toString().toLowerCase().contains("shulker_box")){
			if(!itemStack2.getType().toString().toLowerCase().contains("shulker_box")){
				return false;
			}
			
			// Note: You must reference i1 and i2 here, if you do not then both inventories are identical for some reason... Do not reference the cloned item stacks here...
			BlockStateMeta bsm1 = (BlockStateMeta) i1.getItemMeta();
			BlockStateMeta bsm2 = (BlockStateMeta) i2.getItemMeta();
			Inventory inv1 = ((ShulkerBox) bsm1.getBlockState()).getInventory();
			Inventory inv2 = ((ShulkerBox) bsm2.getBlockState()).getInventory();
			
			ItemStack[] inv1Contents = inv1.getContents();
			ItemStack[] inv2Contents = inv2.getContents();
			
			for(int i = 0; i < inv1Contents.length; i++){
				ItemStack inv1Item = inv1Contents[i];
				ItemStack inv2Item = inv2Contents[i];
				
				if(inv1Item == null && inv2Item == null){
					continue;
				}
				if(inv1Item == null && inv2Item != null){
					return false;
				}
				if(inv1Item != null && inv2Item == null){
					return false;
				}
				if(!itemstacksAreSimilar(inv1Item, inv2Item)){
					return false;
				}
			}
		}
		
		//fix NBT attributes for cached older items to be compatible with Spigot serializer updates
		if(i1Meta != null && i2Meta != null && i1Meta.hasAttributeModifiers() && i2Meta.hasAttributeModifiers()){
			i1Meta.setAttributeModifiers(i1Meta.getAttributeModifiers());
			i2Meta.setAttributeModifiers(i2Meta.getAttributeModifiers());
			itemStack1.setItemMeta(i1Meta);
			itemStack2.setItemMeta(i2Meta);
		}
		
		return itemStack1.isSimilar(itemStack2);
	}
	
	public static double getFunds(OfflinePlayer player, Inventory inventory) {
		switch(Main.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
				double balance = Main.getPlugin().getEconomy().getBalance(player);
				return balance;
			case EXPERIENCE:
				return getExperience(player);
			case ITEM:
				ItemStack currency = Main.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount(1);
				int balanceInt = InventoryUtils.getAmount(inventory, currency);
				return balanceInt;
			default:
				return 0;
		}
	}
	
	//check to see if the player has enough space to accept the funds to deposit [amount]
	//return false if they do not
	public static boolean canAcceptFunds(Inventory inventory, double amount) {
		switch(Main.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
			case EXPERIENCE:
				return true;
			case ITEM:
				ItemStack currency = Main.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount((int) amount);
				
				return InventoryUtils.hasRoom(inventory, currency);
			default:
				return false;
		}
	}
}
