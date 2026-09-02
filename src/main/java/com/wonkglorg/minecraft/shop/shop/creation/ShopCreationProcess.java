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
import com.wonkglorg.minecraft.shop.shop.transaction.party.PlayerTransactionParty;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

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
	
	protected ShopCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
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
	 * Verifies to see if a player has all things needed to create a potential shop <br>!!NOTE!!<br> this does not replace the other checks needed to preverify if a shop is even valid to be placed there or if a shop is already being created by the user. refer to {@link com.wonkglorg.minecraft.shop.listener.ShopListener#onShopCreation(SignChangeEvent)}
	 */
	public boolean canPlayerFulfillsCreationRequirements() {
		if(playerIsOperator){
			logger().debug("Player is op, skipping creation check");
			return true;
		}
		if(!isAllowedInDimension()){
			logger().debug("Dimension check failed");
			lang.request("interaction.issues.create.world-blacklist").sendToAudience(player);
			return false;
		}
		
		//no create permissions for any shop
		if(!PlayerProfile.isAllowedToCreateShop(player)){
			logger().debug("Player lacks permission to create shop of this type");
			lang.request("permission.error.create").sendToAudience(player);
			return false;
		}
		
		int numberOfShops = shopManager.getNumberOfShops(player.getUniqueId());
		int buildPermissionNumber = getShopBuildLimit(player);
		if(numberOfShops >= buildPermissionNumber){
			logger().debug("Player exceeds shop build limit");
			lang.request("permission.error.buildLimit")
				.replace("%user-amount%", numberOfShops)
				.replace("%build-limit%", buildPermissionNumber)
				.sendToAudience(player);
			return false;
		}
		
		//if players must pay to create shops, check that they have enough money first
		double cost = ShopPlugin.getPlugin().getSettingsConfig().getCreationCost();
		if(cost > 0 && new PlayerTransactionParty(player).getAvailableFunds(ShopPlugin.getPlugin().getItemConfig().getCurrencyItem()) < cost){
			logger().debug("Player lacks funds to cover create shop costs");
			lang.request("interaction.issues.create.insufficient-funds").sendToAudience(player);
			return false;
		}
		return true;
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
	
	protected boolean isAllowedToCreateShop() {
		if(!PlayerProfile.isAllowedToCreateShop(player, type)){
			lang.request("permission.error.create-type").replace("%shop-type%", type.toString()).sendToAudience(player);
			return false;
		}
		return true;
	}
}
