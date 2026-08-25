package com.wonkglorg.minecraft.shop.util;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import static com.wonkglorg.minecraft.shop.manager.player.PlayerProfile.getTeleportCooldownRemaining;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import static com.wonkglorg.minecraft.shop.shop.AbstractShop.formatPrice;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.util.Components;
import static com.wonkglorg.minecraft.util.Components.toComponent;
import com.wonkglorg.minecraft.util.date.DateType;
import com.wonkglorg.minecraft.util.date.DurationBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ShopMessage{
	
	private static final Main plugin = Main.getPlugin();
	
	private static final LangManager lang = plugin.getLangManager();
	
	@Getter
	private static int targetMaxLength;
	
	public ShopMessage() {
		targetMaxLength = lang.getDefaultLang().getInt("targetMaxLength", 40);
	}
	
	/**
	 * Formats a message by replacing all placeholders with their respective values.
	 *
	 * @param messageKey The message containing placeholders
	 * @param context The PlaceholderContext instance containing Shop and Player
	 * @return The formatted message with all placeholders replaced
	 */
	public static String formatPlainTextSingle(String messageKey, PlaceholderContext context) {
		return Components.toPlainText(request(messageKey, context).toSingleComponent());
	}
	
	/**
	 * Formats a message by replacing all placeholders with their respective values.
	 *
	 * @param messageKey The message containing placeholders
	 * @param context The PlaceholderContext instance containing Shop and Player
	 * @return The formatted message with all placeholders replaced
	 */
	public static List<String> formatPlainText(String messageKey, PlaceholderContext context) {
		List<String> results = new ArrayList<>();
		
		for(var component : request(messageKey, context).toComponent()){
			results.add(Components.toPlainText(component));
		}
		return results;
	}
	
	/**
	 * Resolves and fills the langreqwuest with it's placeholder context values.
	 *
	 * @param messageKey the key to look for
	 * @param context the context to use to fill it
	 * @return the request.
	 */
	public static LangRequest request(String messageKey, PlaceholderContext context) {
		if(messageKey == null){
			return LangRequest.literal("null-value");
		}
		
		LangRequest request = Main.getPlugin().getLangManager().request(messageKey);
		fillRequest(request, context);
		return request;
	}
	
	/**
	 * Resolves and fills the langreqwuest with it's placeholder context values.
	 *
	 * @param messageKey the key to look for
	 * @param shop the shop to use to fill it
	 * @return the request.
	 */
	public static LangRequest request(String messageKey, AbstractShop shop) {
		return request(messageKey, PlaceholderContext.of(shop));
	}
	
	/**
	 * Resolves and fills the langreqwuest with it's placeholder context values.
	 *
	 * @param messageKey the key to look for
	 * @param shop the shop to use to fill it
	 * @return the request.
	 */
	public static LangRequest request(String messageKey, Player player, AbstractShop shop) {
		return request(messageKey, PlaceholderContext.of(shop).setPlayer(player));
	}
	
	/**
	 * Loads all available placeholders into the map.
	 * This method should be called during the plugin's initialization phase.
	 */
	public static void fillRequest(LangRequest request, @NotNull PlaceholderContext context) {
		//@formatter:off
	Main plugin = Main.getPlugin();
	request.replace("%player%", context.getPlayer() != null ? context.getPlayer().getName() : "");
	
	if(context.getShop() != null){
		AbstractShop.shopPlaceholders(request,context.getShop(),false);
	}
	
	
	request
		   .lazyReplace("%user%", ()-> {
					if(context.getPlayer() != null){
						return context.getPlayer().getName();
					}
					if(context.getOfflinePlayer() != null){
						return context.getOfflinePlayer().getName();
					}
					return"Unknown Player";
		   })
		   .lazyReplace("%shop type%",()-> {
			   if(context.getProcess() != null && context.getProcess().getType() != null){
				   return context.getProcess().getType().toString();
			   }
			   if(context.getShop() != null){
				   return Main.getPlugin().getSettingsConfig().getCreationWord(context.getShop().getType().getCreationWord());
			   }
			   return null;
		   })
		   .replace("%shop types%", ShopMessage.getShopTypesPlaceholder(context))
		   .replace("%total shops%",String.valueOf(plugin.getShopmanager().getNumberOfShops()))
		   .replace("%owner%",()-> {
			   if(context.getProcess() != null){
				   return Component.text(Bukkit.getOfflinePlayer(context.getProcess().getPlayerUUID()).getName());
			   } else if(context.getShop() != null){
				   return context.getShop().isAdmin() ? lang.request("placeholders.server-display-name").toSingleComponent() : context.getShop().getOwnerName();
			   }
			   return null;
		   })
		   .lazyReplace("%user amount%",()-> {
			   if(context.getPlayer() != null){
				   return String.valueOf(plugin.getShopmanager().getNumberOfShops(context.getPlayer().getUniqueId()));
			   } else if(context.getShop().getOwner() != null){
				   return String.valueOf(plugin.getShopmanager().getNumberOfShops(context.getShop().getOwner().getUniqueId()));
			   }
			   return "0";
		   })
		   .lazyReplace("%build limit%",()-> String.valueOf(PlayerProfile.getShopBuildLimit(context.getPlayer())))
		   .replace("%tp time remaining%",DurationBuilder.create(getTeleportCooldownRemaining(context.getPlayer().getUniqueId())).noDecimals().typesToShow(DateType.SECOND).toTimeString())
		   .replace("%currency name%",plugin.getSettingsConfig().getCurrencyName())
		   .replace("%price per item%", context.getShop() != null ? context.getShop().getPricePerItemString() : null)
		   .replace("%offline transactions%",()->{
			   Component numOfTransactions = Component.text(String.valueOf(context.getOfflineTransactions().getNumTransactions()));
			   return numOfTransactions.hoverEvent(showText(getTransactionsHoverEvent(context)));
		   })
		   .lazyReplace("%offline profit%",()->{
			   String boughtString = plugin.getPriceString(context.getOfflineTransactions().getTotalProfit(), false);
			   if(boughtString.equals("free")){
				   boughtString = "0";
			   }
			   return boughtString;
		   })
		   .lazyReplace("%offline spent%",()->{
			   String spentString = plugin.getPriceString(context.getOfflineTransactions().getTotalSpent(), false);
			   if(spentString.equals("free")){
				   spentString = "0";
			   }
			   return spentString;
		   })
		   .replace("%offline items sold%",()->ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsSold()))
		   .replace("%offline items bought%",()->ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsBought()))
		   .replace("%shops out of stock%",()-> ShopMessage.getShopsOutOfStockPlaceholder(context));
		//@formatter:on
	}
	
	private static Component getTransactionsHoverEvent(PlaceholderContext context) {
		return toComponent(context.getOfflineTransactions().getTransactionsLore());
	}
	
	/**
	 * Helper method to handle the %shop types% placeholder.
	 *
	 * @param context The PlaceholderContext instance.
	 * @return A comma-separated list of shop types the player can create.
	 */
	private static String getShopTypesPlaceholder(PlaceholderContext context) {
		List<ShopType> typeList = new ArrayList<>(Arrays.asList(ShopType.values()));
		Player player = context.getPlayer();
		
		if((!PlayerProfile.isOperator(player))){
			typeList.remove(ShopType.GAMBLE);
		}
		
		Iterator<ShopType> typeIterator = typeList.iterator();
		while(typeIterator.hasNext()){
			ShopType type = typeIterator.next();
			if(!player.hasPermission("shop.operator") && !player.hasPermission("shop.create." + type.toString()) && !player.hasPermission(
					"shop.create")){
				typeIterator.remove();
			}
		}
		
		StringBuilder types = new StringBuilder();
		for(int i = 0; i < typeList.size(); i++){
			types.append(typeList.get(i).toCreationWord());
			if(i < typeList.size() - 1){
				types.append(", ");
			}
		}
		return types.toString();
	}
	
	private static Component getOfflineItemsPlaceholder(PlaceholderContext context, Map<ItemStack, Integer> items) {
		Component itemRowsText = Component.text("");
		String itemRow = formatPlainTextSingle("offline.itemRow", context);
		
		int i = 0;
		for(Map.Entry<ItemStack, Integer> entry : items.entrySet()){
			i++;
			// Add a new line character between our rows, don't add it if we are the last item (since we don't want an extra line!
			String addNewLine = i < (items.size()) ? "\n" : "";
			ItemStack item = entry.getKey();
			item.setAmount(entry.getValue());
			
			PlaceholderContext itemContext = new PlaceholderContext();
			itemContext.setPlayer(context.getPlayer());
			itemContext.setItem(item);
			
			Component currentRow = ShopMessage.request(itemRow + addNewLine, itemContext).toSingleComponent();
			itemRowsText = itemRowsText.append(currentRow);
		}
		// If there were no lines added, just return null so that we don't log a blank line!
		if(i == 0){
			return null;
		}
		
		return itemRowsText;
	}
	
	private static Component getShopsOutOfStockPlaceholder(PlaceholderContext context) {
		Component shopsOutOfStock = Component.text("");
		List<AbstractShop> playerShops = Main.getPlugin().getShopmanager().getShops(context.getPlayer().getUniqueId());
		if(playerShops != null && !playerShops.isEmpty()){
			// Collect all the out of stock shops
			List<AbstractShop> outOfStock = new ArrayList<>();
			for(AbstractShop shop : playerShops){
				if(shop.getStock() == 0){
					outOfStock.add(shop);
				}
			}
			// No out of stock shops!
			if(outOfStock.isEmpty()){
				return null;
			}
			
			// Add the lines for each
			int i = 0;
			List<Component> remainingShopsMsgs = new ArrayList<>();
			for(AbstractShop shop : outOfStock){
				i++;
				
				PlaceholderContext shopContext = new PlaceholderContext();
				shopContext.setPlayer(context.getPlayer());
				shopContext.setShop(shop);
				
				// For each item, generate a line based on the template line
				String addNewLine = (i < (outOfStock.size()) && i <= 3) ? "\n" : "";
				Component currentRow = request("offline.outOfStockShop." + addNewLine, shopContext).toSingleComponent();
				// Limit out of stock shops to 3
				if(i > 3){
					remainingShopsMsgs.add(currentRow);
				} else {
					shopsOutOfStock = shopsOutOfStock.append(currentRow);
				}
			}
			//todo:jmd
			/*
			if(!remainingShopsMsgs.isEmpty()){
				lang.request("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.moreOutOfStock").replace("%out-of-stock-remaining%",remainingShopsMsgs)
				String remainingMsg = getUnformattedMessage("offline", "moreOutOfStock");
				TextComponent remaining = format(remainingMsg.replace("%out of stock remaining%", "" + remainingShopsMsgs.size()), context);
				remaining.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, Component.text(String.join("\n", remainingShopsMsgs)).create()));
				shopsOutOfStock = shopsOutOfStock.append(remaining);
			}
			 */
			
			return shopsOutOfStock;
		}
		// No shops for player! don't add anything! p.s. should never get here.
		return null;
	}
	
	// Perform partial formatting to insert transaction purchase amounts since they might differ from shop amounts (partial sales)
	public static String getMessageFromOrders(ShopType transactionType, String subKey, double price, int amount) {
		//todo
		String message = "";//ShopMessage.getUnformattedMessage(transactionType.toString(), subKey);
		String priceStr = Main.getPlugin().getPriceString(price, false);
		message = message.replace("%price%", priceStr);
		message = message.replace("%item amount%", "" + amount);
		if(transactionType == ShopType.BARTER){
			message = message.replace("%barter item amount%", "" + (int) price);
		}
		return message;
	}
	
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
		
		return getSignLines(shop.getType().name().toUpperCase() + "." + shopFormat, shop);
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
			
			request.replace("%item%",shop.getItemStack()::displayName)
					.replace("%amount%",shop.getAmount())
					.replace("%price%",shop.getPriceFormatted())
					.replace("%owner%",shop::getOwnerName)
					.replace("%stock%",shop.getStock());
			
			ItemStack barterStack = shop.getSecondaryItemStack();
			if(barterStack!=null){
				request.replace("%barter-item%",barterStack::displayName);
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
			LangRequest request = lang.request("sign.text." + context.getType().toString().toUpperCase() + ".initialise." + i);
			
			if(context.getItemStack() != null){
				request.replace("%item%",context.getItemStack()::displayName);
			}else{
				request.replace("%item%","");
			}
			
			if(context.getSecondaryStack() != null){
				request.replace("%barter-item%",context.getSecondaryStack()::displayName);
			}else{
				request.replace("%barter-item%","");
			}
			
			request.replace("%amount%",context.getAmount())
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