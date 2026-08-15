package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class ItemDisplay extends AbstractDisplay{
	public ItemDisplay(AbstractShop shop) {
		super(shop, DisplayType.ITEM);
	}
	
	@Override
	public void onSpawn(@NonNull Player player) {
		ItemStack stack = shop.getItemStack().clone();
		stack.setAmount(1);
		spawnItemPacket(player, stack, this.getPrimaryLocation());
		if(shop.getSecondaryItemStack() != null){
			ItemStack secondStack = shop.getSecondaryItemStack().clone();
			secondStack.setAmount(1);
			spawnItemPacket(player, secondStack, this.getBarterLocation());
		}
	}
}
