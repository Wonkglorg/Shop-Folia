package com.wonkglorg.minecraft.shop;

import com.wonkglorg.minecraft.shop.config.ItemConfig.CurrencyDenomination;
import com.wonkglorg.minecraft.shop.config.ItemConfigProvider;
import com.wonkglorg.minecraft.shop.config.SettingsConfig;
import com.wonkglorg.minecraft.shop.config.SettingsConfig.ShopSettings;
import com.wonkglorg.minecraft.shop.mocks.MockHelper;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.shop.transaction.party.PlayerTransactionParty;
import com.wonkglorg.minecraft.shop.shop.transaction.party.ShopTransactionParty;
import com.wonkglorg.minecraft.shop.util.CurrencyType;
import com.wonkglorg.minecraft.shop.util.ShopLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestBase{
	protected static ServerMock server;
	
	protected static Material CURRENCY_TYPE = Material.DIAMOND;
	protected static ItemConfigProvider itemConfigMock;
	protected static SettingsConfig settingsConfig;
	protected static ShopPlugin pluginMock;
	protected static MockedStatic<ShopPlugin> pluginStaticMock;
	protected static Location shopSignLocation;
	
	private static ShopLogger logger;
	
	@BeforeAll
	static void setUp() {
		server = MockBukkit.mock();
		server.addSimpleWorld("world");
		server.addPlayer();
		logger = mockLogger();
		pluginMock = mock(ShopPlugin.class);
		
		itemConfigMock = mockItemConfig();
		settingsConfig = mockSettingsConfig();
		when(pluginMock.getItemConfig()).thenReturn(itemConfigMock);
		
		when(pluginMock.getSettingsConfig()).thenReturn(settingsConfig);
		
		pluginStaticMock = Mockito.mockStatic(ShopPlugin.class);
		
		pluginStaticMock.when(ShopPlugin::getPlugin).thenReturn(pluginMock);
		
		pluginStaticMock.when(ShopPlugin::logger).thenReturn(logger);
		shopSignLocation = new Location(server.getRespawnWorld(), 0, 0, 0);
	}
	
	@AfterAll
	static void tearDown() {
		pluginStaticMock.close();
		
		MockBukkit.unmock();
	}
	
	private static ShopLogger mockLogger() {
		var mock = mock(ShopLogger.class);
		doAnswer(invocation -> {
			System.out.println("warn: " + invocation.getArgument(0));
			return null;
		}).when(mock).warning(anyString());
		
		doAnswer(invocation -> {
			System.out.println("error: " + invocation.getArgument(0));
			return null;
		}).when(mock).severe(anyString());
		doAnswer(invocation -> {
			System.out.println("debug: " + invocation.getArgument(0));
			return null;
		}).when(mock).debug(anyString());
		
		return mock;
	}
	
	private static ItemConfigProvider mockItemConfig() {
		
		ItemConfigProvider mock = mock(ItemConfigProvider.class);
		List<CurrencyDenomination> currencyDenominations = new ArrayList<>();
		currencyDenominations.add(new CurrencyDenomination(new ItemStack(Material.DIAMOND_BLOCK), 9));
		currencyDenominations.sort(Comparator.comparingInt(CurrencyDenomination::value).reversed());
		int smallestCurrencyDenominationMultiplier = currencyDenominations.getLast().value();
		
		when(mock.getCurrencyItem()).thenReturn(new ItemStack(CURRENCY_TYPE));
		when(mock.getCurrencyDenominations()).thenReturn(currencyDenominations);
		when(mock.getSmallestCurrencyDenominationMultiplier()).thenReturn(smallestCurrencyDenominationMultiplier);
		return mock;
	}
	
	private static SettingsConfig mockSettingsConfig() {
		
		SettingsConfig mock = mock(SettingsConfig.class);
		
		ShopSettings shopSettings = new ShopSettings();
		
		when(mock.getCurrencyType()).thenReturn(CurrencyType.ITEM);
		
		when(mock.getShopSettings()).thenReturn(shopSettings);
		
		return mock;
	}
	
	public static void fillInventory(Inventory inventory, Material material) {
		while(inventory.addItem(new ItemStack(material)).isEmpty()){
			// Continue until inventory is full.
		}
	}
	
	protected void setCurrencyType(CurrencyType type) {
		when(settingsConfig.getCurrencyType()).thenReturn(type);
	}
	
	protected PlayerTransactionParty createTransactionParty() {
		return new PlayerTransactionParty(server.addPlayer());
	}
	
	protected ShopTransactionParty createShopTransactionParty(boolean isAdmin) {
		return new ShopTransactionParty(createShop(ShopType.SELL, 1.0, 1, isAdmin, createCurrencyStack(1), null));
	}
	
	protected AbstractShop createShop(ShopType type, double price, int amount, boolean admin, ItemStack mainStack, ItemStack barterStack) {
		
		var player = server.addPlayer();
		
		AbstractShop shop = MockHelper.createShop(type,
				shopSignLocation,
				player.getUniqueId(),
				price,
				amount,
				admin,
				player.getInventory(),
				mainStack);
		
		if(type == ShopType.BARTER){
			shop.setSecondaryItemStack(barterStack.clone());
		}
		
		if(type == ShopType.GAMBLE){
			shop.getInventory().addItem(mainStack.clone());
		}
		
		shop.updateStock();
		
		return shop;
	}
	
	protected ItemStack createCurrencyStack(int amount) {
		return new ItemStack(CURRENCY_TYPE, amount);
	}
	
	protected void addCurrencyDenomination(Material material, int value) {
		var denominations = new ArrayList<>(itemConfigMock.getCurrencyDenominations());
		denominations.add(new CurrencyDenomination(new ItemStack(material), value));
		denominations.sort(Comparator.comparingInt(CurrencyDenomination::value).reversed());
		int smallestCurrencyDenominationMultiplier = denominations.getLast().value();
		when(itemConfigMock.getSmallestCurrencyDenominationMultiplier()).thenReturn(smallestCurrencyDenominationMultiplier);
		when(itemConfigMock.getCurrencyDenominations()).thenReturn(denominations);
	}
	
	protected void removerCurrencyDenomination(int value) {
		var denominations = new ArrayList<>(itemConfigMock.getCurrencyDenominations());
		denominations.removeIf(e -> e.value() == value);
		denominations.sort(Comparator.comparingInt(CurrencyDenomination::value).reversed());
		int smallestCurrencyDenominationMultiplier = denominations.getLast().value();
		when(itemConfigMock.getSmallestCurrencyDenominationMultiplier()).thenReturn(smallestCurrencyDenominationMultiplier);
		when(itemConfigMock.getCurrencyDenominations()).thenReturn(denominations);
	}
}
