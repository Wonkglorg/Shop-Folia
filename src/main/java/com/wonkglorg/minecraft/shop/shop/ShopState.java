package com.wonkglorg.minecraft.shop.shop;

import lombok.Getter;

public enum ShopState{
	/**
	 * Shop is fine to use
	 */
	OK(0),
	/**
	 * Shop has no empty spaces left to accept exchange
	 */
	OVERFILLED(1),
	/**
	 * Shop has no stock
	 */
	EMPTY(2);
	
	@Getter
	private final int weight;
	
	ShopState(int weight) {
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