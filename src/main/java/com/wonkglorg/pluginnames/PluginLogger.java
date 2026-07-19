package com.wonkglorg.pluginnames;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginLogger{
	
	private static Logger logger = Logger.getLogger(PluginLogger.class.getName());
	
	public static void createInstance(JavaPlugin plugin) {
		logger = plugin.getLogger();
	}
	
	public static void debug(String message) {
		logger.log(Level.FINEST, message);
	}
	
	public static void info(String message) {
		logger.info(message);
	}
	
	public static void warn(String message, Throwable throwable) {
		logger.log(Level.WARNING, message, throwable);
	}
	
	public static void warn(String message) {
		logger.log(Level.WARNING, message);
	}
	
	public static void error(String message, Throwable throwable) {
		logger.log(Level.SEVERE, message, throwable);
	}
	
	public static void error(String message) {
		logger.log(Level.SEVERE, message);
	}
	
	public static void log(Level level, String message, Throwable throwable) {
		logger.log(level, message, throwable);
	}
	
	public static void toSender(CommandSender sender, Component message) {
		sender.sendMessage(message);
	}
}