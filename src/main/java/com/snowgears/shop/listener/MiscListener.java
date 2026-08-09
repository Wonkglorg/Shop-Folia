package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.manager.PlayerManager;
import static com.snowgears.shop.manager.PlayerManager.addShopCreationProcess;
import static com.snowgears.shop.manager.PlayerManager.cancelShopCreationProcess;
import static com.snowgears.shop.manager.PlayerManager.cleanupShopCreationProcess;
import static com.snowgears.shop.manager.PlayerManager.getShopCreationProcess;
import static com.snowgears.shop.manager.PlayerManager.isInShopCreationProcess;
import com.snowgears.shop.manager.player.PlayerProfile;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToDestroyShop;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToDestroyShopOther;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.CreationWord;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.PlaceholderContext;
import com.snowgears.shop.util.PricePair;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.ShopCreationProcess;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.util.Components;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MiscListener implements Listener{
	
	private final Shop plugin;
	private final SettingsConfig settingsConfig;
	private final LangManager lang;
	private HashMap<UUID, Long> lastChatCreation = new HashMap<>();
	
	public MiscListener(Shop instance) {
		plugin = instance;
		settingsConfig = instance.getSettingsConfig();
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
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopCreation(SignChangeEvent event) {
		final Block b = event.getBlock();
		final Player player = event.getPlayer();
		
		if(!settingsConfig.isAllowCreateMethodSign()){
			return;
		}
		
		if(!(b.getState() instanceof Sign sign)){
			return;
		}
		
		BlockFace signDirection = null;
		Block chest = null;
		if(b.getBlockData() instanceof WallSign wallSign){
			signDirection = wallSign.getFacing();
			chest = b.getRelative(signDirection.getOppositeFace());
		} else if(b.getBlockData() instanceof Rotatable rotatable){ //regular sign post
			signDirection = rotatable.getRotation();
			//adjust the sign direction to cordinal direction if its not already one
			if(signDirection.toString().indexOf('_') != -1){
				String adjustedDirString = signDirection.toString().substring(0, signDirection.toString().indexOf('_'));
				signDirection = BlockFace.valueOf(adjustedDirString);
			}
			chest = b.getRelative(signDirection.getOppositeFace());
		} else {
			return;
		}
		
		int amount = 0;
		ShopType type = null;
		boolean isAdmin = false;
		if(plugin.getShopmanager().isAllowedContainer(chest)){
			if(Components.toPlainText(event.line(0)).toLowerCase().contains(plugin.getSettingsConfig()
			                                                                      .getCreationWord(CreationWord.SHOP)
			                                                                      .toLowerCase())){
				
				if(!plugin.getShopCreationUtil().shopCanBeCreated(player, chest)){
					cancelShopCreationProcess(player);
					event.setCancelled(true);
					return;
				}
				
				try{
					String line2 = UtilMethods.cleanNumberText(Components.toPlainText(event.line(1)));
					amount = Integer.parseInt(line2);
					if(amount < 1){
						lang.request("interaction_issue.createLine2").sendToAudience(player);
						lang.request("interaction_issue.createCancel").sendToAudience(player);
						cancelShopCreationProcess(player);
						event.setCancelled(true);
						return;
					}
				} catch(NumberFormatException e){
					lang.request("interaction_issue.createLine2").sendToAudience(player);
					lang.request("interaction_issue.createCancel").sendToAudience(player);
					cancelShopCreationProcess(player);
					event.setCancelled(true);
					return;
				}
				
				String line = Components.toPlainText(event.line(3));
				type = plugin.getShopCreationUtil().getShopType(line);
				isAdmin = plugin.getShopCreationUtil().getShopIsAdmin(line);
				
				if(type == null){
					type = ShopType.SELL;
				}
				
				PricePair pricePair = plugin.getShopCreationUtil().getShopPricePair(player, event.getLine(2), type);
				if(pricePair == null){
					event.setCancelled(true);
					return;
				}
				
				AbstractShop shop = plugin.getShopCreationUtil().createShop(player,
						chest,
						sign.getBlock(),
						pricePair,
						amount,
						isAdmin,
						type,
						signDirection,
						false);
				if(shop == null){
					event.setCancelled(true);
					return;
				}
				
				ShopCreationProcess process = new ShopCreationProcess(player, chest, signDirection);
				process.setStep(ShopCreationProcess.ChatCreationStep.SIGN_ITEM);
				addShopCreationProcess(player.getUniqueId(), process);
				
				process.displayFloatingText(type + ".initialize");
				
				//give player a limited amount of time to finish creating the shop until it is deleted
				plugin.getFoliaLib().getScheduler().runLater(() -> {
					//the shop has still not been initialized with an item from a player
					if(!shop.isInitialized()){
						plugin.getShopmanager().unregisterShop(shop);
						if(b.getBlockData() instanceof WallSign){
							List<Component> lines = ShopMessage.getSignLines("timeout", shop);
							SignSide side = sign.getSide(Side.FRONT);
							side.line(0, lines.get(0));
							side.line(1, lines.get(1));
							side.line(2, lines.get(2));
							side.line(3, lines.get(3));
							sign.update(true);
							cancelShopCreationProcess(player);
						}
					}
				}, 30 * 20); // 30 seconds * 20 ticks
			}
		}
	}
	
	public boolean isChestInShopCreationProcess(Location location) {
		for(ShopCreationProcess process : PlayerManager.getPLAYER_SHOP_CREATION_STEP().values()){
			if(process.getClickedChest().getLocation().equals(location)){
				return true;
			}
		}
		return false;
	}
	
	// Fired anytime a player interacts with a block, air, or entity.
	// This is used to handle shop creation and initialization.
	// Generally, events shouldn't be cancelled here, as this is the first event in a chain,
	// and we should allow other event handlers down the chain to handle more specific situations.
	@EventHandler
	public void onBlockInteract(PlayerInteractEvent event) {
		if(event.isCancelled()){
			return;
		}
		try{
			if(event.getHand() == EquipmentSlot.OFF_HAND){
				return; // off hand version, ignore.
			}
		} catch(NoSuchMethodError error){
		}
		
		final Player player = event.getPlayer();
		
		if(event.getAction() == Action.LEFT_CLICK_BLOCK){
			final Block clicked = event.getClickedBlock();
			
			if(clicked.getBlockData() instanceof WallSign){
				
				if(!settingsConfig.isAllowCreateMethodSign()){
					return;
				}
				
				// We only want to handle shops that exist but are not initialized.
				AbstractShop shop = plugin.getShopmanager().getShopBySign(clicked.getLocation());
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
					plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.INIT);
				}
				
			} else if(plugin.getShopmanager().isAllowedContainer(clicked)){
				
				if(!settingsConfig.isAllowCreateMethodChest()){
					return;
				}
				
				//dont let players create shops via chest on shops that already exist
				// This check is also required for chests to be destroyed properly without new shops getting created. This is because PlayerInteractEvent is called before BlockBreakEvent.
				AbstractShop existingShop = plugin.getShopmanager().getShopByContainer(clicked);
				if(existingShop != null){
					return;
				}
				
				//TODO come back to this and allow players to create double chest shops via chest creation method
				
				// Make sure that the shop can be created at all, prior to checking whats in the players hand.
				if(!plugin.getShopCreationUtil().shopCanBeCreated(player, clicked)){
					return;
				}
				
				if(event.getItem() == null || event.getItem().getType() == Material.AIR){
					return;
				}
				ShopCreationProcess currentProcess = getShopCreationProcess(player.getUniqueId());
				plugin.logger().debug("Current Shop Creation Process: " + currentProcess);
				if(currentProcess != null && currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM){
					if(!plugin.getShopCreationUtil().itemsCanBeInitialized(player, currentProcess.getItemStack(), event.getItem())){
						return;
					}
					currentProcess.setBarterItemStack(event.getItem());
					currentProcess.displayFloatingText(currentProcess.getShopType().toString() + ".createHitChestBarterAmount");
					return;
					
				}
				
				if(!player.isSneaking()){
					return;
				}
				
				Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
				if(lastCreatedProcess != null){
					//if the player has created a new process in the last 5 seconds, block them from creating another
					long diff = (new Date().getTime() - lastCreatedProcess);
					if(diff < settingsConfig.getDebugShopCreateCooldown()){
						lang.request("interaction_issue.createCooldown").sendToAudience(player);
						return;
					}
				}
				// Cleanup the last process if needed and cancel the existing shop creation process if it exists
				if(isInShopCreationProcess(player.getUniqueId())){
					cancelShopCreationProcess(player);
					return;
				}
				
				BlockFace signFacing = plugin.getShopCreationUtil().calculateBlockFaceForSign(player, clicked, event.getBlockFace());
				if(signFacing == null){
					return;
				}
				
				//since player is creating a shop via clicking a chest with an item, create a new object to track the steps of that process
				ShopCreationProcess process = new ShopCreationProcess(player, clicked, signFacing);
				process.setItemStack(event.getItem());
				addShopCreationProcess(player.getUniqueId(), process);
				lastChatCreation.put(player.getUniqueId(), new Date().getTime());
				
				//send player text prompts after they have clicked the chest with the item they want to create a shop with
				PlaceholderContext context = new PlaceholderContext().setProcess(process);
				ShopMessage.request("interaction.initialCreateInstruction", context).sendToAudience(player);
				process.displayFloatingText("createHitChest");
				List<String> autocomplete = new ArrayList<>();
				Arrays.asList(ShopType.values()).forEach((shopType -> autocomplete.add(shopType.toString().toLowerCase())));
				try{
					player.setCustomChatCompletions(autocomplete);
				} catch(Error | Exception error){
				} // Suppress error if autocomplete is not supported
				if(PlayerProfile.isOperator(player)){
					context.setPlayer(player);
					ShopMessage.request("interaction.adminCreateHitChest", context).sendToAudience(player);
				}
				
				//give player a limited amount of time to finish creating the shop until it is deleted
				final UUID originalProcessUUID = process.getUniqueID();
				plugin.getFoliaLib().getScheduler().runLater(() -> {
					//the shop has still not been initialized with an item from a player
					ShopCreationProcess currProcess = getShopCreationProcess(player.getUniqueId());
					if(currProcess != null && currProcess.getUniqueID().equals(originalProcessUUID)){
						cleanupShopCreationProcess(player);
						context.setProcess(currProcess).setPlayer(player);
						ShopMessage.request("interactionIssue.createHitChestTimeout", context).sendToAudience(player);
					}
				}, 30 * 20); // 30 seconds * 20 ticks
			}
		}
	}
	
	@EventHandler
	public void onPlayerChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		ShopCreationProcess process = getShopCreationProcess(player.getUniqueId());
		if(process != null){
			String plainMessage = Components.toPlainText(event.message());
			plugin.logger().debug("Shop Creation Process: " + process.getStep() + " Player " + player.getName() + " input: " + plainMessage);
			switch(process.getStep()) {
				case SHOP_TYPE:
					ShopType type = plugin.getShopCreationUtil().getShopType(plainMessage);
					if(type == null){
						cleanupShopCreationProcess(player);
						return;
					}
					boolean isAdmin = plugin.getShopCreationUtil().getShopIsAdmin(plainMessage);
					process.setShopType(type);
					process.setAdmin(isAdmin);
					event.setCancelled(true);
					
					if(type == ShopType.GAMBLE){
						PlaceholderContext context = new PlaceholderContext().setProcess(process).setPlayer(player);
						ShopMessage.request(type + ".createHitChestPrice", context).sendToAudience(player);
					} else {
						process.displayFloatingText(type + ".createHitChestAmount");
					}
					break;
				case ITEM_AMOUNT:
					int amount = 0;
					try{
						String textAmt = UtilMethods.cleanNumberText(plainMessage);
						amount = Integer.parseInt(textAmt);
						if(amount < 1){
							lang.request("interaction_issue.createLine2").sendToAudience(player);
							lang.request("interaction_issue.createCancel").sendToAudience(player);
							event.setCancelled(true);
							return;
						}
					} catch(NumberFormatException e){
						lang.request("interaction_issue.createLine2").sendToAudience(player);
						lang.request("interaction_issue.createCancel").sendToAudience(player);
						cleanupShopCreationProcess(player);
						return;
					}
					process.setItemAmount(amount);
					event.setCancelled(true);
					
					if(process.getShopType() == ShopType.BARTER){
						process.displayFloatingText(process.getShopType() + ".createHitChest");
					} else {
						process.displayFloatingText(process.getShopType().toString() + ".createHitChestPrice");
					}
					break;
				case ITEM_PRICE:
					double price = plugin.getShopCreationUtil().getShopPrice(player, plainMessage, process.getShopType());
					if(price == -1){
						//instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
						cleanupShopCreationProcess(player);
						return;
					}
					process.setPrice(price);
					event.setCancelled(true);
					
					if(process.getStep() == ShopCreationProcess.ChatCreationStep.ITEM_PRICE_COMBO){
						process.displayFloatingText(process.getShopType() + ".createHitChestPriceCombo");
						return;
					}
					if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
						process.createShop(player);
						cleanupShopCreationProcess(player);
					}
					break;
				case ITEM_PRICE_COMBO:
					double priceCombo = plugin.getShopCreationUtil().getShopPriceCombo(player, plainMessage, process.getShopType());
					if(priceCombo == -1){
						//instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
						cleanupShopCreationProcess(player);
						return;
					}
					process.setPriceCombo(priceCombo);
					event.setCancelled(true);
					
					if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
						process.createShop(player);
						cleanupShopCreationProcess(player);
					}
					break;
				case BARTER_ITEM_AMOUNT:
					int barterAmount = 0;
					try{
						String textAmt = UtilMethods.cleanNumberText(plainMessage);
						barterAmount = Integer.parseInt(textAmt);
						if(barterAmount < 1){
							lang.request("interaction_issue.createLine2").sendToAudience(player);
							event.setCancelled(true);
							return;
						}
					} catch(NumberFormatException e){
						lang.request("interaction_issue.createLine2").sendToAudience(player);
						lang.request("interaction_issue.createCancel").sendToAudience(player);
						//instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
						cleanupShopCreationProcess(player);
						return;
					}
					process.setPrice(barterAmount);
					event.setCancelled(true);
					
					if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
						process.createShop(player);
						cleanupShopCreationProcess(player);
					}
					break;
				// ITEM, BARTER_ITEM, or FINISHED
				default:
					// If the user chatted and we were not in one of the earlier steps, cancel the creation process
					// This will happen if the user was meant to select an ITEM or BARTER_ITEM, and exited the window
					// without selecting their item to buy.
					// This prevents chat from being locked for the player
					cleanupShopCreationProcess(player);
					break;
			}
		}
	}
	
	//player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void shopDestroy(BlockBreakEvent event) {
		Block b = event.getBlock();
		Player player = event.getPlayer();
		
		if(b.getBlockData() instanceof WallSign){
			AbstractShop shop = plugin.getShopmanager().getShopBySign(b.getLocation());
			if(shop == null){
				return;
			}
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
					event.setCancelled(true);
					ShopMessage.request("permission.destroy", player, shop).sendToAudience(player);
					return;
				}
				
				//if players must pay to create shops, remove money first
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
				
				PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
				plugin.getServer().getPluginManager().callEvent(e);
				if(e.isCancelled()){
					event.setCancelled(true);
					return;
				}
				
				plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.DESTROY);
				
				if(shop.isFakeSign()){
					event.setDropItems(false);
				}
				
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
				// We already log on ShopActionType.DESTROY in the Log Handler, so don't log the shop destroy reason
				plugin.getShopmanager().unregisterShop(shop);
			}
			//player trying to break other players shop
			else {
				if(!PlayerProfile.isAllowedToDestroyShopOther(player)){
					ShopMessage.request("permission.destroyOther", player, shop).sendToAudience(player);
					event.setCancelled(true);
					
					return;
				}
				PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
				plugin.getServer().getPluginManager().callEvent(e);
				
				if(e.isCancelled()){
					event.setCancelled(true);
					return;
				}
				
				plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.DESTROY);
				
				if(shop.isFakeSign()){
					event.setDropItems(false);
				}
				
				ShopMessage.request("interaction." + shop.getType().toString() + ".opDestroy", player, shop).sendToAudience(player);
				plugin.getShopmanager().unregisterShop(shop);
			}
			
		} else if(plugin.getShopmanager().isAllowedContainer(b)){
			// Shop will not exist in ShopHandler if it is in the middle of a shop creation process
			// protect shops that are in the middle of a shop creation process from being destroyed
			if(this.isChestInShopCreationProcess(b.getLocation())){
				lang.request("interaction_issue.destroyUninitializedChest").sendToAudience(player);
				event.setCancelled(true); // don't break chest
				return;
			}
			
			AbstractShop shop = plugin.getShopmanager().getShopByContainer(b);
			if(shop == null){
				return;
			}
			
			// since we are dealing with an existing shop, cancel the event so that
			// we can explicitly "uncancel" it later if we want to allow the chest to be broken.
			event.setCancelled(true);
			
			InventoryHolder ih = ((InventoryHolder) b.getState()).getInventory().getHolder();
			
			if(ih instanceof DoubleChest){
				if(shop.getOwnerUUID().equals(player.getUniqueId()) || isAllowedToDestroyShopOther(player)){
					
					// the broken block was the initial chest with the sign
					if(shop.getContainerLocation().equals(b.getLocation())){
						ShopMessage.request("interactionIssue.destroyChest", player, shop).sendToAudience(player);
						// event.setCancelled(true);
						shop.sendEffects(false, player);
						return;
					} else {
						PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), false);
						Bukkit.getPluginManager().callEvent(e);
						
						if(e.isCancelled()){
							// event.setCancelled(true);
							return;
						}
						// Explicitly allow the chest to be broken since it is the "Expansion" chest
						// we need to uncancel the event so that the chest can be broken.
						event.setCancelled(false);
					}
				} else {
					ShopMessage.request("permission.destroyOther", player, shop).sendToAudience(player);
					// event.setCancelled(true);
				}
			} else {
				if(shop.getOwnerUUID().equals(player.getUniqueId()) || isAllowedToDestroyShopOther(player)){
					ShopMessage.request("interactionIssue.destroyChest", player, shop).sendToAudience(player);
					shop.sendEffects(false, player);
				} else {
					ShopMessage.request("permission.destroyOther", player, shop).sendToAudience(player);
				}
				// event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBreakBlockUnderShop(BlockBreakEvent event) {
		//if the block under a chest has been broken, check that its a shop chest
		if(DisplayUtil.isChest(event.getBlock().getRelative(BlockFace.UP).getType())){
			AbstractShop shop = plugin.getShopmanager().getShopByContainer(event.getBlock().getRelative(BlockFace.UP));
			if(shop != null){
				//if it is a shop chest, don't allow it to be broken unless its by the owner or someone with permission
				Player player = event.getPlayer();
				if(!(shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || player.hasPermission("shop.operator"))){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onShopExpansion(BlockPlaceEvent event) {
		Block b = event.getBlockPlaced();
		Player player = event.getPlayer();
		
		if(plugin.getShopmanager().isAllowedContainer(b)){
			//find out if the player placed a chest next to an already active shop
			//todo:jmd make it a proper check for chest expansion rather than just any block
			AbstractShop shop = plugin.getShopmanager().getShopTouchingBlock(b);
			if(shop == null || (b.getType() != shop.getContainerLocation().getBlock().getType())){
				return;
			}
			
			//owner is trying to
			if(shop.getOwnerUUID().equals(player.getUniqueId())){
				PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
				Bukkit.getPluginManager().callEvent(e);
				
				if(e.isCancelled()){
					event.setCancelled(true);
				}
			}
			//other player is trying to
			else {
				if(isOperator(player)){
					PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
					Bukkit.getPluginManager().callEvent(e);
					
					if(e.isCancelled()){
						event.setCancelled(true);
					}
					
				} else {
					event.setCancelled(true);
				}
			}
		}
	}
}