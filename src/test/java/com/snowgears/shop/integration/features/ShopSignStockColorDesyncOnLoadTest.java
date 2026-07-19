package com.snowgears.shop.integration.features;

import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

@Tag("integration")
public class ShopSignStockColorDesyncOnLoadTest extends BaseMockBukkitTest {

    @Test
    void load_shouldUpdateStockColorOnSign_whenShopHasStock() {
        ServerMock server = getServer();
        WorldMock world = addSimpleWorldPatched("world");
        PlayerMock owner = server.addPlayer();

        // Arrange: a valid wall sign with a chest behind it
        Location signLoc = new Location(world, 20, 65, 20);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign wallSign = (WallSign) signBlock.getBlockData();
        wallSign.setFacing(BlockFace.NORTH);
        world.setBlockData(signLoc, wallSign);

        Location chestLoc = signBlock.getRelative(BlockFace.NORTH.getOppositeFace()).getLocation();
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);

        // Put enough items into the chest for a positive stock value.
        // Use a clean division so we can set stockOnLoad to the exact computed value.
        int shopAmount = 8;
        int itemsInChest = 64;
        int expectedStock = itemsInChest / shopAmount; // 8
        assertTrue(expectedStock > 0, "Test precondition: expectedStock must be positive");

        Inventory inv = ((InventoryHolder) chestBlock.getState()).getInventory();
        inv.addItem(new ItemStack(Material.DIRT, itemsInChest));

        // Create the shop without loading it (mimics loadShopsFromConfig wiring order).
        AbstractShop shop = AbstractShop.create(
                signLoc,
                owner.getUniqueId(),
                10,
                0,
                shopAmount,
                false,
                ShopType.SELL,
                BlockFace.NORTH
        );
        assertNotNull(shop);

        // Reproduce problematic order:
        // 1) set item first (can render sign using default stock=0 and set signLinesRequireRefresh=false)
        shop.setItemStack(new ItemStack(Material.DIRT));

        // Let scheduled sign update run (updateSign uses a 2 tick delay)
        server.getScheduler().performTicks(3);

        Sign signState = (Sign) signLoc.getBlock().getState();
        String line0BeforeLoad = signState.getLine(0);
        assertTrue(line0BeforeLoad.startsWith("§4"),
                "Precondition: sign should be rendered as out-of-stock (red) before load; was: " + line0BeforeLoad);

        // 2) set stock from disk AFTER sign was rendered
        shop.setStockOnLoad(expectedStock);

        // 3) load should reconcile sign color to in-stock (green)
        assertTrue(shop.load(), "Shop.load() should succeed with valid sign+chest");
        assertEquals(expectedStock, shop.getStock(), "Stock should match the computed value after load()");

        // Give any follow-up scheduled tasks a chance to run
        server.getScheduler().performTicks(3);

        Sign signAfterLoad = (Sign) signLoc.getBlock().getState();
        String line0AfterLoad = signAfterLoad.getLine(0);

        // Expected behavior: sign line 1 should show in-stock color (green) when stock > 0.
        assertTrue(line0AfterLoad.startsWith("§a"),
                "Sign should be in-stock (green) after load when stock > 0; was: " + line0AfterLoad);
    }
}


