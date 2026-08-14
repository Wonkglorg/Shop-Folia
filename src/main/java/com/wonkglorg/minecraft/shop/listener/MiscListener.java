package com.wonkglorg.minecraft.shop.listener;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.config.LangManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class MiscListener implements Listener{
	
	private final Shop plugin;
	
	private final LangManager lang;
	
	public MiscListener(Shop instance) {
		plugin = instance;
		this.lang = instance.getLangManager();
	}
	
	//prevent emptying of bucket when player clicks on shop sign
	//also prevent when emptying on display item itself
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		if(event.isCancelled()){
			return;
		}
		
		Block b = event.getBlockClicked();
		
		if(b.getBlockData() instanceof WallSign){
			AbstractShop shop = plugin.getShopmanager().getShopBySign(b.getLocation());
			if(shop != null){
				event.setCancelled(true);
			}
		}
		Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
		AbstractShop shop = plugin.getShopmanager().getShopByContainer(blockToFill.getRelative(BlockFace.DOWN));
		if(shop != null){
			event.setCancelled(true);
		}
	}
}