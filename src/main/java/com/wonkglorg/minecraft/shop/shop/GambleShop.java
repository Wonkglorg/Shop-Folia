package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GambleShop extends AbstractShop{
	
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
	}
	
	@Override
	protected void logTransaction(TransactionParty party, int multiplier) {
		Main.getPlugin().getShopmanager().getDatabase().logTransaction(id,
				System.currentTimeMillis(),
				party.getPlayer().getUniqueId(),
				getItemStack(),
				1);
	}
	
	@Override
	protected void postTransactionSuccess(Transaction transaction) {
		index = randomIndex();
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, int multiplier, Player player, PlayerProfile owner) {
	
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
		return Main.getPlugin().getItemConfig().getGambleDisplayItem();
	}
	
	@Override
	protected void calculateStock() {
		stock = Integer.MAX_VALUE;
		setShopState(OK, true);
		
		Inventory inventory = getInventory();
		gambleItems.clear();
		for(var item : inventory){
			gambleItems.add(item.clone());
		}
		index = randomIndex();
	}
	
	/**
	 * Gets a random index from the gamble items list
	 */
	private int randomIndex() {
		return RANDOM.nextInt(0, gambleItems.size());
	}
	
	@Override
	public @NonNull Transaction startTransaction(TransactionParty party, int multiplier) {//multiplier is ignored for gambling
		return switch(Main.getPlugin().getSettingsConfig().getCurrencyType()) {
			case VAULT -> new VaultTransaction(new ShopTransactionParty(this), party, amount, price, getItemStack());
			case ITEM -> new ItemTransaction(new ShopTransactionParty(this),
					party,
					amount,
					price,
					getItemStack(),
					Main.getPlugin().getItemConfig().getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(new ShopTransactionParty(this), party, amount, price, getItemStack());
		};
	}
}
