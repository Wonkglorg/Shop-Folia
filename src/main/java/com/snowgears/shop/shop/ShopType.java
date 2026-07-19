package com.snowgears.shop.shop;

import com.snowgears.shop.util.ShopMessage;

public enum ShopType{
	
	SELL(0),
	
	BUY(1),
	
	BARTER(2),
	
	GAMBLE(3),
	
	COMBO(4);
	
	private final int slot;
	
	ShopType(int slot) {
		this.slot = slot;
	}
	
	@Override
	public String toString() {
		return switch(this) {
			case SELL -> "sell";
			case BUY -> "buy";
			case BARTER -> "barter";
			case COMBO -> "combo";
			default -> "gamble";
		};
	}
	
	public String toCreationWord() {
		return switch(this) {
			case SELL -> ShopMessage.getCreationWord("SELL");
			case BUY -> ShopMessage.getCreationWord("BUY");
			case BARTER -> ShopMessage.getCreationWord("BARTER");
			case COMBO -> ShopMessage.getCreationWord("COMBO");
			default -> ShopMessage.getCreationWord("GAMBLE");
		};
	}
}