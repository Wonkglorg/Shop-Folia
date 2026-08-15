package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class InventoryUtils{
	
	//removes itemstack from inventory
	//returns the amount of items it could not remove
	public static int removeItem(Inventory inventory, ItemStack itemStack) {
		if(inventory == null || itemStack.getAmount() >= (27 * 64)) // 27 stacks max, large values > 27 stacks can crash server!
		{
			return itemStack.getAmount();
		}
		if(itemStack == null || itemStack.getAmount() <= 0){
			return 0;
		}
		
		ItemStack[] contents = inventory.getContents();
		int amount = itemStack.getAmount();
		for(int i = 0; i < contents.length; i++){
			ItemStack is = contents[i];
			if(is != null){
				// Check if we are the same item type
				if(itemstacksAreSimilar(is, itemStack)){
					// Take items from stack
					if(is.getAmount() > amount){
						contents[i].setAmount(is.getAmount() - amount);
						inventory.setContents(contents);
						return 0;
					}
					// If we are equal, remove the stack from the inventory
					else if(is.getAmount() == amount){
						contents[i].setType(Material.AIR);
						inventory.setContents(contents);
						return 0;
					}
					// We have less than enough, take the amount
					else {
						amount -= is.getAmount();
						contents[i].setType(Material.AIR);
					}
				}
			}
		}
		inventory.setContents(contents);
		return amount;
	}
	
	//takes an ItemStack and splits it up into multiple ItemStacks with correct stack sizes
	//then adds those items to the given inventory
	public static int addItem(Inventory inventory, ItemStack itemStack) {
		if(inventory == null || itemStack.getAmount() >= (27 * 64)) // 27 stacks max, large values > 27 stacks can crash server!
		{
			return itemStack.getAmount();
		}
		if(itemStack.getAmount() <= 0){
			return 0;
		}
		ArrayList<ItemStack> itemStacksAdding = new ArrayList<ItemStack>();
		
		//break up the itemstack into multiple ItemStacks with correct stack size
		int fullStacks = itemStack.getAmount() / itemStack.getMaxStackSize();
		int partialStack = itemStack.getAmount() % itemStack.getMaxStackSize();
		for(int i = 0; i < fullStacks; i++){
			ItemStack is = itemStack.clone();
			is.setAmount(is.getMaxStackSize());
			itemStacksAdding.add(is);
		}
		ItemStack is = itemStack.clone();
		is.setAmount(partialStack);
		if(partialStack > 0){
			itemStacksAdding.add(is);
		}
		
		//try adding all items from itemStacksAdding and return number of ones you couldnt add
		int amount = 0;
		for(ItemStack addItem : itemStacksAdding){
			HashMap<Integer, ItemStack> noAdd = inventory.addItem(addItem);
			for(ItemStack noAddItemstack : noAdd.values()){
				amount += noAddItemstack.getAmount();
			}
		}
		return amount;
	}
	
	public static boolean hasRoom(Inventory inventory, ItemStack itemStack) {
		if(inventory == null || itemStack.getAmount() >= (27 * 64)) // 27 stacks max, large values > 27 stacks can crash server!
		{
			return false;
		}
		if(itemStack.getAmount() <= 0){
			return true;
		}
		
		// Check a cloned inventory instead of modifying the existing inventory
		Inventory clonedInv = getVirtualInventory(inventory);
		
		// Check if we can successfully add all the items to the players inventory
		int itemsLeftToAdd = addItem(clonedInv, itemStack);
		if(itemsLeftToAdd > 0){
			return false;
		}
		return true;
	}
	
	public static Inventory getVirtualInventory(Inventory inventory) {
		// Check a cloned inventory instead of manipulating the original inventory
		Inventory clonedInv = Bukkit.createInventory(null, inventory.getStorageContents().length);
		clonedInv.setContents(inventory.getStorageContents());
		
		return clonedInv;
	}
	
	//gets the amount of items in inventory
	
	public static boolean isEmpty(Inventory inv) {
		if(inv == null){
			return true;
		}
		for(ItemStack it : inv.getContents()){
			if(it != null){
				return false;
			}
		}
		return true;
	}
	
	public static ItemStack getRandomItem(Inventory inv) {
		if(inv == null){
			return null;
		}
		ArrayList<ItemStack> contents = new ArrayList<>();
		for(ItemStack it : inv.getContents()){
			if(it != null){
				contents.add(it);
			}
			
		}
		if(contents.size() == 0){
			return null;
		}
		Collections.shuffle(contents);
		
		int index = new Random().nextInt(contents.size());
		return contents.get(index);
	}
}
