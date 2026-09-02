package com.wonkglorg.minecraft.shop.config;

import com.wonkglorg.minecraft.config.types.Config;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import com.wonkglorg.minecraft.shop.shop.ShopAction;
import com.wonkglorg.minecraft.shop.shop.ShopClickType;
import com.wonkglorg.minecraft.shop.shop.creation.SignCreationLayoutParser;
import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
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
	// =================================================================== //
	//                      CURRENCY AND ECONOMY                           //
	// =================================================================== //
	/**
	 * The type of currency to use on the server
	 */
	@Getter
	private CurrencyType currencyType;
	/**
	 * Price suffix mappings
	 */
	@Getter
	private TreeMap<Double, String> priceSuffixes = new TreeMap<>();
	// =================================================================== //
	//                           SHOP                                      //
	// =================================================================== //
	/**
	 * If the materials should be displayed in the clients language
	 */
	@Getter
	private boolean useLocalizedMaterials;
	/**
	 * Allows for shop owners to use their own shops
	 */
	@Getter
	private boolean allowUseOwnShop;
	
	/**
	 * If sneaking is required to destroy a shop
	 */
	@Getter
	private boolean destroyShopRequiresSneak;
	
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
	 * The default display new shops spawn with
	 */
	@Getter
	private DisplayType displayTypeDefault;
	
	/**
	 * The cycling order of displays
	 */
	@Getter
	private DisplayType[] displayCycle;
	/**
	 * Light level the display emits when placed
	 */
	@Getter
	private int displayLightLevel;
	
	/**
	 * If the itemframe should glow on shop displays
	 */
	@Getter
	private boolean displayGlowingItemFrame;
	/**
	 * If the shop sign should be made glowing
	 */
	@Getter
	private boolean signGlowingSignText;
	/**
	 * If the shop sign should be auto waxed
	 */
	@Getter
	private boolean signWaxed;
	
	// =================================================================== //
	//                         SHOP INTERACTIONS                           //
	// =================================================================== //
	/**
	 * Mappings for actions on a shop
	 */
	@Getter
	private Map<ShopClickType, ShopAction> clickTypeActionMap = new EnumMap<>(ShopClickType.class);
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
	
	@Getter
	private ShopSettings shopSettings = new ShopSettings();
	// =================================================================== //
	//                                FILTERS                              //
	// =================================================================== //
	/**
	 * No shops can be created in these worlds
	 */
	@Getter
	private @NotNull List<String> worldBlackList = new ArrayList<>();
	/**
	 * What containers are valid shop containers
	 */
	@Getter
	private Set<Material> enabledContainers;
	
	/**
	 * No shop can be created with this material
	 */
	@Getter
	private List<Material> blacklistMaterials = new ArrayList<>();
	/**
	 * Only shops with this material can be created
	 */
	@Getter
	private List<Material> whitelistMaterials = new ArrayList<>();
	
	// =================================================================== //
	//                   SHOP PERFORMANCE OPTIMIZATIONS                    //
	// =================================================================== //
	
	@Getter
	private double displayProcessInterval;
	/**
	 * How far a player has to move before display recalculations can happen
	 */
	@Getter
	private double displayMovementThreshold;
	/**
	 * Gets the maximum distance in chunks at which shops will send visual updates to players.
	 */
	@Getter
	private int maxShopProcessingDistanceChunks;
	/**
	 * Gets the maximum distance in blocks at which shops will send visual updates to players, inherited from {@link #maxShopProcessingDistanceChunks}.
	 */
	@Getter
	private int maxShopProcessingDistanceBlocks;
	
	// =================================================================== //
	//                               MIGRATION                             //
	// =================================================================== //
	
	/**
	 * If data from the old shop plugin should be migrated on the next startup
	 */
	@Getter
	private boolean migrateOldData;
	
	public SettingsConfig() {
		super(ShopPlugin.getPlugin(), Path.of("config.yml"));
		reload();
	}
	
	public void reload() {
		silentLoad();
		logLevel = getString("log-level");
		//todo:mjd compare the current currency to the one last logged in the db and update the action if it differs.
		currencyType = CurrencyType.fromValue(getString("currency.type", "ITEM"));
		
		priceSuffixes = new TreeMap<>();
		for(String suffixKey : getConfigurationSection("price-suffixes").getKeys(false)){
			double suffixValue = getDouble("price-suffixes." + suffixKey + "");
			priceSuffixes.put(suffixValue, suffixKey);
		}
		
		useLocalizedMaterials = getBoolean("use-localized-material-names");
		allowUseOwnShop = getBoolean("allow-use-own-shop");
		destroyShopRequiresSneak = getBoolean("destroy-shop-requires-sneak");
		
		creationCost = getDouble("cost.create");
		destructionCost = getDouble("cost.destroy");
		returnCreationCost = getBoolean("cost.return-creation-cost");
		
		displayTypeDefault = DisplayType.fromValue(getString("display.type"));
		
		List<String> cycle = getStringList("display.cycle");
		if(cycle.isEmpty()){
			for(DisplayType dt : DisplayType.values()){
				cycle.add(dt.name());
			}
		}
		
		displayCycle = new DisplayType[cycle.size()];
		for(int i = 0; i < cycle.size(); i++){
			displayCycle[i] = DisplayType.fromValue(cycle.get(i));
		}
		
		displayLightLevel = getInt("display.light-level");
		displayGlowingItemFrame = getBoolean("display.glowing-item-frame");
		signGlowingSignText = getBoolean("sign.glowing-sign-text");
		signWaxed = getBoolean("sign.waxed-sign");
		
		for(var action : getStringList("mappings.transact-with-shop")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.TRANSACT);
		}
		for(var action : getStringList("mappings. transact-with-shop-full-stack")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.TRANSACT_FULL_STACK);
		}
		
		for(var action : getStringList("mappings.view-shop-details")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.VIEW_DETAILS);
		}
		for(var action : getStringList("mappings.cycle-shop-display")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.CYCLE_DISPLAY);
		}
		for(var action : getStringList("mappings.open-shop-settings")){
			clickTypeActionMap.put(ShopClickType.valueOf(action), ShopAction.OPEN_SETTINGS);
		}
		
		playSounds = getBoolean("play-sounds");
		playEffects = getBoolean("play-effects");
		
		shopSettings.reload(getConfigurationSection("shop.settings"));
		
		worldBlackList.addAll(getStringList("world-blacklist"));
		
		enabledContainers = new HashSet<>();
		for(String materialString : getStringList("enabled-containers")){
			try{
				enabledContainers.add(Material.valueOf(materialString));
			} catch(IllegalArgumentException e){
				logger().warning("Invalid container material config definition " + materialString);
			}
		}
		
		blacklistMaterials.clear();
		whitelistMaterials.clear();
		if(contains("material-blacklist")){
			for(var material : getStringList("material-blacklist")){
				try{
					blacklistMaterials.add(Material.valueOf(material));
				} catch(IllegalArgumentException e){
					logger.warning("Invalid blacklist material:" + material);
				}
			}
		}
		
		if(contains("material-whitelist")){
			for(var material : getStringList("material-whitelist")){
				try{
					whitelistMaterials.add(Material.valueOf(material));
				} catch(IllegalArgumentException e){
					logger.warning("Invalid whitelist material:" + material);
				}
			}
		}
		
		SignCreationLayoutParser.reload(getConfigurationSection("creation-layout"));
		
		// Load shop display optimization settings
		displayProcessInterval = getDouble("display-process-interval");
		
		displayMovementThreshold = getDouble("display-movement-threshold");
		
		maxShopProcessingDistanceChunks = getInt("max-shop-processing-distance-chunks");
		maxShopProcessingDistanceBlocks = maxShopProcessingDistanceChunks * 16;
		
		migrateOldData = getBoolean("migrate-old-data");
	}
	
	/**
	 * If the item is allowed by the black / whitelist
	 */
	public boolean isValidItem(ItemStack itemStack) {
		if(itemStack == null || itemStack.getType().isAir()){
			return false;
		}
		
		if(!blacklistMaterials.isEmpty() && blacklistMaterials.contains(itemStack.getType())){
			return false;
		}
		
		if(!whitelistMaterials.isEmpty() && !whitelistMaterials.contains(itemStack.getType())){
			return false;
		}
		
		return true;
	}
	
	public ShopAction getShopAction(ShopClickType type) {
		return clickTypeActionMap.get(type);
	}
	
	public void setMigrateOldData(boolean migrateOldData) {
		this.migrateOldData = migrateOldData;
		set("migrate-old-data", migrateOldData);
	}
	
	public static class ShopSettings{
		@Getter
		private boolean transactionLimitEnabled;
		@Getter
		private int transactionLimitDefault;
		@Getter
		private boolean transactionCooldownEnabled;
		@Getter
		private long transactionCooldownDefault;
		@Getter
		private boolean outOfStockNotificationEnabled;
		@Getter
		private boolean isOutOfStockNotificationDefault;
		@Getter
		private boolean transactionNotificationEnabled;
		@Getter
		private boolean transactionNotificationDefault;
		@Getter
		private boolean customItemUpdaterEnabled;
		@Getter
		private boolean customItemUpdaterDefault;
		
		public void reload(ConfigurationSection section) {
			transactionLimitEnabled = section.getBoolean("transaction-limit.enabled");
			transactionLimitDefault = section.getInt("transaction-limit.default-value");
			
			transactionCooldownEnabled = section.getBoolean("transaction-cooldown.enabled");
			transactionCooldownDefault = section.getLong("transaction-cooldown.default-value");
			
			outOfStockNotificationEnabled = section.getBoolean("out-of-stock-notification.enabled");
			isOutOfStockNotificationDefault = section.getBoolean("out-of-stock-notification.default-value");
			
			transactionNotificationEnabled = section.getBoolean("transaction-notification.enabled");
			transactionNotificationDefault = section.getBoolean("transaction-notification.default-value");
			
			customItemUpdaterEnabled = section.getBoolean("transaction-notification.enabled");
			customItemUpdaterDefault = section.getBoolean("transaction-notification.default-value");
		}
	}
}
