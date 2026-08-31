package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import static com.wonkglorg.minecraft.shop.shop.AbstractShop.formatPrice;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles player-specific sign updating and changes.
 */
public class SignUpdateHandler implements ShopVisibilityListener{
	private static final LangManager lang = Main.getPlugin().getLangManager();
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		updateSign(player, shop);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		//nothing needs changing
	}
	
	@Override
	public void onShopRefresh(Player player, AbstractShop shop) {
		updateSign(player, shop);
	}
	
	private void updateSign(Player player, AbstractShop shop) {
		if(!player.isOnline()){
			return;
		}
		
		Location location = shop.getSignLocation();
		Main.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(location, () -> {
			List<Component> lines = getSignLines(player, shop);
			if(!player.isOnline()){
				return;
			}
			WallSign realSign = shop.getSign();
			
			if(realSign == null){
				return;
			}
			
			// Create a virtual TileState from the actual block's data.
			if(!(realSign.createBlockState() instanceof Sign sign)){
				return;
			}
			
			SignSide front = sign.getSide(Side.FRONT);
			
			for(int i = 0; i < 4; i++){
				front.line(i, lines.get(i));
			}
			
			front.setGlowingText(Main.getPlugin().getSettingsConfig().isSignGlowingSignText());
			sign.setWaxed(Main.getPlugin().getSettingsConfig().isSignGlowingSignText());
			
			player.sendBlockUpdate(location, sign);
		}, 1);
	}
	
	public void refreshShop(AbstractShop shop) {
		for(UUID playerId : getPlayersSeeingShop(shop)){
			
			Player player = Bukkit.getPlayer(playerId);
			
			if(player != null && player.isOnline()){
				updateSign(player, shop);
			}
		}
	}
	
	//      # %amount% : The amount of items the shop is selling/buying/bartering #
	//      # %price% : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
	//      # %owner% : The name of the shop owner #
	public static List<Component> getSignLines(Player player, AbstractShop shop) {
		String langKey = "sign.text." + shop.getType().toString().toLowerCase() + ".";
		langKey += switch(shop.getClientShopState(player)) {
			case OK -> "in-stock";
			case OVERFILLED -> "overfilled";
			case ON_COOLDOWN -> "transaction-cooldown";
			case LIMIT_REACHED -> "transaction-limit";
			case EMPTY -> "out-of-stock";
		};
		
		DisplayType displayType = shop.getDisplay().getType();
		
		if(displayType == DisplayType.NONE){
			langKey += "-no-display";
		}
		
		return getComponents(shop, langKey);
	}
	
	private static @NonNull List<Component> getComponents(AbstractShop shop, String langKey) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = lang.request(langKey + "." + i);
			
			request.replace("%item%",() -> ItemNameUtil.getName(shop.getItemStack()))
				   .replace("%stock-state%",shop.getShopState())
				   .replace("%amount%",shop.getAmount())
				   .replace("%price%",shop.getPriceFormatted())
				   .replace("%owner%",shop::getOwnerNameFormatted)
				   .replace("%stock%",shop.getStock());
			
			ItemStack barterStack = shop.getSecondaryItemStack();
			if(barterStack!=null){
				request.replace("%barter-item%",() -> ItemNameUtil.getName(shop.getSecondaryItemStack()));
			}
			lines.add(request.toSingleComponent());
			//@formatter:on
		}
		return lines;
	}
	
	/**
	 * Gets the initialize context sign lines
	 */
	public static List<Component> getSignLines(ShopCreationProcess context) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = lang.request("sign.text." + context.getType() + ".initialise." + i);
			
			if(context.getItemStack() != null){
				request.replace("%item%",() -> ItemNameUtil.getName(context.getItemStack()));
			}else{
				request.replace("%item%","");
			}
			
			if(context.getSecondaryStack() != null){
				request.replace("%barter-item%",()->ItemNameUtil.getName(context.getSecondaryStack()));
			}else{
				request.replace("%barter-item%","");
			}
			
			request.replace("%amount%",context.getAmount())
				   .replace("%stock-state%",OK)
				   .replace("%price%",formatPrice(context.getPrice()))
				   .replace("%owner%",context.getPlayer().getName())
				   .replace("%stock%",0);
			lines.add(request.toSingleComponent());
			//@formatter:on
		}
		return lines;
	}
	
	/**
	 * The shop lines defined in the lang config
	 *
	 * @return a list with a capacity of 4
	 */
	public static List<Component> getSignLinesTimeout() {
		List<Component> lines = new ArrayList<>(4);
		
		for(var i = 1; i < 5; i++){
			lines.add(lang.request("sign.text.timeout." + i).toSingleComponent());
		}
		return lines;
	}
	
	/**
	 * @return a default set of sign text to assign when a shop loads so the shop sign looks similar to what the clients state would be in case something loads slow.
	 */
	public static List<Component> getDefaultSignLines(AbstractShop shop) {
		return getComponents(shop, "sign.text." + shop.getType().toString() + ".in-stock");
	}
}