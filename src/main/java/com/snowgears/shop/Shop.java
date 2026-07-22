package com.snowgears.shop;

import com.snowgears.shop.command.ShopCommand;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.LogHandler;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.handler.TransactionHandler;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.DisplayListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.ItemNameUtil;
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
import com.wonkglorg.minecraft.config.types.Config;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

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
	private ItemNameUtil itemNameUtil;
	@Getter
	private ShopCreationUtil shopCreationUtil;
	
	@Getter
	private NMSBullshitHandler nmsBullshitHandler;
	private boolean enableGUI;
	
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
	
	private Config config;
	@Getter
	private LangManager langManager;
	
	private boolean debug_allowUseOwnShop;
	private boolean debug_transactionDebugLogs;
	private int debug_shopCreateCooldown;
	private boolean debug_forceResaveAll;
	
	public static boolean loggedDisplayDisabledWarning = false;
	
	// Return the custom ShopLogger so that we can log at higher levels.
	
	public ShopLogger logger() {return logger;}
	
	@Override
	public void onLoad() {
		plugin = this;
		config = new Config(this, Path.of("config.yml"));
		logger.setLogLevel(config.getString("logLevel"));
		langManager = LangManager.getInstance(this);
	}
	
	@Override
	public void onEnable() {
		// Initialize FoliaLib
		foliaLib = new FoliaLib(this);
		
		signLocationNameSpacedKey = new NamespacedKey(this, "signLocation");
		playerUUIDNameSpacedKey = new NamespacedKey(this, "playerUUID");
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
		
		guiHandler = new ShopGuiHandler(plugin);
		shopHandler = new ShopHandler(plugin);
		guiHandler.loadIconsAndTitles();
		logHandler = new LogHandler(plugin, config);
		
		getServer().getPluginManager().registerEvents(displayListener, this);
		getServer().getPluginManager().registerEvents(shopListener, this);
		getServer().getPluginManager().registerEvents(miscListener, this);
		getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
		getServer().getPluginManager().registerEvents(guiListener, this);
		
		debug_allowUseOwnShop = config.getBoolean("debug.allowUseOwnShop");
		debug_transactionDebugLogs = config.getBoolean("debug.transactionDebugLogs");
		debug_shopCreateCooldown = config.getInt("debug.shopCreateCooldown");
		debug_forceResaveAll = config.getBoolean("debug.forceResaveAll");
		
		displayListener.startRepeatingDisplayViewTask();
		
		this.logger().info("Enabled Shop " + this.getPluginMeta().getVersion());
		
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, registrar -> new ShopCommand().register(registrar));
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
	
	public boolean getAllowCreationMethodSign() {
		return allowCreateMethodSign;
	}
	
	public boolean getAllowCreationMethodChest() {
		return allowCreateMethodChest;
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
			return "free";
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
			return "free";
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
	
	/**
	 * If the user either has the operator permission or is op, giving them full access to all features of the plugin
	 */
	public static boolean isOperator(Permissible player) {
		return player.isOp() || !player.hasPermission(Constants.SHOP_PERMISSION_OPERATOR);
	}
	
	/**
	 * If the user is allowed to create a shop of this type, this does NOT enforce shop build limit
	 */
	public static boolean isAllowedToCreateShopType(Permissible player, ShopType type) {
		return player.hasPermission("shop.create." + type.toString().toLowerCase()) || player.hasPermission("shop.create") || isOperator(player);
	}
	
	/**
	 * If the user is allowed to create a shop of any type, to find out what specific type they can create use {@link #isAllowedToCreateShopType(Permissible, ShopType)} instead
	 */
	public static boolean isAllowedToCreateShop(){
	
	}
	
	public static int getShopBuildLimit(Permissible player) {
		if(player.isOp()){
			return 99999;
		}
		int baseBuildLimit = -1;
		int extraBuildLimit = 0;
		Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
		
		// calculate base buildlimit permission first (highest number)
		for(PermissionAttachmentInfo permInfo : permissions){
			String perm = permInfo.getPermission();
			// Skip if not a shop permission
			if(!perm.startsWith("shop.")){
				continue;
			}
			
			// If it's a base build limit permission, parse the number
			int value = 0;
			try{
				value = Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
			} catch(NumberFormatException e){
				continue;
			}
			if(perm.startsWith("shop.buildlimit.")){
				if(value > baseBuildLimit){
					baseBuildLimit = value;
				}
			}
			
			// If it's an extra build limit permission, parse the number
			else if(perm.startsWith("shop.buildlimitextra.")){
				extraBuildLimit += value;
				
			}
		}
		return baseBuildLimit + extraBuildLimit;
	}
	
}
