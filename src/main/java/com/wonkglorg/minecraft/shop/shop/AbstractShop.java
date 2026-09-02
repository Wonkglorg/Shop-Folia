package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.AdminOfflinePlayer;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopDatabase;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.visibilityManager;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.event.ShopTransactionEvent;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.PlayerNameCache;
import com.wonkglorg.minecraft.shop.manager.ShopManager;
import com.wonkglorg.minecraft.shop.manager.ShopManager.BlockKey;
import com.wonkglorg.minecraft.shop.manager.player.OnlinePlayerProfile;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.isAllowedToCycleDisplay;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.isAllowedToCycleDisplayOther;
import com.wonkglorg.minecraft.shop.manager.visibility.SignUpdateHandler;
import static com.wonkglorg.minecraft.shop.manager.visibility.SignUpdateHandler.getComponents;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.shop.settings.Setting;
import com.wonkglorg.minecraft.shop.shop.settings.Settings;
import static com.wonkglorg.minecraft.shop.shop.settings.Settings.PURCHASE_COOLDOWN;
import static com.wonkglorg.minecraft.shop.shop.settings.Settings.PURCHASE_LIMIT;
import com.wonkglorg.minecraft.shop.shop.transaction.Transaction;
import com.wonkglorg.minecraft.shop.shop.transaction.TransactionResult;
import com.wonkglorg.minecraft.shop.shop.transaction.party.PlayerTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.TransactionParty;
import static com.wonkglorg.minecraft.shop.util.ChestUtil.getOtherChestDirection;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import com.wonkglorg.minecraft.util.date.DateType;
import com.wonkglorg.minecraft.util.date.DurationBuilder;
import it.unimi.dsi.fastutil.Pair;
import static it.unimi.dsi.fastutil.Pair.of;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
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
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.Boolean.TRUE;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings({"unused"})
public abstract class AbstractShop{
	/**
	 * Static reference used for all admin shop owners
	 */
	private static final AdminOfflinePlayer adminOfflinePlayer = new AdminOfflinePlayer();
	/**
	 * Block Data used for positive interaction results
	 */
	private static final @NotNull BlockData EMERALD_BLOCK_DATA = Material.EMERALD_BLOCK.createBlockData();
	/**
	 * Block Data used for negative interaction results
	 */
	private static final @NotNull BlockData REDSTONE_BLOCK_DATA = Material.REDSTONE_BLOCK.createBlockData();
	/**
	 * Decimal Format for prices
	 */
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");
	
	/**
	 * The shops unique identifier
	 */
	@Getter
	protected final UUID id;
	/**
	 * The millisecond time of the shops creation
	 */
	@Getter
	@Setter
	protected long creationDate;
	/**
	 * If the shop needs to be saved to the db due to changed values
	 */
	@Setter
	protected boolean needsSave = false;
	/**
	 * Has the shop been fully loaded via {@link #load()}
	 */
	@Getter
	protected boolean isLoaded = false;
	/**
	 * The location of the sign
	 */
	@Getter
	protected final Location signLocation;
	/**
	 * Represents a block key used for indexing and accessing references
	 */
	@Getter
	protected final BlockKey signKey;
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
	/**
	 * The facing direction of the shop sign
	 */
	@Getter
	protected BlockFace facing;
	
	/**
	 * Shop owners uuid
	 */
	protected final UUID owner;
	/**
	 * The main item being traded
	 */
	protected @Nullable ItemStack item;
	/**
	 * The secondary item being traded used for {@link BarterShop}
	 */
	protected @Nullable ItemStack secondaryItem;
	/**
	 * The display shown above the shop
	 */
	@Getter
	protected @NotNull AbstractDisplay display;
	/**
	 * Price of the shop
	 */
	@Getter
	protected double price;
	/**
	 * Amount this shop sells
	 */
	@Getter
	protected int amount;
	/**
	 * If the shop is considered an admin shop
	 */
	@Getter
	protected boolean isAdmin;
	/**
	 * The type of the shop
	 */
	@Getter
	protected ShopType type;
	/**
	 * If the shop is currently performing a transaction
	 */
	@Getter
	protected boolean isPerformingTransaction;
	
	/**
	 * Custom settings this shop has defined.
	 */
	private final Map<Setting<?>, Object> settings = new ConcurrentHashMap<>();
	/**
	 * If the sign used for creation was spawned by the plugin or not, used to determine if the sign drops when the shop is destroyed
	 */
	@Setter
	@Getter
	protected boolean fakeSign;
	/**
	 * Current stock amount of the shop, represents the stick since the last chest interaction this might have changed and should be updated using {@link #updateSign()} if any plugin container changes were performed
	 */
	@Getter
	protected int stock;
	/**
	 * The current state of the shop stock based on the stock amount and space in the shops inventory
	 */
	@Getter
	private ShopState shopState = OK;
	
	/**
	 *
	 * @param id the unique id of the shop
	 * @param signLoc its sign location
	 * @param player the owner of the shop
	 * @param type the type of shop
	 * @param pri the price of the shop
	 * @param amt the amount being traded
	 * @param admin if the shop an admin shop
	 * @param facing the facing direction of the sign
	 * @param creationDate when the shop was created initially
	 * @param displayType the display type to show
	 */
	protected AbstractShop(@NotNull UUID id,
						   @NotNull Location signLoc,
						   @NotNull UUID player,
						   @NotNull ShopType type,
						   double pri,
						   int amt,
						   boolean admin,
						   @NotNull BlockFace facing,
						   long creationDate,
						   @NotNull DisplayType displayType) {
		this.id = id;
		this.signLocation = signLoc;
		this.signKey = BlockKey.of(signLoc);
		this.type = type;
		this.price = pri;
		this.amount = amt;
		this.isAdmin = admin;
		this.item = null;
		this.secondaryItem = null;
		this.facing = facing;
		this.creationDate = creationDate;
		this.display = AbstractDisplay.createDisplay(displayType, this);
		
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
		} else {
			this.owner = player;
		}
	}
	
	/**
	 *
	 * @param id the unique id of the shop
	 * @param signLoc its sign location
	 * @param player the owner of the shop
	 * @param shopType the type of shop
	 * @param pri the price of the shop
	 * @param amt the amount being traded
	 * @param admin if the shop an admin shop
	 * @param facing the facing direction of the sign
	 * @param creationDate when the shop was created initially
	 * @param type the display type to show
	 * @return a new abstract shop constructed with these values
	 */
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
	
	/**
	 * @return if the chunk this shop is in is loaded
	 */
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
			logger().warning("Error attempting to load shop! No sign found for Shop (detected: AIR), deleting shop: " + this);
			return false;
		}
		
		if(!(signBlock.getBlockData() instanceof WallSign wallSign)){
			logger().warning("Error attempting to load shop! Sign Block for Shop is not a WallSign (detected: " +
							 signBlock.getType() +
							 "), deleting shop: " +
							 this);
			return false;
		}
		
		// Refresh the sign direction from the actual world state.
		facing = wallSign.getFacing();
		
		// The primary container is directly behind the sign.
		Block containerBlock = signBlock.getRelative(facing.getOppositeFace());
		
		if(!shopManager().isAllowedContainer(containerBlock)){
			logger().warning("Error attempting to load shop! Invalid block type detected when trying to load Shop Container (detected: " +
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
		
		//set the default sign data on join before stock gets calculated and per client sign packets get sent
		setDefaultSignData(signBlock);
		
		// Now that the world/container data is valid, refresh stock and state.
		updateStock();
		
		isLoaded = true;
		return true;
	}
	
	/**
	 * Sets the default data of a sign (this will not be seen by the client usually unless their client packet is slow at being sent or otherwise fails)
	 */
	private void setDefaultSignData(Block signBlock) {
		List<Component> lines = SignUpdateHandler.getDefaultSignLines(this);
		Sign sign = (Sign) signBlock.getState();
		SignSide front = sign.getSide(Side.FRONT);
		
		for(int i = 0; i < 4; i++){
			front.line(i, lines.get(i));
		}
		
		front.setGlowingText(ShopPlugin.getPlugin().getSettingsConfig().isSignGlowingSignText());
		sign.setWaxed(ShopPlugin.getPlugin().getSettingsConfig().isSignGlowingSignText());
		sign.update(true);
	}
	
	/**
	 * @return if the shop has outstanding data to be saved
	 */
	public boolean needsSave() {
		return needsSave;
	}
	
	/**
	 * Deletes the shop, same as calling {@link ShopManager#unregisterShop(AbstractShop)}
	 */
	public void delete() {
		shopManager().unregisterShop(this);
	}
	
	/**
	 * How often the given player has used this shop
	 */
	public int usageTimes(PlayerProfile player) {
		return player.getPurchaseCount(this);
	}
	
	/**
	 * When the player has last used the shop, provided in millisecond timestamp
	 */
	public long lastUsedTime(PlayerProfile player) {
		return player.getLastPurchaseTime(this);
	}
	
	/**
	 * Calculates the stock amount of the shop
	 */
	protected abstract void calculateStock();
	
	/**
	 * Recalculates and updates the shops stock
	 */
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
	
	/**
	 * @param stock set the stock amount (should only be used to fill in cached data until the shop can be chunk loaded to get the proper stock count via {@link #updateStock()}
	 */
	public void setStockOnLoad(int stock) {
		this.stock = stock;
	}
	
	/**
	 * @return If the shop is fully initialized and has a valid item
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isInitialized() {
		return (item != null);
	}
	
	/**
	 * @return the sign represented by this shop
	 */
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
	
	/**
	 * @return loads the current inventory from its block state
	 */
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
	
	/**
	 * @return the uuid of the owner
	 */
	public @NotNull UUID getOwnerUUID() {
		return owner;
	}
	
	/**
	 * @return the raw string name of the shop
	 */
	public @NotNull String getOwnerName() {
		if(this.isAdmin){
			return "admin";
		}
		return PlayerNameCache.getName(this.owner);
	}
	
	/**
	 * @return the formatted name of the shop owner based on the configs definition
	 */
	public @NotNull Component getOwnerNameFormatted() {
		if(this.isAdmin){
			//noinspection UnknownLangKey
			return langManager().request("placeholders.server-display-name").toSingleComponent();
		}
		//noinspection UnknownLangKey
		return MiniMessage.miniMessage().deserialize(langManager().request("placeholders.shop-owner-name-color").toSingleStringResult() +
													 PlayerNameCache.getName(this.getOwnerUUID()));
	}
	
	/**
	 * @return the player object of the shop owner or if the shop is an admin shop {@link AdminOfflinePlayer}
	 */
	public @NotNull OfflinePlayer getOwner() {
		if(isAdmin){
			return adminOfflinePlayer;
		}
		return Bukkit.getOfflinePlayer(this.owner);
	}
	
	/**
	 * @return copy of the main item of this shop (always with a count of 1)
	 */
	public @Nullable ItemStack getItemStack() {
		if(item != null){
			ItemStack is = item.clone();
			is.setAmount(1);
			return is;
		}
		return null;
	}
	
	/**
	 * @return copy of the secondary item of this shop (always with a count of 1)
	 */
	public @Nullable ItemStack getSecondaryItemStack() {
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
	
	/**
	 * Sets this shops item stack
	 */
	public void setItemStack(ItemStack is) {
		// If the item stack passed is null, go ahead and just skip it.
		if(is == null){
			return;
		}
		
		this.item = is.clone();
		this.item.setAmount(1);
	}
	
	/**
	 * Sets this shops secondary item stack
	 */
	public void setSecondaryItemStack(ItemStack is) {
		if(is == null){
			return;
		}
		this.secondaryItem = is.clone();
		this.secondaryItem.setAmount(1);
	}
	
	/**
	 * Evaluates the shop state this player should see this shop as (this differs from {@link #shopState} when either a purchase limit or a purchase cooldown is defined
	 *
	 * @param profile the profile of te player
	 */
	public ShopStateClient getClientShopState(PlayerProfile profile) {
		if(PURCHASE_LIMIT.isEnabled()){
			int purchaseLimit = getSetting(PURCHASE_LIMIT);
			if(purchaseLimit > 0){
				if(profile.getPurchaseCount(this) > purchaseLimit){
					return ShopStateClient.LIMIT_REACHED;
				}
			}
		}
		
		if(PURCHASE_COOLDOWN.isEnabled()){
			long purchaseCooldown = getSetting(PURCHASE_COOLDOWN);
			if(purchaseCooldown > 0){
				if((profile.getLastPurchaseTime(this) + purchaseCooldown) < System.currentTimeMillis()){
					return ShopStateClient.ON_COOLDOWN;
				}
			}
			
		}
		
		return shopState.getClientState();
	}
	
	/**
	 * Evaluates the shop state this player should see this shop as (this differs from {@link #shopState} when either a purchase limit or a purchase cooldown is defined
	 *
	 * @param player the player
	 */
	public ShopStateClient getClientShopState(Player player) {
		return getClientShopState(PlayerManager.getOnlineProfileIfCached(player.getUniqueId()));
	}
	
	/**
	 * Sets a new shop state and refreshes sign if shop state changed
	 */
	public void setShopState(ShopState state, boolean updateSign) {
		var oldState = shopState;
		shopState = state;
		if(!updateSign){
			return;
		}
		
		//shop needs to be fully loaded first before sending sign updates to players this otherwise causes issues when a player logs into the server and in the processs of joining loads a shopp which gets sent to them too early
		if(shopState != oldState && isLoaded){
			updateSign();
		}
	}
	
	/**
	 * Prints Shop info about the shop to the player in chat
	 */
	public void printSalesInfo(Player player) {
		LangRequest request = langManager().request("description." + this.getType());
		shopPlaceholders(request, this, true, player);
		request.sendToAudience(player);
	}
	
	/**
	 * Adds the shops placeholder values to the request
	 *
	 * @param request the request to add to
	 * @param shop the shops data
	 * @param includeHover if items should contain hover components
	 * @param player the player this will be sent to (used for floodgate integration)
	 */
	public static void shopPlaceholders(LangRequest request, AbstractShop shop, boolean includeHover, OfflinePlayer player) {
		//@formatter:off
		ItemStack item = shop.item;
		
		if(includeHover && ShopPlugin.floodGateEnabled){
			FloodgateApi api = FloodgateApi.getInstance();
			if(api != null && api.isFloodgateId(player.getUniqueId())){
				includeHover = false;
			}
		}
		
		OnlinePlayerProfile profile = PlayerManager.getOnlineProfileIfCached(player.getUniqueId());
		request.replace("%owner%", shop::getOwnerNameFormatted)
			   .replace("%stock-state%",shop.getShopState().toString())
			   .replace("%price%",shop.getPriceFormatted())
			   .replace("%stock%",shop.getStock())
			   .replace("%amount%",shop.getAmount())
			   .replace("%location%",UtilMethods.getCleanLocation(shop.getSignLocation(),false))
			   .replace("%price-per-item%",shop.price / shop.amount)
			   .replace("%world%",shop.getSignLocation().getWorld().getName())
               .replace("%item-amount%",shop.getAmount());
		
		if(profile != null){
			if(PURCHASE_COOLDOWN.isEnabled()){
				DurationBuilder durationBuilder = DurationBuilder.create(Duration.ofMillis(Math.max(0,shop.getSetting(PURCHASE_COOLDOWN) - profile.getLastPurchaseTime(shop)))).typesToShow(DateType.DAY,DateType.HOUR,DateType.MINUTE,DateType.SECOND).noDecimals();
				request.replace("%cooldown%",durationBuilder.toTimeString());
			}
			if(PURCHASE_LIMIT.isEnabled()){
				request.replace("%current-purchase-limit%", profile.getPurchaseCount(shop))
						.replace("%total-purchase-limit%",shop.getSetting(PURCHASE_LIMIT));
			}
		}
		
		
		if(item != null){
			request.replace("%item-enchants%",()->UtilMethods.getEnchantmentsComponent(item))
				   .replace("%item-type%", item.getType().toString());
			if(includeHover){
				request.replace("%item%", ()->ItemNameUtil.getName(item).hoverEvent(ItemNameUtil.getItemHover(item)));
			}else{
				request.replace("%item%", ()->ItemNameUtil.getName(item));
			}
		}
		
		ItemStack barterItem = shop.secondaryItem;
		if(barterItem != null){
			request.replace("%barter-item-type%", barterItem.getType().toString())
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
	 * Notifies both parties about the transaction
	 *
	 * @param purchaser the player transacting with the shop
	 * @param multiplier how many instances were transacted
	 */
	protected void notifyTransaction(Player purchaser, int multiplier) {
		LangRequest userRequest = langManager().request("transaction.success." + type + ".user");
		shopPlaceholders(userRequest, this, false, purchaser);
		userRequest.replace("%price%", formatPrice(price * multiplier));
		userRequest.replace("%item-amount%", amount * multiplier);
		userRequest.sendToAudience(purchaser);
		OfflinePlayer shopOwner = getOwner();
		if(!shopOwner.isOnline()){
			return;
		}
		Player ownerPlayer = shopOwner.getPlayer();
		assert ownerPlayer != null;
		OnlinePlayerProfile ownerProfile = PlayerManager.getOnlineProfile(ownerPlayer);
		if(getSetting(Settings.TRANSACTION_NOTIFICATION) == TRUE || ownerProfile.isNotifyTransaction()){
			LangRequest ownerRequest = langManager().request("transaction.success." + type + ".owner").replace("%user%", purchaser.getName());
			shopPlaceholders(ownerRequest, this, false, shopOwner);
			ownerRequest.replace("%price%", formatPrice(price * multiplier));
			ownerRequest.replace("%item-amount%", amount * multiplier);
			ownerRequest.sendToAudience(requireNonNull(shopOwner.getPlayer()));
		}
	}
	
	/**
	 * Notifies both parties about the transaction
	 *
	 * @param purchaser the player transacting with the shop
	 * @param multiplier how many instances were transacted
	 */
	protected void notifyNoSpace(Player purchaser, int multiplier) {
		langManager().request("transaction.issue." + type + ".shop-no-space").sendToAudience(purchaser);
		OfflinePlayer shopOwner = getOwner();
		if(!shopOwner.isOnline()){
			return;
		}
		Player ownerPlayer = shopOwner.getPlayer();
		assert ownerPlayer != null;
		OnlinePlayerProfile ownerProfile = PlayerManager.getOnlineProfile(ownerPlayer);
		if(getSetting(Settings.OUT_OF_STOCK_NOTIFICATION) == TRUE || ownerProfile.isNotifyStock()){
			var ownerRequest = langManager().request("transaction.success." + type + ".owner-no-space").replace("%user%", purchaser.getName());
			ownerRequest.replace("%location%", UtilMethods.getCleanLocation(getSignLocation(), false));
			ownerRequest.replace("%price%", formatPrice(price * multiplier));
			ownerRequest.replace("%item-amount%", amount * multiplier);
			ownerRequest.sendToAudience(requireNonNull(ownerPlayer));
		}
	}
	
	/**
	 * Notifies both parties about the transaction
	 *
	 * @param purchaser the player transacting with the shop
	 * @param multiplier how many instances were transacted
	 */
	protected void notifyNoStock(Player purchaser, int multiplier) {
		langManager().request("transaction.issue." + type + ".shop-no-stock").sendToAudience(purchaser);
		OfflinePlayer shopOwner = getOwner();
		if(!shopOwner.isOnline()){
			return;
		}
		Player ownerPlayer = shopOwner.getPlayer();
		assert ownerPlayer != null;
		OnlinePlayerProfile ownerProfile = PlayerManager.getOnlineProfile(ownerPlayer);
		if(getSetting(Settings.OUT_OF_STOCK_NOTIFICATION) == TRUE || ownerProfile.isNotifyStock()){
			var ownerRequest = langManager().request("transaction.success." + type + ".owner-no-stock").replace("%user%", purchaser.getName());
			ownerRequest.replace("%location%", UtilMethods.getCleanLocation(getSignLocation(), false));
			ownerRequest.replace("%price%", formatPrice(price * multiplier));
			ownerRequest.replace("%item-amount%", amount * multiplier);
			ownerRequest.sendToAudience(requireNonNull(shopOwner.getPlayer()));
		}
	}
	
	/**
	 * Starts a transaction with the specified party
	 *
	 * @param party the party this shop transactions with
	 * @param multiplier how many times the shop should transact with the player. (1 = the normal shops amount for the listed price, 2 = 2 times both values)
	 */
	protected abstract @NotNull Transaction startTransaction(TransactionParty party, int multiplier);
	
	/**
	 * executes a transaction between this shop and the other party
	 *
	 * @param party the party trading with this shop
	 * @param requestFullstack if the request to buy a full stack
	 * @return the result of the transaction and how often the trade happened if successful
	 */
	public @NotNull Pair<TransactionResult, Integer> executeTransaction(TransactionParty party, boolean requestFullstack) {
		if(isPerformingTransaction){
			return of(TransactionResult.SHOP_IS_PERFORMING_TRANSACTION, 0);
		}
		ShopLogger logger = logger();
		isPerformingTransaction = true;
		logger.debug("====STARTING SHOP TRANSACTION====");
		
		if(party.getPlayer().getUniqueId().equals(owner) && !ShopPlugin.getPlugin().getSettingsConfig().isAllowUseOwnShop()){
			logger.debug("Owner is trying to transact their own shop while this debug feature is disabled in the config");
			logger.debug("===CANCEL SHOP TRANSACTION====");
			isPerformingTransaction = false;
			return of(TransactionResult.OWNER_CANT_TRANSACT_OWN_SHOP, 0);
		}
		
		OfflinePlayer player = party.getPlayer();
		PlayerProfile profile = PlayerManager.getOnlineProfileIfCached(player.getUniqueId());
		if(profile != null){
			if(PURCHASE_LIMIT.isEnabled()){
				logger.debug("Purchase limit is enabled!");
				int purchaseLimit = getSetting(PURCHASE_LIMIT);
				if(purchaseLimit > 0){
					logger.debug("Shop has a purchase limit setting defined!");
					if(profile.getPurchaseCount(this) > purchaseLimit){
						return of(TransactionResult.PURCHASE_LIMIT_REACHED, 0);
					}
				}
			}
			
			if(PURCHASE_COOLDOWN.isEnabled()){
				logger.debug("Purchase cooldown is enabled!");
				long purchaseCooldown = getSetting(PURCHASE_COOLDOWN);
				if(purchaseCooldown > 0){
					logger.debug("Shop has a purchase cooldown setting defined!");
					if((profile.getLastPurchaseTime(this) + purchaseCooldown) < System.currentTimeMillis()){
						return of(TransactionResult.PURCHASE_COOLDOWN, 0);
					}
				}
			}
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
	protected @NotNull Transaction findAffordableTransaction(TransactionParty party, boolean requestFullstack) {
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
		assert item != null;
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
		shopDatabase().logTransaction(id, System.currentTimeMillis(), party.getPlayer().getUniqueId(), null, multiplier);
		
		var profileIfLoaded = PlayerManager.getOnlineProfileIfCached(party.getPlayer().getUniqueId());
		if(profileIfLoaded != null){
			//update cached values for the online player
			profileIfLoaded.recordPurchase(id, System.currentTimeMillis(), multiplier);
		}
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
		ShopAction action = ShopPlugin.getPlugin().getSettingsConfig().getShopAction(clickType);
		if(action == null){
			return false; //there is no action mapped to this click type
		}
		
		switch(action) {
			case TRANSACT, TRANSACT_FULL_STACK:
				var resultPair = executeTransaction(new PlayerTransactionParty(player), action == ShopAction.TRANSACT_FULL_STACK);
				TransactionResult result = resultPair.first();
				int multiplier = resultPair.right();
				sendTransactionMessage(result, multiplier, player);
				sendEffects(result == TransactionResult.OK, player);
				return true;
			case VIEW_DETAILS:
				this.printSalesInfo(player);
				break;
			case CYCLE_DISPLAY:
				//player clicked another player's shop sign
				if(!this.getOwnerUUID().equals(player.getUniqueId())){
					//player has permission to change another player's shop display
					if((isAllowedToCycleDisplayOther(player))){
						this.cycleDisplay();
						return true;
					}
					return false;
				}
				
				if(!isAllowedToCycleDisplay(player)){
					return false;
				}
				this.cycleDisplay();
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
	 */
	protected abstract void sendTransactionMessage(TransactionResult result, int multiplier, Player player);
	
	/**
	 * @return the currency type being used by the server
	 */
	public static @NotNull CurrencyType getCurrencyType() {
		return ShopPlugin.getPlugin().getSettingsConfig().getCurrencyType();
	}
	
	/**
	 * @return the item being used for currency or null if not defined, this value is present as long as it's defined in the config, use {@link #getCurrencyType()} first to confirm what currency type is currently active on the server
	 */
	public static @Nullable ItemStack getCurrencyItem() {
		return ShopPlugin.getPlugin().getItemConfig().getCurrencyItem();
	}
	
	/**
	 * @return the Shop Transaction Party represented by this shop
	 */
	protected @NotNull ShopTransactionParty getParty() {
		return new ShopTransactionParty(this);
	}
	
	/**
	 * Cycles the display above the shop to the next possible one
	 */
	public void cycleDisplay() {
		if(facing == null){
			return;
		}
		ShopLogger logger = logger();
		logger.debug("===STARTING DISPLAY CYCLE===");
		DisplayType[] cycle = ShopPlugin.getPlugin().getSettingsConfig().getDisplayCycle();
		
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
		Collection<Player> nearbyPlayers = this.getSignLocation().getNearbyPlayers(ShopPlugin.getPlugin()
																							 .getSettingsConfig()
																							 .getMaxShopProcessingDistanceChunks());
		
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
		//refresh the display for all users that can see it
		updateSign();
		setNeedsSave(true);
		logger.debug("===FINISHED DISPLAY CYCLE===");
	}
	
	/**
	 * Refreshes the sign for every player who can currently see it
	 */
	public void updateSign() {
		visibilityManager().updateShop(this);
	}
	
	/**
	 * Gets this shops calculated sign lines for the given player
	 *
	 * @param player the player viewing the shop
	 * @return constructed component list with a lnegth of 4 entries
	 */
	public List<Component> getSignLines(PlayerProfile player) {
		String langKey = "sign.text." + type.toString().toLowerCase() + ".";
		langKey += switch(getClientShopState(player)) {
			case ShopStateClient.OK -> "in-stock";
			case ShopStateClient.OVERFILLED -> "overfilled";
			case ShopStateClient.ON_COOLDOWN -> "transaction-cooldown";
			case ShopStateClient.LIMIT_REACHED -> "transaction-limit";
			case ShopStateClient.EMPTY -> "out-of-stock";
		};
		
		DisplayType displayType = display.getType();
		
		if(displayType == DisplayType.NONE){
			langKey += "-no-display";
		}
		
		return getComponents(this, langKey);
	}
	
	/**
	 * Sends effects to the player based on the config defined options
	 *
	 * @param success if its a success or fail effect
	 * @param player the player to send it to
	 */
	public void sendEffects(boolean success, Player player) {
		SettingsConfig settingsConfig = ShopPlugin.getPlugin().getSettingsConfig();
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
	}
	
	/**
	 * Adds a secondary container location to this shop
	 *
	 * @param location the location of the secondary container
	 */
	@Internal
	public void addSecondaryContainerLocation(@NotNull Location location) {
		if(secondaryContainerLocation != null){
			removeSecondaryContainerLocation();
		}
		secondaryContainerLocation = location;
		shopManager().addSecondaryShopLocation(location, this);
	}
	
	/**
	 * Removes the secondary container location from this shop
	 */
	@Internal
	public void removeSecondaryContainerLocation() {
		if(secondaryContainerLocation != null){
			shopManager().removeSecondaryChestLocation(secondaryContainerLocation, this);
		}
		secondaryContainerLocation = null;
	}
	
	/**
	 * @return the location above the sign
	 */
	public @NotNull Location getAboveSign() {
		return signLocation.clone().add(0, 1, 0);
	}
	
	/**
	 * @return the location above the main shop container
	 */
	public @NotNull Location getAboveContainer() {
		return containerLocation.clone().add(0, 1, 0);
	}
	
	/**
	 * @return the location above the secondary container if exists
	 */
	public @Nullable Location getAboveSecondaryContainer() {
		if(secondaryContainerLocation != null){
			return secondaryContainerLocation.clone().add(0, 1, 0);
		}
		return null;
	}
	
	/**
	 * Formats a double to the decimal price format the shop shows
	 */
	public static @NotNull String formatPrice(double price) {
		return DECIMAL_FORMAT.format(price);
	}
	
	/**
	 * Shows the price of the shop as a proper formatted string to show to players
	 */
	public @NotNull String getPriceFormatted() {
		return formatPrice(price);
	}
	
	/**
	 * Sets the settings value for this shop
	 *
	 * @param setting the setting to set
	 * @param value the value to set it to
	 */
	public <T> void setSetting(Setting<T> setting, T value) {
		T currentValue = getSetting(setting);
		if(Objects.equals(value, currentValue)){
			return;
		}
		if(value == null){
			settings.remove(setting);
		} else {
			settings.put(setting, value);
		}
		shopDatabase().addSetting(this, setting, value);
	}
	
	/**
	 * Gets a shops defined settings value or the default value if not present
	 *
	 * @param setting the setting to get its value of
	 */
	public <T> T getSetting(Setting<T> setting) {
		Object value = settings.get(setting);
		
		if(value == null){
			return setting.getDefaultValue();
		}
		
		return setting.getType().cast(value);
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
			   (isAdmin ? ", isAdmin=true" : "") +
			   ", stock=" +
			   stock +
			   ", owner=" +
			   owner +
			   ", signLocation=" +
			   (signLocation != null ? signLocation.getWorld().getName() +
									   ":" +
									   signLocation.getBlockX() +
									   "/" +
									   signLocation.getBlockY() +
									   "/" +
									   signLocation.getBlockZ() : "null") +
			   ", chestLocation=" +
			   (containerLocation != null ? containerLocation.getWorld().getName() +
											":" +
											containerLocation.getBlockX() +
											"/" +
											containerLocation.getBlockY() +
											"/" +
											containerLocation.getBlockZ() : "null") +
			   '}';
	}
}
