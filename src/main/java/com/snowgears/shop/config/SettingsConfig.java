package com.snowgears.shop.config;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.display.DisplayTagOption;
import com.snowgears.shop.shop.display.DisplayType;
import com.snowgears.shop.shop.CreationWord;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.ShopAction;
import com.snowgears.shop.util.ShopClickType;
import com.wonkglorg.minecraft.config.types.Config;
import lombok.Getter;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class SettingsConfig extends Config{
	/**
	 * The log level to use for console outputs
	 */
	@Getter
	private @Nullable String logLevel;
	@Getter
	private DisplayType displayTypeDefault;
	@Getter
	private DisplayTagOption displayTagOption;
	@Getter
	private DisplayType[] displayCycle;
	@Getter
	private boolean allowCreateMethodSign;
	@Getter
	private boolean playSounds;
	@Getter
	private boolean playEffects;
	@Getter
	private boolean allowCreateMethodChest;
	@Getter
	private boolean setGlowingItemFrame;
	@Getter
	private boolean setGlowingSignText;
	@Getter
	private boolean destroyShopRequiresSneak;
	@Getter
	private boolean checkItemDurability;
	@Getter
	private boolean ignoreItemRepairCost;
	@Getter
	private boolean forceDisplayToNoneIfBlocked;
	/**
	 * The type of currency to use on the server
	 */
	@Getter
	private CurrencyType currencyType;
	@Getter
	private boolean enableGUI;
	@Getter
	private int displayLightLevel;
	@Getter
	private int hoursOfflineToRemoveShops;
	@Getter
	private TreeMap<Double, String> priceSuffixes;
	@Getter
	private double priceSuffixMinimumValue;
	@Getter
	private boolean allowFractionalCurrency;
	@Getter
	private boolean offlinePurchaseNotificationsEnabled;
	/**
	 * Name of the currency to be displayed
	 */
	@Getter
	private String currencyName;
	/**
	 * Currency format
	 */
	@Getter
	private String currencyFormat;
	@Getter
	private Set<Material> enabledContainers;
	@Getter
	private boolean inverseComboShops;
	@Getter
	private double creationCost;
	@Getter
	private double teleportCost;
	@Getter
	private double destructionCost;
	@Getter
	private double teleportCooldown;
	@Getter
	private boolean returnCreationCost;
	@Getter
	private boolean allowPartialSales;
	@Getter
	private ItemListType itemListType;
	@Getter
	private @NotNull List<String> worldBlackList = new ArrayList<>();
	@Getter
	private Map<ShopClickType, ShopAction> clickTypeActionMap = new EnumMap<>(ShopClickType.class);
	
	@Getter
	private Map<CreationWord, String> creationWords = new EnumMap<>(CreationWord.class);
	// Shop display optimization settings
	@Getter
	private double displayMovementThreshold;
	/**
	 * Gets the maximum distance at which shop displays will be shown to players.
	 * Higher values will show shops from further away but may cause client lag.
	 */
	@Getter
	private double maxShopDisplayDistance;
	@Getter
	private double displayProcessInterval;
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
	@Getter
	private boolean debugAllowUseOwnShop;
	@Getter
	private boolean debugTransactionDebugLogs;
	@Getter
	private int debugShopCreateCooldown;
	@Getter
	private boolean debugForceResaveAll;
	@Getter
	private boolean migrateOldData;
	
	public SettingsConfig() {
		super(Shop.getPlugin(), Path.of("config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		logLevel = getString("logLevel");
		try{
			displayTypeDefault = DisplayType.valueOf(getString("displayType"));
		} catch(Exception _){
			displayTypeDefault = DisplayType.ITEM;
		}
		
		try{
			displayTagOption = DisplayTagOption.valueOf(getString("displayNameTags"));
		} catch(Exception e){
			displayTagOption = DisplayTagOption.NONE;
		}
		
		try{
			List<String> cycle = getStringList("displayCycle");
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
		
		allowCreateMethodSign = getBoolean("creationMethod.placeSign");
		allowCreateMethodChest = getBoolean("creationMethod.hitChest");
		enableGUI = getBoolean("enableGUI");
		checkItemDurability = getBoolean("checkItemDurability");
		ignoreItemRepairCost = getBoolean("ignoreItemRepairCost");
		forceDisplayToNoneIfBlocked = getBoolean("forceDisplayToNoneIfBlocked");
		displayLightLevel = getInt("displayLightLevel");
		setGlowingItemFrame = getBoolean("setGlowingItemFrame");
		hoursOfflineToRemoveShops = getInt("deletePlayerShopsAfterXHoursOffline");
		playSounds = getBoolean("playSounds");
		playEffects = getBoolean("playEffects");
		setGlowingSignText = getBoolean("setGlowingSignText");
		priceSuffixes = new TreeMap<>();
		for(String suffixKey : getConfigurationSection("priceSuffixes").getKeys(false)){
			if(suffixKey.equals("minimumValue")){
				priceSuffixMinimumValue = getDouble("priceSuffixes.minimumValue");
			} else {
				boolean enabled = getBoolean("priceSuffixes." + suffixKey + ".enabled");
				if(enabled){
					Double suffixValue = getDouble("priceSuffixes." + suffixKey + ".value");
					priceSuffixes.put(suffixValue, suffixKey);
				}
			}
		}
		
		destroyShopRequiresSneak = getBoolean("destroyShopRequiresSneak");
		
		try{
			currencyType = CurrencyType.valueOf(getString("currency.type"));
		} catch(Exception e){
			currencyType = CurrencyType.ITEM;
		}
		
		if(currencyType == CurrencyType.VAULT){
			allowFractionalCurrency = getBoolean("allowFractionalCurrency");
		}
		
		//todo:mjd compare the current currency to the one last logged in the db and update the action if it differs.
		
		offlinePurchaseNotificationsEnabled = getBoolean("offlinePurchaseNotifications.enabled");
		
		if(offlinePurchaseNotificationsEnabled && getString("logging.type").toUpperCase().equals("OFF")){
			Shop.getPlugin().logger().warning(
					"Offline purchase notifications are enabled in `config.yml` but DB logging is set to `OFF`. Offline purchase notifications will be disabled.");
			Shop.getPlugin().logger().warning(
					"Please set `logging.type` to `FILE` or setup a database in `config.yml` to enable offline purchase notifications.");
			offlinePurchaseNotificationsEnabled = false;
		}
		
		currencyName = getString("currency.name");
		currencyFormat = getString("currency.format", "");
		
		enabledContainers = new HashSet<>();
		for(String materialString : getStringList("enabledContainers")){
			try{
				enabledContainers.add(Material.valueOf(materialString));
			} catch(IllegalArgumentException e){
				Shop.getPlugin().logger().warning("Invalid container material config definition " + materialString);
			}
		}
		
		inverseComboShops = getBoolean("inverseComboShops");
		
		creationCost = getDouble("creationCost");
		destructionCost = getDouble("destructionCost");
		teleportCost = getDouble("teleportCost");
		teleportCooldown = getDouble("teleportCooldown");
		returnCreationCost = getBoolean("returnCreationCost");
		allowPartialSales = getBoolean("allowPartialSales");
		
		try{
			itemListType = ItemListType.valueOf(getString("itemList"));
		} catch(Exception e){
			itemListType = ItemListType.NONE;
		}
		
		worldBlackList.addAll(getStringList("worldBlacklist"));
		
		clickTypeActionMap.put(ShopClickType.valueOf(getString("actionMappings.transactWithShop")), ShopAction.TRANSACT);
		clickTypeActionMap.put(ShopClickType.valueOf(getString("actionMappings.transactWithShopFullStack")), ShopAction.TRANSACT_FULLSTACK);
		clickTypeActionMap.put(ShopClickType.valueOf(getString("actionMappings.viewShopDetails")), ShopAction.VIEW_DETAILS);
		clickTypeActionMap.put(ShopClickType.valueOf(getString("actionMappings.cycleShopDisplay")), ShopAction.CYCLE_DISPLAY);
		
		for(var word : CreationWord.values()){
			creationWords.put(word, getString("sign.creation." + word, word.getDefaultWord()));
		}
		
		// Load shop display optimization settings
		displayProcessInterval = getDouble("displayProcessInterval");
		displayMovementThreshold = getDouble("displayMovementThreshold");
		maxShopDisplayDistance = getDouble("maxShopDisplayDistance");
		shopSearchRadius = getInt("shopSearchRadius");
		displayBatchSize = getInt("displayBatchSize", 10);
		displayBatchDelay = getInt("displayBatchDelay", 2);
		
		debugAllowUseOwnShop = getBoolean("debug.allowUseOwnShop");
		debugTransactionDebugLogs = getBoolean("debug.transactionDebugLogs");
		debugShopCreateCooldown = getInt("debug.shopCreateCooldown");
		debugForceResaveAll = getBoolean("debug.forceResaveAll");
		migrateOldData = getBoolean("debug.migrateOldData");
	}
	
	public String getCreationWord(CreationWord wordKey) {
		return creationWords.get(wordKey);
	}
	
	public ShopAction getShopAction(ShopClickType type) {
		return clickTypeActionMap.get(type);
	}
	
	public void setMigrateOldData(boolean migrateOldData) {
		this.migrateOldData = migrateOldData;
		set("debug.migrateOldData", migrateOldData);
	}
}
