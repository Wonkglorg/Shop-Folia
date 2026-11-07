package com.wonkglorg.minecraft.shop.mocks;

import com.wonkglorg.minecraft.shop.shop.GambleShop;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class GambleShopMock extends GambleShop{
	private final Inventory inventory;
	
	public GambleShopMock(UUID player, Location location, double pri, int amt, Inventory inventory) {
		super(UUID.randomUUID(), location, player, pri, amt, BlockFace.DOWN, System.currentTimeMillis(), null);
		this.inventory = inventory;
	}
	
	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
