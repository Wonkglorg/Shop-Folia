package com.snowgears.shop.integration.features;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import com.snowgears.shop.util.PricePair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;

@Tag("integration")
public class ShopMissingBlocksTest extends BaseMockBukkitTest {

    // Note: We intentionally do not test a null signLocation, as constructing a shop
    // with a null sign is not supported by public creation APIs and would NPE during
    // construction (display initialization). The remaining tests exercise the new
    // defensive branches added to load()/updateSign().

    @Test
    void load_returnsFalse_whenSignBlockIsAir() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        Location signLoc = new Location(world, 5, 65, 5);
        world.getBlockAt(signLoc).setType(Material.AIR);

        AbstractShop shop = AbstractShop.create(signLoc, player.getUniqueId(), 10, 0, 1, false, ShopType.SELL, null);
        assertNotNull(shop);

        boolean loaded = shop.load();
        assertFalse(loaded, "Shop.load() should return false when sign block is AIR");
    }

    @Test
    void load_returnsFalse_whenSignBlockIsNotWallSign() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        Location signLoc = new Location(world, 6, 65, 5);
        world.getBlockAt(signLoc).setType(Material.OAK_SIGN); // standing sign, not a WallSign

        AbstractShop shop = AbstractShop.create(signLoc, player.getUniqueId(), 10, 0, 1, false, ShopType.SELL, null);
        assertNotNull(shop);

        boolean loaded = shop.load();
        assertFalse(loaded, "Shop.load() should return false when sign block is not a WallSign");
    }

    @Test
    void load_returnsFalse_whenChestBehindSignIsInvalid() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        Location signLoc = new Location(world, 7, 65, 5);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign data = (WallSign) signBlock.getBlockData();
        data.setFacing(BlockFace.NORTH);
        world.setBlockData(signLoc, data);

        // Place a non-container behind the sign (opposite of facing)
        Block behind = signBlock.getRelative(BlockFace.NORTH.getOppositeFace());
        behind.setType(Material.STONE);

        AbstractShop shop = AbstractShop.create(signLoc, player.getUniqueId(), 10, 0, 1, false, ShopType.SELL, null);
        assertNotNull(shop);

        boolean loaded = shop.load();
        assertFalse(loaded, "Shop.load() should return false when the block behind a valid WallSign is not a valid container");
    }

    @Test
    void updateSign_deletesShop_whenSignBlockNotASign() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Create a valid shop via chest flow helper so it is registered
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 12, 65, 12, new org.bukkit.inventory.ItemStack(Material.DIRT), "sell", 8, "1");
        assertNotNull(shop);
        Location signLoc = shop.getSignLocation();
        assertNotNull(getPlugin().getShopHandler().getShop(signLoc), "Shop should be registered");

        // Corrupt the sign block so it is no longer a Sign
        world.getBlockAt(signLoc).setType(Material.DIRT);
        world.loadChunk(signLoc.getChunk());

        // Trigger updateSign, which should catch the ClassCastException and delete the shop
        shop.updateSign(true);
        server.getScheduler().waitAsyncTasksFinished();

        assertNull(getPlugin().getShopHandler().getShop(signLoc), "Shop should be deleted when updateSign detects non-Sign block at sign location");
        assertTrue(world.getBlockAt(signLoc).getType() == Material.DIRT, "Sign block should remain dirt");
    }

    @Test
    void load_catchesException_andDeletes() throws Exception {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Create a valid shop first so construction succeeds and it is registered
        AbstractShop realShop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 44, 65, 12, new org.bukkit.inventory.ItemStack(Material.DIRT), "sell", 8, "1");
        assertNotNull(realShop);
        Location originalSignLoc = realShop.getSignLocation();
        assertNotNull(getPlugin().getShopHandler().getShop(originalSignLoc), "Shop should be registered prior to test");

        // Spy the real shop and force an exception during the load cycle
        AbstractShop shop = Mockito.spy(realShop);
        Mockito.doThrow(new RuntimeException("Simulated exception during updateStock"))
                .when(shop).updateStock();

        boolean loaded = shop.load();
        assertFalse(loaded, "Shop.load() should catch unexpected exceptions and return false");
        assertNull(getPlugin().getShopHandler().getShop(originalSignLoc), "No shop should be registered at the sign location when creation fails");
    }

    @Test
    void createShop_returnsNull_whenLoadFails_dueToMismatchedSignAndChest() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setOp(true);

        // Prepare a valid chest far from the sign
        Location chestLoc = new Location(world, 20, 65, 20);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);

        // Prepare a WallSign that does NOT face the chest (and with a non-container behind it)
        Location signLoc = new Location(world, 30, 65, 30);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign data = (WallSign) signBlock.getBlockData();
        data.setFacing(BlockFace.NORTH);
        world.setBlockData(signLoc, data);
        // Ensure behind-the-sign is not a valid container
        signBlock.getRelative(BlockFace.SOUTH).setType(Material.STONE);

        PricePair pricePair = new PricePair(10, 0);
        AbstractShop created = plugin.getShopCreationUtil().createShop(
                player,
                chestBlock,
                signBlock,
                pricePair,
                1,
                false,
                ShopType.SELL,
                BlockFace.NORTH,
                false
        );

        assertNull(created, "ShopCreationUtil.createShop should return null when shop.load() fails due to invalid/mismatched blocks");
        assertNull(plugin.getShopHandler().getShop(signLoc), "No shop should be registered at the sign location when creation fails");
    }
}


