package com.snowgears.shop.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wonkglorg.minecraft.command.AbstractCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.configuration.ConfigurationSection;

public class ShopCommand extends AbstractCommand{
	
	public ShopCommand(ConfigurationSection section) {
	}
	
	@Override
	public LiteralArgumentBuilder<CommandSourceStack> argumentBuilder() {
		return null;
	}
	
	@Override
	public String description() {
		return "";
	}
}
