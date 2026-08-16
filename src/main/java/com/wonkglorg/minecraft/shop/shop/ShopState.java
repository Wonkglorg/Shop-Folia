package com.wonkglorg.minecraft.shop.shop;

public enum ShopState{
	/**
	 * Shop is fine to use
	 */
	OK(),
	/**
	 * Shop has no stock
	 */
	EMPTY(),
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED();
	
	public static ShopState from(String value) {
		for(var state : ShopState.values()){
			if(state.toString().equalsIgnoreCase(value)){
				return state;
			}
		}
		return OK;
	}
}