package com.snowgears.shop;

import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.CommandHandler;
import com.snowgears.shop.handler.LogHandler;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.handler.TransactionHandler;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.DisplayListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.ConfigUpdater;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.Metrics;
import com.snowgears.shop.util.Metrics.AdvancedPie;
import com.snowgears.shop.util.Metrics.SimplePie;
import com.snowgears.shop.util.Metrics.SingleLineChart;
import com.snowgears.shop.util.NMSBullshitHandler;
import com.snowgears.shop.util.PlayerNameCache;
import com.snowgears.shop.util.ShopAction;
import com.snowgears.shop.util.ShopClickType;
import com.snowgears.shop.util.ShopCreationUtil;
import com.snowgears.shop.util.ShopLogger;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.tcoded.folialib.FoliaLib;
import com.wonkglorg.minecraft.config.LangManager;
import io.papermc.paper.configuration.GlobalConfiguration.UpdateChecker;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Shop extends JavaPlugin{
	
	@Getter
	private static Shop plugin;
	private ShopLogger logger = new ShopLogger(this, true);
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
	private Boolean worldGuardExists;
	
	@Getter
	private ShopHandler shopHandler;
	@Getter
	private ShopGuiHandler guiHandler;
	@Getter
	private ItemNameUtil itemNameUtil;
	@Getter
	private ShopCreationUtil shopCreationUtil;
	
	@Getter
	private NMSBullshitHandler nmsBullshitHandler;
	
	private boolean usePerms;
	private boolean checkUpdates;
	private boolean enableGUI;
	
	private boolean hookTowny;
	@Getter
	private String commandAlias;
	@Getter
	private DisplayType displayType;
	@Getter
	private DisplayTagOption displayTagOption;
	@Getter
	private DisplayType[] displayCycle;
	private boolean checkItemDurability;
	private boolean ignoreItemRepairCost;
	private boolean allowCreativeSelection;
	private boolean forceDisplayToNoneIfBlocked;
	@Getter
	private int displayLightLevel;
	private boolean setGlowingItemFrame;
	private boolean setGlowingSignText;
	@Getter
	private NavigableMap<Double, String> priceSuffixes;
	@Getter
	private Double priceSuffixMinimumValue;
	private boolean destroyShopRequiresSneak;
	@Getter
	private int hoursOfflineToRemoveShops;
	private boolean playSounds;
	private boolean playEffects;
	private boolean allowCreateMethodSign;
	private boolean allowCreateMethodChest;
	@Getter
	private ItemStack gambleDisplayItem;
	@Getter
	private ItemStack itemCurrency = null;
	@Getter
	private CurrencyType currencyType;
	@Getter
	private String currencyName = "";
	private String currencyFormat = "";
	private boolean allowFractionalCurrency = false;
	private Economy econ = null;
	@Getter
	private List<Material> enabledContainers;
	private boolean inverseComboShops;
	@Getter
	private double creationCost;
	@Getter
	private double destructionCost;
	@Getter
	private double teleportCost;
	@Getter
	private double teleportCooldown;
	private boolean returnCreationCost;
	private boolean allowPartialSales;
	@Getter
	private double taxPercent;
	private boolean offlinePurchaseNotificationsEnabled;
	@Getter
	private ItemListType itemListType;
	private List<String> worldBlackList;
	private HashMap<ShopClickType, ShopAction> clickTypeActionMap;
	@Getter
	private NamespacedKey signLocationNameSpacedKey;
	@Getter
	private NamespacedKey playerUUIDNameSpacedKey;
	@Getter
	private LogHandler logHandler;
	
	// Shop display optimization settings
	@Getter
	private double displayProcessInterval;
	@Getter
	private double displayMovementThreshold;
	/**
	 * Gets the maximum distance at which shop displays will be shown to players.
	 * Higher values will show shops from further away but may cause client lag.
	 */
	@Getter
	private double maxShopDisplayDistance;
	/**
	 * Gets the radius (in chunks) around a player to search for shops.
	 * Each increment searches exponentially more chunks (1=3x3 area, 2=5x5 area, 3=7x7 area).
	 */
	@Getter
	private int shopSearchRadius;
	/**
	 * Gets the number of shop displays to process in a single batch when sending to a player.
	 * This controls how many displays are sent at once to create a smoother appearance.
	 */
	@Getter
	private int displayBatchSize;
	/**
	 * Gets the delay between batches of shop displays in server ticks (20 ticks = 1 second).
	 * Higher values create a smoother appearance but take longer to show all displays.
	 */
	@Getter
	private int displayBatchDelay;
	
	private YamlConfiguration config;
	@Getter
	private LangManager langManager;
	
	private boolean debug_allowUseOwnShop;
	private boolean debug_transactionDebugLogs;
	private int debug_shopCreateCooldown;
	private boolean debug_forceResaveAll;
	
	private Metrics metrics;
	
	public static boolean loggedDisplayDisabledWarning = false;
	
	// Return the custom ShopLogger so that we can log at higher levels.
	
	public ShopLogger logger() {return logger;}
	
	@Override
	public void onLoad() {
		File configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists()){
			configFile.getParentFile().mkdirs();
			UtilMethods.copy(getResource("config.yml"), configFile);
		}
		config = YamlConfiguration.loadConfiguration(configFile);
		// Load logger
		logger = new ShopLogger(this, config.getBoolean("enableLogColor"));
		this.logger().setLogLevel(config.getString("logLevel"));
		langManager = LangManager.getInstance(this);
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		
		// Initialize FoliaLib
		foliaLib = new FoliaLib(this);
		
		File configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists()){
			configFile.getParentFile().mkdirs();
			UtilMethods.copy(getResource("config.yml"), configFile);
		}
		
		File signConfigFile = new File(getDataFolder(), "signConfig.yml");
		if(!signConfigFile.exists()){
			signConfigFile.getParentFile().mkdirs();
			UtilMethods.copy(getResource("signConfig.yml"), signConfigFile);
		}
		
		File displayConfigFile = new File(getDataFolder(), "displayConfig.yml");
		if(!displayConfigFile.exists()){
			displayConfigFile.getParentFile().mkdirs();
			UtilMethods.copy(getResource("displayConfig.yml"), displayConfigFile);
		}
		
		try{
			// Check if we need to update any legacy config values
			
			// v1.10.0
			// Check if offlinePurchaseNotifications.enabled is a new value
			YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
			// One time update if the Offline Purchase Notifications feature is being started up for the very first time
			// Previous default OFF, new default FILE
			if(oldConfig.get("offlinePurchaseNotifications") == null && oldConfig.getString("logging.type").equals("OFF")){
				logger.info("Config default update: v1.10.0(+) is being run for the first time, setting logging type to FILE from old default OFF");
				oldConfig.set("logging.type", "FILE");
				oldConfig.save(configFile);
			}
			
			// v1.10.2
			// Migrate old hookWorldGuard to new worldGuard.requireAllowShopFlag structure
			if(oldConfig.get("hookWorldGuard") != null && oldConfig.get("worldGuard.requireAllowShopFlag") == null){
				boolean oldValue = oldConfig.getBoolean("hookWorldGuard");
				logger.info("Config migration: moving 'hookWorldGuard' to 'worldGuard.requireAllowShopFlag'");
				oldConfig.set("worldGuard.requireAllowShopFlag", oldValue);
				oldConfig.set("hookWorldGuard", null); // remove old key
				oldConfig.save(configFile);
			}
			
			// Next time we add a migration lets move it to a util class to keep things clean.
			
			ConfigUpdater.update(plugin, "config.yml", configFile, new ArrayList<>());
		} catch(IOException e){
			e.printStackTrace();
		}
		
		try{
			ConfigUpdater.update(plugin, "signConfig.yml", signConfigFile, new ArrayList<>());
		} catch(IOException e){
			e.printStackTrace();
		}
		
		try{
			ConfigUpdater.update(plugin, "displayConfig.yml", displayConfigFile, new ArrayList<>());
		} catch(IOException e){
			e.printStackTrace();
		}
		
		reloadConfig();
		signLocationNameSpacedKey = new NamespacedKey(this, "signLocation");
		playerUUIDNameSpacedKey = new NamespacedKey(this, "playerUUID");
		config = YamlConfiguration.loadConfiguration(configFile);
		// Load logger values again in case the log level was changed on a reload
		this.logger().setLogLevel(config.getString("logLevel"));
		this.logger().enableColor(config.getBoolean("enableLogColor"));
		
		nmsBullshitHandler = new NMSBullshitHandler(this);
		
		shopCreationUtil = new ShopCreationUtil(this);
		
		shopListener = new ShopListener(this);
		transactionHandler = new TransactionHandler(this);
		miscListener = new MiscListener(this);
		creativeSelectionListener = new CreativeSelectionListener(this);
		displayListener = new DisplayListener(this);
		guiListener = new ShopGUIListener(this);
		
		try{
			displayType = DisplayType.valueOf(config.getString("displayType"));
		} catch(Exception e){
			displayType = DisplayType.ITEM;
		}
		
		try{
			displayTagOption = DisplayTagOption.valueOf(config.getString("displayNameTags"));
		} catch(Exception e){
			displayTagOption = DisplayTagOption.NONE;
		}
		
		try{
			List<String> cycle = config.getStringList("displayCycle");
			if(cycle.isEmpty()){
				for(DisplayType dt : DisplayType.values()){
					cycle.add(dt.name());
				}
			}
			
			displayCycle = new DisplayType[cycle.size()];
			for(int i = 0; i < cycle.size(); i++){
				displayCycle[i] = DisplayType.valueOf(cycle.get(i));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		// Load ShopMessage by initializing it once
		new ShopMessage(this);
		itemNameUtil = new ItemNameUtil();
		
		File fileDirectory = new File(this.getDataFolder(), "Data");
		if(!fileDirectory.exists()){
			boolean success;
			success = (fileDirectory.mkdirs());
			if(!success){
				this.logger().severe("[Shop] Data folder could not be created!");
			}
		}
		
		allowCreateMethodSign = config.getBoolean("creationMethod.placeSign");
		allowCreateMethodChest = config.getBoolean("creationMethod.hitChest");
		
		usePerms = config.getBoolean("usePermissions");
		if(usePerms){
			this.logger().info("Permissions enabled, Shop will respect player permissions");
		} else {
			this.logger().info("Permissions disabled, everyone will be able to create/use shops by default");
		}
		checkUpdates = config.getBoolean("checkUpdates");
		enableGUI = config.getBoolean("enableGUI");
		
		commandAlias = config.getString("commandAlias");
		checkItemDurability = config.getBoolean("checkItemDurability");
		ignoreItemRepairCost = config.getBoolean("ignoreItemRepairCost");
		allowCreativeSelection = config.getBoolean("allowCreativeSelection");
		forceDisplayToNoneIfBlocked = config.getBoolean("forceDisplayToNoneIfBlocked");
		displayLightLevel = config.getInt("displayLightLevel");
		setGlowingItemFrame = config.getBoolean("setGlowingItemFrame");
		hoursOfflineToRemoveShops = config.getInt("deletePlayerShopsAfterXHoursOffline");
		playSounds = config.getBoolean("playSounds");
		playEffects = config.getBoolean("playEffects");
		setGlowingSignText = config.getBoolean("setGlowingSignText");
		priceSuffixes = new TreeMap<>();
		for(String suffixKey : config.getConfigurationSection("priceSuffixes").getKeys(false)){
			if(suffixKey.equals("minimumValue")){
				priceSuffixMinimumValue = config.getDouble("priceSuffixes.minimumValue");
			} else {
				boolean enabled = config.getBoolean("priceSuffixes." + suffixKey + ".enabled");
				if(enabled){
					Double suffixValue = config.getDouble("priceSuffixes." + suffixKey + ".value");
					priceSuffixes.put(suffixValue, suffixKey);
				}
			}
		}
		
		destroyShopRequiresSneak = config.getBoolean("destroyShopRequiresSneak");
		
		try{
			currencyType = CurrencyType.valueOf(config.getString("currency.type"));
		} catch(Exception e){
			currencyType = CurrencyType.ITEM;
		}
		
		if(currencyType == CurrencyType.VAULT){
			allowFractionalCurrency = config.getBoolean("allowFractionalCurrency");
		}
		
		offlinePurchaseNotificationsEnabled = config.getBoolean("offlinePurchaseNotifications.enabled");
		
		if(offlinePurchaseNotificationsEnabled && config.getString("logging.type").toUpperCase().equals("OFF")){
			this.logger().warning(
					"Offline purchase notifications are enabled in `config.yml` but DB logging is set to `OFF`. Offline purchase notifications will be disabled.");
			this.logger()
			    .warning("Please set `logging.type` to `FILE` or setup a database in `config.yml` to enable offline purchase notifications.");
			offlinePurchaseNotificationsEnabled = false;
		}
		
		//Loading the itemCurrency from a file makes it easier to allow servers to use detailed itemstacks as the server's economy item
		File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
		if(itemCurrencyFile.exists()){
			YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
			itemCurrency = currencyConfig.getItemStack("item");
			itemCurrency.setAmount(1);
		} else {
			try{
				itemCurrency = new ItemStack(Material.EMERALD);
				itemCurrencyFile.createNewFile();
				
				YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
				currencyConfig.set("item", itemCurrency);
				currencyConfig.save(itemCurrencyFile);
			} catch(Exception e){
			}
		}
		
		//load the gamble display item from it's file
		File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
		if(!gambleDisplayFile.exists()){
			gambleDisplayFile.getParentFile().mkdirs();
			UtilMethods.copy(getResource("GAMBLE_DISPLAY.yml"), gambleDisplayFile);
		}
		try{
			YamlConfiguration gambleItemConfig = YamlConfiguration.loadConfiguration(gambleDisplayFile);
			gambleDisplayItem = gambleItemConfig.getItemStack("GAMBLE_DISPLAY");
		} catch(IllegalArgumentException e){
			this.logger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
			gambleDisplayItem = new ItemStack(Material.DIAMOND);
		} catch(Exception e){
			this.logger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
			gambleDisplayItem = new ItemStack(Material.DIAMOND);
		} catch(Error e){
			this.logger().warning("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
			gambleDisplayItem = new ItemStack(Material.DIAMOND);
		}
		
		if(gambleDisplayItem == null){
			this.logger().severe("Error loading gamble display item from file: " + gambleDisplayFile.getAbsolutePath());
			gambleDisplayItem = new ItemStack(Material.DIAMOND);
		}
		
		currencyName = config.getString("currency.name");
		currencyFormat = config.getString("currency.format");
		
		enabledContainers = new ArrayList<>();
		for(String materialString : config.getStringList("enabledContainers")){
			try{
				enabledContainers.add(Material.valueOf(materialString));
			} catch(IllegalArgumentException e){
			}
		}
		
		inverseComboShops = config.getBoolean("inverseComboShops");
		
		creationCost = config.getDouble("creationCost");
		destructionCost = config.getDouble("destructionCost");
		teleportCost = config.getDouble("teleportCost");
		teleportCooldown = config.getDouble("teleportCooldown");
		returnCreationCost = config.getBoolean("returnCreationCost");
		allowPartialSales = config.getBoolean("allowPartialSales");
		
		try{
			itemListType = ItemListType.valueOf(config.getString("itemList"));
		} catch(Exception e){
			itemListType = ItemListType.NONE;
		}
		
		worldBlackList = config.getStringList("worldBlacklist");
		for(String world : config.getStringList("worldBlacklist")){
			worldBlackList.add(world);
		}
		
		clickTypeActionMap = new HashMap<>();
		clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.transactWithShop")), ShopAction.TRANSACT);
		clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.transactWithShopFullStack")), ShopAction.TRANSACT_FULLSTACK);
		clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.viewShopDetails")), ShopAction.VIEW_DETAILS);
		clickTypeActionMap.put(ShopClickType.valueOf(config.getString("actionMappings.cycleShopDisplay")), ShopAction.CYCLE_DISPLAY);
		
		// Load shop display optimization settings
		displayProcessInterval = config.getDouble("displayProcessInterval");
		displayMovementThreshold = config.getDouble("displayMovementThreshold");
		maxShopDisplayDistance = config.getDouble("maxShopDisplayDistance");
		shopSearchRadius = config.getInt("shopSearchRadius");
		displayBatchSize = config.getInt("displayBatchSize", 10);
		displayBatchDelay = config.getInt("displayBatchDelay", 2);
		
		// Check if we should load VAULT economy
		if(currencyType == CurrencyType.VAULT){
			if(setupEconomy()){
				this.logger().info("Shops will use the Vault economy (" + currencyName + ") as currency on the server.");
			} else {
				this.logger().severe("Unable to connect to Vault Economy! Are both Vault AND an Economy plugin installed?");
				this.logger().severe(
						"Plugin Disabled: Invalid configuration value `economy.type` config.yml. If you do not wish to use Vault with Shop, make sure to set `economy.type` in the config file to `ITEM`.");
				getServer().getPluginManager().disablePlugin(plugin);
				return;
			}
		} else {
			if(itemCurrency == null){
				this.logger().severe("Plugin Disabled: Invalid value for `itemCurrencyID` in `config.yml`");
				getServer().getPluginManager().disablePlugin(plugin);
				return;
			}
			this.logger().info("Shops will use " + ItemNameUtil.getNameAsPlainText(itemCurrency) + "(s) as the currency on the server.");
		}
		
		// Load CommandHandler by initializing it once
		new CommandHandler(this,
				null,
				commandAlias,
				"Base command for the Shop plugin",
				"/shop",
				new ArrayList<>(Collections.singletonList(commandAlias)));
		
		guiHandler = new ShopGuiHandler(plugin);
		shopHandler = new ShopHandler(plugin);
		guiHandler.loadIconsAndTitles();
		logHandler = new LogHandler(plugin, config);
		
		getServer().getPluginManager().registerEvents(displayListener, this);
		getServer().getPluginManager().registerEvents(shopListener, this);
		getServer().getPluginManager().registerEvents(miscListener, this);
		getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
		getServer().getPluginManager().registerEvents(guiListener, this);
		
		//only define different listener hooks if the plugins are present on the server
		if(getServer().getPluginManager().getPlugin("WorldGuard") != null){
			this.logger().notice("WorldGuard is installed, creating WorldGuard listener");
			this.worldGuardExists = true;
		} else {
			this.worldGuardExists = false;
		}
		
		int bstatsPluginId = 25211;
		metrics = new Metrics(plugin, bstatsPluginId);
		metrics.addCustomChart(new SingleLineChart("transactions", () -> logHandler.getRecentTransactionCount()));
		metrics.addCustomChart(new SingleLineChart("item_volume", () -> logHandler.getRecentItemVolume()));
		metrics.addCustomChart(new SingleLineChart("shops", () -> shopHandler.getNumberOfShops()));
		metrics.addCustomChart(new AdvancedPie("shop_types", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Buy", shopHandler.getNumberOfShops(ShopType.BUY));
			valueMap.put("Sell", shopHandler.getNumberOfShops(ShopType.SELL));
			valueMap.put("Barter", shopHandler.getNumberOfShops(ShopType.BARTER));
			valueMap.put("Combo", shopHandler.getNumberOfShops(ShopType.COMBO));
			valueMap.put("Gamble", shopHandler.getNumberOfShops(ShopType.GAMBLE));
			return valueMap;
		}));
		metrics.addCustomChart(new AdvancedPie("shop_display_types", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Floating Item", shopHandler.getNumberOfShopDisplayTypes(DisplayType.ITEM));
			valueMap.put("Large Item", shopHandler.getNumberOfShopDisplayTypes(DisplayType.LARGE_ITEM));
			valueMap.put("Item Frame", shopHandler.getNumberOfShopDisplayTypes(DisplayType.ITEM_FRAME));
			valueMap.put("Glass Case", shopHandler.getNumberOfShopDisplayTypes(DisplayType.GLASS_CASE));
			valueMap.put("None", shopHandler.getNumberOfShopDisplayTypes(DisplayType.NONE));
			return valueMap;
		}));
		metrics.addCustomChart(new AdvancedPie("shop_containers", () -> shopHandler.getShopContainerCounts()));
		metrics.addCustomChart(new SimplePie("economy_type", () -> currencyType.toString()));
		metrics.addCustomChart(new SimplePie("fractional_currency", () -> String.valueOf(allowFractionalCurrency)));
		// Add metrics for more configuration options
		metrics.addCustomChart(new SimplePie("use_permissions", () -> String.valueOf(usePerms)));
		metrics.addCustomChart(new SimplePie("allow_partial_sales", () -> String.valueOf(allowPartialSales)));
		
		// Group these into an advanced pie
		metrics.addCustomChart(new AdvancedPie("shop_creation_methods", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Sign Creation", allowCreateMethodSign ? 1 : 0);
			valueMap.put("Chest Creation", allowCreateMethodChest ? 1 : 0);
			valueMap.put("Signs Disabled", allowCreateMethodSign ? 0 : 1);
			valueMap.put("Chests Disabled", allowCreateMethodChest ? 0 : 1);
			return valueMap;
		}));
		
		metrics.addCustomChart(new SimplePie("offline_purchase_notifications", () -> String.valueOf(offlinePurchaseNotificationsEnabled)));
		metrics.addCustomChart(new SimplePie("shop_gui_enabled", () -> String.valueOf(enableGUI)));
		metrics.addCustomChart(new SimplePie("allow_searching_items", () -> String.valueOf(allowCreativeSelection)));
		metrics.addCustomChart(new SimplePie("check_item_durability", () -> String.valueOf(checkItemDurability)));
		metrics.addCustomChart(new SimplePie("ignore_item_repair_cost", () -> String.valueOf(ignoreItemRepairCost)));
		metrics.addCustomChart(new AdvancedPie("sounds_and_effects", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Sounds Enabled", playSounds ? 1 : 0);
			valueMap.put("Effects Enabled", playEffects ? 1 : 0);
			valueMap.put("Sounds Disabled", playSounds ? 0 : 1);
			valueMap.put("Effects Disabled", playEffects ? 0 : 1);
			return valueMap;
		}));
		
		metrics.addCustomChart(new SimplePie("worldguard_enabled", () -> {return String.valueOf(worldGuardExists);}));
		metrics.addCustomChart(new SimplePie("towny_enabled", () -> {return String.valueOf(hookTowny);}));
		metrics.addCustomChart(new SimplePie("database_type", () -> String.valueOf(config.getString("logging.type"))));
		
		// Track display type preferences
		metrics.addCustomChart(new SimplePie("item_hover_display_type", () -> displayType.toString()));
		metrics.addCustomChart(new SimplePie("hover_text_activation_type", () -> displayTagOption.toString()));
		
		// Track if shop auto-deletion is enabled
		metrics.addCustomChart(new SimplePie("auto_cleanup_dead_shops", () -> String.valueOf(hoursOfflineToRemoveShops > 0)));
		// Track if destroying shops requires sneaking
		metrics.addCustomChart(new SimplePie("destroy_requires_sneak", () -> String.valueOf(destroyShopRequiresSneak)));
		// Track if combo shops are inverted
		metrics.addCustomChart(new SimplePie("inverse_combo_shops", () -> String.valueOf(inverseComboShops)));
		
		// Add container types tracking - group by container categories
		metrics.addCustomChart(new AdvancedPie("enabled_containers", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			// Track basic chest types
			boolean hasChests = enabledContainers.contains(Material.CHEST) || enabledContainers.contains(Material.TRAPPED_CHEST);
			valueMap.put("Chests Allowed", hasChests ? 1 : 0);
			valueMap.put("Chests Disabled", hasChests ? 0 : 1);
			
			// Track barrels
			boolean hasBarrel = enabledContainers.contains(Material.BARREL);
			valueMap.put("Barrels Allowed", hasBarrel ? 1 : 0);
			valueMap.put("Barrels Disabled", hasBarrel ? 0 : 1);
			
			// Track if any shulker box is enabled
			boolean hasShulker = enabledContainers.stream().anyMatch(m -> m.name().endsWith("SHULKER_BOX"));
			valueMap.put("Shulker Boxes Allowed", hasShulker ? 1 : 0);
			valueMap.put("Shulker Boxes Disabled", hasShulker ? 0 : 1);
			
			return valueMap;
		}));
		
		// Track economic barriers (costs)
		metrics.addCustomChart(new AdvancedPie("economic_barriers", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Creation Cost", creationCost > 0 ? 1 : 0);
			valueMap.put("No Creation Cost", creationCost > 0 ? 0 : 1);
			valueMap.put("Destruction Cost", destructionCost > 0 ? 1 : 0);
			valueMap.put("No Destruction Cost", destructionCost > 0 ? 0 : 1);
			valueMap.put("Teleport Cost", teleportCost > 0 ? 1 : 0);
			valueMap.put("No Teleport Cost", teleportCost > 0 ? 0 : 1);
			valueMap.put("Return Creation Cost", returnCreationCost ? 1 : 0);
			valueMap.put("Do not Return Creation Cost", returnCreationCost ? 0 : 1);
			return valueMap;
		}));
		
		// Track display enhancement features (1.17+)
		metrics.addCustomChart(new AdvancedPie("display_enhancements", () -> {
			Map<String, Integer> valueMap = new HashMap<>();
			valueMap.put("Custom Light Level", displayLightLevel > 0 ? 1 : 0);
			valueMap.put("Normal Light Level", displayLightLevel > 0 ? 0 : 1);
			valueMap.put("Glowing Item Frames", setGlowingItemFrame ? 1 : 0);
			valueMap.put("Normal Item Frames", setGlowingItemFrame ? 0 : 1);
			valueMap.put("Glowing Sign Text", setGlowingSignText ? 1 : 0);
			valueMap.put("Normal Sign Text", setGlowingSignText ? 0 : 1);
			return valueMap;
		}));
		
		// Track which click types are used for each action
		// Find which click type is assigned to TRANSACT
		metrics.addCustomChart(new SimplePie("transaction_action_mapping", () -> {
			for(Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()){
				if(entry.getValue() == ShopAction.TRANSACT){
					return entry.getKey().toString();
				}
			}
			return "NOT_SET";
		}));
		// Find which click type is assigned to TRANSACT_FULLSTACK
		metrics.addCustomChart(new SimplePie("full_stack_transaction_action_mapping", () -> {
			for(Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()){
				if(entry.getValue() == ShopAction.TRANSACT_FULLSTACK){
					return entry.getKey().toString();
				}
			}
			return "NOT_SET";
		}));
		// Find which click type is assigned to VIEW_DETAILS
		metrics.addCustomChart(new SimplePie("view_details_action_mapping", () -> {
			for(Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()){
				if(entry.getValue() == ShopAction.VIEW_DETAILS){
					return entry.getKey().toString();
				}
			}
			return "NOT_SET";
		}));
		// Find which click type is assigned to CYCLE_DISPLAY
		metrics.addCustomChart(new SimplePie("cycle_display_action_mapping", () -> {
			for(Map.Entry<ShopClickType, ShopAction> entry : clickTypeActionMap.entrySet()){
				if(entry.getValue() == ShopAction.CYCLE_DISPLAY){
					return entry.getKey().toString();
				}
			}
			return "NOT_SET";
		}));
		
		metrics.addCustomChart(new SimplePie("display_processing_interval", () -> String.valueOf(displayProcessInterval)));
		metrics.addCustomChart(new SimplePie("display_movement_threshold", () -> String.valueOf(displayMovementThreshold)));
		metrics.addCustomChart(new SimplePie("display_max_shop_distance", () -> String.valueOf(maxShopDisplayDistance)));
		metrics.addCustomChart(new SimplePie("display_shop_search_radius", () -> String.valueOf(shopSearchRadius)));
		metrics.addCustomChart(new SimplePie("display_batch_size", () -> String.valueOf(displayBatchSize)));
		metrics.addCustomChart(new SimplePie("display_batch_delay", () -> String.valueOf(displayBatchDelay)));
		
		debug_allowUseOwnShop = config.getBoolean("debug.allowUseOwnShop");
		debug_transactionDebugLogs = config.getBoolean("debug.transactionDebugLogs");
		debug_shopCreateCooldown = config.getInt("debug.shopCreateCooldown");
		debug_forceResaveAll = config.getBoolean("debug.forceResaveAll");
		
		displayListener.startRepeatingDisplayViewTask();
		
		this.logger().info("Enabled Shop " + this.getPluginMeta().getVersion());
	}
	
	@Override
	public void onDisable() {
		// Cancel all FoliaLib scheduled tasks
		if(foliaLib != null){
			foliaLib.getScheduler().cancelAllTasks();
		}
		
		//save any remaining shops (usually not required but just in case)
		if(shopHandler != null){
			shopHandler.saveAllShops();
		}
		
		// Save player name cache to ensure no data loss
		PlayerNameCache.saveToFile();
		
		// shutdown the database
		if(logHandler != null){
			logHandler.shutdown();
		}
		if(metrics != null){
			metrics.shutdown();
		}
		
		this.logger().info("Disabled Shop " + this.getPluginMeta().getVersion());
	}
	
	public void reload() {
		this.logger().info("Reloading Shop " + this.getPluginMeta().getVersion());
		
		HandlerList.unregisterAll(displayListener);
		HandlerList.unregisterAll(shopListener);
		HandlerList.unregisterAll(miscListener);
		HandlerList.unregisterAll(creativeSelectionListener);
		HandlerList.unregisterAll(guiListener);
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
	
	public boolean usePerms() {
		return usePerms;
	}
	
	public boolean getAllowCreationMethodSign() {
		return allowCreateMethodSign;
	}
	
	public boolean getAllowCreationMethodChest() {
		return allowCreateMethodChest;
	}
	
	public boolean worldGuardExists() {return worldGuardExists;}
	
	public boolean hookTowny() {
		return hookTowny;
	}
	
	public boolean checkItemDurability() {
		return checkItemDurability;
	}
	
	public boolean ignoreItemRepairCost() {
		return ignoreItemRepairCost;
	}
	
	public boolean allowCreativeSelection() {
		return allowCreativeSelection;
	}
	
	public boolean forceDisplayToNoneIfBlocked() {
		return forceDisplayToNoneIfBlocked;
	}
	
	public boolean getGlowingItemFrame() {
		return setGlowingItemFrame;
	}
	
	public boolean playSounds() {
		return playSounds;
	}
	
	public boolean playEffects() {
		return playEffects;
	}
	
	public boolean getGlowingSignText() {
		return setGlowingSignText;
	}
	
	public boolean useGUI() {
		return enableGUI;
	}
	
	public boolean offlinePurchaseNotificationsEnabled() {
		return offlinePurchaseNotificationsEnabled;
	}
	
	private Boolean isMockBukkit = null;
	
	public boolean isMockBukkit() {
		if(this.isMockBukkit == null){
			this.isMockBukkit = plugin.getServer().getClass().getPackage().getName().contains("mockbukkit");
		}
		return this.isMockBukkit;
	}
	
	public boolean getDebugAllowUseOwnShop() {return debug_allowUseOwnShop;}
	
	public boolean getDebugTransactionDebugLogs() {return debug_transactionDebugLogs;}
	
	public int getDebugShopCreateCooldown() {return debug_shopCreateCooldown;}
	
	public boolean getDebugForceResaveAll() {return debug_forceResaveAll;}
	
	public void setItemCurrency(ItemStack itemCurrency) {
		this.itemCurrency = itemCurrency;
		
		try{
			File fileDirectory = new File(getDataFolder(), "Data");
			File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
			YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
			currencyConfig.set("item", plugin.getItemCurrency());
			currencyConfig.save(itemCurrencyFile);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setGambleDisplayItem(ItemStack is) {
		this.gambleDisplayItem = is;
		
		try{
			File fileDirectory = new File(plugin.getDataFolder(), "Data");
			File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
			if(!gambleDisplayFile.exists()){
				gambleDisplayFile.getParentFile().mkdirs();
				gambleDisplayFile.createNewFile();
			}
			YamlConfiguration config = YamlConfiguration.loadConfiguration(gambleDisplayFile);
			
			config.set("GAMBLE_DISPLAY", is);
			config.save(gambleDisplayFile);
			
			plugin.reload();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String getPriceString(double price, boolean pricePer) {
		if(price == 0){
			return ShopMessage.getFreePriceWord();
		}
		
		String format = currencyFormat;
		
		if(format.contains("[name]")){
			format = format.replace("[name]", currencyName);
		}
		if(format.contains("[price]")){
			if(currencyType == CurrencyType.VAULT){
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
			return ShopMessage.getFreePriceWord();
		}
		
		String format = currencyFormat;
		
		if(format.contains("[name]")){
			format = format.replace("[name]", currencyName);
		}
		if(format.contains("[price]")){
			if(currencyType == CurrencyType.VAULT)
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
	
	public boolean getAllowFractionalCurrency() {
		return allowFractionalCurrency;
	}
	
	public boolean inverseComboShops() {
		return inverseComboShops;
	}
	
	public boolean getDestroyShopRequiresSneak() {
		return destroyShopRequiresSneak;
	}
	
	public boolean returnCreationCost() {
		return returnCreationCost;
	}
	
	public boolean getAllowPartialSales() {
		return allowPartialSales;
		
	}
	
	public List<String> getWorldBlacklist() {
		return worldBlackList;
	}
	
	public ShopAction getShopAction(ShopClickType shopClickType) {
		return clickTypeActionMap.get(shopClickType);
	}
	
}
