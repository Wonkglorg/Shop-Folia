package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.AdminOfflinePlayer;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.event.ShopTransactionEvent;
import com.wonkglorg.minecraft.shop.manager.PlayerNameCache;
import com.wonkglorg.minecraft.shop.manager.ShopManager.BlockKey;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.isOperator;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.offline;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.online;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
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
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import it.unimi.dsi.fastutil.Pair;
import static it.unimi.dsi.fastutil.Pair.of;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class AbstractShop{
	private static final AdminOfflinePlayer adminOfflinePlayer = new AdminOfflinePlayer();
	
	private static final @NotNull BlockData EMERALD_BLOCK_DATA = Material.EMERALD_BLOCK.createBlockData();
	private static final @NotNull BlockData REDSTONE_BLOCK_DATA = Material.REDSTONE_BLOCK.createBlockData();
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");
	
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
	@Getter
	private ShopState shopState = OK;
	
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
		this.signLinesRequireRefresh = true; // Reload signs on load in case config changed!
		
		//can be null for legacy config imports.
		if(facing != null){
			//infer the container location where it should be
			this.containerLocation = new Location(signLoc.getWorld(),
					signLoc.getBlockX() - (double) facing.getModX(),
					signLoc.getBlockY() - (double) facing.getModY(),
					signLoc.getBlockZ() - (double) facing.getModZ());
			this.containerKey = BlockKey.of(containerLocation);
		}
		fakeSign = false;
		
		if(isAdmin){
			owner = AdminOfflinePlayer.getAdminUUID();
			stock = Integer.MAX_VALUE;
		}
	}
	
	public static AbstractShop create(UUID id,
	                                  Location signLoc,
	                                  UUID player,
	                                  double pri,
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
		if(containerBlock.getBlockData() instanceof Chest chestData && chestData.getType() != Type.SINGLE){
			
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
		
		//if the stock update did not already change the sign do it now (mostly needed when running a plugin reload to update all shop signs to the new lang entries)
		if(signLinesRequireRefresh){
			updateSign(true);
			signLinesRequireRefresh = false;
		}
		
		isLoaded = true;
		return true;
	}
	
	public boolean needsSave() {
		return needsSave;
	}
	
	/**
	 * Calculates the stock amount of the shop
	 */
	protected abstract void calculateStock();
	
	public void updateStock() {
		int oldStock = stock;
		
		// Update the stock
		this.calculateStock();
		
		// Update sign if needed
		boolean hasStockChange = stock != oldStock;
		if(hasStockChange){
			needsSave = true;
		}
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
			return Main.getPlugin().getLangManager().request("placeholders.server-display-name").toSingleComponent();
		}
		
		if(this.getOwnerUUID() != null){
			// Use cache first - this avoids expensive disk I/O
			return MiniMessage.miniMessage().deserialize(Main.getPlugin()
			                                                 .getLangManager()
			                                                 .request("placeholders.shop-owner-name-color")
			                                                 .toSingleStringResult() + PlayerNameCache.getName(this.getOwnerUUID()));
		}
		
		return Main.getPlugin().getLangManager().request("placeholders.closed-shop").toSingleComponent();
	}
	
	public OfflinePlayer getOwner() {
		if(isAdmin){
			return adminOfflinePlayer;
		}
		return Bukkit.getOfflinePlayer(this.owner);
	}
	
	/**
	 * @return the main item of this shop (always with a count of 1)
	 */
	public ItemStack getItemStack() {
		if(item != null){
			ItemStack is = item.clone();
			is.setAmount(1);
			return is;
		}
		return null;
	}
	
	/**
	 * @return the secondary item of this shop (always with a count of 1)
	 */
	public ItemStack getSecondaryItemStack() {
		if(secondaryItem != null){
			ItemStack is = secondaryItem.clone();
			is.setAmount(1);
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
	}
	
	public void setSecondaryItemStack(ItemStack is) {
		if(is == null){
			return;
		}
		this.secondaryItem = is.clone();
		this.secondaryItem.setAmount(1);
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
	
	/**
	 * Sets a new shop state and refreshes sign if shop state changed
	 */
	public void setShopState(ShopState state, boolean updateSign) {
		if(shopState != state && updateSign){
			updateSign(true);
			signLinesRequireRefresh = false;
		}
		shopState = state;
	}
	
	public void printSalesInfo(Player player) {
		LangRequest request = Main.getPlugin().getLangManager().request("description." + this.getType().toString().toUpperCase());
		shopPlaceholders(request, this, true);
		request.sendToAudience(player);
	}
	
	public static void shopPlaceholders(LangRequest request, AbstractShop shop, boolean includeHover) {
		//@formatter:off
		ItemStack item = shop.item;
		
		request.replace("%owner%", shop::getOwnerName)
			   .replace("%price%",shop.getPriceFormatted())
			   .replace("%stock%",shop.getStock())
			   .replace("%amount%",shop.getAmount())
			   .replace("%location%",UtilMethods.getCleanLocation(shop.getSignLocation(),false))
			   .replace("%price-per-item%",shop.price / shop.amount)
			   .replace("%world%",shop.getSignLocation().getWorld().getName())
               .replace("%item-type%", item.getType())
               .replace("%item-amount%",shop.getAmount())
               .replace("%item-enchants%",()->UtilMethods.getEnchantmentsComponent(item));
		
		if(includeHover){
			request.replace("%item%", ()->ItemNameUtil.getName(item).hoverEvent(ItemNameUtil.getItemHover(item)));
		}else{
			request.replace("%item%", ()->ItemNameUtil.getName(item));
		}
		ItemStack barterItem = shop.secondaryItem;
		if(barterItem != null){
			request.replace("%barter-item-type%", barterItem.getType())
			       .replace("%barter-durability%",UtilMethods.getDurabilityPercent(barterItem))
			       .replace("%barter-item-amount%", barterItem.getAmount())
				   .replace("%barter-item-enchants%",()->UtilMethods.getEnchantmentsComponent(barterItem));
			
			if(includeHover){
				request.replace("%barter-item%", ()->ItemNameUtil.getName(barterItem).hoverEvent(ItemNameUtil.getItemHover(barterItem)));
			}else{
				request.replace("%barter-item%", ()->ItemNameUtil.getName(barterItem));
			}
		}
		//@formatter:on
	}
	
	/**
	 * Starts a transaction with the specified party
	 *
	 * @param party the party this shop transactions with
	 * @param multiplier how many times the shop should transact with the player. (1 = the normal shops amount for the listed price, 2 = 2 times both values)
	 */
	protected abstract Transaction startTransaction(TransactionParty party, int multiplier);
	
	/**
	 * executes a transaction between this shop and the other party
	 */
	public Pair<TransactionResult, Integer> executeTransaction(TransactionParty party, boolean requestFullstack) {
		if(isPerformingTransaction){
			return of(TransactionResult.SHOP_IS_PERFORMING_TRANSACTION, 0);
		}
		ShopLogger logger = Main.getPlugin().logger();
		isPerformingTransaction = true;
		logger.debug("====STARTING SHOP TRANSACTION====");
		
		if(party.getPlayer().getUniqueId().equals(owner) && !Main.getPlugin().getSettingsConfig().isDebugAllowUseOwnShop()){
			logger.debug("Owner is trying to transact their own shop while this debug feature is disabled in the config");
			logger.debug("===CANCEL SHOP TRANSACTION====");
			isPerformingTransaction = false;
			return of(TransactionResult.OWNER_CANT_TRANSACT_OWN_SHOP, 0);
		}
		
		logger.debug("Transaction with shop " + this + " and party " + party);
		
		Transaction transaction = findAffordableTransaction(party, requestFullstack);
		logger.debug("Opened transaction " + transaction);
		int multiplier = transaction.getAmount() / amount;
		if(requestFullstack){
			logger.debug("Set biggest possible transaction multiplier to: " + multiplier);
		}
		var result = transaction.getResult();
		if(result != TransactionResult.OK){
			logger.debug("Transaction could not be fulfilled " + result);
			logger.debug("===CANCEL SHOP TRANSACTION====");
			isPerformingTransaction = false;
			return of(result, 0);
		}
		
		var event = new ShopTransactionEvent(this, party.getPlayer());
		Bukkit.getPluginManager().callEvent(event);
		
		if(event.isCancelled()){
			logger.debug("Transaction was cancelled by external plugin");
			logger.debug("===CANCEL SHOP TRANSACTION====");
			isPerformingTransaction = false;
			return of(TransactionResult.CANCELLED, 0);
		}
		
		transaction.execute();
		logTransaction(party, multiplier);
		logger.debug("===FINISHED SHOP TRANSACTION====");
		calculateStock();
		postTransactionSuccess(transaction);
		isPerformingTransaction = false;
		return of(result, multiplier);
	}
	
	/**
	 * Find the largest possible transaction that can be done
	 *
	 * @param party the party transactions with the shop
	 * @param requestFullstack if the request should try to do multiple transactions in one
	 * @return check its {@link Transaction#getResult()} before continuing with the transaction to check if it could succeed
	 */
	private @NotNull Transaction findAffordableTransaction(TransactionParty party, boolean requestFullstack) {
		if(!requestFullstack){
			Transaction transaction = startTransaction(party, 1);
			transaction.canFulfill();
			return transaction;
		}
		
		int maxMultiplier = getMaximumFullStackMultiplier();
		
		Transaction transaction = null;
		
		for(int multiplier = maxMultiplier; multiplier >= 1; multiplier--){
			transaction = startTransaction(party, multiplier);
			if(transaction.canFulfill() == TransactionResult.OK){
				return transaction;
			}
		}
		
		assert transaction != null;
		
		return transaction;
	}
	
	/**
	 *
	 * @return te maximum multiplier this shop allows based on the item being transacted.
	 */
	protected int getMaximumFullStackMultiplier() {
		int maxStackSize = item.getMaxStackSize();
		
		if(amount > maxStackSize){
			return 1;
		}
		
		return Math.max(1, maxStackSize / amount);
	}
	
	/**
	 * Logs the transaction between the shop and the party
	 */
	protected void logTransaction(TransactionParty party, int multiplier) {
		Main.getPlugin().getShopmanager().getDatabase().logTransaction(id,
				System.currentTimeMillis(),
				party.getPlayer().getUniqueId(),
				null,
				multiplier);
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
			case TRANSACT, TRANSACT_FULL_STACK:
				var resultPair = executeTransaction(new PlayerTransactionParty(player), action == ShopAction.TRANSACT_FULL_STACK);
				TransactionResult result = resultPair.first();
				int multiplier = resultPair.right();
				OfflinePlayer shopOwner = getOwner();
				sendTransactionMessage(result, multiplier, player, shopOwner.isConnected() ? online(shopOwner.getPlayer()) : offline(shopOwner));
				sendEffects(result == TransactionResult.OK, player);
				return true;
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
				return true;
		}
		return true;
	}
	
	/**
	 * The transaction messages to send
	 *
	 * @param result the result of the transaction
	 * @param multiplier how many times this item was traded in this transaction
	 * @param player the player transacting with the shop
	 * @param owner the shop owners profile
	 */
	protected abstract void sendTransactionMessage(TransactionResult result, int multiplier, Player player, PlayerProfile owner);
	
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
		
		if(currentType == nextType){
			logger.debug("no changes, next type is same as current type");
			logger.debug("===FINISHED DISPLAY CYCLE===");
			return;
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
			display.spawn(nearbyPlayer);
		}
		updateSign(true);
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
					player.getWorld().playEffect(containerLocation, Effect.DESTROY_BLOCK, EMERALD_BLOCK_DATA);
				}
			} else {
				if(settingsConfig.isPlaySounds()){
					player.playSound(signLocation, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
				}
				if(settingsConfig.isPlayEffects()){
					player.getWorld().playEffect(containerLocation, Effect.DESTROY_BLOCK, REDSTONE_BLOCK_DATA);
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
	
	public static String formatPrice(double price) {
		return DECIMAL_FORMAT.format(price);
	}
	
	public String getPriceFormatted() {
		return formatPrice(price);
	}
	
	@Override
	public String toString() {
		return "AbstractShop{" +
		       "id=" +
		       id +
		       ", type=" +
		       type.toString().toUpperCase() +
		       ", item=" +
		       item +
		       ", price=" +
		       price +
		       ", amount=" +
		       amount +
		       (secondaryItem != null ? ", secondaryItem=" + secondaryItem : "") +
		       (isAdmin ? ", isAdmin=" + isAdmin : "") +
		       ", stock=" +
		       stock +
		       ", owner=" +
		       owner +
		       ", signLocation=" +
		       ((signLocation != null) ? signLocation.getWorld().getName() +
		                                 ":" +
		                                 signLocation.getBlockX() +
		                                 "/" +
		                                 signLocation.getBlockY() +
		                                 "/" +
		                                 signLocation.getBlockZ() : "null") +
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
