package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ItemDisplay extends AbstractDisplay{
	public ItemDisplay(AbstractShop shop) {
		super(shop, DisplayType.ITEM);
	}
	
	@Override
	public List<Integer> spawn(@NonNull Player player) {
		ItemStack stack = shop.getDisplayItem();
		stack.setAmount(1);
		List<Integer> entityIds = new ArrayList<>();
		entityIds.add(spawnItemPacket(player, stack, this.getPrimaryLocation()));
		ItemStack secondaryStack = shop.getSecondaryDisplayItem();
		if(secondaryStack != null){
			secondaryStack.setAmount(1);
			entityIds.add(spawnItemPacket(player, secondaryStack, this.getBarterLocation()));
		}
		return entityIds;
	}
}
