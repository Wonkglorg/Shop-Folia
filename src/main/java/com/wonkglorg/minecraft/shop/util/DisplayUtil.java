package com.wonkglorg.minecraft.shop.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class DisplayUtil{
	
	//main groups of angles
	public static EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0));
	public static EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100), Math.toRadians(-90), Math.toRadians(0));
	
	//very specific case angles
	public static EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80), Math.toRadians(-90), Math.toRadians(0));
	public static EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(5), Math.toRadians(-10));
	public static EulerAngle crossBowAngle = new EulerAngle(3.193952531149623, 0.0, 0.2792526803190927);
	public static EulerAngle shieldAngle = new EulerAngle(Math.toRadians(90), Math.toRadians(0), Math.toRadians(0));
	
	public static ArmorStandData getArmorStandData(ItemStack itemStack, Location blockLocation, BlockFace facing, boolean isCase) {
		EquipmentSlot equipmentSlot = getEquipmentSlot(itemStack);
		Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, equipmentSlot);
		standLocation.setY(standLocation.getY() - 0.7);
		
		ArmorStandData armorStandData = new ArmorStandData();
		
		switch(equipmentSlot) {
			case HEAD, CHEST, LEGS, FEET:
				armorStandData.setSmall(true);
				if(itemStack.getType() == Material.ELYTRA){
					standLocation.setY(standLocation.getY() + 0.7);
				}
				break;
			case HAND:
				armorStandData.setRightArmPose(getArmAngle(itemStack));
				standLocation.setY(standLocation.getY() + 0.7);
				if(itemStack.getType() == Material.SHIELD){
					armorStandData.setSmall(true);
				}
				break;
		}
		
		if(isCase){
			standLocation.setY(standLocation.getY() - 0.7);
			armorStandData.setSmall(false);
		}
		
		armorStandData.setLocation(standLocation);
		armorStandData.setEquipment(itemStack);
		armorStandData.setEquipmentSlot(equipmentSlot);
		
		boolean isShield = false;
		if(itemStack.getType() == Material.SHIELD){
			isShield = true;
		}
		
		//make the stand face the correct direction when it spawns
		armorStandData.setYaw(blockfaceToYaw(facing));
		//fences and bows and shields are always 90 degrees off
		if(isFence(itemStack.getType()) || itemStack.getType() == Material.BOW || isShield){ //used to have banner here too
			armorStandData.setYaw(blockfaceToYaw(nextFace(facing)));
		}
		
		return armorStandData;
	}
	
	public static EquipmentSlot getEquipmentSlot(ItemStack itemStack) {
		
		Material type = itemStack.getType();
		String sType = type.toString().toUpperCase();
		
		if(isHeldBlock(type)){
			return EquipmentSlot.HAND;
		} else if(sType.contains("_HELMET") || type == Material.PLAYER_HEAD || type.isBlock()){
			return EquipmentSlot.HEAD;
		} else if(sType.contains("_CHESTPLATE") || type == Material.ELYTRA){
			return EquipmentSlot.CHEST;
		} else if(sType.contains("_LEGGINGS")){
			return EquipmentSlot.LEGS;
		} else if(sType.contains("_BOOTS")){
			return EquipmentSlot.FEET;
		}
		return EquipmentSlot.HAND;
	}
	
	public static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, EquipmentSlot equipmentSlot) {
		
		Location standLocation = null;
		switch(equipmentSlot) {
			case HEAD:
				standLocation = blockLocation.clone().add(0.5, 0, 0.5);
				break;
			case CHEST:
				standLocation = blockLocation.clone().add(0.5, 0.4, 0.5);
				if(material == Material.ELYTRA){
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.5, -0.1, 0.2);
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.7, -0.1, 0.5);
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0.5, -0.1, 0.7);
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.3, -0.1, 0.5);
							break;
					}
				}
				break;
			case LEGS:
				standLocation = blockLocation.clone().add(0.5, 0.7, 0.5);
				break;
			case FEET:
				standLocation = blockLocation.clone().add(0.5, 0.8, 0.5);
				break;
			case HAND:
				standLocation = blockLocation;
				
				if(isTool(material)){
					double rodOffset = 0.1;
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.7, -1.3, 0.6);
							if(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK){
								standLocation = standLocation.add(rodOffset, 0, 0);
							}
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
							if(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK){
								standLocation = standLocation.add(0, 0, rodOffset);
							}
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
							if(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK){
								standLocation = standLocation.add(-rodOffset, 0, 0);
							}
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
							if(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK){
								standLocation = standLocation.add(0, 0, -rodOffset);
							}
							break;
					}
				} else if(material == Material.BOW){
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.99, -0.8, 0.84);
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.15, -0.8, 0.99);
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0, -0.8, 0.15);
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.85, -0.8, 0);
							break;
					}
				} else if(material == Material.CROSSBOW){
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.25, -1.6, 0.44);
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.6, -1.6, 0.25);
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0.8, -1.6, 0.6);
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.4, -1.6, 0.75);
							break;
					}
				} else if(material.toString().contains("BANNER") && !material.toString().contains("PATTERN")){
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.99, -1.4, 0.86);
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.12, -1.4, 1);
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0.01, -1.4, 0.12);
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.88, -1.4, 0);
							break;
					}
				}
				//the material is a simple, default item
				else {
					switch(facing) {
						case NORTH:
							standLocation = blockLocation.clone().add(0.125, -1.4, 0.95);
							break;
						case EAST:
							standLocation = blockLocation.clone().add(0.005, -1.4, 0.11);
							break;
						case SOUTH:
							standLocation = blockLocation.clone().add(0.88, -1.4, 0.005);
							break;
						case WEST:
							standLocation = blockLocation.clone().add(0.99, -1.4, 0.88);
							break;
					}
				}
				if(material == Material.SHIELD){
					standLocation.add(0, 1, 0);
					switch(facing) {
						case NORTH:
							standLocation.add(0.17, 0, -0.2);
							break;
						case EAST:
							standLocation.add(0.2, 0, 0.17);
							break;
						case SOUTH:
							standLocation.add(-0.17, 0, 0.2);
							break;
						case WEST:
							standLocation.add(-0.2, 0, -0.17);
							break;
					}
				}
				
				break;
		}
		
		boolean isShield = false;
		if(material == Material.SHIELD){
			isShield = true;
		}
		
		if(facing == null){
			facing = BlockFace.NORTH;
		}
		assert standLocation != null;
		//make the stand face the correct direction when it spawns
		standLocation.setYaw(blockfaceToYaw(facing));
		//fences and bows and shields are always 90 degrees off
		if(isFence(material) || material == Material.BOW || material.toString().contains("BANNER") || isShield){
			standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
		}
		
		//material is an item
		return standLocation;
	}
	
	public static EulerAngle getArmAngle(ItemStack itemStack) {
		
		Material material = itemStack.getType();
		
		boolean isShield = material == Material.SHIELD;
		
		if(isTool(material) && !(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)){
			return toolAngle;
		} else if(material == Material.BOW){
			return bowAngle;
		} else if(material == Material.CROSSBOW){
			return crossBowAngle;
		} else if(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK){
			return rodAngle;
		} else if(isShield){
			return shieldAngle;
		}
		return itemAngle;
	}
	
	/**
	 * Converts a BlockFace direction to a yaw (float) value
	 * Return:
	 * - float: the yaw value of the BlockFace direction provided
	 */
	public static float blockfaceToYaw(BlockFace bf) {
		if(bf.equals(BlockFace.SOUTH)){
			return 0F;
		} else if(bf.equals(BlockFace.WEST)){
			return 90F;
		} else if(bf.equals(BlockFace.NORTH)){
			return 180F;
		} else if(bf.equals(BlockFace.EAST)){
			return 270F;
		}
		return 0F;
	}
	
	public static boolean isBlock(Material material) {
		if(material.isBlock() && !material.toString().toUpperCase().contains("CHEST")){
			return true;
		}
		return false;
	}
	
	public static boolean isHeldNonItem(Material material) {
		String sType = material.toString().toUpperCase();
		return isTool(material) ||
			   sType.contains("SWORD") ||
			   material == Material.BOW ||
			   material == Material.FISHING_ROD ||
			   material == Material.CARROT_ON_A_STICK ||
			   material == Material.CROSSBOW;
	}
	
	public static boolean isTool(Material material) {
		String sMaterial = material.toString().toUpperCase();
		return (sMaterial.contains("_AXE") ||
				sMaterial.contains("_HOE") ||
				sMaterial.contains("_PICKAXE") ||
				sMaterial.contains("_SPADE") ||
				sMaterial.contains("_SWORD") ||
				sMaterial.contains("MACE") ||
				material == Material.BONE ||
				material == Material.STICK ||
				material == Material.BLAZE_ROD ||
				material == Material.CARROT_ON_A_STICK ||
				material == Material.FISHING_ROD);
	}
	
	public static boolean isFence(Material material) {
		String sMaterial = material.toString().toUpperCase();
		return (sMaterial.contains("FENCE") && (material != Material.IRON_BARS) && !sMaterial.contains("GATE"));
	}
	
	public static boolean isHeldBlock(Material material) {
		if(material.isBlock()){
			if(material.toString().contains("THIN_GLASS") || material.toString().contains("GLASS_PANE") || material.toString().contains("SAPLING")){
				return true;
			}
			switch(material) {
				case LADDER:
				case VINE:
				case RAIL:
				case POWERED_RAIL:
				case ACTIVATOR_RAIL:
				case DETECTOR_RAIL:
				case TRIPWIRE_HOOK:
				case LEVER:
				case TORCH:
				case COBWEB:
				case TALL_GRASS:
				case DEAD_BUSH:
				case POPPY:
				case DANDELION:
				case BROWN_MUSHROOM:
				case RED_MUSHROOM:
				case REDSTONE_TORCH:
				case IRON_BARS:
				case LILY_PAD:
				case HOPPER:
				case BARRIER:
				case CHORUS_PLANT:
				case STRUCTURE_VOID:
				case END_ROD:
					return true;
			}
		}
		return false;
	}
	
	public static BlockFace nextFace(BlockFace face) {
		BlockFace[] faces = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
		BlockFace direction = null;
		if(face == faces[faces.length - 1]){
			direction = faces[0];
		} else {
			for(int i = 0; i < faces.length; i++){
				if(face == faces[i]){
					direction = faces[i + 1];
					break;
				}
			}
		}
		return direction;
	}
}
