package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopDatabase;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import static com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty.createVirtualInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GambleShop extends AbstractShop{
	/**
	 * Used when no valid item is defined in the shop
	 */
	private static final ItemStack DEFAULT_EMPTY_ITEM = new ItemStack(Material.STONE);
	
	private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
	/**
	 * Items contained inside this shop that can be gambled for
	 */
	private final List<ItemStack> gambleItems = new ArrayList<>();
	/**
	 * Index of the next item to give out when a player successfully completes a transaction with this shop
	 */
	private int index = 0;
	
	public GambleShop(UUID shopId, Location signLoc, UUID player, double pri, int amt, BlockFace facing, long creationDate, DisplayType type) {
		super(shopId, signLoc, player, ShopType.GAMBLE, pri, amt, true, facing, creationDate, type);
		stock = Integer.MAX_VALUE;
	}
	
	@Override
	protected void logTransaction(TransactionParty party, int multiplier) {
		shopDatabase().logTransaction(id, System.currentTimeMillis(), party.getPlayer().getUniqueId(), getItemStack(), 1);
	}
	
	@Override
	protected void postTransactionSuccess(Transaction transaction) {
		setItemStack(getItemStack());
		index = randomIndex();
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player) {
		var lang = langManager();
		switch(result) {
			case OK -> {
				LangRequest userRequest = lang.request("transaction.success.gamble.user");
				shopPlaceholders(userRequest, this, false, player);
				userRequest.replace("%price%", formatPrice(price * multiplier));
				userRequest.replace("%item-amount%", amount * multiplier);
				userRequest.sendToAudience(player);
			}
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.gamble.shop-performing-transaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.gamble.cancelled-external").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> lang.request("transaction.issue.gamble.player-no-stock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> { // can't happen for gamble shops
			}
			case INVENTORY_FULL_BUYER -> lang.request("transaction.issue.gamble.player-no-space").sendToAudience(player);
			case INVENTORY_FULL_SELLER -> lang.request("transaction.issue.gamble.shop-no-stock").sendToAudience(player);
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.gamble.use-own-shop").sendToAudience(player);
			case PURCHASE_COOLDOWN -> notifyCooldownReached(player,multiplier);
			case PURCHASE_LIMIT_REACHED -> lang.request("transaction.issue.gamble.player-transaction-limit-reached").sendToAudience(player);
		}
		
	}
	
	@Override
	public ItemStack getItemStack() {
		if(gambleItems.isEmpty()){
			return null;
		}
		return gambleItems.get(index);
	}
	
	@Override
	public ItemStack getDisplayItem() {
		//todo:mjd allow for rotating display items when viewing the gamble shop.
		return ShopPlugin.getPlugin().getItemConfig().getGambleDisplayItem();
	}
	
	@Override
	protected void calculateStock() {
		Inventory inventory = getInventory();
		gambleItems.clear();
		for(var item : inventory){
			if(item == null){
				continue;
			}
			gambleItems.add(item.clone());
		}
		index = randomIndex();
		if(gambleItems.isEmpty()){
			setShopState(ShopState.EMPTY, true);
		} else {
			setShopState(OK, true);
		}
	}
	
	/**
	 * Gets a random index from the gamble items list
	 */
	private int randomIndex() {
		if(gambleItems.isEmpty()){
			return 0;
		}
		return RANDOM.nextInt(0, gambleItems.size());
	}
	
	//override the default party and give it a fake virtual inventory to do trades with, as the real one should never be changed by a transaction
	@Override
	protected @NotNull ShopTransactionParty getParty() {
		return new ShopTransactionParty(this, createVirtualInventory(getInventory()));
	}
	
	//gamble shop can not have anything besides 1x transaction
	@Override
	protected @NotNull Transaction findAffordableTransaction(TransactionParty party, boolean requestFullstack) {
		Transaction transaction = startTransaction(party, 1);
		//populates the transaction result object with the result state
		transaction.canFulfill();
		return transaction;
	}
	
	@Override
	public @NonNull Transaction startTransaction(TransactionParty party, int multiplier) {//multiplier is ignored for gambling
		ItemStack itemStack = getItemStack();
		if(itemStack == null){
			amount = 9999; //use a big amount number to prevent the trade from passing (no item is in the gamble shop)
			itemStack = DEFAULT_EMPTY_ITEM;
		} else {
			amount = itemStack.getAmount();
		}
		return switch(ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT -> new VaultTransaction(party, getParty(), amount, price, itemStack);
			case ITEM -> new ItemTransaction(party, getParty(), amount, price, itemStack, ShopPlugin.getPlugin().getItemConfig().getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(party, getParty(), amount, price, itemStack);
		};
	}
}
