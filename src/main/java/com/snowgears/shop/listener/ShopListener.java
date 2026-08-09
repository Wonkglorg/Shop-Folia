package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.manager.PlayerManager;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.OfflineTransactions;
import com.snowgears.shop.util.PlayerExperience;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopClickType;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ShopListener implements Listener{
	
	private Shop plugin;
	private final LangManager lang;
	private HashMap<UUID, OfflineTransactions> transactionsWhileOffline = new HashMap<>();
	
	public ShopListener(Shop instance) {
		plugin = instance;
		lang = plugin.getLangManager();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		plugin.getFoliaLib().getScheduler().runLater(() -> {
			// Cache player name for performance optimization
			PlayerManager.loadProfile(event.getPlayer());
			PlayerNameCache.cacheName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
		}, 5);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onShopSignClick(PlayerInteractEvent event) {
		if(event.getHand() == EquipmentSlot.OFF_HAND){
			return; // off hand version, ignore.
		}
		
		Player player = event.getPlayer();
		
		//player clicked the sign of a shop
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK){
			if(event.getClickedBlock().getBlockData() instanceof WallSign){
				AbstractShop shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
				if(shop == null || !shop.isInitialized()){
					return;
				}
				
				boolean actionPerformed;
				if(player.isSneaking()){
					if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
						actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_RIGHT_CLICK_SIGN);
					} else {
						actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_LEFT_CLICK_SIGN);
					}
				} else {
					if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
						actionPerformed = shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_SIGN);
					} else {
						actionPerformed = shop.executeClickAction(event, ShopClickType.LEFT_CLICK_SIGN);
					}
				}
				if(actionPerformed){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopChestClick(PlayerInteractEvent event) {
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(plugin.getShopHandler().isAllowedContainer(event.getClickedBlock())){
				try{
					if(event.getHand() == EquipmentSlot.OFF_HAND){
						return; // off hand version, ignore.
					}
				} catch(NoSuchMethodError error){
				}
				
				Player player = event.getPlayer();
				AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
				if(shop == null){
					return;
				}
				
				if((!plugin.getShopHandler().isAllowedContainer(shop.getChestLocation().getBlock())) ||
				   !(shop.getSignLocation().getBlock().getBlockData() instanceof WallSign)){
					plugin.logger().warning("Deleting Shop because chest does not exist, or sign is not exist! " + shop);
					shop.delete();
					return;
				}
				
				//player is sneaking and clicks a chest of a shop
				if(player.isSneaking()){
					//don't execute the action and cancel event if player is holding a sign (may be trying to place directly onto chest)
					if(!Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())){
						
						boolean actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_RIGHT_CLICK_CHEST);
						
						if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
							shop.getDisplay().showDisplayTags(player);
						}
						
						if(actionPerformed){
							event.setCancelled(true);
							// Stop processing since we cancelled the event, if no action was performed, continue with logic below
							return;
						}
					}
				}
				//non-owner is trying to open shop
				if(!shop.getOwnerUUID().equals(player.getUniqueId())){
					if(isOperator(player)){
						if(shop.isAdmin()){
							if(shop.getType() == ShopType.GAMBLE){
								//allow gamble shops to be opened by operators
								return;
							}
							event.setCancelled(true);
							
							shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_CHEST);
							//we are cancelling this event regardless so no need to check if the action was performed
							
						} else {
							LangRequest request = lang.request("interaction." + shop.getType().toString() + "opOpen");
							AbstractShop.shopPlaceholders(request, shop);
							request.sendToAudience(player);
						}
					} else {
						// Cancel event to prevent other players from opening the chest
						event.setCancelled(true);
						
						boolean actionPerformed = shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_CHEST);
						if(!actionPerformed){
							LangRequest request = lang.request("permission.error.openOther");
							AbstractShop.shopPlaceholders(request, shop);
							request.sendToAudience(player);
						}
						
						if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
							shop.getDisplay().showDisplayTags(player);
						}
					}
				}
			}
		} else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
			if(plugin.getShopHandler().isAllowedContainer(event.getClickedBlock())){
				try{
					if(event.getHand() == EquipmentSlot.OFF_HAND){
						return; // off hand version, ignore.
					}
				} catch(NoSuchMethodError error){
				}
				
				Player player = event.getPlayer();
				AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
				if(shop == null){
					return;
				}
				
				boolean actionPerformed;
				if(player.isSneaking()){
					actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_LEFT_CLICK_CHEST);
				} else {
					actionPerformed = shop.executeClickAction(event, ShopClickType.LEFT_CLICK_CHEST);
				}
				if(actionPerformed){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onExplosion(EntityExplodeEvent event) {
		//save all potential shop blocks (for sake of time during explosion)
		Iterator<Block> blockIterator = event.blockList().iterator();
		AbstractShop shop = null;
		while(blockIterator.hasNext()){
			
			Block block = blockIterator.next();
			if(Tag.WALL_SIGNS.isTagged(block.getType())){
				shop = plugin.getShopHandler().getShop(block.getLocation());
			} else if(plugin.getShopHandler().isAllowedContainer(block)){
				shop = plugin.getShopHandler().getShopByChest(block);
			}
			
			if(shop != null){
				blockIterator.remove();
			}
		}
	}
	
	@EventHandler
	public void onShopExpansion(BlockPlaceEvent event) {
		Block b = event.getBlockPlaced();
		Player player = event.getPlayer();
		
		if(b.getType() == Material.HOPPER){
			AbstractShop shop = plugin.getShopHandler().getShopByChest(b.getRelative(BlockFace.UP));
			if(shop != null){
				if(!player.isOp() && !shop.getOwnerUUID().equals(player.getUniqueId())){
					event.setCancelled(true);
				}
			}
		}
	}
	
	//REMOVING AND REPLACING WITH CHECK FOR PLACING HOPPERS (was slowing down servers with many hoppers)
	//    @EventHandler (priority = EventPriority.HIGHEST)
	//    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
	//        /* DO NOT USE InventoryMoveItemEvent IT CAUSES SO MUCH LAG */
	//    }
	@EventHandler
	public void onLogin(PlayerJoinEvent event) {
		//delete all shops from players that have not played in X amount of hours (if configured)
		int hoursOfflineToRemoveShops = plugin.getSettingsConfig().getHoursOfflineToRemoveShops();
		if(hoursOfflineToRemoveShops != 0){
			for(OfflinePlayer offlinePlayer : plugin.getShopHandler().getShopOwners()){
				if(offlinePlayer.getName() != null){
					long msSinceLastPlayed = System.currentTimeMillis() - offlinePlayer.getLastPlayed();
					long hoursSinceLastPlayed = TimeUnit.MILLISECONDS.toHours(msSinceLastPlayed);
					
					if(hoursSinceLastPlayed >= hoursOfflineToRemoveShops){
						for(AbstractShop shop : plugin.getShopHandler().getShops(offlinePlayer.getUniqueId())){
							plugin.logger().notice("Deleting Shop because player " +
							                       offlinePlayer.getName() +
							                       " has not logged in within the required " +
							                       (int) hoursSinceLastPlayed +
							                       " hours! " +
							                       shop);
							shop.delete();
						}
					}
				}
			}
		}
		final Player player = event.getPlayer();
		
		plugin.getFoliaLib().getScheduler().runLater(() -> {
			if(plugin.getSettingsConfig().getCurrencyType() == CurrencyType.EXPERIENCE){
				PlayerExperience exp = PlayerExperience.loadFromFile(player);
				if(exp != null){
					exp.apply();
				}
			}
			plugin.getShopHandler().clearShopDisplaysNearPlayer(player);
			// Force process shop displays on login - ignore movement threshold
			plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
		}, 20);
		
		//setup a repeating task that checks if async sql calculations are still running, if they are done, send messages and cancel task
		OfflineTransactions offlineTransactions = transactionsWhileOffline.get(player.getUniqueId());
		if(offlineTransactions != null){
			BukkitRunnable runnable = new BukkitRunnable(){
				public void run() {
					if(transactionsWhileOffline.containsKey(player.getUniqueId())){
						if(offlineTransactions != null && !offlineTransactions.isCalculating()){
							//only display the message if some transactions happened while they were offline
							if(offlineTransactions.getNumTransactions() > 0){
								//List<String> messageList = ShopMessage.getUnformattedMessageList("offline", "summary");
								//for(String message : messageList){
								//	ShopMessage.sendMessage(message, player, offlineTransactions);
								//}
							}
							transactionsWhileOffline.remove(player.getUniqueId());
						}
					}
				}
			};
			WrappedTask task = plugin.getFoliaLib().getScheduler().runTimer(runnable, 1, 20);
			// Let it attempt to run for 5 seconds before cancelling
			plugin.getFoliaLib().getScheduler().runLater(() -> {
				plugin.getFoliaLib().getScheduler().cancelTask(task);
			}, 100);
		}
	}
	
	@EventHandler
	public void onLogin(AsyncPlayerPreLoginEvent event) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUniqueId());
		long lastPlayed = player.getLastPlayed();
		
		//create an object that will calculate offline transactions (if sql is being used)
		if(plugin.getSettingsConfig().isOfflinePurchaseNotificationsEnabled()){
			OfflineTransactions offlineTransactions = new OfflineTransactions(player.getUniqueId(), lastPlayed);
			transactionsWhileOffline.put(event.getUniqueId(), offlineTransactions);
		}
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerManager.removeProfile(player);
		
		// Clear shop displays and connection cache for this player
		plugin.getShopHandler().clearShopDisplaysNearPlayer(player);
		
		if(plugin.getSettingsConfig().getCurrencyType() == CurrencyType.EXPERIENCE){
			//this automatically saves to file
			new PlayerExperience(player);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		
		// Skip shop display processing if player is in creative selection mode
		CreativeSelectionListener creativeModeListener = plugin.getCreativeSelectionListener();
		if(creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)){
			plugin.logger().debug("Skipping shop display refresh for " + player.getName() + " (in creative selection)");
			return;
		}
		
		// Immediate attempt right after teleport
		plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
		
		// Staggered display updates after teleport
		// First delayed attempt - wait for chunks to load
		plugin.getFoliaLib().getScheduler().runLater(() -> {
			if(player.isOnline()){
				// Check again inside the delayed task in case player entered selection during the delay
				if(creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)){
					plugin.logger().debug("Skipping delayed shop display refresh for " + player.getName() + " (in creative selection)");
					return;
				}
				plugin.logger().debug("First display refresh for " + player.getName() + " after teleport");
				plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
			}
		}, 5); // 5 ticks (250ms) delay
		
		// Second attempt - for completeness
		plugin.getFoliaLib().getScheduler().runLater(() -> {
			if(player.isOnline()){
				// Check again inside the delayed task in case player entered selection during the delay
				if(creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)){
					plugin.logger().debug("Skipping delayed shop display refresh for " + player.getName() + " (in creative selection)");
					return;
				}
				plugin.logger().debug("Second display refresh for " + player.getName() + " after teleport");
				plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
			}
		}, 15); // 750ms delay
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		// Also rebuild shop displays for any players near this chunk
		// This ensures displays reappear after chunk unload/load cycles
		plugin.getShopHandler().rebuildDisplaysInChunk(event.getChunk());
	}
}