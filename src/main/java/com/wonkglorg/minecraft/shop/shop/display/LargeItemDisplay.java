package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.ArmorStandData;
import com.wonkglorg.minecraft.shop.util.DisplayUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class LargeItemDisplay extends AbstractDisplay{
	protected LargeItemDisplay(AbstractShop shop) {
		super(shop, DisplayType.LARGE_ITEM);
	}
	
	@Override
	public void onSpawn(@NotNull Player player) {
		Location leftLoc = shop.getContainerLocation().clone().add(0, 1, 0);
		leftLoc.add(getLargeItemBarterOffset(false));
		ItemStack itemStack = shop.getItemStack().clone();
		itemStack.setAmount(1);
		ArmorStandData armorStandData = DisplayUtil.getArmorStandData(itemStack, leftLoc, shop.getFacing(), false);
		spawnArmorStandPacket(player, armorStandData);
		
		if(shop.getSecondaryItemStack() != null){
			ItemStack barterItem = shop.getSecondaryItemStack().clone();
			barterItem.setAmount(1);
			Location rightLoc = shop.getContainerLocation().clone().add(0, 1, 0);
			rightLoc.add(getLargeItemBarterOffset(true));
			ArmorStandData armorStandData2 = DisplayUtil.getArmorStandData(barterItem, rightLoc, shop.getFacing(), false);
			spawnArmorStandPacket(player, armorStandData2);
		}
	}
	
	private Vector getLargeItemBarterOffset(boolean isBarterItem) {
		AbstractShop shop = this.getShop();
		
		Vector offset = new Vector(0, 0, 0);
		double space = 0.24;
		if(shop.getType() == ShopType.BARTER){
			switch(shop.getFacing()) {
				case NORTH:
					if(isBarterItem){
						offset.setX(-space);
					} else {
						offset.setX(space);
					}
					break;
				case EAST:
					if(isBarterItem){
						offset.setZ(-space);
					} else {
						offset.setZ(space);
					}
					break;
				case SOUTH:
					if(isBarterItem){
						offset.setX(space);
					} else {
						offset.setX(-space);
					}
					break;
				case WEST:
					if(isBarterItem){
						offset.setZ(space);
					} else {
						offset.setZ(-space);
					}
					break;
			}
		}
		return offset;
	}
}
