package com.wonkglorg.minecraft.shop.listener;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.GambleShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DisplayListener implements Listener{
	
	public final Main plugin;
	private static final Random RANDOM = ThreadLocalRandom.current();
	private final List<ItemStack> allServerRecipeResults = new ArrayList<>();
	
	public DisplayListener(Main instance) {
		plugin = instance;
		
		// Load all recipes on server once all other plugins are loaded
		plugin.getFoliaLib().getScheduler().runLater(_ -> {
			HashMap<ItemStack, Boolean> recipes = new HashMap<>();
			Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
			while(recipeIterator.hasNext()){
				ItemStack result = recipeIterator.next().getResult();
				if(result.getAmount() != 0){
					recipes.put(result, true);
				}
			}
			allServerRecipeResults.addAll(recipes.keySet());
			Collections.shuffle(allServerRecipeResults);
		}, 1);
	}
	
	public ItemStack getRandomItem(AbstractShop shop) {
		if(shop == null){
			return new ItemStack(Material.AIR);
		}
		
		if(InventoryUtils.isEmpty(shop.getInventory())){
			int index = RANDOM.nextInt(allServerRecipeResults.size());
			return allServerRecipeResults.get(index);
		} else {
			return InventoryUtils.getRandomItem(shop.getInventory());
		}
	}
	
	@EventHandler
	public void onWaterFlow(BlockFromToEvent event) {
		AbstractShop shop = plugin.getShopmanager().getShopByContainer(event.getToBlock().getRelative(BlockFace.DOWN));
		if(shop != null){
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent event) {
		AbstractShop shop = plugin.getShopmanager().getShopByContainer(event.getBlock()
		                                                                    .getRelative(event.getDirection())
		                                                                    .getRelative(BlockFace.DOWN));
		if(shop != null && shop.getDisplay().getType() != DisplayType.NONE){
			event.setCancelled(true);
		}
		
		shop = plugin.getShopmanager().getShopByContainer(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.UP));
		if(shop != null){
			event.setCancelled(true);
		}
		
		for(Block pushedBlock : event.getBlocks()){
			shop = plugin.getShopmanager().getShopByContainer(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
			if(shop != null && shop.getDisplay().getType() != DisplayType.NONE){
				event.setCancelled(true);
				return;
			}
			
			shop = plugin.getShopmanager().getShopByContainer(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.UP));
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
		AbstractShop shop = plugin.getShopmanager().getShopByContainer(pulledBlock);
		if(shop != null){
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		AbstractShop shop = plugin.getShopmanager().getShopByContainer(event.getBlock().getRelative(BlockFace.DOWN));
		if(shop != null){
			if(shop.getDisplay().getType() == null && plugin.getSettingsConfig().getDisplayTypeDefault() != DisplayType.NONE){
				event.setCancelled(true);
			} else if(shop.getDisplay().getType() != null && shop.getDisplay().getType() != DisplayType.NONE){
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopInventoryClose(InventoryCloseEvent event) {
		if(event.getInventory().getHolder() instanceof Container container){
			AbstractShop shop = plugin.getShopmanager().getShopByContainer(container.getBlock());
			
			if(shop == null){
				return;
			}
			
			shop.updateStock();
			
			//make sure to set gamble item again if player set it to new custom items
			if(shop.getType() == ShopType.GAMBLE){
				((GambleShop) shop).setGambleItem();
			}
		}
		//for some reason, DoubleChest does not extend Container like Chest does
		else if(event.getInventory().getHolder() instanceof DoubleChest doubleChest){
			AbstractShop shop = plugin.getShopmanager().getShopByContainer(doubleChest.getLocation().getBlock());
			
			if(shop == null){
				return;
			}
			
			shop.updateStock();
			
			//make sure to set gamble item again if player set it to new custom items
			if(shop.getType() == ShopType.GAMBLE){
				((GambleShop) shop).setGambleItem();
			}
		}
	}
}
