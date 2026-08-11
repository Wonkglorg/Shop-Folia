package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.manager.ShopManager;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToDestroyShop;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToDestroyShopOther;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.CreationWord;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.shop.creation.SignCreationProcess;
import com.snowgears.shop.shop.display.DisplayTagOption;
import static com.snowgears.shop.util.ChestUtil.getOtherChestDirection;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.ShopClickType;
import com.snowgears.shop.util.ShopMessage;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.util.Components;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;

public class ShopListener implements Listener{
	
	private final Shop plugin;
	private final LangManager lang;
	private final SettingsConfig settingsConfig;
	private final ShopManager shopManager;
	
	public ShopListener(Shop instance) {
		plugin = instance;
		lang = plugin.getLangManager();
		settingsConfig = plugin.getSettingsConfig();
		shopManager = plugin.getShopmanager();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopCreation(SignChangeEvent event) {
		final Block block = event.getBlock();
		final Player player = event.getPlayer();
		
		if(!settingsConfig.isAllowCreateMethodSign()){
			return;
		}
		
		if(!(block.getState() instanceof WallSign wallSign)){
			return;
		}
		
		Sign sign = (Sign) block.getBlockData();
		
		BlockFace signDirection = wallSign.getFacing();
		Block chest = block.getRelative(signDirection.getOppositeFace());
		
		if(!shopManager.isAllowedContainer(chest)){
			return;
		}
		
		if(shopManager.isCreatingShop(player)){
			//player is already creating another shop do nothing here
			return;
		}
		
		//no creation word is present not a shop creation.
		if(!Components.toPlainText(event.line(0)).toLowerCase().contains(plugin.getSettingsConfig()
		                                                                       .getCreationWord(CreationWord.SHOP)
		                                                                       .toLowerCase())){
			return;
		}
		
		if(shopManager.getShopByContainer(chest.getLocation()) != null){
			Shop.getPlugin().getLangManager().request("interaction_issue.createOtherShop").sendToAudience(player);
			event.setCancelled(true);
			return;
		}
		
		final SignCreationProcess process = new SignCreationProcess(player, sign, chest, signDirection);
		
		//do some checks with the provided data to verify the player has all required things to create a shop here
		if(!process.canPlayerFulfillsCreationRequirements()){
			return;
		}
		
		if(!process.readSignLines(event.lines())){
			return;
		}
		PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, process.toImmutableProgress());
		Shop.getPlugin().getServer().getPluginManager().callEvent(e);
		if(!event.isCancelled()){
			e.setCancelled(true);
			return;
		}
		shopManager.getDatabase().logAction(player, process.getPlayerUUID(), process.getShopId(), ShopActionType.CREATE);
		shopManager.addPlayerShopCreation(player, process);
	}
	
	@EventHandler
	public void onShopInitialisation(PlayerInteractEvent event) {
		
		if(event.getHand() != EquipmentSlot.HAND){
			return; // off hand version, ignore.
		}
		
		final Player player = event.getPlayer();
		
		if(event.getAction() != Action.LEFT_CLICK_BLOCK){
			return;
		}
		if(shopManager.is){
		
		}
		
		final Block clicked = event.getClickedBlock();
		
		if(clicked == null){
			return;
		}
		
		if(!(clicked.getBlockData() instanceof WallSign)){
			return;
		}
		
		if(!settingsConfig.isAllowCreateMethodSign()){
			return;
		}
		
		// We only want to handle shops that exist but are not initialized.
		AbstractShop shop = shopManager.getShopCreationProcess(clicked.getLocation());
		if(shop == null || shop.isInitialized()){
			return;
		}
		
		//creative selection listener will handle if item is null
		if(event.getItem() == null || event.getItem().getType() == Material.AIR){
			return;
		}
		
		boolean initializedShop;
		if(shop.getType() == ShopType.BARTER && shop.getItemStack() != null && shop.getSecondaryItemStack() == null){
			initializedShop = plugin.getShopCreationUtil().initializeShop(shop, player, shop.getItemStack(), event.getItem());
		} else {
			initializedShop = plugin.getShopCreationUtil().initializeShop(shop, player, event.getItem(), null);
		}
		
		if(initializedShop){
			plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
			shopManager.registerShop(shop);
			shopManager.finishShopCreation(shop.getSignLocation());
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onShopSignClick(PlayerInteractEvent event) {
		//only do main hand, otherwise it fires twice
		if(event.getHand() != EquipmentSlot.HAND){
			return;
		}
		
		if(event.getAction() != Action.LEFT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_BLOCK){
			return;
		}
		
		Block block = event.getClickedBlock();
		if(block == null){
			return;
		}
		
		if(!(block.getBlockData() instanceof WallSign)){
			return;
		}
		
		AbstractShop shop = shopManager.getShopBySign(block.getLocation());
		if(shop == null || !shop.isInitialized()){
			return;
		}
		
		Player player = event.getPlayer();
		
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
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopContainerClick(PlayerInteractEvent event) {
		//only do main hand, otherwise it fires twice
		if(event.getHand() != EquipmentSlot.HAND){
			return;
		}
		
		if(event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK){
			return;
		}
		
		Block block = event.getClickedBlock();
		if(block == null){
			return;
		}
		
		if(!shopManager.isAllowedContainer(block)){
			return;
		}
		
		AbstractShop shop = shopManager.getShopByContainer(block);
		if(shop == null){
			return;
		}
		
		final Player player = event.getPlayer();
		switch(event.getAction()) {
			case LEFT_CLICK_BLOCK -> leftClickShopContainer(event, shop, player);
			case RIGHT_CLICK_BLOCK -> rightClickShopContainer(event, shop, player);
			default -> throw new IllegalStateException("Unexpected click action during container click event: " + event.getAction());
		}
	}
	
	private void leftClickShopContainer(PlayerInteractEvent event, AbstractShop shop, Player player) {
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
	
	private void rightClickShopContainer(PlayerInteractEvent event, AbstractShop shop, Player player) {
		if((!shopManager.isAllowedContainer(shop.getContainerLocation().getBlock())) ||
		   !(shop.getSignLocation().getBlock().getBlockData() instanceof WallSign)){
			plugin.logger().warning("Deleting Shop because chest does not exist, or sign is not exist! " + shop);
			shopManager.unregisterShop(shop);
			return;
		}
		
		//player is sneaking and clicks a chest of a shop
		if(player.isSneaking()){
			//don't execute the action and cancel event if player is holding a sign (may be trying to place directly onto chest)
			if(Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())){
				return;
			}
			
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
					LangRequest request = lang.request("interaction." + shop.getType().toString() + ".opOpen");
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
	
	@EventHandler
	public void onExplosion(EntityExplodeEvent event) {
		//save all potential shop blocks
		Iterator<Block> blockIterator = event.blockList().iterator();
		AbstractShop shop = null;
		while(blockIterator.hasNext()){
			
			Block block = blockIterator.next();
			if(Tag.WALL_SIGNS.isTagged(block.getType())){
				shop = shopManager.getShopBySign(block.getLocation());
			} else if(shopManager.isAllowedContainer(block)){
				shop = shopManager.getShopByContainer(block);
			}
			
			if(shop != null){
				blockIterator.remove();
			}
		}
	}
	
	@EventHandler
	public void onHopperPlacementBelowShop(BlockPlaceEvent event) {
		Block b = event.getBlockPlaced();
		Player player = event.getPlayer();
		
		if(b.getType() == Material.HOPPER){
			AbstractShop shop = shopManager.getShopByContainer(b.getRelative(BlockFace.UP));
			if(shop != null){
				if(!player.isOp() && !shop.getOwnerUUID().equals(player.getUniqueId())){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onLogin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		PlayerManager.loadProfile(player);
		PlayerNameCache.cacheName(player.getUniqueId(), player.getName());
		
		shopManager.removeOutdatedShops();
		
		if(!player.hasPlayedBefore()){
			return;
		}
		
		//todo:mjd properly implement offline handling.
		//if(plugin.getSettingsConfig().isOfflinePurchaseNotificationsEnabled()){
		//	OfflineTransactions offlineTransactions = new OfflineTransactions(player.getUniqueId(), player.getLastLogin());
		//	transactionsWhileOffline.put(player.getUniqueId(), offlineTransactions);
		//}
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerManager.removeProfile(player);
		
		shopManager.getDisplayManager().clearDisplaysForPlayer(player);
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		shopManager.processUnloadedShopsInChunk(event.getChunk());
	}
	
	//player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onShopDestroy(BlockBreakEvent event) {
		Block b = event.getBlock();
		Player player = event.getPlayer();
		
		if(b.getBlockData() instanceof WallSign){
			AbstractShop shop = shopManager.getShopBySign(b.getLocation());
			if(shop == null){
				return;
			}
			breakShopSign(event, shop);
			return;
		}
		
		if(shopManager.isAllowedContainer(b)){
			// Shop will not exist in ShopHandler if it is in the middle of a shop creation process
			// protect shops that are in the middle of a shop creation process from being destroyed
			if(Shop.getPlugin().getShopmanager().isContainerInShopCreationProcess(b.getLocation())){
				lang.request("interaction_issue.destroyUninitializedChest").sendToAudience(player);
				event.setCancelled(true); // don't break chest
				return;
			}
			
			AbstractShop shop = shopManager.getShopByContainer(b);
			if(shop == null){
				return;
			}
			breakShopContainer(event, shop);
		}
	}
	
	@EventHandler
	public void onShopContainerExpansion(BlockPlaceEvent event) {
		Block b = event.getBlockPlaced();
		Player player = event.getPlayer();
		
		//not whitelisted by the config
		if(!shopManager.isAllowedContainer(b)){
			return;
		}
		
		//only chests can be expanded
		if(!(b.getBlockData() instanceof Chest placedChest)){
			return;
		}
		
		//its a single chest after placement, no need to further check
		if(placedChest.getType() == Type.SINGLE){
			return;
		}
		
		//calculate the other half of the chest if its a double chest
		BlockFace otherChestDirection = getOtherChestDirection(placedChest.getType(), placedChest.getFacing());
		
		if(otherChestDirection == null){
			return;
		}
		
		Block otherChest = b.getRelative(otherChestDirection);
		
		AbstractShop shop = shopManager.getShopByContainer(otherChest);
		
		//check if a shop is there and if its the same chest type (should not be the case but just in case)
		if(shop == null || (b.getType() != shop.getContainerLocation().getBlock().getType())){
			return;
		}
		
		// if placer is not owner and not op they are not allowed to place it
		if(!shop.getOwnerUUID().equals(player.getUniqueId()) && !isOperator(player)){
			event.setCancelled(true);
			return;
		}
		
		PlayerResizeShopEvent resizeEvent = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
		Bukkit.getPluginManager().callEvent(resizeEvent);
		
		if(resizeEvent.isCancelled()){
			event.setCancelled(true);
		}
	}
	
	/**
	 * Called when a shop sign is trying to be broken
	 */
	private void breakShopSign(@NonNull BlockBreakEvent event, AbstractShop shop) {
		Player player = event.getPlayer();
		// Disable dropping sign if its fake
		if(shop.isFakeSign()){
			event.setDropItems(false);
		}
		if(!shop.isInitialized()){
			event.setCancelled(true);
			return;
		}
		
		if(settingsConfig.isDestroyShopRequiresSneak() && !player.isSneaking()){
			event.setCancelled(true);
			Shop.getPlugin().logger().trace("[MiscListener.shopDestroy : getDestroyShopRequiresSneak] updateSign");
			shop.updateSign();
			return;
		}
		
		//player trying to break their own shop
		if(shop.getOwnerUUID().equals(player.getUniqueId())){
			if(!isAllowedToDestroyShop(player, shop.getType())){
				ShopMessage.request("permission.destroy", player, shop).sendToAudience(player);
				event.setCancelled(true);
				return;
			}
		} else { //trying to break other shop
			if(!isAllowedToDestroyShopOther(player)){
				ShopMessage.request("permission.destroyOther", player, shop).sendToAudience(player);
				event.setCancelled(true);
				return;
			}
		}
		
		PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
		plugin.getServer().getPluginManager().callEvent(e);
		if(e.isCancelled()){
			event.setCancelled(true);
			return;
		}
		
		//if no one canceled the destroy event remove any cost if one exists.
		double cost = settingsConfig.getDestructionCost();
		if(cost > 0){
			// Check for funds
			if(!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)){
				ShopMessage.request("interaction_issue.destroyInsufficientFunds", player, shop).sendToAudience(player);
				event.setCancelled(true);
				return;
			}
			// Remove funds
			boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
			if(!removed){
				ShopMessage.request("interaction_issue.destroyInsufficientFunds", player, shop).sendToAudience(player);
				event.setCancelled(true);
				return;
			}
		}
		
		shopManager.getDatabase().logAction(player, shop, ShopActionType.DESTROY);
		
		if((!shop.isAdmin()) && settingsConfig.isReturnCreationCost() && settingsConfig.getCreationCost() > 0){
			if(settingsConfig.getCurrencyType() != CurrencyType.ITEM){
				EconomyUtils.addFunds(shop.getOwner(), player.getInventory(), settingsConfig.getCreationCost());
			} else {
				ItemStack currencyDrop = plugin.getItemConfig().getCurrencyItem().clone();
				currencyDrop.setAmount((int) settingsConfig.getCreationCost());
				shop.getContainerLocation().getWorld().dropItemNaturally(shop.getContainerLocation(), currencyDrop);
			}
		}
		ShopMessage.request("interaction." + shop.getType().toString() + ".destroy", player, shop).sendToAudience(player);
		//remove the whole shop registration
		shopManager.unregisterShop(shop);
	}
	
	/**
	 * Called when a shop container is being broken
	 */
	private void breakShopContainer(BlockBreakEvent event, AbstractShop shop) {
		Player player = event.getPlayer();
		// since we are dealing with an existing shop, cancel the event so that
		// we can explicitly "uncancel" it later if we want to allow the chest to be broken.
		event.setCancelled(true);
		
		//this is the primary container of the shop, do not allow destruction, the sign needs to be broken not the chest.
		Location blockLocation = event.getBlock().getLocation();
		
		if(shop.getContainerLocation().equals(blockLocation)){
			if(shop.getOwnerUUID().equals(player.getUniqueId()) || isAllowedToDestroyShopOther(player)){
				ShopMessage.request("interactionIssue.destroyChest", player, shop).sendToAudience(player);
				shop.sendEffects(false, player);
			} else {
				ShopMessage.request("permission.destroyOther", player, shop).sendToAudience(player);
			}
			return;
		}
		
		if(shop.getOwnerUUID().equals(player.getUniqueId()) || isAllowedToDestroyShopOther(player)){
			PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, blockLocation, false);
			Bukkit.getPluginManager().callEvent(e);
			
			if(e.isCancelled()){
				event.setCancelled(true);
				return;
			}
			// Explicitly allow the chest to be broken since it is the "Expansion" chest
			// we need to uncancel the event so that the chest can be broken.
			event.setCancelled(false);
			Shop.getPlugin().getShopmanager().removeSecondaryChestLocation(shop.getSecondaryContainerLocation(), shop);
		}
	}
}