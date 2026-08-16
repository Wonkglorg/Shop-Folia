package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.AdminOfflinePlayer;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.event.ShopTransactionEvent;
import com.wonkglorg.minecraft.shop.manager.PlayerNameCache;
import com.wonkglorg.minecraft.shop.manager.ShopManager.BlockKey;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.isOperator;
import static com.wonkglorg.minecraft.shop.shop.ShopState.EMPTY;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OVERFILLED;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.party.PlayerTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import static com.wonkglorg.minecraft.shop.util.ChestUtil.getOtherChestDirection;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import static com.wonkglorg.minecraft.shop.util.ItemNameUtil.getItemHover;
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class AbstractShop{
	private static final AdminOfflinePlayer adminOfflinePlayer = new AdminOfflinePlayer();
	
	@Getter
	@Setter
	protected UUID id;
	@Getter
	protected long creationDate;
	@Setter
	protected boolean needsSave = false;
	@Getter
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
	
	protected AbstractShop(UUID id,
	                       Location signLoc,
	                       UUID player,
	                       ShopType type,
	                       double pri,
	                       int amt,
	                       boolean admin,
	                       BlockFace facing,
	                       long creationDate,
	                       DisplayType displayType) {
		this.id = id;
		this.signLocation = signLoc;
		this.signKey = BlockKey.of(signLoc);
		this.owner = player;
		this.type = type;
		this.price = pri;
		this.amount = amt;
		this.isAdmin = admin;
		this.item = null;
		this.facing = facing;
		this.creationDate = creationDate;
		this.display = AbstractDisplay.createDisplay(displayType, this);
		this.signLines = ShopMessage.getSignLines(this);
		this.signLinesRequireRefresh = true; // Reload signs on load in case config changed!
		
		//infer the container location where it should be
		this.containerLocation = new Location(signLoc.getWorld(),
				signLoc.getBlockX() - (double) facing.getModX(),
				signLoc.getBlockY() - (double) facing.getModY(),
				signLoc.getBlockZ() - (double) facing.getModZ());
		
		this.containerKey = BlockKey.of(containerLocation);
		fakeSign = false;
		
		if(isAdmin){
			owner = AdminOfflinePlayer.getAdminUUID();
			stock = Integer.MAX_VALUE;
			shopState = OK;
		}
	}
	
	public static AbstractShop create(UUID id,
	                                  Location signLoc,
	                                  UUID player,
	                                  double pri,
	                                  double priCombo,
	                                  int amt,
	                                  Boolean admin,
	                                  ShopType shopType,
	                                  BlockFace facing,
	                                  long creationDate,
	                                  DisplayType type) {
		
		return switch(shopType) {
			case SELL -> new SellShop(id, signLoc, player, pri, amt, admin, facing, creationDate, type);
			case BUY -> new BuyShop(id, signLoc, player, pri, amt, admin, facing, creationDate, type);
			case BARTER -> new BarterShop(id, signLoc, player, pri, amt, admin, facing, creationDate, type);
			case GAMBLE -> new GambleShop(id, signLoc, player, pri, amt, facing, creationDate, type);
		};
	}
	
	public boolean isChunkLoaded() {
		return signLocation.isChunkLoaded();
	}
	
	/**
	 * Loads the shops chunk data and replaces it with the one currently cached
	 *
	 * @return if the shop fails to load due to no longer being valid or another issue returns false
	 */
	public boolean load() {
		Block signBlock = signLocation.getBlock();
		if(signBlock.getType() == Material.AIR){
			Main.getPlugin().logger().warning("Error attempting to load shop! No sign found for Shop (detected: AIR), deleting shop: " + this);
			return false;
		}
		
		if(!(signBlock.getBlockData() instanceof WallSign wallSign)){
			Main.getPlugin().logger().warning("Error attempting to load shop! Sign Block for Shop is not a WallSign (detected: " +
			                                  signBlock.getType() +
			                                  "), deleting shop: " +
			                                  this);
			return false;
		}
		
		// Refresh the sign direction from the actual world state.
		facing = wallSign.getFacing();
		
		// The primary container is directly behind the sign.
		Block containerBlock = signBlock.getRelative(facing.getOppositeFace());
		
		if(!Main.getPlugin().getShopmanager().isAllowedContainer(containerBlock)){
			Main.getPlugin().logger().warning(
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
		removeSecondaryContainerLocation();
		
		// Cache the second half when the attached container is a double chest.
		if(containerBlock.getBlockData() instanceof Chest chestData && chestData.getType() != Chest.Type.SINGLE){
			
			BlockFace otherChestDirection = getOtherChestDirection(chestData.getType(), chestData.getFacing());
			
			if(otherChestDirection != null){
				addSecondaryContainerLocation(new Location(containerLocation.getWorld(),
						containerKey.x() + (double) otherChestDirection.getModX(),
						containerKey.y() + (double) otherChestDirection.getModY(),
						containerKey.z() + (double) otherChestDirection.getModZ()));
			}
		}
		
		// Force sign lines to refresh on load.
		signLinesRequireRefresh = true;
		
		// Now that the world/container data is valid, refresh stock and state.
		updateStock();
		
		isLoaded = true;
		return true;
	}
	
	public boolean needsSave() {
		return needsSave;
	}
	
	/**
	 * Calculates the stock amount of the shop
	 */
	protected void calculateStock() {
		if(this.isAdmin){
			// There is always stock in the admin shop!
			stock = Integer.MAX_VALUE;
			shopState = OK;
			return;
		}
		if(item == null){
			//leave cached value
			return;
		}
		ShopTransactionParty party = getParty();
		
		double availableFunds = party.getAvailableFunds(item);
		stock = (int) (availableFunds / this.getAmount());
		
		if(stock < 1){
			shopState = EMPTY;
			return;
		}
		
		if(party.canAcceptPayment(item, price)){
			shopState = OK;
			return;
		}
		
		shopState = OVERFILLED;
	}
	
	public void updateStock() {
		int oldStock = stock;
		
		// Update the stock
		this.calculateStock();
		
		// Update sign if needed
		boolean hasStockChange = stock != oldStock;
		if(hasStockChange){
			Main.getPlugin().logger().debug("[AbstractShop.updateStock] updateSign, new stock != oldStock! newStock: " +
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
		BlockData signBlockData = signLocation.getBlock().getBlockData();
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
	
	public UUID getOwnerUUID() {
		return owner;
	}
	
	public Component getOwnerName() {
		if(this.isAdmin()){
			return Component.text(adminOfflinePlayer.getName());
		}
		
		if(this.getOwnerUUID() != null){
			// Use cache first - this avoids expensive disk I/O
			return Component.text(PlayerNameCache.getName(this.getOwnerUUID()));
		}
		
		return Component.text("CLOSED").color(TextColor.color(255, 0, 0));
	}
	
	public OfflinePlayer getOwner() {
		if(isAdmin){
			return adminOfflinePlayer;
		}
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
	
	/**
	 * The item used to visually display on {@link AbstractDisplay}
	 */
	public ItemStack getDisplayItem() {
		return getItemStack();
	}
	
	/**
	 * The Display item for the secondary shop item if present
	 */
	public ItemStack getSecondaryDisplayItem() {
		return getSecondaryItemStack();
	}
	
	public double getPricePerItem() {
		// Calculate pricePerItem for partial sales, round up!
		return this.getPrice() / this.getAmount();
	}
	
	public double getItemsPerPriceUnit() {
		// Calculate items you can get for each price unit, round down!
		return this.getAmount() / this.getPrice();
	}
	
	public String getPricePerItemString() {
		double pricePer = this.getPricePerItem();
		return Main.getPlugin().getPriceString(pricePer, true);
	}
	
	//only use this method if the shop has not been added to the main handler maps yet
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
		if(isAdmin){
			this.owner = AdminOfflinePlayer.getAdminUUID();
		}
	}
	
	public void setItemStack(ItemStack is) {
		// If the item stack passed is null, go ahead and just skip it.
		if(is == null){
			return;
		}
		
		this.item = is.clone();
		this.item.setAmount(1);
		this.calculateStock();
		this.updateSign(true);
	}
	
	public void setSecondaryItemStack(ItemStack is) {
		this.secondaryItem = is.clone();
		this.secondaryItem.setAmount(1);
		this.calculateStock();
		this.updateSign(true);
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
		Main.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(signLocation, _ -> {
			// Update the GUI Icon since the sign needs an update.
			if(!(signLocation.getBlock().getState() instanceof Sign sign)){
				Main.getPlugin().logger().warning("Error attempting to update Shop sign! Sign Block for Shop is not a Sign (detected: " +
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
			boolean shouldGlow = Main.getPlugin().getSettingsConfig().isSetGlowingSignText();
			if(shouldGlow != frontSideSign.isGlowingText()){
				hasSignUpdate = true;
				frontSideSign.setGlowingText(shouldGlow);
			}
			// Update the sign if it has changed
			if(hasSignUpdate){
				sign.update(true);
			}
		}, 2);
	}
	
	public void printSalesInfo(Player player) {
		LangRequest request = Main.getPlugin().getLangManager().request("description." + this.getType().toString().toUpperCase());
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
               .replace("%item-amount%",shop.getAmount())
               .replace("%item-item-lore%", UtilMethods.getLore(item))
               .replace("%item-enchants%",UtilMethods.getEnchantmentsComponent(item));
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
	
	/**
	 * Starts a transaction with the specified party
	 */
	protected abstract Transaction startTransaction(TransactionParty party);
	
	/**
	 * executes a transaction between this shop and the other party
	 */
	public TransactionResult executeTransaction(TransactionParty party) {
		if(isPerformingTransaction){
			return TransactionResult.SHOP_IS_PERFORMING_TRANSACTION;
		}
		isPerformingTransaction = true;
		ShopLogger logger = Main.getPlugin().logger();
		logger.debug("====STARTING SHOP TRANSACTION====");
		logger.debug("Transaction with shop " + this + " and party " + party);
		Transaction transaction = startTransaction(party);
		logger.debug("Opened transaction " + transaction);
		var result = transaction.canFulfill();
		if(result != TransactionResult.OK){
			logger.debug("Transaction could not be fulfilled " + result);
			return result;
		}
		
		var event = new ShopTransactionEvent(this, party.getPlayer());
		Bukkit.getPluginManager().callEvent(event);
		
		if(event.isCancelled()){
			logger.debug("Transaction was cancelled by external plugin");
			return TransactionResult.CANCELLED;
		}
		
		transaction.execute();
		logTransaction(party);
		logger.debug("===FINISHED SHOP TRANSACTION====");
		postTransactionSuccess(transaction);
		isPerformingTransaction = false;
		return result;
	}
	
	/**
	 * Logs the transaction between the shop and the party
	 */
	protected void logTransaction(TransactionParty party) {
		Main.getPlugin().getShopmanager().getDatabase().logTransaction(id, System.currentTimeMillis(), party.getPlayer().getUniqueId(), null);
	}
	
	/**
	 * Runs after a transaction has been successfully made
	 */
	protected void postTransactionSuccess(Transaction transaction) {
		//do nothing
	}
	
	/**
	 * Executes a click on this shop
	 *
	 * @return true if something was done false if nothing happened
	 */
	public boolean executeClickAction(Player player, ShopClickType clickType) {
		ShopAction action = Main.getPlugin().getSettingsConfig().getShopAction(clickType);
		if(action == null){
			return false; //there is no action mapped to this click type
		}
		
		switch(action) {
			case TRANSACT:
				TransactionResult result = executeTransaction(new PlayerTransactionParty(player));
				if(result != TransactionResult.OK){
					return false;
				}
				break;
			case VIEW_DETAILS:
				this.printSalesInfo(player);
				break;
			case CYCLE_DISPLAY:
				//player clicked another player's shop sign
				if(!this.getOwnerUUID().equals(player.getUniqueId())){
					//player has permission to change another player's shop display
					if((isOperator(player))){
						this.cycleDisplay();
					}
					//player clicked own shop sign
				} else {
					if(!player.hasPermission("shop.setdisplay")){
						return false;
					}
					
					this.cycleDisplay();
				}
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * @return the currency type being used by the server
	 */
	public static CurrencyType getCurrencyType() {
		return Main.getPlugin().getSettingsConfig().getCurrencyType();
	}
	
	/**
	 * @return the item being used for currency or null if not defined, this value is present as long as its defined in the config, use {@link #getCurrencyType()} first to confirm what currency type is currently active on the server
	 */
	public static @Nullable ItemStack getCurrencyItem() {
		return Main.getPlugin().getItemConfig().getCurrencyItem();
	}
	
	/**
	 * @return the Shop Transaction Party represented by this shop
	 */
	protected ShopTransactionParty getParty() {
		return new ShopTransactionParty(this);
	}
	
	/**
	 * Cycles the display above the shop to the next possible one
	 */
	public void cycleDisplay() {
		if(facing == null){
			return;
		}
		ShopLogger logger = Main.getPlugin().logger();
		logger.debug("===STARTING DISPLAY CYCLE===");
		DisplayType[] cycle = Main.getPlugin().getSettingsConfig().getDisplayCycle();
		
		if(cycle.length == 0){
			logger.debug("Cycle list is empty cannot cycle");
			logger.debug("===CANCEL DISPLAY CYCLE===");
			return;
		}
		logger.debug("Cycling display");
		
		DisplayType currentType = getDisplay().getType();
		
		logger.debug("Current display " + currentType);
		logger.debug("Cycle: " + Arrays.toString(cycle));
		
		int currentIndex = -1;
		
		for(int i = 0; i < cycle.length; i++){
			if(cycle[i] == currentType){
				currentIndex = i;
				break;
			}
		}
		
		int startIndex = currentIndex == -1 ? 0 : (currentIndex + 1) % cycle.length;
		logger.debug("Current index " + currentIndex);
		DisplayType nextType = DisplayType.NONE;
		
		for(int offset = 0; offset < cycle.length; offset++){
			int index = (startIndex + offset) % cycle.length;
			
			DisplayType candidate = cycle[index];
			logger.debug("Check spawn requirements " + candidate);
			if(candidate.canSpawn(this)){
				logger.debug("Meets spawn requirements " + candidate);
				nextType = candidate;
				break;
			}
		}
		
		logger.debug("Next Display " + nextType);
		logger.debug("Removing old displays");
		Collection<Player> nearbyPlayers = this.getSignLocation().getNearbyPlayers(Main.getPlugin().getSettingsConfig().getMaxShopDisplayDistance());
		
		//remove display from all players nearby
		for(var nearbyPlayer : nearbyPlayers){
			this.display.remove(nearbyPlayer);
		}
		this.display = AbstractDisplay.createDisplay(nextType, this);
		logger.debug("Sending shop display update to nearby players");
		
		//refresh the shop display for all players within range of the shop
		for(var nearbyPlayer : nearbyPlayers){
			Main.getPlugin().getShopmanager().getDisplayManager().processShopDisplaysNearPlayer(nearbyPlayer, true);
		}
		setNeedsSave(true);
		logger.debug("===FINISHED DISPLAY CYCLE===");
	}
	
	public void sendEffects(boolean success, Player player) {
		try{
			SettingsConfig settingsConfig = Main.getPlugin().getSettingsConfig();
			if(success){
				if(settingsConfig.isPlaySounds()){
					player.playSound(signLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
				}
				if(settingsConfig.isPlayEffects()){
					player.getWorld().playEffect(containerLocation, Effect.DESTROY_BLOCK, Material.EMERALD_BLOCK);
				}
			} else {
				if(settingsConfig.isPlaySounds()){
					player.playSound(signLocation, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
				}
				if(settingsConfig.isPlayEffects()){
					player.getWorld().playEffect(containerLocation, Effect.DESTROY_BLOCK, Material.REDSTONE_BLOCK);
				}
			}
		} catch(Exception _){
		}
	}
	
	public void addSecondaryContainerLocation(@NotNull Location location) {
		if(secondaryContainerLocation != null){
			removeSecondaryContainerLocation();
		}
		secondaryContainerLocation = location;
		Main.getPlugin().getShopmanager().addSecondaryShopLocation(location, this);
	}
	
	public void removeSecondaryContainerLocation() {
		if(secondaryContainerLocation != null){
			Main.getPlugin().getShopmanager().removeSecondaryChestLocation(secondaryContainerLocation, this);
		}
		secondaryContainerLocation = null;
	}
	
	public Location getAboveSign() {
		return signLocation.clone().add(0, 1, 0);
	}
	
	public Location getAboveContainer() {
		return containerLocation.clone().add(0, 1, 0);
	}
	
	public Location getAboveSecondaryContainer() {
		if(secondaryContainerLocation != null){
			return secondaryContainerLocation.clone().add(0, 1, 0);
		}
		return null;
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
