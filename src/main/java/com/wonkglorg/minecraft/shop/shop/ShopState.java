package com.wonkglorg.minecraft.shop.shop;

import lombok.Getter;

public enum ShopState{
	/**
	 * Shop is fine to use
	 */
	OK(0, ShopStateClient.OK),
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED(1, ShopStateClient.OVERFILLED),
	/**
	 * Shop has no stock
	 */
	EMPTY(2, ShopStateClient.EMPTY);
	
	@Getter
	private final int weight;
	
	@Getter
	private final ShopStateClient clientState;
	
	ShopState(int weight, ShopStateClient state) {
		this.weight = weight;
		this.clientState = state;
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