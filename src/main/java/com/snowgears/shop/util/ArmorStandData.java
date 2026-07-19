package com.snowgears.shop.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

@Setter
@Getter
public class ArmorStandData{
	
	private Location location;
	private double yaw;
	private ItemStack equipment;
	private EquipmentSlot equipmentSlot;
	private boolean isSmall;
	private EulerAngle rightArmPose;
	
}
