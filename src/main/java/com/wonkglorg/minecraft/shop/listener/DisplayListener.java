package com.wonkglorg.minecraft.shop.listener;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public record DisplayListener(ShopPlugin plugin) implements Listener{
	
	@EventHandler
	public void onWaterFlow(BlockFromToEvent event) {
		AbstractShop shop = shopManager().getShopByContainer(event.getToBlock().getRelative(BlockFace.DOWN));
		if(shop != null){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent event) {
		AbstractShop shop = shopManager().getShopByContainer(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
		if(shop != null && shop.getDisplay().getType() != DisplayType.NONE){
			event.setCancelled(true);
		}
		
		shop = shopManager().getShopByContainer(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.UP));
		if(shop != null){
			event.setCancelled(true);
		}
		
		for(Block pushedBlock : event.getBlocks()){
			shop = shopManager().getShopByContainer(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
			if(shop != null && shop.getDisplay().getType() != DisplayType.NONE){
				event.setCancelled(true);
				return;
			}
			
			shop = shopManager().getShopByContainer(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.UP));
			if(shop != null){
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent event) {
		Block pulledBlock = event.getBlock()
								 .getRelative(event.getDirection().getOppositeFace())
								 .getRelative(event.getDirection().getOppositeFace())
								 .getRelative(BlockFace.UP);
		AbstractShop shop = shopManager().getShopByContainer(pulledBlock);
		if(shop != null){
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		AbstractShop shop = shopManager().getShopByContainer(event.getBlock().getRelative(BlockFace.DOWN));
		if(shop != null && shop.getDisplay().getType() != DisplayType.NONE){
			event.setCancelled(true);
		}
		
	}
}
