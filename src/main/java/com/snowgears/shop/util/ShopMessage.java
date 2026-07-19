package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.handler.ShopGuiHandler;
import static com.snowgears.shop.handler.ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import static com.snowgears.shop.util.ItemNameUtil.getName;
import static com.snowgears.shop.util.ItemNameUtil.getNameTranslatable;
import static com.snowgears.shop.util.PlayerSettings.Option.NOTIFICATION_SALE_OWNER;
import static com.snowgears.shop.util.PlayerSettings.Option.NOTIFICATION_SALE_USER;
import static com.snowgears.shop.util.PlayerSettings.Option.NOTIFICATION_STOCK;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.util.Components;
import static com.wonkglorg.minecraft.util.Components.toComponent;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import static net.kyori.adventure.text.event.HoverEvent.Action;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import net.kyori.adventure.text.event.HoverEventSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShopMessage{
	
	private static final Shop plugin = Shop.getPlugin();
	
	private static final boolean disableItemHover = false;
	
	private static HashMap<String, String> messageMap = new HashMap<String, String>();
	private static HashMap<String, String[]> shopSignTextMap = new HashMap<String, String[]>();
	private static HashMap<String, List<String>> displayTextMap = new HashMap<String, List<String>>();
	@Getter
	private static String freePriceWord;
	@Getter
	private static String adminStockWord;
	@Getter
	private static String serverDisplayName;
	private static HashMap<String, String> creationWords = new HashMap<String, String>();
	private static YamlConfiguration signConfig;
	private static YamlConfiguration displayConfig;
	private static int targetMaxLength;
	
	public ShopMessage(Shop plugin) {
		File signConfigFile = new File(plugin.getDataFolder(), "signConfig.yml");
		signConfig = YamlConfiguration.loadConfiguration(signConfigFile);
		File displayConfigFile = new File(plugin.getDataFolder(), "displayConfig.yml");
		displayConfig = YamlConfiguration.loadConfiguration(displayConfigFile);
		
		loadMessagesFromConfig();
		loadSignTextFromConfig();
		loadDisplayTextFromConfig();
		loadCreationWords();
		
		freePriceWord = signConfig.getString("sign_text.zeroPrice");
		adminStockWord = signConfig.getString("sign_text.adminStock");
		serverDisplayName = signConfig.getString("sign_text.serverDisplayName");
		targetMaxLength = displayConfig.getInt("targetMaxLength", 40);
	}
	
	/**
	 * Formats a message by replacing all placeholders with their respective values.
	 *
	 * @param message The message containing placeholders
	 * @param context The PlaceholderContext instance containing Shop and Player
	 * @return The formatted message with all placeholders replaced
	 */
	public static Component format(String message, PlaceholderContext context) {
		if(message == null){
			return Component.text("");
		}
		
		LangRequest request = LangRequest.literal(message);
		fillRequest(request, context);
		return request.toSingleComponent();
	}
	
	/**
	 * Formats a message by replacing all placeholders with their respective values.
	 *
	 * @param message The message containing placeholders
	 * @param context The PlaceholderContext instance containing Shop and Player
	 * @return The formatted message with all placeholders replaced
	 */
	public static String formatPlainText(String message, PlaceholderContext context) {
		if(message == null){
			return "";
		}
		
		LangRequest request = LangRequest.literal(message);
		fillRequest(request, context);
		return Components.toPlainText(request.toSingleComponent());
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player, PlaceholderContext context) {
		Component fancyMessage = format(message, context);
		plugin.logger().debug("Sent msg to player " + player.getName() + ": " + Components.toPlainText(fancyMessage), true);
		player.sendMessage(fancyMessage);
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		sendMessage(message, player, context);
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player, ItemStack item) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setItem(item);
		sendMessage(message, player, context);
	}
	
	/**
	 * Loads message, swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String key, String subkey, Player player, AbstractShop shop) {
		String message = getUnformattedMessage(key, subkey);
		if(message != null && !message.isEmpty()){
			sendMessage(message, player, shop);
		}
	}
	
	/**
	 * Loads message, swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String key, String subkey, ShopCreationProcess process, Player player) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setProcess(process);
		String message = getUnformattedMessage(key, subkey);
		if(message != null && !message.isEmpty()){
			sendMessage(message, player, context);
		}
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player, AbstractShop shop) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setShop(shop);
		sendMessage(message, player, context);
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player, Player user, AbstractShop shop) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(user);
		context.setShop(shop);
		sendMessage(message, player, context);
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, ShopCreationProcess process, Player player) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setProcess(process);
		sendMessage(message, player, context);
	}
	
	/**
	 * Swaps in placeholder values, sends fancy message with Click/Hover events to Player
	 */
	public static void sendMessage(String message, Player player, OfflineTransactions offlineTxs) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setOfflineTransactions(offlineTxs);
		sendMessage(message, player, context);
	}
	
	/**
	 * Loads all available placeholders into the map.
	 * This method should be called during the plugin's initialization phase.
	 */
	public static void fillRequest(LangRequest request, PlaceholderContext context) {
		//@formatter:off
	Shop plugin = Shop.getPlugin();
	request.replace("[plugin]", plugin.getCommandAlias())
		   .replace("[server name]", ShopMessage.getServerDisplayName())
		   .replace("[player]", context.getPlayer() != null ? context.getPlayer().getName() : "")
		   .lazyReplace("[user]", ()-> {
					if(context.getPlayer() != null){
						return context.getPlayer().getName();
					}
					if(context.getOfflinePlayer() != null){
						return context.getOfflinePlayer().getName();
					}
					return"Unknown Player";
		   })
		   .lazyReplace("[shop type]",()-> {
			   if(context.getProcess() != null && context.getProcess().getShopType() != null){
				   return context.getProcess().getShopType().toString();
			   }
			   if(context.getShop() != null){
				   return ShopMessage.getCreationWord(context.getShop().getType().toString().toUpperCase());
			   }
			   return null;
		   })
		   .replace("[shop types]", ShopMessage.getShopTypesPlaceholder(context))
		   .replace("[total shops]",String.valueOf(plugin.getShopHandler().getNumberOfShops()))
		   .lazyReplace("[owner]",()-> {
			   if(context.getProcess() != null){
				   return String.valueOf(Bukkit.getOfflinePlayer(context.getProcess().getPlayerUUID()));
			   } else if(context.getShop() != null){
				   return context.getShop().isAdmin() ? ShopMessage.getServerDisplayName() : context.getShop().getOwnerName();
			   }
			   return null;
		   })
		   .lazyReplace("[user amount]",()-> {
			   if(context.getPlayer() != null){
				   return String.valueOf(plugin.getShopHandler().getNumberOfShops(context.getPlayer()));
			   } else if(context.getShop().getOwner() != null){
				   return String.valueOf(plugin.getShopHandler().getNumberOfShops(context.getShop().getOwner().getUniqueId()));
			   }
			   return "0";
		   })
		   .replace("[build limit]",plugin.getShopListener().getBuildLimit(context.getPlayer()))
		   .replace("[tp time remaining]",String.valueOf(plugin.getShopListener().getTeleportCooldownRemaining(context.getPlayer())))
		   .lazyReplace("[world]",()-> {
			   if(context.getProcess() != null && context.getProcess().getClickedChest() != null){
				   return context.getProcess().getClickedChest().getWorld().getName();
			   } else if(context.getShop() != null){
				   return context.getShop().getSignLocation().getWorld().getName();
			   }
			   return null;
		   })
		   .replace("[location]",()-> {
			   Location loc = null;
			   if(context.getLocation() != null){
				   loc = context.getLocation();
			   } else if(context.getProcess() != null && context.getProcess().getClickedChest() != null){
				   loc = context.getProcess().getClickedChest().getLocation();
			   } else if(context.getShop() != null){
				   loc = context.getShop().getSignLocation();
			   }
			   if(loc == null){
				   return null;
			   }
			   Component text = Component.text(UtilMethods.getCleanLocation(loc, false));
			   if(context.getProcess() == null && context.getShop() == null){
				   return text;
			   }
			   
			   return text.hoverEvent(getShopInfoHoverEvent(context));
		   })
		   .replace("[currency name]",plugin.getCurrencyName())
		   .replace("[currency item]",()->embedItem(getName(plugin.getItemCurrency()), plugin.getItemCurrency()))
		   .replace("[item]", ()-> ShopMessage.getItemPlaceholder(context))
		   .lazyReplace("[item amount]", ()-> {
			   if(context.getItem() != null){
				   return String.valueOf(context.getItem().getAmount());
			   } else if(context.getProcess() != null){
				   return String.valueOf(context.getProcess().getItemAmount());
			   } else if(context.getShop() != null && context.getShop().getItemStack() != null){
				   return String.valueOf(context.getShop().getItemStack().getAmount());
			   }
			   return null;
		   })
		   .replace("[item enchants]",()-> {
			   if(context.getShop() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getShop().getItemStack()), context.getShop().getItemStack());
			   }
			   if(context.getProcess() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getProcess().getItemStack()), context.getProcess().getItemStack());
			   }
			   if(context.getItem() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getItem()), context.getItem());
			   }
			   return null;
		   })
			.replace("[item lore]",()-> {
				if(context.getShop() != null){
					return embedItem(UtilMethods.getLoreString(context.getShop().getItemStack()), context.getShop().getItemStack());
				}
				if(context.getProcess() != null){
					return embedItem(UtilMethods.getLoreString(context.getProcess().getItemStack()), context.getProcess().getItemStack());
				}
				if(context.getItem() != null){
					return embedItem(UtilMethods.getLoreString(context.getItem()), context.getItem());
				}
				return null;
			})
			.replace("[item durability]", context.getShop() != null ? String.valueOf(context.getShop().getItemDurabilityPercent()): null)
			.lazyReplace("[item type]", ()-> {
				if(context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE){
					return "???";
				} else {
					return Components.toPlainText(getNameTranslatable(context.getShop().getItemStack().getType()));
				}
			})
		   .lazyReplace("[gamble item amount]",()-> {
			   if(context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE){
				   return String.valueOf(context.getShop().getAmount());
			   }
			   return null;
		   })
			.replace("[gamble item]", ()->{
				if(context.getShop() != null && context.getShop().getType() == ShopType.GAMBLE){
					return embedItem(getName(plugin.getGambleDisplayItem()), plugin.getGambleDisplayItem());
				}
				return null;
			})
		   .lazyReplace("[barter item amount]", ()->{
			   if(context.getBarterItem() != null){
				   return String.valueOf(context.getBarterItem().getAmount());
			   }
			   if(context.getShop() != null && context.getShop().getSecondaryItemStack() != null){
				   return String.valueOf(context.getShop().getSecondaryItemStack().getAmount());
			   }
			   if(context.getProcess() != null){
				   return String.valueOf(context.getProcess().getBarterItemAmount());
			   }
			   if(context.getItem() != null){
				   return String.valueOf(context.getItem().getAmount());
			   }
			   return null;
		   })
		   .replace("[barter item]",()-> ShopMessage.getBarterItemPlaceholder(context))
		   .lazyReplace("[barter item durability]",()-> {
			   if(context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null){
				   return String.valueOf(context.getShop().getSecondaryItemDurabilityPercent());
			   }
			   return null;
		   })
		   .replace("[barter item type]",()->{
			   if(context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null){
				   return getNameTranslatable(context.getShop().getSecondaryItemStack().getType());
			   }
			   return null;
		   })
		   .replace("[barter item enchants]",()->{
			   if(context.getBarterItem() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getBarterItem()), context.getBarterItem());
			   }
			   if(context.getShop() != null && context.getShop().getSecondaryItemStack() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getShop().getSecondaryItemStack()),
						   context.getShop().getSecondaryItemStack());
			   }
			   if(context.getProcess() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getProcess().getBarterItemStack()),
						   context.getProcess().getBarterItemStack());
			   }
			   if(context.getItem() != null){
				   return embedItem(UtilMethods.getEnchantmentsComponent(context.getItem()), context.getItem());
			   }
			   return null;
		   })
		   .replace("[barter item lore]",()->{
			   if(context.getBarterItem() != null){
				   return embedItem(UtilMethods.getLoreString(context.getBarterItem()), context.getBarterItem());
			   }
			   if(context.getShop() != null && context.getShop().getType() == ShopType.BARTER && context.getShop().getSecondaryItemStack() != null){
				   return embedItem(UtilMethods.getLoreString(context.getShop().getSecondaryItemStack()), context.getShop().getSecondaryItemStack());
			   }
			   if(context.getProcess() != null){
				   return embedItem(UtilMethods.getLoreString(context.getProcess().getBarterItemStack()), context.getProcess().getBarterItemStack());
			   }
			   if(context.getItem() != null){
				   return embedItem(UtilMethods.getLoreString(context.getItem()), context.getItem());
			   }
			   return null;
		   })
		   .replace("[amount]", context.getShop() != null ? String.valueOf(context.getShop().getAmount()): null)
		   .lazyReplace("[price sell]",()->{
			   if(context.getShop() != null && context.getShop().getType() == ShopType.COMBO){
				   return((ComboShop) context.getShop()).getPriceSellString();
			   }
			   return null;
		   })
		   .lazyReplace("[price sell per item]",()->{
			   if(context.getShop() != null && context.getShop().getType() == ShopType.COMBO){
				   return ((ComboShop) context.getShop()).getPriceSellPerItemString();
			   }
			   return null;
		   })
		   .lazyReplace("[price combo]",()->{
			   if(context.getShop() != null && context.getShop().getType() == ShopType.COMBO){
				   return ((ComboShop) context.getShop()).getPriceComboString();
			   }
			   return null;
		   })
		   .replace("[price per item]", context.getShop() != null ? context.getShop().getPricePerItemString() : null)
		   .replace("[price]", context.getShop() != null ? context.getShop().getPriceString() : null)
		   .lazyReplace("[stock]" ,()->{
			   if(context.getShop() == null){
				   return null;
			   } else if(context.getShop().isAdmin()){
				   return String.valueOf(ShopMessage.getAdminStockWord());
			   } else {
				   return String.valueOf(context.getShop().getStock());
			   }
		   })
		   .replace("[stock color]",()->{
			   if(context.getShop() == null){
				   return null;
			   }
			   if(context.getShop().getStock() < 1){
				   return format(getUnformattedMessage("signtext", "outofstockcolor"), context);
			   }
			   return format(getUnformattedMessage("signtext", "instockcolor"), context);
			})
		   .lazyReplace("[notify user]",()->{
			   String text_on = getUnformattedMessage("command", "notify_on");
			   String text_off = getUnformattedMessage("command", "notify_off");
			   
			   var guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), NOTIFICATION_SALE_USER);
			   return guiIcon == SETTINGS_NOTIFY_USER_ON ? text_on : text_off;
		   })
		   .lazyReplace("[notify owner]",()->{
			   String text_on = getUnformattedMessage("command", "notify_on");
			   String text_off = getUnformattedMessage("command", "notify_off");
			   
			   var guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), NOTIFICATION_SALE_OWNER);
			   return guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON ? text_on : text_off;
		   })
		   .lazyReplace("[notify stock]",()->{
			   String text_on = getUnformattedMessage("command", "notify_on");
			   String text_off = getUnformattedMessage("command", "notify_off");
			   
			   var guiIcon = plugin.getGuiHandler().getIconFromOption(context.getPlayer(), NOTIFICATION_STOCK);
			   return guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON ? text_on : text_off;
		   })
		   .replace("[offline transactions]",()->{
			   Component numOfTransactions = Component.text(String.valueOf(context.getOfflineTransactions().getNumTransactions()));
			   return numOfTransactions.hoverEvent(showText(getTransactionsHoverEvent(context)));
		   })
		   .lazyReplace("[offline profit]",()->{
			   String boughtString = plugin.getPriceString(context.getOfflineTransactions().getTotalProfit(), false);
			   if(boughtString.equals(freePriceWord)){
				   boughtString = "0";
			   }
			   return boughtString;
		   })
		   .lazyReplace("[offline spent]",()->{
			   String spentString = plugin.getPriceString(context.getOfflineTransactions().getTotalSpent(), false);
			   if(spentString.equals(freePriceWord)){
				   spentString = "0";
			   }
			   return spentString;
		   })
		   .replace("[offline items sold]",()->ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsSold()))
		   .replace("[offline items bought]",()->ShopMessage.getOfflineItemsPlaceholder(context, context.getOfflineTransactions().getItemsBought()))
		   .replace("[shops out of stock]",()-> ShopMessage.getShopsOutOfStockPlaceholder(context));
		//@formatter:on
	}
	
	private static net.kyori.adventure.text.event.HoverEvent<?> getItemHoverEvent(ItemStack item) {
		if(item == null || disableItemHover){
			return null;
		}
		
		if(Shop.getPlugin().isMockBukkit()){
			return Component.empty().hoverEvent();
			(Action.SHOW_ITEM, new net.md_5.bungee.api.chat.hover.content.Item(item.getType().getKey().toString(), item.getAmount(), null));
		}
		
		return ItemHoverEventHelper.createFrom(item);
	}
	
	private static Component embedItem(String message, ItemStack item) {
		return embedItem(Component.text(message), item);
	}
	
	private static Component embedItem(Component message, ItemStack item) {
		// If we have any NBT errors, don't try to embed the item hover text
		if(disableItemHover){
			return message;
		}
		try{
			if(item == null){
				return null;
			}
			Component msg = componentFromLegacy(UtilMethods.removeColorsIfOnlyWhite(message.toLegacyText()));
			HoverEvent event = getItemHoverEvent(item);
			if(event != null){
				msg.setHoverEvent(event);
			}
			return (TextComponent) msg;
		} catch(Error | Exception e){
			plugin.logger().severe("Unable to embed item hover text, disabling item hover text for all players! Your version of : " + e.getMessage());
			plugin.logger().debug("Error details: ", e);
			// disableItemHover = true;
			return message;
		}
	}
	
	private static Component getTransactionsHoverEvent(PlaceholderContext context) {
		return toComponent(context.getOfflineTransactions().getTransactionsLore());
	}
	
	private static HoverEventSource<Component> getShopInfoHoverEvent(PlaceholderContext context) {
		try{
			Component hoverText = Component.text("");
			List<String> hoverLines = getUnformattedMessageList("hover", "location");
			int i = 0;
			for(String line : hoverLines){
				i++;
				// Add new lines between text
				hoverText = hoverText.append((format(line + (i == hoverLines.size() ? "" : "\n"), context)));
			}
			return showText(hoverText);
		} catch(Exception _){
		}
		return null;
	}
	
	/**
	 * Helper method to handle the [shop types] placeholder.
	 *
	 * @param context The PlaceholderContext instance.
	 * @return A comma-separated list of shop types the player can create.
	 */
	private static String getShopTypesPlaceholder(PlaceholderContext context) {
		List<ShopType> typeList = new ArrayList<>(Arrays.asList(ShopType.values()));
		Player player = context.getPlayer();
		
		if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))){
			typeList.remove(ShopType.GAMBLE);
		}
		
		if(plugin.usePerms()){
			Iterator<ShopType> typeIterator = typeList.iterator();
			while(typeIterator.hasNext()){
				ShopType type = typeIterator.next();
				if(!player.hasPermission("shop.operator") && !player.hasPermission("shop.create." + type.toString()) && !player.hasPermission(
						"shop.create")){
					typeIterator.remove();
				}
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
	
	/**
	 * Helper method to handle the [item] placeholder with truncation for signs.
	 *
	 * @param context The PlaceholderContext instance.
	 * @return The item name, potentially truncated to fit sign constraints.
	 */
	private static Component getItemPlaceholder(PlaceholderContext context) {
		ItemStack item = null;
		if(context.getItem() != null){
			item = context.getItem();
		} else if(context.getProcess() != null){
			item = context.getProcess().getItemStack();
		} else if(context.getShop() != null || context.getShop().getItemStack() != null){
			item = context.getShop().getItemStack();
		}
		if(item == null){
			return null;
		}
		
		Component itemName = getName(item);
		if(context.isForSign()){
			return Component.text(UtilMethods.trimForSign(Components.toPlainText(itemName)));
		}
		return embedItem(itemName, item);
	}
	
	/**
	 * Helper method to handle the [barter item] placeholder with truncation for signs.
	 *
	 * @param context The PlaceholderContext instance.
	 * @return The barter item name, potentially truncated to fit sign constraints.
	 */
	private static Component getBarterItemPlaceholder(PlaceholderContext context) {
		ItemStack item = null;
		if(context.getBarterItem() != null){
			item = context.getBarterItem();
		} else if(context.getItem() != null){
			item = context.getItem();
		} else if(context.getProcess() != null){
			item = context.getProcess().getBarterItemStack();
		} else if(context.getShop() != null && context.getShop().getSecondaryItemStack() != null){
			if(context.getShop().getType() != ShopType.BARTER){
				return null;
			}
			item = context.getShop().getSecondaryItemStack();
		}
		if(item == null){
			return null;
		}
		
		Component itemName = getName(item);
		if(context.isForSign()){
			return UtilMethods.trimForSign(itemName);
		}
		return embedItem(itemName, item);
	}
	
	private static TextComponent getOfflineItemsPlaceholder(PlaceholderContext context, Map<ItemStack, Integer> items) {
		TextComponent itemRowsText = new TextComponent("");
		String itemRow = getUnformattedMessage("offline", "itemRow");
		
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
			
			TextComponent currentRow = format(itemRow + addNewLine, itemContext);
			itemRowsText.addExtra(currentRow);
		}
		// If there were no lines added, just return null so that we don't log a blank line!
		if(i == 0){
			return null;
		}
		
		return itemRowsText;
	}
	
	private static Component getShopsOutOfStockPlaceholder(PlaceholderContext context) {
		Component shopsOutOfStock = Component.text("");
		List<AbstractShop> playerShops = Shop.getPlugin().getShopHandler().getShops(context.getPlayer().getUniqueId());
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
				Component currentRow = format(getUnformattedMessage("offline", "outOfStockShop") + addNewLine, shopContext);
				// Limit out of stock shops to 3
				if(i > 3){
					remainingShopsMsgs.add(currentRow);
				} else {
					shopsOutOfStock.addExtra(currentRow);
				}
			}
			
			if(!remainingShopsMsgs.isEmpty()){
				String remainingMsg = getUnformattedMessage("offline", "moreOutOfStock");
				TextComponent remaining = format(remainingMsg.replace("[out of stock remaining]", "" + remainingShopsMsgs.size()), context);
				remaining.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(String.join("\n", remainingShopsMsgs)).create()));
				shopsOutOfStock.addExtra(remaining);
			}
			
			return shopsOutOfStock;
		}
		// No shops for player! don't add anything! p.s. should never get here.
		return null;
	}
	
	public static String getCreationWord(String type) {
		return creationWords.get(type);
	}
	
	public static String getUnformattedMessage(String key, String subKey) {
		String message;
		if(subKey != null){
			message = messageMap.get(key + "_" + subKey);
		} else {
			message = messageMap.get(key);
		}
		return message;
	}
	
	public static String formatMessage(String unformattedMessage, AbstractShop shop) {
		PlaceholderContext context = new PlaceholderContext();
		context.setShop(shop);
		Component formattedMessage = format(unformattedMessage, context);
		// Return the legacy version since we are requesting the legacy formatter!
		return ChatColor.translateAlternateColorCodes('§', formattedMessage.toLegacyText());
	}
	
	public static Component formatMessage(String unformattedMessage, AbstractShop shop, Player player, boolean forSign) {
		PlaceholderContext context = new PlaceholderContext();
		context.setPlayer(player);
		context.setShop(shop);
		context.setForSign(forSign);
		// Return the legacy version since we are requesting the legacy formatter!
		return format(unformattedMessage, context);
	}
	
	// Perform partial formatting to insert transaction purchase amounts since they might differ from shop amounts (partial sales)
	public static String getMessageFromOrders(ShopType transactionType, String subKey, double price, int amount) {
		String message = ShopMessage.getUnformattedMessage(transactionType.toString(), subKey);
		String priceStr = Shop.getPlugin().getPriceString(price, false);
		message = message.replace("[price]", priceStr);
		message = message.replace("[item amount]", "" + amount);
		if(transactionType == ShopType.BARTER){
			message = message.replace("[barter item amount]", "" + (int) price);
		}
		return message;
	}
	
	//      # [amount] : The amount of items the shop is selling/buying/bartering #
	//      # [price] : The price of the items the shop is selling (adjusted to match virtual or physical currency) #
	//      # [owner] : The name of the shop owner #
	//      # [server name] : The name of the server #
	public static String[] getSignLines(AbstractShop shop, ShopType shopType) {
		
		DisplayType displayType = null;
		if(shop.getDisplay() != null){
			displayType = shop.getDisplay().getType();
		}
		if(displayType == null){
			displayType = Shop.getPlugin().getDisplayType();
		}
		
		String shopFormat;
		if(shop.isAdmin()){
			shopFormat = "admin";
		} else {
			shopFormat = "normal";
		}
		
		if(displayType == DisplayType.NONE){
			shopFormat += "_no_display";
		}
		
		String[] lines = getUnformattedShopSignLines(shopType, shopFormat);
		
		for(int i = 0; i < lines.length; i++){
			lines[i] = formatMessage(lines[i], shop, null, true);
			lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
			lines[i] = UtilMethods.trimForSign(lines[i]);
		}
		return lines;
	}
	
	public static String[] getSignLines(String key, AbstractShop shop) {
		String[] lines = shopSignTextMap.get(key);
		for(int i = 0; i < lines.length; i++){
			lines[i] = formatMessage(lines[i], shop, null, true);
			lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
			lines[i] = UtilMethods.trimForSign(lines[i]);
		}
		return lines;
	}
	
	public static ArrayList<String> getDisplayTags(AbstractShop shop, ShopType shopType) {
		ArrayList<String> formattedLines = new ArrayList<>();
		List<String> lines = displayTextMap.get(shopType.toString().toUpperCase() + "_normal");
		
		String formattedLine;
		for(String line : lines){
			formattedLine = formatMessage(line, shop, null, false);
			
			Boolean splitLine = formattedLine.contains("[split]");
			formattedLine = formattedLine.replace("[split]", "");
			if(formattedLine != null && !formattedLine.isEmpty() && !ChatColor.stripColor(formattedLine).trim().isEmpty()){
				if(splitLine){
					List<String> splitLines = UtilMethods.splitStringIntoLines(formattedLine, targetMaxLength);
					formattedLines.addAll(splitLines);
				} else {
					formattedLines.add(formattedLine);
				}
			}
		}
		return formattedLines;
	}
	
	public static List<String> getUnformattedMessageList(String key, String subKey) {
		List<String> messages = new ArrayList<>();
		
		int count = 1;
		String message = "-1";
		while(message != null && !message.isEmpty()){
			message = getUnformattedMessage(key, subKey + count);
			if(message != null && !message.isEmpty()){
				messages.add(message);
			}
			count++;
		}
		return messages;
	}
	
	private static String[] getUnformattedShopSignLines(ShopType type, String subtype) {
		return shopSignTextMap.get(type.toString() + "_" + subtype).clone();
	}
	
	private static void loadMessagesFromConfig() {
		
		for(ShopType type : ShopType.values()){
			messageMap.put(type.toString() + "_user", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".user"));
			messageMap.put(type.toString() + "_owner", chatConfig.getString("transaction." + type.toString().toUpperCase() + ".owner"));
			
			messageMap.put(type.toString() + "_initialize", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initialize"));
			if(type == ShopType.BUY || type == ShopType.COMBO){
				messageMap.put(type.toString() + "_initializeAlt",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeAlt"));
			} else if(type == ShopType.BARTER){
				messageMap.put(type.toString() + "_initializeInfo",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeInfo"));
				messageMap.put(type.toString() + "_initializeBarter",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeBarter"));
				messageMap.put(type.toString() + "_createHitChest",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChest"));
				messageMap.put(type.toString() + "_createHitChestBarterAmount",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestBarterAmount"));
				messageMap.put(type.toString() + "_initializeBarterAlt",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".initializeBarterAlt"));
			}
			messageMap.put(type.toString() + "_create", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".create"));
			messageMap.put(type.toString() + "_destroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".destroy"));
			messageMap.put(type.toString() + "_opDestroy", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opDestroy"));
			messageMap.put(type.toString() + "_opOpen", chatConfig.getString("interaction." + type.toString().toUpperCase() + ".opOpen"));
			
			messageMap.put(type.toString() + "_shopNoStock",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoStock"));
			messageMap.put(type.toString() + "_ownerNoStock",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".ownerNoStock"));
			messageMap.put(type.toString() + "_shopNoSpace",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".shopNoSpace"));
			messageMap.put(type.toString() + "_ownerNoSpace",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".ownerNoSpace"));
			messageMap.put(type.toString() + "_playerNoStock",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoStock"));
			messageMap.put(type.toString() + "_playerNoSpace",
					chatConfig.getString("transaction_issue." + type.toString().toUpperCase() + ".playerNoSpace"));
			
			if(type != ShopType.GAMBLE){
				messageMap.put(type.toString() + "_createHitChestAmount",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestAmount"));
			}
			if(type != ShopType.BARTER){
				messageMap.put(type.toString() + "_createHitChestPrice",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestPrice"));
			}
			if(type == ShopType.COMBO){
				messageMap.put(type.toString() + "_createHitChestPriceCombo",
						chatConfig.getString("interaction." + type.toString().toUpperCase() + ".createHitChestPriceCombo"));
			}
			
			int count = 1;
			for(String s : chatConfig.getStringList("description." + type.toString().toUpperCase())){
				messageMap.put(type.toString() + "_description" + count, s);
				count++;
			}
		}
		messageMap.put("initialCreateInstruction", chatConfig.getString("interaction.initialCreateInstruction"));
		messageMap.put("createHitChest", chatConfig.getString("interaction.createHitChest"));
		messageMap.put("adminCreateHitChest", chatConfig.getString("interaction.adminCreateHitChest"));
		
		messageMap.put("permission_use", chatConfig.getString("permission.use"));
		messageMap.put("permission_create", chatConfig.getString("permission.create"));
		messageMap.put("permission_destroy", chatConfig.getString("permission.destroy"));
		messageMap.put("permission_destroyOther", chatConfig.getString("permission.destroyOther"));
		messageMap.put("permission_openOther", chatConfig.getString("permission.openOther"));
		messageMap.put("permission_buildLimit", chatConfig.getString("permission.buildLimit"));
		
		messageMap.put("creativeSelection_disabled", chatConfig.getString("creativeSelection.disabled"));
		
		messageMap.put("interactionIssue_line2", chatConfig.getString("interaction_issue.createLine2"));
		messageMap.put("interactionIssue_line3", chatConfig.getString("interaction_issue.createLine3"));
		messageMap.put("interactionIssue_noItem", chatConfig.getString("interaction_issue.createNoItem"));
		messageMap.put("interactionIssue_direction", chatConfig.getString("interaction_issue.createDirection"));
		messageMap.put("interactionIssue_sameItem", chatConfig.getString("interaction_issue.createSameItem"));
		messageMap.put("interactionIssue_displayRoom", chatConfig.getString("interaction_issue.createDisplayRoom"));
		messageMap.put("interactionIssue_signRoom", chatConfig.getString("interaction_issue.createSignRoom"));
		messageMap.put("interactionIssue_createOtherPlayer", chatConfig.getString("interaction_issue.createOtherShop"));
		messageMap.put("interactionIssue_createInsufficientFunds", chatConfig.getString("interaction_issue.createInsufficientFunds"));
		messageMap.put("interactionIssue_createCooldown", chatConfig.getString("interaction_issue.createCooldown"));
		messageMap.put("interactionIssue_destroyInsufficientFunds", chatConfig.getString("interaction_issue.destroyInsufficientFunds"));
		messageMap.put("interactionIssue_createCancel", chatConfig.getString("interaction_issue.createCancel"));
		messageMap.put("interactionIssue_teleportInsufficientFunds", chatConfig.getString("interaction_issue.teleportInsufficientFunds"));
		messageMap.put("interactionIssue_teleportInsufficientCooldown", chatConfig.getString("interaction_issue.teleportInsufficientCooldown"));
		messageMap.put("interactionIssue_initialize", chatConfig.getString("interaction_issue.initializeOtherShop"));
		messageMap.put("interactionIssue_destroyChest", chatConfig.getString("interaction_issue.destroyChest"));
		messageMap.put("interactionIssue_destroyUninitializedChest", chatConfig.getString("interaction_issue.destroyUninitializedChest"));
		messageMap.put("interactionIssue_useOwnShop", chatConfig.getString("interaction_issue.useOwnShop"));
		messageMap.put("interactionIssue_useShopAlreadyInUse", chatConfig.getString("interaction_issue.useShopAlreadyInUse"));
		messageMap.put("interactionIssue_adminOpen", chatConfig.getString("interaction_issue.adminOpen"));
		messageMap.put("interactionIssue_worldBlacklist", chatConfig.getString("interaction_issue.worldBlacklist"));
		messageMap.put("interactionIssue_regionRestriction", chatConfig.getString("interaction_issue.regionRestriction"));
		messageMap.put("interactionIssue_itemListDeny", chatConfig.getString("interaction_issue.itemListDeny"));
		messageMap.put("interactionIssue_createHitChestTimeout", chatConfig.getString("interaction_issue.createHitChestTimeout"));
		
		int count = 1;
		for(String s : chatConfig.getStringList("hover.location")){
			messageMap.put("hover_location" + count, s);
			count++;
		}
		count = 1;
		for(String s : chatConfig.getStringList("creativeSelection.enter")){
			messageMap.put("creativeSelection_enter" + count, s);
			count++;
		}
		count = 1;
		for(String s : chatConfig.getStringList("creativeSelection.prompt")){
			messageMap.put("creativeSelection_prompt" + count, s);
			count++;
		}
		
		count = 1;
		for(String s : chatConfig.getStringList("guiSearchSelection.enter")){
			messageMap.put("guiSearchSelection_enter" + count, s);
			count++;
		}
		count = 1;
		for(String s : chatConfig.getStringList("guiSearchSelection.prompt")){
			messageMap.put("guiSearchSelection_prompt" + count, s);
			count++;
		}
		
		count = 1;
		for(String s : chatConfig.getStringList("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.summary")){
			messageMap.put("offline_summary" + count, s);
			count++;
		}
		messageMap.put("offline_itemRow", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.itemRow"));
		messageMap.put("offline_outOfStockShop", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.outOfStockShop"));
		messageMap.put("offline_moreOutOfStock", chatConfig.getString("transaction.OFFLINE_TRANSACTIONS_NOTIFICATION.moreOutOfStock"));
		
		messageMap.put("command_list", chatConfig.getString("command.list"));
		messageMap.put("command_list_output_total", chatConfig.getString("command.list_output_total"));
		messageMap.put("command_list_output_perms", chatConfig.getString("command.list_output_perms"));
		messageMap.put("command_list_output_noperms", chatConfig.getString("command.list_output_noperms"));
		messageMap.put("command_currency", chatConfig.getString("command.currency"));
		messageMap.put("command_currency_output", chatConfig.getString("command.currency_output"));
		messageMap.put("command_currency_output_tip", chatConfig.getString("command.currency_output_tip"));
		messageMap.put("command_setcurrency", chatConfig.getString("command.setcurrency"));
		messageMap.put("command_setcurrency_output", chatConfig.getString("command.setcurrency_output"));
		messageMap.put("command_setgamble", chatConfig.getString("command.setgamble"));
		messageMap.put("command_itemrefresh", chatConfig.getString("command.itemrefresh"));
		messageMap.put("command_itemrefresh_output", chatConfig.getString("command.itemrefresh_output"));
		messageMap.put("command_itemlist", chatConfig.getString("command.itemlist"));
		messageMap.put("command_itemlist_add", chatConfig.getString("command.itemlist_add"));
		messageMap.put("command_itemlist_remove", chatConfig.getString("command.itemlist_remove"));
		messageMap.put("command_reload", chatConfig.getString("command.reload"));
		messageMap.put("command_reload_output", chatConfig.getString("command.reload_output"));
		messageMap.put("command_error_novault", chatConfig.getString("command.error_novault"));
		messageMap.put("command_error_nohand", chatConfig.getString("command.error_nohand"));
		messageMap.put("command_not_authorized", chatConfig.getString("command.not_authorized"));
		messageMap.put("command_notify_user", chatConfig.getString("command.notify_user"));
		messageMap.put("command_notify_owner", chatConfig.getString("command.notify_owner"));
		messageMap.put("command_notify_stock", chatConfig.getString("command.notify_stock"));
		messageMap.put("command_notify_on", chatConfig.getString("command.notify_on"));
		messageMap.put("command_notify_off", chatConfig.getString("command.notify_off"));
	}
	
	private String[] getSignConfigLines(String key) {return getConfigLines(signConfig, key);}
	
	private String[] getConfigLines(YamlConfiguration config, String key) {
		List<String> lines = new ArrayList<>();
		int count = 1;
		try{
			String message = config.getString(key + "." + count);
			while(message != null){
				lines.add(message);
				count++;
				message = config.getString(key + "." + count);
			}
		} catch(NullPointerException e){
		}
		return lines.toArray(new String[0]);
	}
	
	private void loadSignTextFromConfig() {
		messageMap.put("signtext_instockcolor", signConfig.getString("stock_color.in_stock"));
		messageMap.put("signtext_outofstockcolor", signConfig.getString("stock_color.out_of_stock"));
		Set<String> allTypes = signConfig.getConfigurationSection("sign_text").getKeys(false);
		for(String typeString : allTypes){
			ShopType type = null;
			try{
				type = ShopType.valueOf(typeString);
			} catch(IllegalArgumentException e){
			}
			
			if(type != null){
				this.shopSignTextMap.put(type.toString() + "_normal", getSignConfigLines("sign_text." + typeString + ".normal"));
				this.shopSignTextMap.put(type.toString() + "_admin", getSignConfigLines("sign_text." + typeString + ".admin"));
				this.shopSignTextMap.put(type.toString() + "_normal_no_display",
						getSignConfigLines("sign_text." + typeString + ".normal_no_display"));
				this.shopSignTextMap.put(type.toString() + "_admin_no_display", getSignConfigLines("sign_text." + typeString + ".admin_no_display"));
			}
		}
		this.shopSignTextMap.put("timeout", getSignConfigLines("sign_text.timeout"));
		this.shopSignTextMap.put("deleted", getSignConfigLines("sign_text.deleted"));
	}
	
	private void loadDisplayTextFromConfig() {
		displayTextMap = new HashMap<>();
		Set<String> allTypes = displayConfig.getConfigurationSection("display_tag_text").getKeys(false);
		for(String typeString : allTypes){
			
			ShopType type = null;
			try{
				type = ShopType.valueOf(typeString);
			} catch(IllegalArgumentException e){
			}
			
			if(type != null){
				try{
					List<String> normalLines = displayConfig.getStringList("display_tag_text." + typeString.toUpperCase() + ".normal");
					this.displayTextMap.put(type.toString().toUpperCase() + "_normal", normalLines);
				} catch(NullPointerException e){
				}
			}
		}
	}
	
	private void loadCreationWords() {
		String shopString = signConfig.getString("sign_creation.SHOP");
		if(shopString != null){
			creationWords.put("SHOP", shopString.toLowerCase());
		} else {
			creationWords.put("SHOP", "[shop]");
		}
		
		String sellString = signConfig.getString("sign_creation.SELL");
		if(sellString != null){
			creationWords.put("SELL", sellString.toLowerCase());
		} else {
			creationWords.put("SELL", "sell");
		}
		
		String buyString = signConfig.getString("sign_creation.BUY");
		if(buyString != null){
			creationWords.put("BUY", buyString.toLowerCase());
		} else {
			creationWords.put("BUY", "buy");
		}
		
		String barterString = signConfig.getString("sign_creation.BARTER");
		if(barterString != null){
			creationWords.put("BARTER", barterString.toLowerCase());
		} else {
			creationWords.put("BARTER", "barter");
		}
		
		String gambleString = signConfig.getString("sign_creation.GAMBLE");
		if(gambleString != null){
			creationWords.put("GAMBLE", gambleString.toLowerCase());
		} else {
			creationWords.put("BARTER", "barter");
		}
		
		String adminString = signConfig.getString("sign_creation.ADMIN");
		if(adminString != null){
			creationWords.put("ADMIN", adminString.toLowerCase());
		} else {
			creationWords.put("ADMIN", "admin");
		}
		
		String comboString = signConfig.getString("sign_creation.COMBO");
		if(comboString != null){
			creationWords.put("COMBO", comboString.toLowerCase());
		} else {
			creationWords.put("COMBO", "combo");
		}
	}
	
	public static int getTargetMaxLength() {
		return targetMaxLength;
	}
}