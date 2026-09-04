package com.wonkglorg.minecraft.shop.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wonkglorg.minecraft.command.AbstractCommand;
import com.wonkglorg.minecraft.config.LangManager;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_COMMAND;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_PERMISSION_OPERATOR;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_PERMISSION_USER;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopDatabase;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import static io.papermc.paper.command.brigadier.Commands.literal;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
				.then(literal("reload").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::reload))
				.then(literal("setcurrency").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setCurrency))
				.then(literal("setgamble").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setGamble));
		//@formatter:on
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
			//@formatter:on
		} else {
			lang.request("command.shop.list.success-console").replace("%total-shops%", shopManager().getNumberOfShops()).sendToAudience(sender);
		}
		return 1;
	}
	
	@Override
	public String description() {
		return "Command to view and modify the shop plugin data.";
	}
}
