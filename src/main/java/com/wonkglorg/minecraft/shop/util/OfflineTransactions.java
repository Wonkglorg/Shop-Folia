package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OfflineTransactions{
	
	@Getter
	private UUID playerUUID;
	@Getter
	private long lastPlayed;
	@Getter
	private boolean isCalculating;
	@Setter
	@Getter
	private int numTransactions;
	@Setter
	@Getter
	private double totalProfit;
	@Setter
	@Getter
	private double totalSpent;
	@Setter
	@Getter
	private Map<ItemStack, Integer> itemsBought;
	@Setter
	@Getter
	private Map<ItemStack, Integer> itemsSold;
	
	private final List<String> txStrings = new ArrayList<>();
	
	public OfflineTransactions(UUID playerUUID, long lastPlayed) {
		this.playerUUID = playerUUID;
		this.lastPlayed = lastPlayed;
		calculate();
	}
	
	public void addTx(Location location,
	                  ShopType transactionType,
	                  double price,
	                  OfflinePlayer purchaser,
	                  int amount,
	                  ItemStack itemSold,
	                  ItemStack barterItem) {
		// load message and perform initial formatting
		String formattedMessage = ShopMessage.getMessageFromOrders(transactionType, "owner", price, amount);
		// Add rest of the formatting
		PlaceholderContext context = new PlaceholderContext();
		context.setOfflinePlayer(purchaser);
		context.setItem(itemSold);
		context.setBarterItem(barterItem);
		context.setLocation(location);
		formattedMessage = ShopMessage.formatPlainTextSingle("• " + formattedMessage, context);
		txStrings.add(formattedMessage);
	}
	
	public String getTransactionsLore() {
		return String.join("\n", txStrings);
	}
	
	private void calculate() {
		isCalculating = true;
		Main.getPlugin().getShopmanager().getDatabase().calculateOfflineTransactions(this);
	}
	
	public void setIsCalculating(boolean isCalculating) {
		this.isCalculating = isCalculating;
	}
}
