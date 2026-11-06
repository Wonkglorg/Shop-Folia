package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.manager.ShopManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.getShopBuildLimit;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.DIMENSION_BLACKLIST;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.INSUFFICIENT_FUNDS;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.LACKS_ADMIN_CREATION_PERMISSION;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.LACKS_CREATION_PERMISSION;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.NO_DATA;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.SHOP_LIMIT_REACHED;
import static com.wonkglorg.minecraft.shop.shop.creation.CreationValidationResult.SUCCESS;
import com.wonkglorg.minecraft.shop.shop.transaction.party.PlayerTransactionParty;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A shop currently in creation process and not finished being setup yet
 */
public abstract class ShopCreationProcess{
	protected final LangManager lang;
	protected final ShopManager shopManager;
	@Getter
	protected final Player player;
	/**
	 * If the player creating the shop is an operator
	 */
	protected final boolean playerIsOperator;
	@Getter
	protected final UUID playerUUID;
	/**
	 * The id of the shop when created
	 */
	@Getter
	protected final UUID shopId;
	/**
	 * The sign this shop is related to
	 */
	@Getter
	protected Sign sign;
	/**
	 * The container this shop references
	 */
	@Getter
	protected Block container;
	protected BlockFace signDirection;
	
	@Getter
	protected ShopType type = ShopType.SELL;
	/**
	 * How much this shop sells
	 */
	@Getter
	protected int amount = 0;
	/**
	 * For what price
	 */
	@Getter
	protected double price = 0;
	/**
	 * The shop is an admin shop
	 */
	@Getter
	protected boolean adminShop = false;
	/**
	 * If the shop was created via a method that required no sign
	 */
	protected boolean isFakeSign = false;
	@Setter
	@Getter
	protected ItemStack itemStack = null;
	@Setter
	@Getter
	protected ItemStack secondaryStack = null;
	
	@Getter
	protected boolean finishedInitialisation = false;
	@Getter
	protected boolean isCancelled;
	
	/**
	 * If the data has been read in by the inheriting child class
	 */
	protected boolean hasData = false;
	
	protected ShopCreationProcess(@NotNull Player player, @NotNull Sign sign, @NotNull Block container, @NotNull BlockFace signDirection) {
		this.player = player;
		this.playerUUID = player.getUniqueId();
		this.shopId = UUID.randomUUID();
		this.sign = sign;
		this.container = container;
		this.signDirection = signDirection;
		this.playerIsOperator = PlayerProfile.isOperator(player);
		lang = langManager();
		shopManager = shopManager();
	}
	
	public ImmutableShopCreationProcess toImmutableProgress() {
		return new ImmutableShopCreationProcess(player,
				playerIsOperator,
				playerUUID,
				shopId,
				sign,
				container,
				signDirection,
				type,
				amount,
				price,
				adminShop,
				isFakeSign,
				itemStack,
				secondaryStack,
				finishedInitialisation,
				isCancelled);
	}
	
	/**
	 * Verifies if the shop creation process can be completed based on the provided data, this does not verify the data up to the creation of this object previously relevant data needs to be verified before this object was created example in {@link com.wonkglorg.minecraft.shop.listener.ShopListener#onShopCreation(SignChangeEvent)}
	 */
	public CreationValidationResult validateProcess() {
		if(!hasData){
			logger().debug("Player can't fulfill creation requirements");
			return NO_DATA;
		}
		if(playerIsOperator){
			logger().debug("Player is op, skipping creation check");
			return SUCCESS;
		}
		
		if(adminShop && !PlayerProfile.isAllowedToCreateAdminShop(player)){
			logger().debug("Player tried building an admin shop but lacks permission to do so");
			return LACKS_ADMIN_CREATION_PERMISSION;
		}
		
		if(!isAllowedInDimension()){
			logger().debug("Dimension check failed");
			return DIMENSION_BLACKLIST;
		}
		
		//no create permissions for any shop
		if(!PlayerProfile.isAllowedToCreateShop(player, type)){
			logger().debug("Player lacks permission to create shop of type " + type);
			return LACKS_CREATION_PERMISSION;
		}
		
		int numberOfShops = shopManager.getNumberOfShops(player.getUniqueId());
		int buildPermissionNumber = getShopBuildLimit(player);
		if(numberOfShops >= buildPermissionNumber){
			logger().debug("Player exceeds shop build limit has %s out of allowed %s".formatted(numberOfShops, buildPermissionNumber));
			return SHOP_LIMIT_REACHED;
		}
		
		//if players must pay to create shops, check that they have enough money first
		double cost = ShopPlugin.getPlugin().getSettingsConfig().getCreationCost();
		if(cost > 0 && new PlayerTransactionParty(player).getAvailableFunds(ShopPlugin.getPlugin().getItemConfig().getCurrencyItem(),
				ItemStack::isSimilar) < cost){
			logger().debug("Player lacks funds to cover create shop costs");
			return INSUFFICIENT_FUNDS;
		}
		
		return SUCCESS;
	}
	
	/**
	 * @return If the shop can be created in this world
	 */
	protected boolean isAllowedInDimension() {
		return !ShopPlugin.getPlugin().getSettingsConfig().getWorldBlackList().contains(container.getWorld().getName());
	}
	
	/**
	 * Creates a new abstract shop from all the data provided, this does not register the shop use {@link com.wonkglorg.minecraft.shop.manager.ShopManager#registerShop(AbstractShop)} to register and save the shop into the database
	 */
	public AbstractShop createShop() {
		
		final AbstractShop shop = AbstractShop.create(shopId,
				sign.getLocation(),
				player.getUniqueId(),
				price,
				amount,
				adminShop,
				type,
				signDirection,
				System.currentTimeMillis(),
				ShopPlugin.getPlugin().getSettingsConfig().getDisplayTypeDefault());
		shop.setFakeSign(isFakeSign);
		
		if(type != ShopType.GAMBLE){
			shop.setItemStack(itemStack);
			if(secondaryStack != null){
				shop.setSecondaryItemStack(secondaryStack);
			}
		}
		
		boolean loaded = shop.load();
		if(!loaded){
			logger().warning("Shop creation failed, unable to load the shop. Aborting shop creation."); // only seen this happen in tests
			return null;
		}
		
		shop.updateSign();
		finishedInitialisation = true;
		return shop;
	}
}
