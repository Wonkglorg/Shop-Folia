package com.wonkglorg.minecraft.shop.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;

public class ChestUtil{
	private ChestUtil() {
		//utility class
	}
	
	public static BlockFace getOtherChestDirection(Chest.Type chestType, BlockFace facing) {
		return switch(chestType) {
			case LEFT -> switch(facing) {
				case NORTH -> BlockFace.EAST;
				case EAST -> BlockFace.SOUTH;
				case SOUTH -> BlockFace.WEST;
				case WEST -> BlockFace.NORTH;
				default -> null;
			};
			
			case RIGHT -> switch(facing) {
				case NORTH -> BlockFace.WEST;
				case EAST -> BlockFace.NORTH;
				case SOUTH -> BlockFace.EAST;
				case WEST -> BlockFace.SOUTH;
				default -> null;
			};
			
			case SINGLE -> null;
		};
	}
	
}
