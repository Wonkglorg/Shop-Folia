package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.Main;
import lombok.Getter;

public enum ShopType{
	
	SELL(0, CreationWord.SELL),
	
	BUY(1, CreationWord.BUY),
	
	BARTER(2, CreationWord.BARTER),
	
	GAMBLE(3, CreationWord.GAMBLE);
	
	private final int weight;
	@Getter
	private final CreationWord creationWord;
	
	ShopType(int slot, CreationWord creationWord) {
		this.weight = slot;
		this.creationWord = creationWord;
	}
	
	@Override
	public String toString() {
		return switch(this) {
			case SELL -> "sell";
			case BUY -> "buy";
			case BARTER -> "barter";
			default -> "gamble";
		};
	}
	
	public static ShopType from(String input) {
		for(var value : ShopType.values()){
			if(value.toString().equalsIgnoreCase(input)){
				return value;
			}
		}
		return null;
	}
	
	public static ShopType typeFromString(String typeString) {
		if(typeString == null){
			return SELL;
		}
		if(typeString.contains("sell")){
			return ShopType.SELL;
		} else if(typeString.contains("buy")){
			return ShopType.BUY;
		} else if(typeString.contains("barter")){
			return ShopType.BARTER;
		} else {
			return ShopType.GAMBLE;
		}
	}
}