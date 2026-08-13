package com.snowgears.shop.shop.creation;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class CommandCreationProcess extends ShopCreationProcess{
	protected CommandCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		super(player, sign, container, signDirection);
	}
	//done via commands by looking at the sign and typing /create shop
	// instead of doing it via sign
}
