package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.util.ArmorStandData;
import com.wonkglorg.minecraft.shop.util.DisplayUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GlassCaseDisplay extends AbstractDisplay{
	protected GlassCaseDisplay(AbstractShop shop) {
		super(shop, DisplayType.GLASS_CASE);
	}
	
	@Override
	public void onSpawn(@NotNull Player player) {
		//put the extra large glass casing down
		Location caseLoc = shop.getContainerLocation().clone().add(0, 1, 0);
		ArmorStandData caseStandData = DisplayUtil.getArmorStandData(new ItemStack(Material.GLASS), caseLoc, shop.getFacing(), true);
		spawnArmorStandPacket(player, caseStandData);
		
		//Drop initial display item
		var stack = shop.getDisplayItem();
		stack.setAmount(1);
		spawnItemPacket(player, stack, this.getPrimaryLocation());
		
		ItemStack secondaryDisplayItem = shop.getSecondaryDisplayItem();
		if(secondaryDisplayItem != null){
			secondaryDisplayItem.setAmount(1);
			spawnItemPacket(player, secondaryDisplayItem, this.getBarterLocation());
		}
	}
}
