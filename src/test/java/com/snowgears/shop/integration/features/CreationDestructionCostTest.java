package com.snowgears.shop.integration.features;

import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.EconomyUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

@Tag("integration")
public class CreationDestructionCostTest extends BaseMockBukkitTest {

    @Test
    void destroy_cost_insufficientFunds_prevents_destroy() {
        setPluginField("currencyType", CurrencyType.ITEM);
        setPluginField("destructionCost", 5.0);

        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 28, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Ensure player has 2 currency
        player.getInventory().addItem(new ItemStack(Material.EMERALD, 2));
        // Try to destroy shop
        new PlayerSimulation(player).simulateBlockBreak(shop.getSignLocation().getBlock());
        // §cYou do not have the funds required to create this shop.
        assertEquals(2, EconomyUtils.getFunds(player, player.getInventory()), "Player should have 2 emeralds");
        assertEquals("§cYou do not have the funds required to destroy this shop.", waitForNextMessage(player), "Player should be sent dialog after failing to destroy shop");
        assertNotNull(getPlugin().getShopHandler().getShop(shop.getSignLocation()), "Shop should remain when player cannot afford destruction cost");
    }

    @Test
    void destroy_cost_sufficientFunds_allows_destroy() {
        setPluginField("currencyType", CurrencyType.ITEM);
        setPluginField("destructionCost", 5.0);

        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 30, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");
        
        player.getInventory().addItem(new ItemStack(Material.EMERALD, 15));
        new PlayerSimulation(player).simulateBlockBreak(shop.getSignLocation().getBlock());

        assertEquals(10, EconomyUtils.getFunds(player, player.getInventory()), "Player should have 10 emeralds");
        assertEquals("§7You have destroyed your selling shop.", waitForNextMessage(player), "Player should be sent dialog after destroying shop sign");
        assertEquals(Material.AIR, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should be destroyed when player can afford destruction cost");
        assertNull(getPlugin().getShopHandler().getShop(shop.getSignLocation()), "Shop should be destroyed when player can afford destruction cost");
    }

    @Test
    void destroy_returns_creation_cost_experience_currency() {
        setPluginField("currencyType", CurrencyType.EXPERIENCE);
        setPluginField("returnCreationCost", true);
        setPluginField("destructionCost", 1.0);
        setPluginField("creationCost", 10.0);

        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        player.giveExp(1000);
        assertEquals(1000, EconomyUtils.getFunds(player, player.getInventory()), "Player should have 1000 experience");

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 32, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        assertEquals(990, EconomyUtils.getFunds(player, player.getInventory()), "Player should have 15 emeralds");

        new PlayerSimulation(player).simulateBlockBreak(shop.getSignLocation().getBlock());

        assertEquals(999, EconomyUtils.getFunds(player, player.getInventory()), "Player should gain at least creationCost experience when destroying their own shop with returnCreationCost=true");
    }
}