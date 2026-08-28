package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import static com.wonkglorg.minecraft.shop.shop.AbstractShop.formatPrice;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopSignUtil{
	
	private static final Main plugin = Main.getPlugin();
	
	private static final LangManager lang = plugin.getLangManager();
	
	//      # %amount% : The amount of items the shop is selling/buying/bartering #
	//      # %price% : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
	//      # %owner% : The name of the shop owner #
	public static List<Component> getSignLines(AbstractShop shop) {
		
		DisplayType displayType = shop.getDisplay().getType();
		
		String shopFormat;
		if(shop.isAdmin()){
			shopFormat = "admin";
		} else {
			shopFormat = "normal";
		}
		
		if(displayType == DisplayType.NONE){
			shopFormat += "_no_display";
		}
		
		return getSignLines(shop.getType() + "." + shopFormat, shop);
	}
	
	/**
	 * The shop lines defined in the lang config
	 *
	 * @param key the key to search in the config for starts at "sign.text."
	 * @param shop the shop this sign belongs to
	 * @return a list with a capacity of 4
	 */
	public static List<Component> getSignLines(String key, AbstractShop shop) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = lang.request("sign.text." + key + "." + i);
			
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
			lines.add(lang.request("sign.text.TIMEOUT." + i).toSingleComponent());
		}
		return lines;
	}
}