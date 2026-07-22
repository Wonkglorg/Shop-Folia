package com.snowgears.shop.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import static com.snowgears.shop.Constants.SHOP_COMMAND;
import static com.snowgears.shop.Constants.SHOP_PERMISSION_OPERATOR;
import static com.snowgears.shop.Constants.SHOP_PERMISSION_USER;
import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.wonkglorg.minecraft.command.AbstractCommand;
import com.wonkglorg.minecraft.config.LangManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import static io.papermc.paper.command.brigadier.Commands.literal;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand extends AbstractCommand{
	
	private final Shop plugin;
	private final LangManager lang;
	private final String description;
	
	public ShopCommand(String name, String description) {
		this.plugin = Shop.getPlugin();
		this.lang = plugin.getLangManager();
		this.description = description;
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
						.then(literal("user").executes(this::notifyUser))
						.then(literal("owner").executes(this::notifyOwner))
						.then(literal("stock").executes(this::notifyStock))
					 )
				.then(literal("reload").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::reload))
				.then(literal("setcurrency").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setCurrency))
				.then(literal("setgamble").requires(permissions(SHOP_PERMISSION_OPERATOR)).executes(this::setGamble))

				
				
				;
		//@formatter:on
	}
	
	private int usageNotify(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int notifyOwner(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int notifyStock(CommandContext<CommandSourceStack> ctx) {
	
	
	}
	
	private int notifyUser(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int setGamble(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int setCurrency(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int currency(CommandContext<CommandSourceStack> ctx) {
		return 0;
	}
	
	private int reload(CommandContext<CommandSourceStack> ctx) {
		plugin.reload();
		plugin.getShopHandler().removeLegacyDisplays();
		lang.request("command.reload.success").sendToAudience(ctx.getSource().getSender());
		return 0;
	}
	
	private int usageMessage(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		if(plugin.useGUI() && sender instanceof Player player){
			ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);
			window.open();
		} else {
			lang.request("command.usage.user").sendToAudience(sender);
			if(sender.hasPermission("shop.operator") || sender.isOp()){
				lang.request("command.usage.admin").sendToAudience(sender);
			}
		}
		
	}
	
	private int list(CommandContext<CommandSourceStack> ctx) {
		CommandSender sender = ctx.getSource().getSender();
		
		if(sender instanceof Player player){
			//@formatter:off
			lang.request("command.list.success-player")
				.replace("%total-shops%", plugin.getShopHandler().getNumberOfShops())
				.replace("%user-amount%", plugin.getShopHandler().getNumberOfShops(player))
				.replace("%build-limit%",plugin.getShopListener().getBuildLimit(player))
				.sendToAudience(sender);
			//@formatter:on
		} else {
			lang.request("command.list.success-console").replace("%total-shops%", plugin.getShopHandler().getNumberOfShops()).sendToAudience(sender);
		}
		return 1;
	}
	
	@Override
	public String description() {
		return description;
	}
}
