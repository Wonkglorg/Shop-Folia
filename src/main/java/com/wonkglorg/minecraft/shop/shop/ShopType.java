package com.wonkglorg.minecraft.shop.shop;

public enum ShopType{
	
	SELL(0),
	
	BUY(1),
	
	BARTER(2),
	
	GAMBLE(3);
	
	private final int weight;
	
	ShopType(int slot) {
		this.weight = slot;
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