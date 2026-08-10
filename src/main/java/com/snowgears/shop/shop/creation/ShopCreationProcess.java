package com.snowgears.shop.shop.creation;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.display.CreationDisplay;
import com.snowgears.shop.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.manager.ShopManager;
import com.snowgears.shop.manager.player.PlayerProfile;
import static com.snowgears.shop.manager.player.PlayerProfile.getShopBuildLimit;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.ShopActionType;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.wonkglorg.minecraft.config.LangManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Light;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopCreationProcess{
	protected final LangManager lang;
	protected final ShopManager shopManager;
	@Getter
	protected final Player player;
	protected final boolean playerIsOperator;
	@Getter
	protected final UUID playerUUID;
	@Getter
	protected Sign sign;
	@Getter
	protected Block container;
	protected BlockFace signDirection;
	
	protected ShopType type = ShopType.SELL;
	protected int amount = 0;
	protected double price = 0;
	protected double priceCombo = 0;
	protected boolean adminShop = false;
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
	protected CreationDisplay display;
	
	protected ShopCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		this.player = player;
		this.playerUUID = player.getUniqueId();
		this.sign = sign;
		this.container = container;
		this.signDirection = signDirection;
		this.playerIsOperator = PlayerProfile.isOperator(player);
		lang = Shop.getPlugin().getLangManager();
		shopManager = Shop.getPlugin().getShopmanager();
	}
	
	/**
	 * Verifies to see if a player has all things needed to create a potential shop <br>!!NOTE!!<br> this does not replace the other checks needed to preverify if a shop is even valid to be placed there or if a shop is already being created by the user. refer to {@link com.snowgears.shop.listener.ShopListener#onShopCreation(SignChangeEvent)}
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
	 * Creates a new abstract shop from all the data provided, this does not register the shop use {@link com.snowgears.shop.manager.ShopManager#registerShop(AbstractShop)} to register and save the shop into the database
	 */
	public AbstractShop createShop() {
		
		final AbstractShop shop = AbstractShop.create(sign.getLocation(),
				player.getUniqueId(),
				price,
				priceCombo,
				amount,
				adminShop,
				type,
				signDirection,
				System.currentTimeMillis());
		shop.setFakeSign(isFakeSign);
		
		//removed all the direction checking code. just make sure its a container
		//make sure that the sign is in front of the chest, unless it is a shulker box
		if(chestBlock.getState() instanceof Container){
			existingShop = plugin.getShopmanager().getShopByContainer(chestBlock);
			if(existingShop != null){
				//if the block they are adding a sign to is already a shop, do not let them
				if(chestBlock.getLocation().equals(existingShop.getContainerLocation())){
					ShopMessage.request("interactionIssue.createOtherPlayer", player, shop).sendToAudience(player);
					return null;
				}
			}
			
			if(!(signBlock.getBlockData() instanceof WallSign)){
				if(!signBlock.getType().toString().contains("_SIGN")){
					return null;
				}
				String wallSignString = signBlock.getType().toString().replace("_SIGN", "_WALL_SIGN");
				signBlock.setType(Material.valueOf(wallSignString));
				
				Directional wallSignData = (Directional) signBlock.getBlockData();
				wallSignData.setFacing(signDirection);
				signBlock.setBlockData(wallSignData);
			}
			Sign signBlockState = (Sign) signBlock.getState();
			signBlockState.update();
			
			shop.setAdmin(isAdmin);
			boolean loaded = shop.load();
			if(!loaded){
				plugin.getLogger()
				      .warning("Shop creation failed, unable to load the shop. Aborting shop creation."); // only seen this happen in tests
				return null;
			}
			
			PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
			plugin.getServer().getPluginManager().callEvent(e);
			
			plugin.getShopmanager().getDatabase().logAction(player, shop, ShopActionType.CREATE);
			
			if(e.isCancelled()){
				return null;
			}
			
			if(plugin.getSettingsConfig().getDisplayLightLevel() > 0){
				Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
				if(UtilMethods.materialIsNonIntrusive(displayBlock.getType())){
					displayBlock.setType(Material.LIGHT);
					Light data = (Light) displayBlock.getBlockData();
					data.setLevel(plugin.getSettingsConfig().getDisplayLightLevel());
					displayBlock.setBlockData(data);
				}
			}
			
			if(type == ShopType.GAMBLE){
				shop.setItemStack(plugin.getItemConfig().getGambleDisplayItem());
				shop.setAmount(1);
				shop.getDisplay().setType(DisplayType.LARGE_ITEM, false);
				
				plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
				plugin.getShopmanager().registerShop(shop);
				return null;
			}
			
			Shop.getPlugin().logger().trace("[ShopCreationUtil.createShop] updateSign");
			shop.updateSign();
		}
		return shop;
		
	}
	
	protected boolean isAllowedToCreateShop() {
		if(!PlayerProfile.isAllowedToCreateShop(player, type)){
			lang.request("permission.error.create").replace("%shop-type%", type).sendToAudience(player);
			return false;
		}
	}
	
}
