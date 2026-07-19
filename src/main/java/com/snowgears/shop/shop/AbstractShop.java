package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.PlaceholderContext;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopAction;
import com.snowgears.shop.util.ShopClickType;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import static com.snowgears.shop.util.UtilMethods.isMCVersion17Plus;
import static com.wonkglorg.minecraft.util.Components.toPlainText;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractShop{
	
	protected UUID id = UUID.randomUUID();
	@Setter
	protected boolean needsSave = false;
	protected boolean isLoaded = false;
	@Getter
	protected Location signLocation;
	@Getter
	protected Location chestLocation;
	@Getter
	protected BlockFace facing;
	@Setter
	protected UUID owner;
	protected ItemStack item;
	protected ItemStack secondaryItem;
	@Getter
	protected AbstractDisplay display;
	@Setter
	@Getter
	protected double price;
	@Setter
	@Getter
	protected int amount;
	@Getter
	protected boolean isAdmin;
	@Getter
	protected ShopType type;
	@Getter
	protected String[] signLines;
	protected boolean signLinesRequireRefresh;
	@Getter
	protected boolean isPerformingTransaction;
	protected ItemStack guiIcon;
	@Setter
	@Getter
	protected boolean fakeSign;
	
	protected int stock;
	
	public AbstractShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
		this.signLocation = signLoc;
		this.owner = player;
		this.price = pri;
		this.amount = amt;
		this.isAdmin = admin;
		this.item = null;
		this.facing = facing;
		
		this.signLinesRequireRefresh = true; // Reload signs on load in case config changed!
		
		display = Shop.getPlugin().getShopHandler().createDisplay(this.signLocation);
		fakeSign = false;
		
		if(isAdmin){
			owner = Shop.getPlugin().getShopHandler().getAdminUUID();
		}
	}
	
	public static AbstractShop create(Location signLoc,
	                                  UUID player,
	                                  double pri,
	                                  double priCombo,
	                                  int amt,
	                                  Boolean admin,
	                                  ShopType shopType,
	                                  BlockFace facing) {
		
		switch(shopType) {
			case SELL:
				return new SellShop(signLoc, player, pri, amt, admin, facing);
			case BUY:
				return new BuyShop(signLoc, player, pri, amt, admin, facing);
			case BARTER:
				return new BarterShop(signLoc, player, pri, amt, admin, facing);
			case GAMBLE:
				return new GambleShop(signLoc, player, pri, amt, admin, facing);
			case COMBO:
				return new ComboShop(signLoc, player, pri, priCombo, amt, admin, facing);
		}
		return null;
	}
	
	public void setId(UUID newId) {
		this.id = newId;
	}
	
	public UUID getId() {
		return id;
	}
	
	public boolean isChunkLoaded() {
		return UtilMethods.isChunkLoaded(this.getSignLocation());
	}
	
	//this calls BlockData which loads the chunk the shop is in by doing so
	public boolean load() {
		try{
			Block signBlock = signLocation.getBlock();
			if(signBlock.getType() == Material.AIR){
				Shop.getPlugin().logger().warning("Error attempting to load shop! No sign found for Shop (detected: AIR), deleting shop: " + this);
				this.delete();
				return false;
			}
			if(!(signBlock.getBlockData() instanceof WallSign)){
				Shop.getPlugin().logger().warning("Error attempting to load shop! Sign Block for Shop is not a WallSign (detected: " +
				                                  signBlock.getType() +
				                                  "), deleting shop: " +
				                                  this);
				this.delete();
				return false;
			}
			facing = ((WallSign) signBlock.getBlockData()).getFacing();
			Block chestBlock = signBlock.getRelative(facing.getOppositeFace());
			chestLocation = chestBlock.getLocation();
			
			if(!Shop.getPlugin().getShopHandler().isChest(chestBlock)){
				Shop.getPlugin().logger().warning(
						"Error attempting to load shop! Invalid block type detected when trying to load Shop Chest (detected: " +
						chestBlock.getType() +
						"), deleting shop: " +
						this);
				this.delete();
				return false;
			}
			// Now that we are loaded, we can update the stock
			// Force sign lines to refresh on load. This avoids stale sign text when the cached stock
			// matches the newly calculated stock (updateStock() only forces sign updates on change).
			this.signLinesRequireRefresh = true;
			this.updateStock();
			Shop.getPlugin().logger().debug("Loaded shop successfully: " + this);
			isLoaded = true;
			return true;
		} catch(Error | Exception error){
			//this shop has no sign on it. return false
			Shop.getPlugin().logger().warning("Unknown error while attempting to load Shop sign and/or chest! Deleting shop: " + this);
			this.delete();
			return false;
		}
	}
	
	public boolean needsSave() {
		return needsSave;
	}
	
	//abstract methods that must be implemented in each shop subclass
	
	protected int calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			return stock;
		}
		if(this.getInventory() == null || this.getItemStack() == null){
			//if stock is already calculated but now inventory is null, use old stock value
			if(stock != -1){
				return stock;
			} else {
				stock = -1;
			}
			return stock;
		}
		int itemsInShop = InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
		stock = itemsInShop / this.getAmount();
		if(stock == 0 && Shop.getPlugin().getAllowPartialSales()){
			// Calculate the minimum items required to show as in stock
			int minItemAmountRequired = (int) Math.ceil(1 / this.getPricePerItem());
			
			if(itemsInShop >= minItemAmountRequired){
				stock = 1;
			}
		}
		return stock;
	}
	
	public void updateStock() {
		int oldStock = stock;
		
		// Update the stock
		this.calculateStock();
		
		// Update sign if needed
		boolean hasStockChange = stock != oldStock;
		if(hasStockChange){
			Shop.getPlugin().logger().trace("[AbstractShop.updateStock] updateSign, new stock != oldStock! newStock: " +
			                                stock +
			                                " old stock: " +
			                                oldStock +
			                                "\n" +
			                                this);
			this.updateSign(true);
			needsSave = true;
			return;
		}
		
		// Allow sign to update if there is a pending change (signLinesRequireRefresh)
		this.updateSign();
	}
	
	public int getStock() {
		if(isAdmin){
			return Integer.MAX_VALUE;
		}
		return stock;
	}
	
	public void setStockOnLoad(int stock) {
		this.stock = stock;
	}
	
	public boolean isInitialized() {
		return (item != null);
	}
	
	//getter methods
	
	public WallSign getSign() {
		if(!this.isChunkLoaded()){
			return null;
		}
		BlockData signBlockData = this.getSignLocation().getBlock().getBlockData();
		if(signBlockData instanceof WallSign wallSign){
			return wallSign;
		}
		return null;
	}
	
	public Inventory getInventory() {
		if(chestLocation == null || signLocation == null || !this.isChunkLoaded()){
			return null;
		}
		Block chestBlock = chestLocation.getBlock();
		if(chestBlock.getState() instanceof InventoryHolder){
			return ((InventoryHolder) (chestBlock.getState())).getInventory();
		}
		return null;
	}
	
	public Material getContainerType() {
		if(chestLocation == null || !this.isChunkLoaded()){
			return null;
		}
		try{
			return chestLocation.getBlock().getType();
		} catch(Exception _){
			return null;
		}
	}
	
	public UUID getOwnerUUID() {
		return owner;
	}
	
	public String getOwnerName() {
		if(this.isAdmin()){
			return "admin";
		}
		
		if(this.getOwnerUUID() != null){
			// Use cache first - this avoids expensive disk I/O
			return PlayerNameCache.getName(this.getOwnerUUID());
		}
		
		return ChatColor.RED + "CLOSED";
	}
	
	public OfflinePlayer getOwner() {
		return Bukkit.getOfflinePlayer(this.owner);
	}
	
	public ItemStack getItemStack() {
		if(item != null){
			ItemStack is = item.clone();
			is.setAmount(this.getAmount());
			return is;
		}
		return null;
	}
	
	public ItemStack getSecondaryItemStack() {
		if(secondaryItem != null){
			ItemStack is = secondaryItem.clone();
			is.setAmount((int) this.getPrice());
			return is;
		}
		return null;
	}
	
	public double getPricePerItem() {
		// Calculate pricePerItem for partial sales, round up!
		return this.getPrice() / this.getAmount();
	}
	
	public double getItemsPerPriceUnit() {
		// Calculate items you can get for each price unit, round down!
		return this.getAmount() / this.getPrice();
	}
	
	public String getPriceString() {
		if(this.type == ShopType.BARTER && this.isInitialized()){
			return (int) this.getPrice() + " " + toPlainText(ItemNameUtil.getName(this.getSecondaryItemStack()));
		}
		return Shop.getPlugin().getPriceString(this.price, false);
	}
	
	public String getPricePerItemString() {
		double pricePer = this.getPricePerItem();
		return Shop.getPlugin().getPriceString(pricePer, true);
	}
	
	//only use this method if the shop has not been added to the main handler maps yet
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
		if(isAdmin){
			this.owner = Shop.getPlugin().getShopHandler().getAdminUUID();
		}
	}
	
	public ItemStack getGuiIcon() {
		// Load it when it is first called
		if(guiIcon == null){
			this.refreshGuiIcon();
		}
		return guiIcon;
	}
	
	//setter methods
	
	public void setItemStack(ItemStack is) {
		// If the item stack passed is null, go ahead and just skip it.
		if(is == null){
			return;
		}
		
		// Remove "0 Damage" from item meta (old config bug)
		this.item = this.removeZeroDamageMeta(is.clone());
		this.calculateStock();
		this.updateSign(true);
	}
	
	public void setSecondaryItemStack(ItemStack is) {
		this.secondaryItem = this.removeZeroDamageMeta(is.clone());
		this.calculateStock();
		this.updateSign(true);
	}
	
	public ItemStack removeZeroDamageMeta(ItemStack item) {
		try{
			// In the past we used to explicitly set the durability of an item to be 0, this caused blocks/items to be saved
			// with extra NBT data that we don't actually want. For example, dirt shouldn't have a damage of 0.
			// Detect if we set it to 0, and if so, remove it from the ItemMeta!
			if(item.getItemMeta() instanceof Damageable damageable && damageable.getDamage() == 0){
				String components = item.getItemMeta().getAsComponentString(); // example: "[minecraft:damage=53]"
				
				// Remove it from the array
				components = components.replace(",minecraft:damage=0", ""); // Middle of an array
				components = components.replace("minecraft:damage=0,", ""); // Start of an array
				components = components.replace("minecraft:damage=0", ""); // Only object in array
				
				// Convert it back into an item
				String itemTypeKey = item.getType().getKey().toString(); // example: "minecraft:diamond_sword"
				String itemAsString = itemTypeKey + components; // results in: "minecraft:diamond_sword[minecraft:damage=53]"
				return Bukkit.getItemFactory().createItemStack(itemAsString);
			}
			
			// Default return original item
			return item;
		} catch(Exception | Error e){
			Shop.getPlugin().logger().debug("Error removing zero damage meta from item: " + item);
			Shop.getPlugin().logger().helpful("checkItemDurability feature may be unsupported on your version of Paper/Spigot!");
			return item;
		}
	}
	
	public void refreshGuiIcon() {
		if(this.type != ShopType.GAMBLE){
			if(this.getItemStack() == null){
				return;
			}
			guiIcon = this.getItemStack().clone();
			guiIcon.setAmount(1);
		} else {
			guiIcon = Shop.getPlugin().getGambleDisplayItem().clone();
			guiIcon.setAmount(1);
		}
		
		//get the placeholder icon with all of the unformatted fields
		ItemStack placeHolderIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.ALL_SHOP_ICON, null, null);
		
		Component name = ShopMessage.formatMessage(placeHolderIcon.getItemMeta().getDisplayName(), this, null, false);
		List<Component> lore = new ArrayList<>();
		for(String loreLine : placeHolderIcon.getItemMeta().getLore()){
			// Don't add barter line to non barter shops
			if(loreLine.contains("[barter item]") && this.getType() != ShopType.BARTER){
				continue;
			}
			// Add all lore lines
			PlaceholderContext context = new PlaceholderContext();
			context.setShop(this);
			lore.add(ShopMessage.format(loreLine, context));
		}
		
		ItemMeta iconMeta = guiIcon.getItemMeta();
		iconMeta.displayName(name);
		iconMeta.lore(lore);
		
		PersistentDataContainer container = iconMeta.getPersistentDataContainer();
		container.set(Shop.getPlugin().getSignLocationNameSpacedKey(),
				PersistentDataType.STRING,
				UtilMethods.getCleanLocation(this.getSignLocation(), true));
		
		guiIcon.setItemMeta(iconMeta);
	}
	
	public int getItemDurabilityPercent() {
		ItemStack item = this.getItemStack().clone();
		return UtilMethods.getDurabilityPercent(item);
	}
	
	public int getSecondaryItemDurabilityPercent() {
		ItemStack item = this.getSecondaryItemStack().clone();
		return UtilMethods.getDurabilityPercent(item);
	}
	
	public void updateSign() {this.updateSign(false);}
	
	public void updateSign(boolean forceUpdate) {
		// If we don't need to update the lines, then don't update them!
		if(!signLinesRequireRefresh && !forceUpdate){
			return;
		}
		// Do not trigger the sign update if the chunk has not been loaded yet
		if(!this.isChunkLoaded()){
			if(forceUpdate){
				signLinesRequireRefresh = true;
			}
			return;
		}
		// Immediately set to false to prevent multiple calls to updateSign overlapping
		signLinesRequireRefresh = false;
		signLines = ShopMessage.getSignLines(this, this.type);
		
		// Use the sign's location to ensure the update runs in the correct region in Folia
		Shop.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(signLocation, task -> {
			// Update the GUI Icon since the sign needs an update.
			refreshGuiIcon();
			
			Sign signBlock;
			try{
				signBlock = (Sign) signLocation.getBlock().getState(); // this will load the sign
			} catch(ClassCastException e){
				Shop.getPlugin().logger().warning("Error attempting to update Shop sign! Sign Block for Shop is not a Sign (detected: " +
				                                  signLocation.getBlock().getType() +
				                                  "), deleting shop: " +
				                                  this);
				this.delete();
				return;
			}
			
			String[] oldLines = signBlock.getLines();
			String[] newLines = signLines.clone();
			boolean hasSignUpdate = false;
			// If the sign lines are the same, don't update them!
			boolean linesMatch = newLines[0].equals(oldLines[0]) &&
			                     newLines[1].equals(oldLines[1]) &&
			                     newLines[2].equals(oldLines[2]) &&
			                     newLines[3].equals(oldLines[3]);
			
			if(!isInitialized()){
				hasSignUpdate = true; // force update the sign
				signBlock.setLine(0, ChatColor.RED + ChatColor.stripColor(newLines[0]));
				signBlock.setLine(1, ChatColor.RED + ChatColor.stripColor(newLines[1]));
				signBlock.setLine(2, ChatColor.RED + ChatColor.stripColor(newLines[2]));
				signBlock.setLine(3, ChatColor.RED + ChatColor.stripColor(newLines[3]));
			} else if(!linesMatch){
				hasSignUpdate = true; // force update the sign
				signBlock.setLine(0, newLines[0]);
				signBlock.setLine(1, newLines[1]);
				signBlock.setLine(2, newLines[2]);
				signBlock.setLine(3, newLines[3]);
			}
			// If the sign is glowing, update it if the setting has changed
			if(isMCVersion17Plus()){
				boolean shouldGlow = Shop.getPlugin().getGlowingSignText();
				if(shouldGlow != signBlock.isGlowingText()){
					hasSignUpdate = true;
					signBlock.setGlowingText(shouldGlow);
				}
			}
			// Update the sign if it has changed
			if(hasSignUpdate){
				signBlock.update(true);
			}
			
			// Update the floating holograms for anybody who currently has them open
			if(display != null){
				display.updateDisplayTags();
			}
		}, 2);
	}
	
	public void delete() {this.delete(true);}
	
	public void delete(boolean forceSave) {
		try{
			// First, remove the shop from the shop handler in case of any errors with later methods.
			Shop.getPlugin().getShopHandler().removeShop(this, forceSave);
			
			if(UtilMethods.isMCVersion17Plus() && Shop.getPlugin().getDisplayLightLevel() > 0 && this.getChestLocation() != null){
				Block chestBlock = this.getChestLocation().getBlock();
				if(chestBlock != null && Shop.getPlugin().getShopHandler().isChest(chestBlock)){
					Block displayBlock = chestBlock.getRelative(BlockFace.UP);
					if(UtilMethods.materialIsNonIntrusive(displayBlock.getType())){
						displayBlock.setType(Material.AIR);
					}
				}
			}
			
			Block b = this.getSignLocation().getBlock();
			if(b.getBlockData() instanceof WallSign){
				Sign signBlock = (Sign) b.getState();
				String[] deletedLines = ShopMessage.getSignLines("deleted", this);
				signBlock.setLine(0, deletedLines[0]);
				signBlock.setLine(1, deletedLines[1]);
				signBlock.setLine(2, deletedLines[2]);
				signBlock.setLine(3, deletedLines[3]);
				signBlock.update(true);
			}
			
			// Finally, remove any active displays
			if(display != null){
				display.remove(null);
			}
			Shop.getPlugin().logger().debug("Deleted Shop " + this);
		} catch(Error | Exception e){
			Shop.getPlugin().logger().severe("Unknown error attempting to delete shop, deletion might not have fully completed successfully: " +
			                                 e.getMessage());
			Shop.getPlugin().logger().debug("Full stack trace for shop deletion error: ", e);
		}
	}
	
	public void teleportPlayer(Player player) {
		if(player == null){
			return;
		}
		
		if(chestLocation == null){
			this.load();
			Location loc = this.getSignLocation().getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5);
			player.teleport(loc);
		} else {
			Location loc = this.getSignLocation().getBlock().getRelative(facing).getLocation().add(0.5, 0, 0.5);
			loc.setYaw(UtilMethods.faceToYaw(facing.getOppositeFace()));
			loc.setPitch(25.0f);
			
			player.teleport(loc);
		}
		Shop.getPlugin().getShopListener().addTeleportCooldown(player);
	}
	
	public void printSalesInfo(Player player) {
		for(String message : ShopMessage.getUnformattedMessageList(this.getType().toString(), "description")){
			if(message != null && !message.isEmpty()){
				Map<ItemStack, Integer> items = new HashMap<>();
				items.put(this.item, this.amount);
				if(this.getSecondaryItemStack() != null){
					items.put(this.getSecondaryItemStack(), (int) this.price);
				}
				ShopMessage.sendMessage(message, player, this);
			}
		}
	}
	
	public boolean executeClickAction(PlayerInteractEvent event, ShopClickType clickType) {
		ShopAction action = Shop.getPlugin().getShopAction(clickType);
		if(action == null){
			return false; //there is no action mapped to this click type
		}
		Player player = event.getPlayer();
		
		switch(action) {
			case TRANSACT:
				Shop.getPlugin().getTransactionHelper().executeTransactionFromEvent(event, this, false);
				break;
			case TRANSACT_FULLSTACK:
				Shop.getPlugin().getTransactionHelper().executeTransactionFromEvent(event, this, true);
				break;
			case VIEW_DETAILS:
				this.printSalesInfo(player);
				break;
			case CYCLE_DISPLAY:
				//player clicked another player's shop sign
				if(!this.getOwnerName().equals(player.getName())){
					//player has permission to change another player's shop display
					if((!Shop.getPlugin().usePerms() && player.isOp()) || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))){
						this.getDisplay().cycleType(player);
					}
					//player clicked own shop sign
				} else {
					if(Shop.getPlugin().usePerms() && !player.hasPermission("shop.setdisplay")){
						return false;
					}
					
					this.getDisplay().cycleType(player);
				}
				break;
			default:
				break;
		}
		return true;
	}
	
	public void sendEffects(boolean success, Player player) {
		try{
			if(success){
				if(Shop.getPlugin().playSounds()){
					player.playSound(this.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
				}
				if(Shop.getPlugin().playEffects()){
					player.getWorld().playEffect(this.getChestLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK);
				}
			} else {
				if(Shop.getPlugin().playSounds()){
					player.playSound(this.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
				}
				if(Shop.getPlugin().playEffects()){
					player.getWorld().playEffect(this.getChestLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
				}
			}
		} catch(Error | Exception _){
		}
	}
	
	@Override
	public String toString() {
		return "AbstractShop{" +
		       "type=" +
		       type.toString().toUpperCase() +
		       ", item=" +
		       item +
		       ", price=" +
		       price +
		       (secondaryItem != null ? ", secondaryItem=" + secondaryItem : "") +
		       (isAdmin ? ", isAdmin=" + isAdmin : "") +
		       ", stock=" +
		       stock +
		       ", owner=" +
		       owner +
		       ", chestLocation=" +
		       ((chestLocation != null) ? chestLocation.getWorld().getName() +
		                                  ":" +
		                                  chestLocation.getBlockX() +
		                                  "/" +
		                                  chestLocation.getBlockY() +
		                                  "/" +
		                                  chestLocation.getBlockZ() : "null") +
		       '}';
	}
}
