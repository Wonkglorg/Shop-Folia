package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.util.TransactionParty;
import lombok.Getter;
import org.bukkit.Location;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public enum ShopState{
	/**
	 * Shop is fine to use
	 */
	OK(0, s -> true),
	/**
	 * Shop has no stock
	 */
	EMPTY(2, s -> s.getStock() <= 0),
	//if the chest is full and purchasing from this plotShop once would not be enough to free up an entire slot for the purchase to be accepted)
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED(1, s -> !new TransactionParty(false, false, s.getOwner(), s.getInventory()).canAcceptPayment(s.getPrice()));
	
	/**
	 * The Weight of each state (the lower the higher it is ranked in the result order)
	 */
	@Getter
	private final int weight;
	/**
	 * Function used to verify a shops state
	 */
	private final Predicate<AbstractShop> stateVerifier;
	
	ShopState(int weight, Predicate<AbstractShop> stateVerifier) {
		this.weight = weight;
		this.stateVerifier = stateVerifier;
	}
	
	public static ShopState getShopState(AbstractShop shop) {
		if(shop.isAdmin){
			return OK;
		}
		
		if(EMPTY.stateVerifier.test(shop)){
			return EMPTY;
		} else if(OVERFILLED.stateVerifier.test(shop)){
			return OVERFILLED;
		} else {
			return OK;
		}
	}
	
	public static ShopState from(String input) {
		for(var value : ShopState.values()){
			if(value.toString().equalsIgnoreCase(input)){
				return value;
			}
		}
		return EMPTY;
	}
	
	public static CompletableFuture<ShopState> getShopStateAsync(AbstractShop shop) {
		CompletableFuture<ShopState> future = new CompletableFuture<>();
		
		Location loc = shop.getSignLocation();
		
		Shop.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(loc, () -> {
			try{
				future.complete(getShopState(shop));
			} catch(Exception t){
				future.completeExceptionally(t);
			}
		}, 2);
		return future;
	}
}