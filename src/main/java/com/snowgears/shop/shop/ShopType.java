package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;

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
		SettingsConfig settingsConfig = Shop.getPlugin().getSettingsConfig();
		return switch(this) {
			case SELL -> settingsConfig.getCreationWord(CreationWord.SELL);
			case BUY -> settingsConfig.getCreationWord(CreationWord.BUY);
			case BARTER -> settingsConfig.getCreationWord(CreationWord.BARTER);
			case COMBO -> settingsConfig.getCreationWord(CreationWord.COMBO);
			default -> settingsConfig.getCreationWord(CreationWord.GAMBLE);
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
		} else if(typeString.contains("combo")){
			return ShopType.COMBO;
		} else {
			return ShopType.GAMBLE;
		}
	}
}