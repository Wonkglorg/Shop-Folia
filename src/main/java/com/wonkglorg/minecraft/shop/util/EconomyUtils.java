package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.Shop;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EconomyUtils{
	
	//check to see if the player has enough funds to take out [amount]
	//return false if they do not
	public static boolean hasSufficientFunds(OfflinePlayer player, Inventory inventory, double amount) {
		switch(Shop.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
				double balance = Shop.getPlugin().getEconomy().getBalance(player);
				return (balance >= amount);
			case ITEM:
				ItemStack currency = Shop.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount(1);
				int stock = InventoryUtils.getAmount(inventory, currency);
				return (stock >= amount);
			case EXPERIENCE:
				int exp = getExperience(player);
				return (exp > amount);
			default:
				return false;
		}
	}
	
	//check to see if the player has enough space to accept the funds to deposit [amount]
	//return false if they do not
	public static boolean canAcceptFunds(Inventory inventory, double amount) {
		switch(Shop.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
			case EXPERIENCE:
				return true;
			case ITEM:
				ItemStack currency = Shop.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount((int) amount);
				
				return InventoryUtils.hasRoom(inventory, currency);
			default:
				return false;
		}
	}
	
	//gets the current funds of the player
	public static double getFunds(OfflinePlayer player, Inventory inventory) {
		switch(Shop.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
				double balance = Shop.getPlugin().getEconomy().getBalance(player);
				return balance;
			case EXPERIENCE:
				return getExperience(player);
			case ITEM:
				ItemStack currency = Shop.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount(1);
				int balanceInt = InventoryUtils.getAmount(inventory, currency);
				return balanceInt;
			default:
				return 0;
		}
	}
	
	//removes [amount] of funds from the player
	//return false if the player did not have sufficient funds or if something went wrong
	public static boolean removeFunds(OfflinePlayer player, Inventory inventory, double amount) {
		switch(Shop.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
				EconomyResponse response = Shop.getPlugin().getEconomy().withdrawPlayer(player, amount);
				if(response.transactionSuccess()){
					return true;
				}
				return false;
			case EXPERIENCE:
				Player onlinePlayer = player.getPlayer();
				if(onlinePlayer != null){
					setTotalExperience(onlinePlayer, getTotalExperience(onlinePlayer) - (int) amount);
					return true;
				} else {
					PlayerExperience expData = PlayerExperience.loadFromFile(player);
					if(expData != null){
						expData.removeExperienceAmount((int) amount);
						return true;
					} else {
						return false;
					}
				}
			case ITEM:
				ItemStack currency = Shop.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount((int) amount);
				int unremoved = InventoryUtils.removeItem(inventory, currency);
				if(unremoved > 0){
					currency.setAmount(((int) amount) - unremoved);
					InventoryUtils.addItem(inventory, currency);
					return false;
				}
				return true;
			default:
				return false;
		}
	}
	
	//adds [amount] of funds to the player
	//return false if the player did not have enough room for items or if something went wrong
	public static boolean addFunds(OfflinePlayer player, Inventory inventory, double amount) {
		switch(Shop.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT:
				EconomyResponse response = Shop.getPlugin().getEconomy().depositPlayer(player, amount);
				if(response.transactionSuccess()){
					return true;
				}
			case EXPERIENCE:
				Player onlinePlayer = player.getPlayer();
				if(onlinePlayer != null){
					setTotalExperience(onlinePlayer, getTotalExperience(onlinePlayer) + (int) amount);
					return true;
				} else {
					PlayerExperience expData = PlayerExperience.loadFromFile(player);
					if(expData != null){
						expData.addExperienceAmount((int) amount);
						return true;
					} else {
						return false;
					}
				}
			case ITEM:
				ItemStack currency = Shop.getPlugin().getItemConfig().getCurrencyItem().clone();
				currency.setAmount((int) amount);
				int unadded = InventoryUtils.addItem(inventory, currency);
				if(unadded > 0){
					currency.setAmount(((int) amount) - unadded);
					InventoryUtils.removeItem(inventory, currency);
					return false;
				}
				return true;
			default:
				return false;
		}
	}
	
	private static int getExperience(OfflinePlayer player) {
		if(player.getPlayer() != null){
			return getTotalExperience(player.getPlayer());
		} else {
			PlayerExperience expData = PlayerExperience.loadFromFile(player);
			if(expData != null){
				return expData.getExperience();
			}
		}
		return 0;
	}
	
	public static int getTotalExperience(Player player) {
		int level = player.getLevel();
		
		if(level <= 16){
			return level * level + 6 * level + (int) (player.getExp() * getExperienceToNextLevel(level));
		}
		
		if(level <= 31){
			return (int) (2.5 * level * level - 40.5 * level + 360 + player.getExp() * getExperienceToNextLevel(level));
		}
		
		return (int) (4.5 * level * level - 162.5 * level + 2220 + player.getExp() * getExperienceToNextLevel(level));
	}
	
	public static void setTotalExperience(Player player, int totalExperience) {
		totalExperience = Math.max(0, totalExperience);
		
		player.setLevel(0);
		player.setExp(0);
		player.setTotalExperience(0);
		
		if(totalExperience == 0){
			return;
		}
		
		int level = getLevelForExperience(totalExperience);
		int experienceAtLevel = getExperienceAtLevel(level);
		int experienceIntoLevel = totalExperience - experienceAtLevel;
		int experienceToNextLevel = getExperienceToNextLevel(level);
		
		player.setLevel(level);
		player.setExp((float) experienceIntoLevel / experienceToNextLevel);
		player.setTotalExperience(totalExperience);
	}
	
	public static int getLevelForExperience(int experience) {
		if(experience < 0){
			return 0;
		}
		
		if(experience < 352){
			return (int) ((Math.sqrt(72 * experience + 81) - 9) / 2);
		}
		
		if(experience < 1507){
			return (int) ((Math.sqrt(40 * experience - 7839) + 81) / 10);
		}
		
		return (int) ((Math.sqrt(72 * experience - 54215) + 325) / 18);
	}
	
	public static int getExperienceAtLevel(int level) {
		if(level <= 16){
			return level * level + 6 * level;
		}
		
		if(level <= 31){
			return (int) (2.5 * level * level - 40.5 * level + 360);
		}
		
		return (int) (4.5 * level * level - 162.5 * level + 2220);
	}
	
	public static int getExperienceToNextLevel(int level) {
		if(level >= 30){
			return 9 * level - 158;
		}
		
		if(level >= 15){
			return 5 * level - 38;
		}
		
		return 2 * level + 7;
	}
}
