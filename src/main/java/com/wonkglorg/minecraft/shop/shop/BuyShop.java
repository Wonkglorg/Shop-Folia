package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.shop.ShopState.EMPTY;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OVERFILLED;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.ExpirienceTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.ItemTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.VaultTransaction;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BuyShop extends AbstractShop{
	
	public BuyShop(UUID shopId,
	               Location signLoc,
	               UUID player,
	               double pri,
	               int amt,
	               Boolean admin,
	               BlockFace facing,
	               long creationDate,
	               DisplayType type) {
		super(shopId, signLoc, player, ShopType.BUY, pri, amt, admin, facing, creationDate, type);
	}
	
	@Override
	protected void calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			setShopState(OK, true);
			return;
		}
		if(item == null){
			//leave cached value
			return;
		}
		Transaction transaction = startTransaction(null);
		
		double availableFunds = transaction.getBuyerAvailableFunds();
		stock = (int) (availableFunds / this.getPrice());
		
		if(stock < 1){
			setShopState(EMPTY, true);
			return;
		}
		
		if(transaction.canBuyerAcceptPayment()){
			setShopState(OK, true);
			return;
		}
		
		setShopState(OVERFILLED, true);
	}
	
	@Override
	public Transaction startTransaction(TransactionParty party) {
		return switch(getCurrencyType()) {
			case VAULT -> new VaultTransaction(getParty(), party, amount, price, item);
			case ITEM -> new ItemTransaction(getParty(), party, amount, price, item, getCurrencyItem());
			case EXPERIENCE -> new ExpirienceTransaction(getParty(), party, amount, price, item);
		};
	}
	
	@Override
	protected void sendTransactionMessage(TransactionResult result, Player player, PlayerProfile owner) {
		var lang = Main.getPlugin().getLangManager();
		switch(result) {
			case OK -> {
				LangRequest userRequest = lang.request("transaction.success.BUY.user");
				shopPlaceholders(userRequest, this);
				userRequest.sendToAudience(player);
				if(owner.isNotifyOwner() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.success.BUY.owner").replace("%user%", player.getName());
					shopPlaceholders(ownerRequest, this);
					ownerRequest.sendToAudience(online.getPlayer());
				}
			}
			case SHOP_IS_PERFORMING_TRANSACTION -> lang.request("transaction.issue.BUY.shopPerformingTransaction").sendToAudience(player);
			case CANCELLED -> lang.request("transaction.issue.BUY.cancelledExternal").sendToAudience(player);
			case INSUFFICIENT_FUNDS_SELLER -> lang.request("transaction.issue.BUY.playerNoStock").sendToAudience(player);
			case INSUFFICIENT_FUNDS_BUYER -> {
				lang.request("transaction.issue.BUY.shopNoStock").sendToAudience(player);
				if(owner.isNotifyStock() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.issue.BUY.ownerNoStock");
					shopPlaceholders(ownerRequest, this);
					ownerRequest.replace("%user%", player.getName()).sendToAudience(online.getPlayer());
				}
			}
			case INVENTORY_FULL_SELLER -> lang.request("transaction.issue.BUY.playerNoSpace").sendToAudience(player);
			case INVENTORY_FULL_BUYER -> {
				lang.request("transaction.issue.BUY.shopNoSpace").sendToAudience(player);
				if(owner.isNotifyStock() && owner instanceof OnlinePlayerProfile online){
					LangRequest ownerRequest = lang.request("transaction.issue.BUY.ownerNoSpace");
					shopPlaceholders(ownerRequest, this);
					ownerRequest.replace("%user%", player.getName()).sendToAudience(online.getPlayer());
				}
			}
			case OWNER_CANT_TRANSACT_OWN_SHOP -> lang.request("transaction.issue.BUY.useOwnShop").sendToAudience(player);
		}
		
	}
}
