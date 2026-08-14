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
	protected GlassCaseDisplay(AbstractShop shop, DisplayType type) {
		super(shop, type);
	}
	
	@Override
	public void spawn(@NotNull Player player) {
		//put the extra large glass casing down
		Location caseLoc = shop.getContainerLocation().clone().add(0, 1, 0);
		ArmorStandData caseStandData = DisplayUtil.getArmorStandData(new ItemStack(Material.GLASS), caseLoc, shop.getFacing(), true);
		spawnArmorStandPacket(player, caseStandData, null);
		
		//Drop initial display item
		spawnItemPacket(player, item, this.getItemDropLocation(false));
		
		//Drop the barter display item
		spawnItemPacket(player, barterItem, this.getItemDropLocation(true));
		break;
	}
}
