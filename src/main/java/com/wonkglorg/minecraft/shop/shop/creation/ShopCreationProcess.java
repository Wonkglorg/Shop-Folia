package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.manager.ShopManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.getShopBuildLimit;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.display.CreationTextDisplay;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.EconomyUtils;
import com.wonkglorg.minecraft.config.LangManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
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
	protected int amount = 0;
	/**
	 * For what price
	 */
	protected double price = 0;
	/**
	 * If its a combo shop this is the sell price and {@link #price} the buy price
	 */
	protected double priceCombo = 0;
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
	protected ItemStack barterStack = null;
	
	@Getter
	protected boolean finishedInitialisation;
	@Getter
	protected boolean isCancelled;
	
	/**
	 * Display that represents the text lines above the shop giving visual feedback
	 */
	@Getter
	protected CreationTextDisplay display;
	
	protected ShopCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		this.player = player;
		this.playerUUID = player.getUniqueId();
		this.shopId = UUID.randomUUID();
		this.sign = sign;
		this.container = container;
		this.signDirection = signDirection;
		this.playerIsOperator = PlayerProfile.isOperator(player);
		lang = Shop.getPlugin().getLangManager();
		shopManager = Shop.getPlugin().getShopmanager();
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
				priceCombo,
				adminShop,
				isFakeSign,
				itemStack,
				barterStack,
				finishedInitialisation,
				isCancelled);
	}
	
	/**
	 * Verifies to see if a player has all things needed to create a potential shop <br>!!NOTE!!<br> this does not replace the other checks needed to preverify if a shop is even valid to be placed there or if a shop is already being created by the user. refer to {@link com.wonkglorg.minecraft.shop.listener.ShopListener#onShopCreation(SignChangeEvent)}
	 */
	public boolean canPlayerFulfillsCreationRequirements() {
		if(playerIsOperator){
			return true;
		}
		//can't build shop in this dimension
		Shop plugin = Shop.getPlugin();
		if(!isAllowedInDimension()){
			plugin.getLangManager().request("interaction_issue.worldBlacklist").sendToAudience(player);
			return false;
		}
		
		//no create permissions for any shop
		if(!PlayerProfile.isAllowedToCreateShop(player)){
			plugin.getLangManager().request("permission.error.create").sendToAudience(player);
			return false;
		}
		
		//if players must pay to create shops, check that they have enough money first
		double cost = Shop.getPlugin().getSettingsConfig().getCreationCost();
		if(cost > 0 && !EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)){
			lang.request("interaction_issue.createInsufficientFunds").sendToAudience(player);
			return false;
		}
		
		int numberOfShops = plugin.getShopmanager().getNumberOfShops(player.getUniqueId());
		int buildPermissionNumber = getShopBuildLimit(player);
		if(numberOfShops >= buildPermissionNumber){
			plugin.getLangManager().request("permission.error.buildLimit").sendToAudience(player);
			return false;
		}
		return true;
	}
	
	/**
	 * Verifies if the blocks are still valid to create a shop
	 */
	public boolean verifyBlocks() {
		//container has been broken or otherwise no longer a valid container, sign is gone or changed orientation
		return shopManager.isAllowedContainer(container) &&
		       sign.getBlock() instanceof WallSign wallSign &&
		       wallSign.getFacing().equals(signDirection);
	}
	
	/**
	 * @return If the shop can be created in this world
	 */
	protected boolean isAllowedInDimension() {
		return !Shop.getPlugin().getSettingsConfig().getWorldBlackList().contains(container.getWorld().getName());
	}
	
	/**
	 * Creates a new abstract shop from all the data provided, this does not register the shop use {@link com.wonkglorg.minecraft.shop.manager.ShopManager#registerShop(AbstractShop)} to register and save the shop into the database
	 */
	public AbstractShop createShop() {
		
		final AbstractShop shop = AbstractShop.create(shopId,
				sign.getLocation(),
				player.getUniqueId(),
				price,
				priceCombo,
				amount,
				adminShop,
				type,
				signDirection,
				System.currentTimeMillis());
		shop.setFakeSign(isFakeSign);
		boolean loaded = shop.load();
		if(!loaded){
			Shop.getPlugin()
			    .getLogger()
			    .warning("Shop creation failed, unable to load the shop. Aborting shop creation."); // only seen this happen in tests
			return null;
		}
		
		if(type == ShopType.GAMBLE){
			shop.setItemStack(Shop.getPlugin().getItemConfig().getGambleDisplayItem());
			shop.setAmount(1);
			shop.getDisplay().setType(DisplayType.LARGE_ITEM, false);
			return null;
		}
		
		Shop.getPlugin().logger().trace("[ShopCreationUtil.createShop] updateSign");
		shop.updateSign();
		return shop;
	}
	
	protected boolean isAllowedToCreateShop() {
		if(!PlayerProfile.isAllowedToCreateShop(player, type)){
			lang.request("permission.error.create").replace("%shop-type%", type).sendToAudience(player);
			return false;
		}
		return true;
	}
}
