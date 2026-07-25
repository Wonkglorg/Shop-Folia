package com.snowgears.shop.testsupport;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ShopCreationUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import java.util.Collections;

/**
 * Base class for MockBukkit based tests.
 * <p>
 * Brings up a single {@link ServerMock} once per test-class and tears it down afterwards.
 * Sub-classes can call {@link #getServer()} or {@link #getPlugin()}.
 * Example usage:
public class PluginLoadTest extends BaseMockBukkitTest {
    @Test
    void pluginShouldEnable() {
        assertNotNull(getPlugin(), "Plugin should be loaded by MockBukkit");
        assertTrue(getPlugin().isEnabled(), "Plugin should be enabled inside MockBukkit environment");
    }
}
 */
public abstract class BaseMockBukkitTest {

    private static ServerMock server;
    private static Shop plugin;

    @BeforeEach
    void initServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Shop.class);

        server.getScheduler().waitAsyncTasksFinished();

        setConfig("checkUpdates", false);
        // setRawConfig("logging.type", "OFF"); // happens after the plugin is loaded so kinda useless...
        // Disable displays to avoid NMS/NBT code paths in tests
        setConfig("displayType", DisplayType.NONE);
        // No cooldown between shop creations to allow us to create multiple. We can change this in tests if needed.
        setConfig("debug_shopCreateCooldown", 0);

        // Test worlds can opt into a chunk auto-load patch via addSimpleWorldPatched(name)
    }

    @AfterEach
    void tearDownServer() {
        // Must disable, otherwise shutdown is slow at test end
        plugin.onDisable();
        server.getScheduler().waitAsyncTasksFinished();

        // Unmock the server to cleanup after ourselves
        MockBukkit.unmock();

        server = null;
        plugin = null;
    }

    protected ServerMock getServer() {
        return server;
    }

    protected Shop getPlugin() {
        return plugin;
    }

    // ---------- Test tooling helpers ----------
    protected static void sendChatMessage(PlayerMock player, String message) {
        AsyncPlayerChatEvent amountEvent = new AsyncPlayerChatEvent(true, player, message, Collections.emptySet());
        try {
            server.getScheduler().executeAsyncEvent(amountEvent).get();
        } catch (Error | Exception e) {
            throw new RuntimeException("Failed to send chat message", e);
        }
    }

    protected static String waitForNextMessage(PlayerMock player) {
        String nextMessage = null;
        int attempts = 0;   
        while (nextMessage == null && attempts < 1000) {
            nextMessage = player.nextMessage();
            if (nextMessage == null) {
                server.getScheduler().performTicks(1);
                attempts++;
            }
        }
        System.out.println("ticks: " + attempts + ", message: `" + nextMessage + "`");
        return nextMessage;
    }

    protected static void setRawConfig(String fieldName, String value) {
        plugin.getConfig().set(fieldName, value);
    }

    protected static <T> T getPluginField(String fieldName) {
        try {
            var field = Shop.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(plugin);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field '" + fieldName + "' on Shop", e);
        }
    }

    protected static void setPluginField(String fieldName, Object value) {
        try {
            var field = Shop.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(plugin, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on Shop", e);
        }
    }

    protected static void setConfig(String fieldName, Object value) {
        setPluginField(fieldName, value);
    }

    protected static void setConfig(String fieldName, boolean value) {
        setPluginField(fieldName, value);
    }

    protected static void setConfig(String fieldName, int value) {
        setPluginField(fieldName, value);
    }

    protected static void setConfig(String fieldName, double value) {
        setPluginField(fieldName, value);
    }

    protected static <E extends Enum<E>> void setConfig(String fieldName, E value) {
        setPluginField(fieldName, value);
    }

    // Allow tests to stub calculateBlockFaceForSign to avoid MockBukkit material checks
    protected static void stubCalculateBlockFaceForSign(BlockFace face) {
        ShopCreationUtil original = getPluginField("shopCreationUtil");
        if (original != null && !MockUtil.isMock(original)) {
            ShopCreationUtil spy = Mockito.spy(original);
            Mockito.doReturn(face).when(spy).calculateBlockFaceForSign(Mockito.any(), Mockito.any(), Mockito.any());
            setPluginField("shopCreationUtil", spy);
        }
    }
    protected static void setupEconomy() {
        // Inject Economy mock if Vault currency is enabled
        if (plugin.getSettingsConfig().getCurrencyType() == CurrencyType.VAULT) {
            Economy mockedEconomy = Mockito.mock(Economy.class);
            Mockito.when(mockedEconomy.getBalance(Mockito.any(org.bukkit.OfflinePlayer.class))).thenReturn(10_000.0);
            Mockito.when(mockedEconomy.withdrawPlayer(Mockito.any(org.bukkit.OfflinePlayer.class), Mockito.anyDouble()))
                    .thenAnswer(inv -> new EconomyResponse(inv.getArgument(1), 10_000.0, ResponseType.SUCCESS, "ok"));
            Mockito.when(mockedEconomy.depositPlayer(Mockito.any(org.bukkit.OfflinePlayer.class), Mockito.anyDouble()))
                    .thenAnswer(inv -> new EconomyResponse(inv.getArgument(1), 10_000.0, ResponseType.SUCCESS, "ok"));
            setPluginField("econ", mockedEconomy);
        } 
    }

    // ---------- World helpers to simulate Bukkit's implicit chunk loading ----------
    /**
     * Creates a WorldMock and returns a spy that auto-loads chunks on common accessors
     * (getBlockAt, getChunkAt). Tests should prefer this over server.addSimpleWorld(...)
     * when verifying chunk-load behavior.
     * This can be removed once we have pulled our changes into MockBukkit.
     */
    protected WorldMock addSimpleWorldPatched(String name) {
        WorldMock world = getServer().addSimpleWorld(name);
        WorldMock spyWorld = Mockito.spy(world);

        // getBlockAt(Location) -> ensure chunk loaded
        Mockito.doAnswer(inv -> {
            Location loc = inv.getArgument(0);
            if (loc != null && loc.getWorld() != null) {
                int cx = loc.getBlockX() >> 4;
                int cz = loc.getBlockZ() >> 4;
                spyWorld.loadChunk(cx, cz);
            }
            return inv.callRealMethod();
        }).when(spyWorld).getBlockAt(Mockito.any(Location.class));

        // getBlockAt(int, int, int) -> ensure chunk loaded
        Mockito.doAnswer(inv -> {
            int x = inv.getArgument(0);
            int z = inv.getArgument(2);
            int cx = x >> 4;
            int cz = z >> 4;
            spyWorld.loadChunk(cx, cz);
            return inv.callRealMethod();
        }).when(spyWorld).getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());

        // getChunkAt(Location)
        Mockito.doAnswer(inv -> {
            Location loc = inv.getArgument(0);
            if (loc != null && loc.getWorld() != null) {
                int cx = loc.getBlockX() >> 4;
                int cz = loc.getBlockZ() >> 4;
                spyWorld.loadChunk(cx, cz);
            }
            return inv.callRealMethod();
        }).when(spyWorld).getChunkAt(Mockito.any(Location.class));

        // getChunkAt(int, int)
        Mockito.doAnswer(inv -> {
            int cx = inv.getArgument(0);
            int cz = inv.getArgument(1);
            spyWorld.loadChunk(cx, cz);
            return inv.callRealMethod();
        }).when(spyWorld).getChunkAt(Mockito.anyInt(), Mockito.anyInt());

        return spyWorld;
    }
}
