package com.snowgears.shop.integration.features;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

@Tag("integration")
public class EfficientChunkLoadTest extends BaseMockBukkitTest {

    private static boolean isChunkLoaded(World world, Location loc) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        return world.isChunkLoaded(cx, cz);
    }

    private static void unloadChunk(WorldMock world, Location loc) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        world.unloadChunk(cx, cz);
        assertFalse(world.isChunkLoaded(cx, cz), "Precondition: chunk should be unloaded for test");
    }

    @Test
    void updateSign_forceDoesNotLoadChunk_whenUnloaded() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        WorldMock world = addSimpleWorldPatched("world");
        PlayerMock player = server.addPlayer();

        // Create a valid shop via chest flow
        AbstractShop shop = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 100, 65, 100), new ItemStack(Material.DIRT), "sell", 8, "1"
        );
        assertNotNull(shop);
        assertTrue(isChunkLoaded(world, shop.getSignLocation()), "Precondition: chunk should be loaded after creation");

        // Unload the sign's chunk
        Location signLoc = shop.getSignLocation();
        unloadChunk(world, signLoc);

        // Act: Force an update while unloaded
        shop.updateSign(true);
        server.getScheduler().performTicks(2);

        // Assert: chunk remains unloaded; sign update should be deferred until it loads
        assertFalse(isChunkLoaded(world, signLoc), "updateSign(true) should not load the chunk when unloaded");
    }

    @Test
    void getInventory_returnsNull_andDoesNotLoadChunk_whenUnloaded() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        WorldMock world = addSimpleWorldPatched("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 104, 65, 100), new ItemStack(Material.DIRT), "sell", 8, "1"
        );
        assertNotNull(shop);
        assertTrue(isChunkLoaded(world, shop.getSignLocation()), "Precondition: chunk should be loaded after creation");

        Location signLoc = shop.getSignLocation();
        unloadChunk(world, signLoc);

        // Act
        var inv = shop.getInventory();

        // Assert
        assertNull(inv, "Inventory should be null when the chunk is unloaded");
        assertFalse(isChunkLoaded(world, signLoc), "getInventory should not load the chunk when unloaded");
    }

    @Test
    void delete_forcesChunkLoad_andClearsSign() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        WorldMock world = addSimpleWorldPatched("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 108, 65, 100), new ItemStack(Material.DIRT), "sell", 8, "1"
        );
        assertNotNull(shop);
        Location signLoc = shop.getSignLocation();
        assertTrue(isChunkLoaded(world, signLoc), "Precondition: chunk should be loaded after creation");

        unloadChunk(world, signLoc);
        assertFalse(isChunkLoaded(world, signLoc), "Precondition: chunk should be unloaded for test");

        // Act: delete should force load and clear the sign text
        shop.delete();

        // Assert: chunk is now loaded
        assertTrue(isChunkLoaded(world, signLoc), "delete() should force-load the chunk to clear the sign");

        // Sign should be cleared
        Sign sign = (Sign) signLoc.getBlock().getState();
        String[] lines = sign.getLines();
        assertEquals("§cSHOP CLOSED", lines[0]); // default "deleted" sign text
        assertEquals("", lines[1]);
        assertEquals("", lines[2]);
        assertEquals("", lines[3]);

        // And the shop should be removed from the handler
        assertNull(plugin.getShopmanager().getShopBySign(signLoc), "Shop should be removed from handler after delete");
    }

    @Test
    void getContainerType_shouldNotLoadChunk_whenUnloaded() {
        // This test documents intended behavior: querying container type should not load chunks.
        // If it fails, it indicates a bug where getContainerType() is calling getBlock() without a chunk-loaded guard.
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        WorldMock world = addSimpleWorldPatched("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 112, 65, 100), new ItemStack(Material.DIRT), "sell", 8, "1"
        );
        assertNotNull(shop);
        assertTrue(isChunkLoaded(world, shop.getSignLocation()), "Precondition: chunk should be loaded after creation");

        Location signLoc = shop.getSignLocation();
        unloadChunk(world, signLoc);

        // Act
        shop.getContainerType();

        // Assert: should not have loaded the chunk
        assertFalse(isChunkLoaded(world, signLoc), "getContainerType() should not load the chunk when unloaded");
    }
}


