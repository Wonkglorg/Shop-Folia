package com.wonkglorg.minecraft.shop.util;

public enum CurrencyType{
	ITEM,
	VAULT,
	EXPERIENCE;
	
	public static CurrencyType fromValue(String value) {
		for(var type : CurrencyType.values()){
			if(type.toString().equalsIgnoreCase(value)){
				return type;
			}
		}
		return ITEM;
	}
}
