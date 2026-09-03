package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.util.ArmorStandData;
import com.wonkglorg.minecraft.shop.util.DisplayUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GlassCaseDisplay extends AbstractDisplay{
	protected GlassCaseDisplay(AbstractShop shop) {
		super(shop, DisplayType.GLASS_CASE);
	}
	
	@Override
	public List<Integer> spawn(@NotNull Player player) {
		//put the extra large glass casing down
		Location caseLoc = shop.getContainerLocation().clone().add(0, 1, 0);
		ArmorStandData caseStandData = DisplayUtil.getArmorStandData(new ItemStack(Material.GLASS), caseLoc, shop.getFacing(), true);
		List<Integer> entityIds = new ArrayList<>();
		entityIds.add(spawnArmorStandPacket(player, caseStandData));
		
		//Drop initial display item
		var stack = shop.getDisplayItem();
		stack.setAmount(1);
		entityIds.add(spawnItemPacket(player, stack, this.getPrimaryLocation()));
		
		ItemStack secondaryDisplayItem = shop.getSecondaryDisplayItem();
		if(secondaryDisplayItem != null){
			secondaryDisplayItem.setAmount(1);
			entityIds.add(spawnItemPacket(player, secondaryDisplayItem, this.getBarterLocation()));
		}
		return entityIds;
	}
}
