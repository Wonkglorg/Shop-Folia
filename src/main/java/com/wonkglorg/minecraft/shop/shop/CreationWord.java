package com.wonkglorg.minecraft.shop.shop;

public enum CreationWord{
	SHOP("[shop]"),
	SELL("sell"),
	BUY("buy"),
	BARTER("barter"),
	GAMBLE("gamble"),
	ADMIN("admin"),
	COMBO("combo");
	
	private final String defaultName;
	
	CreationWord(String defaultName) {
		this.defaultName = defaultName;
	}
	
	public String getDefaultWord() {
		return defaultName;
	}
}
