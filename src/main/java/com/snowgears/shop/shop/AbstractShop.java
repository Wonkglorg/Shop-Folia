package com.snowgears.shop.shop;

import com.snowgears.shop.Constants;
import com.snowgears.shop.Shop;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.display.AbstractDisplay;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.manager.ShopManager.BlockKey;
import static com.snowgears.shop.manager.player.PlayerProfile.isOperator;
import static com.snowgears.shop.shop.ShopState.OK;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ItemNameUtil;
import static com.snowgears.shop.util.ItemNameUtil.getItemHover;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopAction;
import com.snowgears.shop.util.ShopClickType;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import static com.wonkglorg.minecraft.util.Components.toPlainText;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public abstract class AbstractShop{
	
	@Getter
	@Setter
	protected UUID id = UUID.randomUUID();
	@Getter
	protected long creationDate;
	@Setter
	protected boolean needsSave = false;
	protected boolean isLoaded = false;
	/**
	 * The location of the sign
	 */
	@Getter
	protected Location signLocation;
	/**
	 * Represents a block key used for indexing and accessing references
	 */
	@Getter
	protected BlockKey signKey;
	/**
	 * The location of the container attached to the sign
	 */
	@Getter
	protected Location containerLocation;
	
	/**
	 * Filled when the shop container is a double chest
	 */
	@Getter
	protected Location secondaryContainerLocation;
	
	/**
	 * Represents a block key used for indexing and accessing references
	 */
	@Getter
	protected BlockKey containerKey;
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
	protected CreationWord creationWord;
	@Getter
	protected List<Component> signLines;
	protected boolean signLinesRequireRefresh;
	@Getter
	protected boolean isPerformingTransaction;
	@Setter
	@Getter
	protected boolean fakeSign;
	
	@Getter
	protected int stock;
	/**
	 * The current state of the shop stock
	 */
	@Setter
	@Getter
	protected ShopState shopState;
	
	protected AbstractShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing, long creationDate) {
		this.signLocation = signLoc;
		this.signKey = BlockKey.of(signLoc);
		this.owner = player;
		this.price = pri;
		this.amount = amt;
		this.isAdmin = admin;
		this.item = null;
		this.facing = facing;
		this.creationDate = creationDate;
		this.signLinesRequireRefresh = true; // Reload signs on load in case config changed!
		
		//infer the container location where it should be
		this.containerLocation = new Location(signLoc.getWorld(),
				signLoc.getBlockX() - facing.getModX(),
				signLoc.getBlockY() - facing.getModY(),
				signLoc.getBlockZ() - facing.getModZ());
		this.containerKey = BlockKey.of(containerLocation);
		display = Shop.getPlugin().getShopmanager().getDisplayManager().createDisplay(this.signLocation);
		fakeSign = false;
		
		if(isAdmin){
			owner = Constants.getAdminUUID();
			stock = Integer.MAX_VALUE;
			shopState = OK;
		}
	}
	
	public static AbstractShop create(Location signLoc,
	                                  UUID player,
	                                  double pri,
	                                  double priCombo,
	                                  int amt,
	                                  Boolean admin,
	                                  ShopType shopType,
	                                  BlockFace facing,
	                                  long creationDate) {
		
		return switch(shopType) {
			case SELL -> new SellShop(signLoc, player, pri, amt, admin, facing, creationDate);
			case BUY -> new BuyShop(signLoc, player, pri, amt, admin, facing, creationDate);
			case BARTER -> new BarterShop(signLoc, player, pri, amt, admin, facing, creationDate);
			case GAMBLE -> new GambleShop(signLoc, player, pri, amt, admin, facing, creationDate);
			case COMBO -> new ComboShop(signLoc, player, pri, priCombo, amt, admin, facing, creationDate);
		};
	}
	
	public boolean isChunkLoaded() {
		return UtilMethods.isChunkLoaded(this.getSignLocation());
	}
	
	//this calls BlockData which loads the chunk the shop is in by doing so
	
	/**
	 * Loads the shops chunk data and replaces it with the one currently cached
	 *
	 * @return if the shop fails to load due to no longer being valid or another issue returns false
	 */
	public boolean load() {
		Block signBlock = signLocation.getBlock();
		if(signBlock.getType() == Material.AIR){
			Shop.getPlugin().logger().warning("Error attempting to load shop! No sign found for Shop (detected: AIR), deleting shop: " + this);
			return false;
		}
		
		if(!(signBlock.getBlockData() instanceof WallSign wallSign)){
			Shop.getPlugin().logger().warning("Error attempting to load shop! Sign Block for Shop is not a WallSign (detected: " +
			                                  signBlock.getType() +
			                                  "), deleting shop: " +
			                                  this);
			return false;
		}
		
		// Refresh the sign direction from the actual world state.
		facing = wallSign.getFacing();
		
		// The primary container is directly behind the sign.
		Block containerBlock = signBlock.getRelative(facing.getOppositeFace());
		
		if(!Shop.getPlugin().getShopmanager().isAllowedContainer(containerBlock)){
			Shop.getPlugin().logger().warning(
					"Error attempting to load shop! Invalid block type detected when trying to load Shop Container (detected: " +
					containerBlock.getType() +
					"), deleting shop: " +
					this);
			return false;
		}
		
		// Refresh the primary container references.
		containerLocation = containerBlock.getLocation();
		containerKey = BlockKey.of(containerBlock);
		
		// Always reset the secondary container first. This handles cases where
		// a previously-double chest has since become a single chest or another container.
		secondaryContainerLocation = null;
		
		// Cache the second half when the attached container is a double chest.
		if(containerBlock.getBlockData() instanceof Chest chestData && chestData.getType() != Chest.Type.SINGLE){
			
			BlockFace otherChestDirection = getOtherChestDirection(chestData.getType(), chestData.getFacing());
			
			if(otherChestDirection != null){
				secondaryContainerLocation = new Location(containerLocation.getWorld(),
						containerKey.x() + otherChestDirection.getModX(),
						containerKey.y() + otherChestDirection.getModY(),
						containerKey.z() + otherChestDirection.getModZ());
				//add the secondary location to the manager to handle
				Shop.getPlugin().getShopmanager().addSecondaryShopLocation(secondaryContainerLocation, this);
			}
		}
		
		// Force sign lines to refresh on load.
		signLinesRequireRefresh = true;
		
		// Now that the world/container data is valid, refresh stock and state.
		updateStock();
		
		Shop.getPlugin().logger().debug("Loaded shop successfully: " + this);
		
		isLoaded = true;
		return true;
		
	}
	
	private BlockFace getOtherChestDirection(Chest.Type chestType, BlockFace facing) {
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
	
	public boolean needsSave() {
		return needsSave;
	}
	
	//abstract methods that must be implemented in each shop subclass
	
	/**
	 * Calculates the stock amount of the shop
	 */
	protected void calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			return;
		}
		if(this.getInventory() == null || this.getItemStack() == null){
			//leave the cached value as it was
			return;
		}
		int itemsInShop = InventoryUtils.getAmount(this.getInventory(), this.getItemStack());
		stock = itemsInShop / this.getAmount();
		if(stock == 0 && Shop.getPlugin().getSettingsConfig().isAllowPartialSales()){
			// Calculate the minimum items required to show as in stock
			int minItemAmountRequired = (int) Math.ceil(1 / this.getPricePerItem());
			
			if(itemsInShop >= minItemAmountRequired){
				stock = 1;
			}
		}
	}
	
	public void updateStock() {
		int oldStock = stock;
		
		// Update the stock
		this.calculateStock();
		shopState = ShopState.getShopState(this);
		
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
		if(containerLocation == null || signLocation == null || !this.isChunkLoaded()){
			return null;
		}
		Block chestBlock = containerLocation.getBlock();
		if(chestBlock.getState() instanceof InventoryHolder){
			return ((InventoryHolder) (chestBlock.getState())).getInventory();
		}
		return null;
	}
	
	public Material getContainerType() {
		if(containerLocation == null || !this.isChunkLoaded()){
			return null;
		}
		try{
			return containerLocation.getBlock().getType();
		} catch(Exception _){
			return null;
		}
	}
	
	public UUID getOwnerUUID() {
		return owner;
	}
	
	public Component getOwnerName() {
		if(this.isAdmin()){
			return Component.text("admin");
		}
		
		if(this.getOwnerUUID() != null){
			// Use cache first - this avoids expensive disk I/O
			return Component.text(PlayerNameCache.getName(this.getOwnerUUID()));
		}
		
		return Component.text("CLOSED").color(TextColor.color(255, 0, 0));
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
			this.owner = Constants.getAdminUUID();
		}
	}
	
	public void setItemStack(ItemStack is) {
		// If the item stack passed is null, go ahead and just skip it.
		if(is == null){
			return;
		}
		
		// Remove "0 Damage" from item meta (old config bug)
		this.item = is.clone();
		this.calculateStock();
		shopState = ShopState.getShopState(this);
		this.updateSign(true);
	}
	
	public void setSecondaryItemStack(ItemStack is) {
		this.secondaryItem = is.clone();
		this.calculateStock();
		shopState = ShopState.getShopState(this);
		this.updateSign(true);
	}
	
	public int getItemDurabilityPercent() {
		return UtilMethods.getDurabilityPercent(item);
	}
	
	public int getSecondaryItemDurabilityPercent() {
		return UtilMethods.getDurabilityPercent(secondaryItem);
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
		signLines = ShopMessage.getSignLines(this);
		
		// Use the sign's location to ensure the update runs in the correct region in Folia
		Shop.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(signLocation, task -> {
			// Update the GUI Icon since the sign needs an update.
			if(!(signLocation.getBlock().getState() instanceof Sign sign)){
				Shop.getPlugin().logger().warning("Error attempting to update Shop sign! Sign Block for Shop is not a Sign (detected: " +
				                                  signLocation.getBlock().getType() +
				                                  "), deleting shop: " +
				                                  this);
				return;
			}
			
			SignSide frontSideSign = sign.getSide(Side.FRONT);
			List<Component> oldLines = frontSideSign.lines();
			boolean hasSignUpdate = false;
			// If the sign lines are the same, don't update them!
			//@formatter:off
			boolean linesMatch = signLines.get(0).equals(oldLines.get(0)) &&
			                     signLines.get(1).equals(oldLines.get(1)) &&
			                     signLines.get(2).equals(oldLines.get(2)) &&
			                     signLines.get(3).equals(oldLines.get(3));
			
			if(!isInitialized()){
				hasSignUpdate = true; // force update the sign
				TextColor red = TextColor.color(255,0,0);
				frontSideSign.line(0, signLines.get(0).color(red));
				frontSideSign.line(1, signLines.get(1).color(red));
				frontSideSign.line(2, signLines.get(2).color(red));
				frontSideSign.line(3, signLines.get(3).color(red));
			} else if(!linesMatch){
				hasSignUpdate = true; // force update the sign
				frontSideSign.line(0, signLines.get(0));
				frontSideSign.line(1, signLines.get(1));
				frontSideSign.line(2, signLines.get(2));
				frontSideSign.line(3, signLines.get(3));
			}
			//@formatter:on
			// If the sign is glowing, update it if the setting has changed
			boolean shouldGlow = Shop.getPlugin().getSettingsConfig().isSetGlowingSignText();
			if(shouldGlow != frontSideSign.isGlowingText()){
				hasSignUpdate = true;
				frontSideSign.setGlowingText(shouldGlow);
			}
			// Update the sign if it has changed
			if(hasSignUpdate){
				sign.update(true);
			}
			
			// Update the floating holograms for anybody who currently has them open
			if(display != null){
				display.updateDisplayTags();
			}
		}, 2);
	}
	
	public void teleportPlayer(Player player) {
		if(player == null){
			return;
		}
		
		if(containerLocation == null){
			this.load();
			Location loc = this.getSignLocation().getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5);
			player.teleport(loc);
		} else {
			Location loc = this.getSignLocation().getBlock().getRelative(facing).getLocation().add(0.5, 0, 0.5);
			loc.setYaw(UtilMethods.faceToYaw(facing.getOppositeFace()));
			loc.setPitch(25.0f);
			
			player.teleport(loc);
		}
		PlayerManager.addTeleportCooldown(player.getUniqueId());
	}
	
	public void printSalesInfo(Player player) {
		LangRequest request = Shop.getPlugin().getLangManager().request("description." + this.getType().toString());
		shopPlaceholders(request, this);
		request.sendToAudience(player);
	}
	
	public static void shopPlaceholders(LangRequest request, AbstractShop shop) {
		//@formatter:off
		ItemStack item = shop.item;
		
		request.replace("%owner%", shop.getOwnerName())
			   .replace("%price%",shop.getPrice())
			   .replace("%stock%",shop.getStock())
			   .replace("%amount%",shop.getAmount())
			   .replace("%location%",UtilMethods.getCleanLocation(shop.getSignLocation(),false))
			   .replace("%world%",shop.getSignLocation().getWorld().getName())
			   .replace("%item%", ItemNameUtil.getName(item).hoverEvent(getItemHover(item)))
               .replace("%item-type%", item.getType())
			   .replace("%item-durability%",shop.getItemDurabilityPercent())
               .replace("%item-amount%",shop.getAmount())
               .replace("%item-item-lore%", UtilMethods.getLore(item))
               .replace("%item-enchants%",UtilMethods.getEnchantmentsComponent(item));
		
		if(shop.getType() == ShopType.GAMBLE){
			GambleShop gambleShop = (GambleShop) shop;
			ItemStack displayItem = Shop.getPlugin().getItemConfig().getGambleDisplayItem();
			ItemStack gambleItem = gambleShop.getGambleItem();
			request.replace("%gamble-item%", ItemNameUtil.getName(displayItem).hoverEvent(getItemHover(displayItem)))
			       .replace("%gamble-item-type%", gambleItem.getType())
			       .replace("%gamble-durability%",UtilMethods.getDurabilityPercent(gambleItem))
			       .replace("%gamble-item-amount%", shop.getAmount())
			       .replace("%gamble-item-lore%", UtilMethods.getLore(gambleItem))
			       .replace("%gamble-item-enchants%",UtilMethods.getEnchantmentsComponent(gambleItem));
		}
		ItemStack barterItem = shop.secondaryItem;
		if(barterItem != null){
			request.replace("%barter-item%", ItemNameUtil.getName(barterItem).hoverEvent(getItemHover(barterItem)))
			       .replace("%barter-item-type%", barterItem.getType())
			       .replace("%barter-durability%",UtilMethods.getDurabilityPercent(barterItem))
			       .replace("%barter-item-amount%", barterItem.getAmount())
			       .replace("%barter-item-lore%", UtilMethods.getLore(barterItem))
				   .replace("%barter-item-enchants%",UtilMethods.getEnchantmentsComponent(barterItem));
		}
		//@formatter:on
	}
	
	public boolean executeClickAction(PlayerInteractEvent event, ShopClickType clickType) {
		ShopAction action = Shop.getPlugin().getSettingsConfig().getShopAction(clickType);
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
					if((isOperator(player))){
						this.getDisplay().cycleType(player);
					}
					//player clicked own shop sign
				} else {
					if(!player.hasPermission("shop.setdisplay")){
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
			SettingsConfig settingsConfig = Shop.getPlugin().getSettingsConfig();
			if(success){
				if(settingsConfig.isPlaySounds()){
					player.playSound(this.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
				}
				if(settingsConfig.isPlayEffects()){
					player.getWorld().playEffect(this.getContainerLocation(), Effect.DESTROY_BLOCK, Material.EMERALD_BLOCK);
				}
			} else {
				if(settingsConfig.isPlaySounds()){
					player.playSound(this.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
				}
				if(settingsConfig.isPlayEffects()){
					player.getWorld().playEffect(this.getContainerLocation(), Effect.DESTROY_BLOCK, Material.REDSTONE_BLOCK);
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
		       ((containerLocation != null) ? containerLocation.getWorld().getName() +
		                                      ":" +
		                                      containerLocation.getBlockX() +
		                                      "/" +
		                                      containerLocation.getBlockY() +
		                                      "/" +
		                                      containerLocation.getBlockZ() : "null") +
		       '}';
	}
}
