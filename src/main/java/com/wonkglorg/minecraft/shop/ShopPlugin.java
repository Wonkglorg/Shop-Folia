package com.wonkglorg.minecraft.shop;

import com.tcoded.folialib.FoliaLib;
import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.shop.command.ShopCommand;
import com.wonkglorg.minecraft.shop.config.ItemConfig;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.db.ShopDatabase;
import com.wonkglorg.minecraft.shop.listener.DisplayListener;
import com.wonkglorg.minecraft.shop.listener.ShopListener;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.ShopManager;
import com.wonkglorg.minecraft.shop.manager.client.ShopClientManager;
import com.wonkglorg.minecraft.shop.service.ShopService;
import com.wonkglorg.minecraft.shop.service.ShopServiceProvider;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopPlugin extends JavaPlugin{
	public static boolean floodGateEnabled = false;
	@Getter
	private static ShopPlugin plugin;
	private static ShopLogger logger;
	// Getter for FoliaLib
	@Getter
	private FoliaLib foliaLib;
	
	@Getter
	private DisplayListener displayListener;
	
	private static ShopManager shopmanager;
	@Getter
	private String commandAlias;
	private Economy econ = null;
	
	@Getter
	private SettingsConfig settingsConfig;
	@Getter
	private ItemConfig itemConfig;
	private static LangManager langManager;
	@Getter
	private ShopServiceProvider shopServiceProvider;
	
	@Getter
	private boolean immediateShutdown = false;
	
	public static ShopLogger logger() {return logger;}
	
	@Override
	public void onLoad() {
		ShopPlugin.logger = new ShopLogger(this);
		ShopPlugin.plugin = this;
		settingsConfig = new SettingsConfig();
		itemConfig = new ItemConfig();
		logger.setLogLevel(settingsConfig.getLogLevel());
		ShopPlugin.langManager = LangManager.getInstance(this);
		foliaLib = new FoliaLib(this);
	}
	
	@Override
	public void onEnable() {
		ShopPlugin.floodGateEnabled = Bukkit.getPluginManager().getPlugin("floodgate") != null;
		if(itemConfig.getGambleDisplayItem() == null){
			itemConfig.setGambleDisplayItem(new ItemStack(Material.DIAMOND));
		}
		
		// Check if we should load VAULT economy
		CurrencyType currencyType = settingsConfig.getCurrencyType();
		if(currencyType == CurrencyType.VAULT){
			if(setupEconomy()){
				this.logger().info("Shops will use the Vault economy as currency on the server.");
			} else {
				this.logger().severe("Unable to connect to Vault Economy! Are both Vault AND an Economy plugin installed?");
				this.logger().severe(
						"Plugin Disabled: Invalid configuration value `economy.type` config.yml. If you do not wish to use Vault with Shop, make sure to set `economy.type` in the config file to `ITEM`.");
				getServer().getPluginManager().disablePlugin(plugin);
				return;
			}
		}
		
		shopServiceProvider = new ShopServiceProvider(this);
		
		getServer().getServicesManager().register(ShopService.class, shopServiceProvider, this, ServicePriority.Normal);
		try{
			ShopPlugin.shopmanager = new ShopManager(plugin);
		} catch(Exception e){
			logger.severe("Unable to load shop database " + e.getMessage(), e);
			logger.debug("Unable to load shop database", e);
			immediateShutdown();
		}
		
		reload();
		
		getServer().getPluginManager().registerEvents(new DisplayListener(this), this);
		getServer().getPluginManager().registerEvents(new ShopListener(this), this);
		
		this.logger().info("Enabled Shop " + this.getPluginMeta().getVersion());
		
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, registrar -> new ShopCommand().register(registrar));
	}
	
	/**
	 * Shuts down plugin and does not save any changed shop data (this should only ever be called when saving shop data is no longer possible due to file system issues
	 */
	public void immediateShutdown() {
		immediateShutdown = true;
		Bukkit.getPluginManager().disablePlugin(this);
	}
	
	@Override
	public void onDisable() {
		// Cancel all FoliaLib scheduled tasks
		if(foliaLib != null){
			foliaLib.getScheduler().cancelAllTasks();
		}
		
		//save any remaining shops (usually not required but just in case)
		if(shopmanager != null){
			shopmanager.saveAllShops();
		}
		
		if(shopDatabase() != null){
			shopDatabase().close();
		}
		
		logger().info("Disabled Shop " + this.getPluginMeta().getVersion());
	}
	
	public void reload() {
		logger().info("Loading Shop " + this.getPluginMeta().getVersion());
		PlayerManager.reload();
		settingsConfig.reload();
		itemConfig.reload();
		logger.setLogLevel(settingsConfig.getLogLevel());
		langManager.silentLoad();
		shopmanager.reload();
	}
	
	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null){
			return false;
		}
		logger().info("Vault is installed, creating Vault integration for Economy support");
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null){
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	public Economy getEconomy() {
		
		if(econ == null){
			setupEconomy();
		}
		
		return econ;
	}
	
	public static LangManager langManager() {
		return langManager;
	}
	
	public static ShopManager shopManager() {
		return shopmanager;
	}
	
	/**
	 * !!Do not use try with resources on this database!!! only the shop plugin should handle closing the database!
	 */
	public static ShopDatabase shopDatabase() {
		return shopmanager.getDatabase();
	}
	
	public static ShopClientManager shopClientManager() {
		return shopmanager.getShopClientManager();
	}
}
