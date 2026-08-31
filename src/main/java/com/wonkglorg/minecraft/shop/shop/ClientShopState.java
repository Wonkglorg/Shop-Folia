package com.wonkglorg.minecraft.shop.shop;

import lombok.Getter;

/**
 * The Shopstate specific to a player
 */
public enum ClientShopState{
	/**
	 * Shop is fine to use
	 */
	OK(0),
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED(1),
	/**
	 * Shop is currently on cooldown for the given player
	 */
	ON_COOLDOWN(2),
	/**
	 * User reached their limit for interactions with this shop
	 */
	LIMIT_REACHED(3),
	
	/**
	 * Shop has no stock
	 */
	EMPTY(4);
	
	@Getter
	private final int weight;
	
	ClientShopState(int weight) {
		this.weight = weight;
	}
	
	public static ClientShopState from(String value) {
		for(var state : ClientShopState.values()){
			if(state.toString().equalsIgnoreCase(value)){
				return state;
			}
		}
		return OK;
	}
}