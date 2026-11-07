package com.wonkglorg.minecraft.shop.mocks;

import com.wonkglorg.minecraft.shop.shop.BarterShop;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BarterShopMock extends BarterShop{
	private final Inventory inventory;
	
	public BarterShopMock(UUID player, Location location, double pri, int amt, boolean admin, Inventory inventory) {
		super(UUID.randomUUID(), location, player, pri, amt, admin, BlockFace.DOWN, System.currentTimeMillis(), null);
		this.inventory = inventory;
	}
	
	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
