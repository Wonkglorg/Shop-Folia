package com.snowgears.shop;

import com.snowgears.shop.command.ShopCommand;
import com.snowgears.shop.config.ItemConfig;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.shop.display.DisplayTagOption;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.TransactionHandler;
import com.snowgears.shop.listener.DisplayListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.manager.ShopManager;
import com.snowgears.shop.service.ShopService;
import com.snowgears.shop.service.ShopServiceProvider;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.ShopLogger;
import com.snowgears.shop.util.UtilMethods;
import com.tcoded.folialib.FoliaLib;
import com.wonkglorg.minecraft.config.LangManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Shop extends JavaPlugin{
	
	@Getter
	private static Shop plugin;
	private ShopLogger logger = new ShopLogger(this);
	// Getter for FoliaLib
	@Getter
	private FoliaLib foliaLib;
	
	@Getter
	private DisplayListener displayListener;
	private TransactionHandler transactionHandler;
	
	@Getter
	private ShopManager shopmanager;
	@Getter
	private String commandAlias;
	@Getter
	private DisplayTagOption displayTagOption;
	private Economy econ = null;
	@Getter
	private ItemListType itemListType;
	@Getter
	private NamespacedKey signLocationNameSpacedKey;
	@Getter
	private NamespacedKey playerUUIDNameSpacedKey;
	
	@Getter
	private SettingsConfig settingsConfig;
	@Getter
	private ItemConfig itemConfig;
	@Getter
	private LangManager langManager;
	@Getter
	private ShopServiceProvider shopServiceProvider;
	
	public static boolean loggedDisplayDisabledWarning = false;
	
	private boolean isMockBukkit = false;
	
	@Getter
	private boolean immediateShutdown = false;
	
	public ShopLogger logger() {return logger;}
	
	@Override
	public void onLoad() {
		plugin = this;
		settingsConfig = new SettingsConfig();
		itemConfig = new ItemConfig();
		logger.setLogLevel(settingsConfig.getLogLevel());
		langManager = LangManager.getInstance(this);
		foliaLib = new FoliaLib(this);
	}
	
	@Override
	public void onEnable() {
		this.isMockBukkit = plugin.getServer().getClass().getPackage().getName().contains("mockbukkit");
		
		signLocationNameSpacedKey = new NamespacedKey(this, "signLocation");
		playerUUIDNameSpacedKey = new NamespacedKey(this, "playerUUID");
		
		transactionHandler = new TransactionHandler(this);
		if(itemConfig.getGambleDisplayItem() == null){
			itemConfig.setGambleDisplayItem(new ItemStack(Material.DIAMOND));
		}
		
		// Check if we should load VAULT economy
		CurrencyType currencyType = settingsConfig.getCurrencyType();
		if(currencyType == CurrencyType.VAULT){
			if(setupEconomy()){
				this.logger().info("Shops will use the Vault economy (" + settingsConfig.getCurrencyName() + ") as currency on the server.");
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
			shopmanager = new ShopManager(plugin);
		} catch(Exception e){
			logger.severe("Unable to load shop database" + e.getMessage());
			logger.debug("Unable to load shop database", e);
			immediateShutdown();
		}
		
		shopmanager.loadShops();
		
		displayListener = new DisplayListener(this);
		getServer().getPluginManager().registerEvents(displayListener, this);
		getServer().getPluginManager().registerEvents(new ShopListener(this), this);
		getServer().getPluginManager().registerEvents(new MiscListener(this), this);
		getServer().getPluginManager().registerEvents(new ShopGUIListener(), this);
		
		displayListener.startRepeatingDisplayViewTask();
		
		this.logger().info("Enabled Shop " + this.getPluginMeta().getVersion());
		
		if(!isMockBukkit){
			this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, registrar -> new ShopCommand().register(registrar));
		}
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
		
		this.logger().info("Disabled Shop " + this.getPluginMeta().getVersion());
	}
	
	public void reload() {
		this.logger().info("Loading Shop " + this.getPluginMeta().getVersion());
		PlayerManager.reload();
	}
	
	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null){
			return false;
		}
		this.logger().notice("Vault is installed, creating Vault integration for Economy support");
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null){
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	public TransactionHandler getTransactionHelper() {
		return transactionHandler;
	}
	
	public String getPriceString(double price, boolean pricePer) {
		if(price == 0){
			return "free";
		}
		
		String format = settingsConfig.getCurrencyFormat();
		
		if(format.contains("[name]")){
			format = format.replace("[name]", settingsConfig.getCurrencyName());
		}
		if(format.contains("[price]")){
			if(settingsConfig.getCurrencyType() == CurrencyType.VAULT){
				return format.replace("[price]", UtilMethods.formatLongToKString(price, true));
			} else if(pricePer){
				return format.replace("[price]", UtilMethods.formatLongToKString(price, false));
			} else {
				return format.replace("[price]", "" + (int) price);
			}
		}
		return format;
	}
	
	public String getPriceComboString(double price, double priceSell, boolean pricePer) {
		if(price == 0){
			return "free";
		}
		
		String format = settingsConfig.getCurrencyFormat();
		
		if(format.contains("[name]")){
			format = format.replace("[name]", settingsConfig.getCurrencyName());
		}
		if(format.contains("[price]")){
			if(settingsConfig.getCurrencyType() == CurrencyType.VAULT){
				return format.replace("[price]",
						UtilMethods.formatLongToKString(price, true) + "/" + UtilMethods.formatLongToKString(priceSell, true));
			} else if(pricePer){
				return format.replace("[price]",
						UtilMethods.formatLongToKString(price, false) + "/" + UtilMethods.formatLongToKString(priceSell, true));
			} else {
				return format.replace("[price]", "" + (int) price + "/" + (int) priceSell);
			}
		}
		return format;
	}
	
	public Economy getEconomy() {
		
		if(econ == null){
			setupEconomy();
		}
		
		return econ;
	}
}
