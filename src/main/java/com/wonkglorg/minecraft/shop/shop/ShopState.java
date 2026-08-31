package com.wonkglorg.minecraft.shop.shop;

import lombok.Getter;

public enum ShopState{
	/**
	 * Shop is fine to use
	 */
	OK(0,ClientShopState.OK),
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED(1,ClientShopState.OVERFILLED),
	/**
	 * Shop has no stock
	 */
	EMPTY(2,ClientShopState.EMPTY);
	
	@Getter
	private final int weight;
	
	@Getter
	private final ClientShopState clientShopState;
	
	ShopState(int weight, ClientShopState state) {
		this.weight = weight;
	}
	
	public static ShopState from(String value) {
		for(var state : ShopState.values()){
			if(state.toString().equalsIgnoreCase(value)){
				return state;
			}
		}
		return OK;
	}
}