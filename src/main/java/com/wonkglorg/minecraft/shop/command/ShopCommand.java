package com.wonkglorg.minecraft.shop.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wonkglorg.minecraft.command.AbstractCommand;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_COMMAND;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_PERMISSION_OPERATOR;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_PERMISSION_USER;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopCommand extends AbstractCommand{
	
	private final ShopPlugin plugin;
	private final LangManager lang;
	
	public ShopCommand() {
		this.plugin = ShopPlugin.getPlugin();
		this.lang = ShopPlugin.langManager();
	}
	
	@Override
	public LiteralArgumentBuilder<CommandSourceStack> argumentBuilder() {
		//@formatter:off
		return literal(SHOP_COMMAND)
				.requires(permissions(SHOP_PERMISSION_USER))
				.executes(this::usageMessage)
				.then(literal("list").executes(this::list))
				.then(literal("currency").executes(this::currency))
				.then(literal("notify")
						.executes(this::usageNotify)
						.then(literal("transaction").executes(this::notifyTransaction))
						.then(literal("stock").executes(this::notifyStock))
					 )
				.then(literal("reload").requires(permissions(SHOP_PERMISSION_OPERATOR))
						.then(literal("lang").executes(this::reloadLang))
						.then(literal("config").executes(this::reloadConfig))
						.executes(this::reload))
				.then(literal("debug").requires(permissions(SHOP_PERMISSION_OPERATOR))
					    .executes(this::debugUsage)
					    .then(literal("player").then(argument("player",StringArgumentType.string()).executes(this::debugPlayer)))
					    .then(literal("nearby")
					  		  .executes(this::debugNearby)
					  		  .then(argument("radius",IntegerArgumentType.integer(1, 32)).executes(this::debugNearby)))
					    .then(literal("location").executes(this::debugLocation))
					    .then(literal("stats").executes(this::debugStats))
						.then(literal("shop")
								.then(argument("shop-id",StringArgumentType.greedyString()).executes(this::debugShop))))
				.then(literal("setcurrency").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setCurrency))
				.then(literal("setgamble").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setGamble));
		//@formatter:on
	}
	
	private int debugShop(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(!(sender instanceof Player player)){
			lang.request("command.shop.debug.shop.error-no-console").sendToAudience(sender);
			return -1;
		}
		
		String shopIdString = ctx.getArgument("shop-id", String.class);
		
		UUID shopId;
		try{
			shopId = UUID.fromString(shopIdString);
		} catch(IllegalArgumentException e){
			lang.request("command.shop.debug.shop.not-found").replace("%shop-id%", shopIdString).sendToAudience(sender);
			return -1;
		}
		
		AbstractShop shop = shopManager().getAllShops().get(shopId);
		
		if(shop == null){
			lang.request("command.shop.debug.shop.not-found").replace("%shop-id%", shopIdString).sendToAudience(sender);
			return 1;
		}
		
		LangRequest request = lang.request("command.shop.debug.shop.result");
		
		AbstractShop.shopPlaceholders(request, shop, true, player);
		//@formatter:off
		request
				.replace("%shop-id%", String.valueOf(shop.getId()))
				.replace("%shop-type%", shop.getType())
				.replace("%owner%", shop.getOwnerName())
				.replace("%owner-uuid%", String.valueOf(shop.getOwnerUUID()))
				.replace("%admin%", shop.isAdmin())
				.replace("%loaded%", shop.isLoaded())
				.replace("%chunk-loaded%", shop.isChunkLoaded())
				.replace("%needs-save%", shop.needsSave())
				.replace("%performing-transaction%", shop.isPerformingTransaction())
				.replace("%fake-sign%", shop.isFakeSign())
				.replace("%facing%", shop.getFacing())
				.replace("%price%", shop.getPriceFormatted())
				.replace("%amount%", shop.getAmount())
				.replace("%stock%", shop.getStock())
				.replace("%stock-state%", shop.getShopState())
				.replace("%creation-date%", shop.getCreationDate())
				.replace("%sign-location%", formatLocation(shop.getSignLocation()))
				.replace("%container-location%", formatLocation(shop.getContainerLocation()))
				.replace("%secondary-container-location%",
						shop.getSecondaryContainerLocation() == null
						? "none"
						: formatLocation(shop.getSecondaryContainerLocation()))
				.replace("%item%", formatItem(shop.getItemStack()))
				.replace("%secondary-item%", formatItem(shop.getSecondaryItemStack()))
				.replace("%display%", shop.getDisplay().getType())
				.sendToAudience(player);
		//@formatter:om
		return 1;
	}
	
	private String formatLocation(Location location) {
		if (location == null || location.getWorld() == null) {
			return "null";
		}
		
		return location.getWorld().getName()
			   + ":"
			   + location.getBlockX()
			   + "/"
			   + location.getBlockY()
			   + "/"
			   + location.getBlockZ();
	}
	
	private String formatItem(ItemStack item) {
		if (item == null) {
			return "none";
		}
		
		return item.getType() + " x" + item.getAmount();
	}
	
	private int reloadConfig(CommandContext<CommandSourceStack> ctx) {
		ShopPlugin.getPlugin().getSettingsConfig().silentLoad();
		ShopPlugin.getPlugin().getItemConfig().silentLoad();
		lang.request("command.shop.reload.success").sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int reloadLang(CommandContext<CommandSourceStack> ctx) {
		lang.silentLoad();
		lang.request("command.shop.reload.success").sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int usageNotify(CommandContext<CommandSourceStack> ctx) {
		lang.request("command.shop.notify.usage").sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int notifyTransaction(CommandContext<CommandSourceStack> ctx) {
		
		if(!(ctx.getSource().getSender() instanceof Player player)){
			return -1;
		}
		
		String state = PlayerManager.getOnlineProfile(player).toggleNotifyTransaction() ? "<green>On" : "<red>Off";
		lang.request("command.shop.notify.transaction.success").replace("%notify-state%", state).sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int notifyStock(CommandContext<CommandSourceStack> ctx) {
		if(!(ctx.getSource().getSender() instanceof Player player)){
			return -1;
		}
		
		String state = PlayerManager.getOnlineProfile(player).toggleNotifyStock() ? "<green>On" : "<red>Off";
		lang.request("command.shop.notify.stock.success").replace("%notify-state%", state).sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int setGamble(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(!(sender instanceof Player player)){
			lang.request("command.shop.set-gamble.error-no-console").sendToAudience(sender);
			return -1;
		}
		
		ItemStack heldItem = player.getInventory().getItemInMainHand().clone();
		if(heldItem.getType() == Material.AIR){
			lang.request("command.shop.set-gamble.error-no-item-in-hand").sendToAudience(sender);
			return 1;
		}
		heldItem.setAmount(1);
		plugin.getItemConfig().setGambleDisplayItem(player.getInventory().getItemInMainHand());
		lang.request("command.shop.set-gamble.success")
			.replace("%held-item%", ItemNameUtil.getName(plugin.getItemConfig().getGambleDisplayItem()))
			.sendToAudience(sender);
		return 0;
	}
	
	private int setCurrency(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(!(sender instanceof Player player)){
			lang.request("command.shop.set-currency.error-no-console").sendToAudience(sender);
			return -1;
		}
		
		if(plugin.getSettingsConfig().getCurrencyType() != CurrencyType.ITEM){
			lang.request("command.shop.set-currency.error-digital-currency").sendToAudience(sender);
			return 1;
		}
		
		ItemStack heldItem = player.getInventory().getItemInMainHand().clone();
		if(heldItem.getType() == Material.AIR){
			lang.request("command.shop.set-currency.error-no-item-in-hand").sendToAudience(sender);
			return 1;
		}
		heldItem.setAmount(1);
		plugin.getItemConfig().setCurrencyItem(heldItem);
		lang.request("command.shop.set-currency.success")
			.replace("%held-item%", () -> ItemNameUtil.getName(plugin.getItemConfig().getCurrencyItem()))
			.sendToAudience(sender);
		return 0;
	}
	
	private int currency(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		lang.request("command.shop.currency.success").sendToAudience(sender);
		if(sender.hasPermission(SHOP_PERMISSION_OPERATOR)){
			lang.request("command.shop.currency.tip").sendToAudience(sender);
		}
		return 0;
	}
	
	private int reload(CommandContext<CommandSourceStack> ctx) {
		plugin.reload();
		lang.request("command.shop.reload.success").sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int usageMessage(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		lang.request("command.shop.usage.user").sendToAudience(sender);
		if(PlayerProfile.isOperator(sender)){
			lang.request("command.shop.usage.admin").sendToAudience(sender);
		}
		return 1;
	}
	
	private int list(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(sender instanceof Player player){
			//@formatter:off
			lang.request("command.shop.list.success-player")
				.replace("%total-shops%", shopManager().getNumberOfShops())
				.replace("%user-amount%", shopManager().getNumberOfShops(player.getUniqueId()))
				.lazyReplace("%build-limit%",() -> String.valueOf(PlayerProfile.getShopBuildLimit(player)))
				.sendToAudience(sender);
		} else {
			lang.request("command.shop.list.success-console").replace("%total-shops%", shopManager().getNumberOfShops()).sendToAudience(sender);
		}
		return 1;
	}
	
	private int debugUsage(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		lang.request("command.shop.debug.usage").sendToAudience(sender);
		return 1;
	}
	
	
	private int debugPlayer(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		String targetString = ctx.getArgument("player", String.class);
		
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetString);
		
		var shops = shopManager().getShops(offlinePlayer.getUniqueId());
		
		lang.request("command.shop.debug.player.header")
			.replace("%player%", offlinePlayer.getName())
			.replace("%uuid%", String.valueOf(offlinePlayer.getUniqueId()))
			.replace("%online%", offlinePlayer.isOnline())
			.replace("%shop-count%", shops.size())
			.sendToAudience(sender);
		
		if(shops.isEmpty()){
			lang.request("command.shop.debug.player.no-shops")
				.sendToAudience(sender);
			
			return 1;
		}
		
		for(var shop : shops){
			Location location = shop.getSignLocation();
			
			lang.request("command.shop.debug.player.shop")
				.replace("%shop-id%", String.valueOf(shop.getId()))
				.replace("%shop-type%", shop.getType())
				.replace("%world%", location.getWorld().getName())
				.replace("%x%", location.getBlockX())
				.replace("%y%", location.getBlockY())
				.replace("%z%", location.getBlockZ())
				.sendToAudience(sender);
		}
		
		return 1;
	}
	
	private int debugNearby(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(!(sender instanceof Player player)){
			lang.request("command.shop.debug.nearby.error-no-console")
				.sendToAudience(sender);
			
			return -1;
		}
		
		int radius = getArgument(ctx, "radius", Integer.class, 3);
		
		Location location = player.getLocation();
		var shops = shopManager().getShopsNearLocation(location, radius);
		
		lang.request("command.shop.debug.nearby.header")
			.replace("%world%", location.getWorld().getName())
			.replace("%x%", location.getBlockX())
			.replace("%y%", location.getBlockY())
			.replace("%z%", location.getBlockZ())
			.replace("%radius%", radius)
			.replace("%shop-count%", shops.size())
			.sendToAudience(sender);
		
		for(var shop : shops){
			Location shopLocation = shop.getSignLocation();
			
			lang.request("command.shop.debug.nearby.shop")
				.replace("%shop-id%", String.valueOf(shop.getId()))
				.replace("%world%", shopLocation.getWorld().getName())
				.replace("%x%", shopLocation.getBlockX())
				.replace("%y%", shopLocation.getBlockY())
				.replace("%z%", shopLocation.getBlockZ())
				.sendToAudience(sender);
		}
		
		return 1;
	}
	
	private int debugLocation(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(!(sender instanceof Player player)){
			sender.sendMessage("No console allowed!");
			return -1;
		}
		
		Location location = player.getLocation();
		
		var signShop = shopManager().getShopBySign(location);
		var containerShop = shopManager().getShopByContainer(location);
		
		lang.request("command.shop.debug.location.header")
			.replace("%world%", location.getWorld().getName())
			.replace("%x%", location.getBlockX())
			.replace("%y%", location.getBlockY())
			.replace("%z%", location.getBlockZ())
			.sendToAudience(sender);
		
		if(signShop == null){
			lang.request("command.shop.debug.location.sign.none")
				.sendToAudience(sender);
		} else {
			lang.request("command.shop.debug.location.sign.found")
				.replace("%shop-id%", String.valueOf(signShop.getId()))
				.sendToAudience(sender);
		}
		
		if(containerShop == null){
			lang.request("command.shop.debug.location.container.none")
				.sendToAudience(sender);
		} else {
			lang.request("command.shop.debug.location.container.found")
				.replace("%shop-id%", String.valueOf(containerShop.getId()))
				.sendToAudience(sender);
		}
		
		if(signShop != null
		   && containerShop != null
		   && signShop != containerShop){
			
			lang.request("command.shop.debug.location.mismatch")
				.sendToAudience(sender);
		}
		
		return 1;
	}
	
	private int debugStats(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		lang.request("command.shop.debug.stats.header")
			.replace("%total-shops%", shopManager().getNumberOfShops())
			.sendToAudience(sender);
		
		return 1;
	}
	
	@Override
	public String description() {
		return "Command to view and modify the shop plugin data.";
	}
}
