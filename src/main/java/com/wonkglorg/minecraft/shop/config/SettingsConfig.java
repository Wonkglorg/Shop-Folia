package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.CreationWord;
import com.wonkglorg.minecraft.shop.shop.ShopAction;
import com.wonkglorg.minecraft.shop.shop.ShopClickType;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
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
	/***
	 * How many hours a player can be offline before their shops get deleted
	 */
	@Getter
	private int hoursOfflineToRemoveShops;
	/**
	 * The type of currency to use on the server
	 */
	@Getter
	private CurrencyType currencyType;
	/**
	 * Words used to create shops with
	 */
	@Getter
	private Map<CreationWord, String> signCreationWords = new EnumMap<>(CreationWord.class);
	/**
	 * The currency cost when creating a shop
	 */
	@Getter
	private double creationCost;
	/**
	 * The currency cost when destroying a shop
	 */
	@Getter
	private double destructionCost;
	/**
	 * If the cost for creating the shop should be returned on destruction
	 */
	@Getter
	private boolean returnCreationCost;
	/**
	 * Price suffix mappings
	 */
	@Getter
	private TreeMap<Double, String> priceSuffixes = new TreeMap<>();
	@Getter
	private double priceSuffixMinimumValue;
	/**
	 * The default display new shops spawn with
	 */
	@Getter
	private DisplayType displayTypeDefault;
	/**
	 * The cycle order of shop displays
	 */
	@Getter
	private DisplayType[] displayCycle;
	/**
	 * If signs can be used to create a chest shop
	 */
	@Getter
	private boolean allowCreateMethodSign;
	/**
	 * If commands can be used to create a chest shop
	 */
	@Getter
	private boolean allowCreateMethodCommand;
	/**
	 * Play sounds on shop interactions
	 */
	@Getter
	private boolean playSounds;
	/**
	 * Play particle effects on shop interactions
	 */
	@Getter
	private boolean playEffects;
	/**
	 * If the itemframe should glow on shop displays
	 */
	@Getter
	private boolean setGlowingItemFrame;
	/**
	 * If the sign text should glow
	 */
	@Getter
	private boolean setGlowingSignText;
	/**
	 * If the sign should be waxed
	 */
	@Getter
	private boolean setWaxedSign;
	
	/**
	 * If the materials should be displayed in the clients language
	 */
	@Getter
	private boolean useLocalizedMaterials;
	/**
	 * If sneaking is required to destroy a shop
	 */
	@Getter
	private boolean destroyShopRequiresSneak;
	/**
	 * Light level the display emits when placed
	 */
	@Getter
	private int displayLightLevel;
	/**
	 * If offline purchases should be logged when owner joins
	 */
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
	/**
	 * What containers are valid shop containers
	 */
	@Getter
	private Set<Material> enabledContainers;
	/**
	 * No shops can be created in these worlds
	 */
	@Getter
	private @NotNull List<String> worldBlackList = new ArrayList<>();
	/**
	 * Mappings for actions on a shop
	 */
	@Getter
	private Map<ShopClickType, ShopAction> clickTypeActionMap = new EnumMap<>(ShopClickType.class);
	
	/**
	 * How far a player has to move before display recalculations can happen
	 */
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
	@Getter
	private boolean debugAllowUseOwnShop;
	@Getter
	private boolean debugForceResaveAll;
	@Getter
	private boolean migrateOldData;
	
	public SettingsConfig() {
		super(Main.getPlugin(), Path.of("config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		logLevel = getString("logLevel");
		hoursOfflineToRemoveShops = getInt("deletePlayerShopsAfterXHoursOffline");
		//todo:mjd compare the current currency to the one last logged in the db and update the action if it differs.
		currencyType = CurrencyType.fromValue(getString("currency.type", "ITEM"));
		
		for(var word : CreationWord.values()){
			signCreationWords.put(word, getString("sign.creation." + word, word.getDefaultWord()));
		}
		
		creationCost = getDouble("cost.shop.creation");
		destructionCost = getDouble("cost.shop.destruction");
		returnCreationCost = getBoolean("cost.returnCreationCosat");
		
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
		
		displayTypeDefault = DisplayType.fromValue(getString("displayType"));
		
		List<String> cycle = getStringList("displayCycle");
		if(cycle.isEmpty()){
			for(DisplayType dt : DisplayType.values()){
				cycle.add(dt.name());
			}
		}
		
		displayCycle = new DisplayType[cycle.size()];
		for(int i = 0; i < cycle.size(); i++){
			displayCycle[i] = DisplayType.fromValue(cycle.get(i));
		}
		
		displayLightLevel = getInt("displayLightLevel");
		setGlowingItemFrame = getBoolean("setGlowingItemFrame");
		setGlowingSignText = getBoolean("setGlowingSignText");
		setWaxedSign = getBoolean("setWaxedSign");
		useLocalizedMaterials = getBoolean("useLocalizedMaterialNames");
		
		for(var action : getStringList("actionMappings.transactWithShop")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.TRANSACT);
		}
		for(var action : getStringList("actionMappings.transactWithShopFullStack")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.TRANSACT_FULL_STACK);
		}
		
		for(var action : getStringList("actionMappings.viewShopDetails")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.VIEW_DETAILS);
		}
		for(var action : getStringList("actionMappings.cycleShopDisplay")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.CYCLE_DISPLAY);
		}
		for(var action : getStringList("actionMappings.openShopSettings")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.OPEN_SETTINGS);
		}
		
		
		allowCreateMethodSign = getBoolean("creationMethod.placeSign");
		allowCreateMethodCommand = getBoolean("creationMethod.placeSign");
		
		playSounds = getBoolean("playSounds");
		playEffects = getBoolean("playEffects");
		
		destroyShopRequiresSneak = getBoolean("destroyShopRequiresSneak");
		
		offlinePurchaseNotificationsEnabled = getBoolean("offlinePurchaseNotifications.enabled");
		
		currencyName = getString("currency.name");
		currencyFormat = getString("currency.format", "");
		
		enabledContainers = new HashSet<>();
		for(String materialString : getStringList("enabledContainers")){
			try{
				enabledContainers.add(Material.valueOf(materialString));
			} catch(IllegalArgumentException e){
				Main.getPlugin().logger().warning("Invalid container material config definition " + materialString);
			}
		}
		
		worldBlackList.addAll(getStringList("worldBlacklist"));
		
		// Load shop display optimization settings
		displayProcessInterval = getDouble("displayProcessInterval");
		displayMovementThreshold = getDouble("displayMovementThreshold");
		maxShopDisplayDistance = getDouble("maxShopDisplayDistance");
		shopSearchRadius = getInt("shopSearchRadius");
		
		debugAllowUseOwnShop = getBoolean("debug.allowUseOwnShop");
		debugForceResaveAll = getBoolean("debug.forceResaveAll");
		migrateOldData = getBoolean("debug.migrateOldData");
	}
	
	public String getCreationWord(CreationWord wordKey) {
		return signCreationWords.get(wordKey);
	}
	
	public ShopAction getShopAction(ShopClickType type) {
		return clickTypeActionMap.get(type);
	}
	
	public void setMigrateOldData(boolean migrateOldData) {
		this.migrateOldData = migrateOldData;
		set("debug.migrateOldData", migrateOldData);
	}
}
