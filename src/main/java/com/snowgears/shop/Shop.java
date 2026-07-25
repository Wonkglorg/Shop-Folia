package com.snowgears.shop;

import com.snowgears.shop.command.ShopCommand;
import com.snowgears.shop.config.ItemConfig;
import com.snowgears.shop.config.SettingsConfig;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.LogHandler;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.handler.TransactionHandler;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.DisplayListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.manager.PlayerManager;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopCreationUtil;
import com.snowgears.shop.util.ShopLogger;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.tcoded.folialib.FoliaLib;
import com.wonkglorg.minecraft.config.LangManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Shop extends JavaPlugin{
	
	@Getter
	private static Shop plugin;
	private ShopLogger logger = new ShopLogger(this);
	// Getter for FoliaLib
	@Getter
	private FoliaLib foliaLib;
	
	@Getter
	private ShopListener shopListener;
	@Getter
	private DisplayListener displayListener;
	private TransactionHandler transactionHandler;
	@Getter
	private MiscListener miscListener;
	@Getter
	private CreativeSelectionListener creativeSelectionListener;
	private ShopGUIListener guiListener;
	
	@Getter
	private ShopHandler shopHandler;
	@Getter
	private ShopGuiHandler guiHandler;
	@Getter
	private ShopCreationUtil shopCreationUtil;
	
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
	private LogHandler logHandler;
	
	@Getter
	private SettingsConfig settingsConfig;
	@Getter
	private ItemConfig itemConfig;
	@Getter
	private LangManager langManager;
	
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
		
		shopCreationUtil = new ShopCreationUtil(this);
		
		shopListener = new ShopListener(this);
		transactionHandler = new TransactionHandler(this);
		miscListener = new MiscListener(this);
		creativeSelectionListener = new CreativeSelectionListener(this);
		displayListener = new DisplayListener(this);
		guiListener = new ShopGUIListener();
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
		guiHandler = new ShopGuiHandler();
		shopHandler = new ShopHandler(plugin);
		//guiHandler.loadIconsAndTitles();
		logHandler = new LogHandler(plugin, settingsConfig);
		
		getServer().getPluginManager().registerEvents(displayListener, this);
		getServer().getPluginManager().registerEvents(shopListener, this);
		getServer().getPluginManager().registerEvents(miscListener, this);
		getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
		getServer().getPluginManager().registerEvents(guiListener, this);
		
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
		if(shopHandler != null){
			ShopHandler.saveAllShops();
		}
		
		// Save player name cache to ensure no data loss
		PlayerNameCache.saveToFile();
		
		// shutdown the database
		if(logHandler != null){
			logHandler.shutdown();
		}
		
		this.logger().info("Disabled Shop " + this.getPluginMeta().getVersion());
	}
	
	public void reload() {
		this.logger().info("Loading Shop " + this.getPluginMeta().getVersion());
		
		HandlerList.unregisterAll(displayListener);
		HandlerList.unregisterAll(shopListener);
		HandlerList.unregisterAll(miscListener);
		HandlerList.unregisterAll(creativeSelectionListener);
		HandlerList.unregisterAll(guiListener);
		PlayerManager.reload();
		plugin.getShopHandler().removeAllDisplays(null);
		
		onDisable();
		onEnable();
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
				//return format.replace("[price]", new DecimalFormat("0.00").format(price).toString());
			} else if(pricePer){
				return format.replace("[price]", UtilMethods.formatLongToKString(price, false));
				//return format.replace("[price]", new DecimalFormat("#.##").format(price).toString());
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
			if(settingsConfig.getCurrencyType() == CurrencyType.VAULT)
			//return format.replace("[price]", new DecimalFormat("0.00").format(price)+"/"+new DecimalFormat("0.00").format(priceSell).toString());
			{
				return format.replace("[price]",
						UtilMethods.formatLongToKString(price, true) + "/" + UtilMethods.formatLongToKString(priceSell, true));
			} else if(pricePer)
			//return format.replace("[price]", new DecimalFormat("#.##").format(price).toString()+"/"+new DecimalFormat("0.00").format(priceSell).toString());
			{
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
