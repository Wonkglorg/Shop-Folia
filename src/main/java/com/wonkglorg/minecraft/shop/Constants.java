package com.wonkglorg.minecraft.shop;

import lombok.Getter;

import java.util.UUID;

public class Constants{
	@Getter
	public static final UUID adminUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	
	public static final String SHOP_COMMAND = "shop";
	public static final String SHOP_PERMISSION_USER = "shop.user";
	public static final String SHOP_PERMISSION_OPERATOR = "shop.operator";
}

