package com.snowgears.shop.integration.features;

import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import com.snowgears.shop.util.InventoryUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@Tag("integration")
public class ShopTransactionsTest extends BaseMockBukkitTest {

    private ServerMock server;
    private World world;
    private PlayerMock owner;

    @BeforeEach
    void setup() {
        server = getServer();
        world = server.addSimpleWorld("world");
        owner = server.addPlayer();
        owner.setOp(true);
        owner.setSneaking(false);
    }

    private AbstractShop createInitializedShopAt(Location chestLoc) {
        return ShopCreationChestTest.createShop(server, getPlugin(), owner, world, chestLoc, new ItemStack(Material.DIRT), "sell", 8, "1");
    }
    
    @Test
    void sign_rightClick_other_performTransaction() {
        // Ensure mapping for transact is RIGHT_CLICK_SIGN by default
        AbstractShop shop = createInitializedShopAt(new Location(world, 54, 65, 10));
        // For this test, no need to spy, use real executeClickAction to drive TransactionHandler path
        
        PlayerMock other = server.addPlayer();
        other.setOp(false);
        setConfig("usePerms", false);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(other, Action.RIGHT_CLICK_BLOCK, other.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertEquals("§cYou do not have sufficient funds to buy from this shop.", waitForNextMessage(other));
        assertNull(owner.nextMessage(), "No chat message expected for owner");

        // add funds to other player
        other.getInventory().addItem(new ItemStack(Material.EMERALD, 10));

        event = new PlayerInteractEvent(other, Action.RIGHT_CLICK_BLOCK, other.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertEquals("§cThis shop is out of stock.", waitForNextMessage(other));
        assertEquals("§c[Shop] Your selling shop at <(54, 65, 9)> is out of stock.", waitForNextMessage(owner));
        // Shop sign should also show out of stock color/text
        assertEquals("§4§4§l[sell shop]", shop.getSignLines().getFirst());
        // add stock to shop
        shop.getContainerLocation().getChunk().load(true); // chest location is null if chunk is not loaded for MockBukkit
        shop.getInventory().addItem(new ItemStack(Material.DIRT, 8));
        shop.updateStock();
        // Shop sign should now show in stock color/text
        assertEquals("§a§a§l[sell shop]", shop.getSignLines().getFirst());

        // Make sure we setup inventories correctly and verify the current contents
        assertEquals(0, InventoryUtils.getAmount(other.getInventory(), new ItemStack(Material.DIRT)));
        assertEquals(10, InventoryUtils.getAmount(other.getInventory(), new ItemStack(Material.EMERALD)));

        assertEquals(8, InventoryUtils.getAmount(shop.getInventory(), new ItemStack(Material.DIRT)));
        assertEquals(0, InventoryUtils.getAmount(shop.getInventory(), new ItemStack(Material.EMERALD)));

        // Trigger the real purchase
        event = new PlayerInteractEvent(other, Action.RIGHT_CLICK_BLOCK, other.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertEquals(8, InventoryUtils.getAmount(other.getInventory(), new ItemStack(Material.DIRT)));
        assertEquals(9, InventoryUtils.getAmount(other.getInventory(), new ItemStack(Material.EMERALD)));
        String msg = waitForNextMessage(other);
        assertTrue(msg.startsWith("§7You bought §f8 "), "§7You bought §f8 " + msg);
        assertTrue(msg.contains("§b §7from Player0 for §a1 "), "§b §7from Player0 for §a1 " + msg);
        assertTrue(msg.contains("(s)§7."), "(s)§7." + msg);

        assertEquals(0, InventoryUtils.getAmount(shop.getInventory(), new ItemStack(Material.DIRT)));
        assertEquals(1, InventoryUtils.getAmount(shop.getInventory(), new ItemStack(Material.EMERALD)));
        String msg2 = waitForNextMessage(owner);
        assertTrue(msg2.startsWith("§7Player1 bought §f8 "), "§7Player1 bought §f8 " + msg2);
        assertTrue(msg2.contains("§b §7from you for §a1 "), "§b §7from you for §a1 " + msg2);
        assertTrue(msg2.contains("(s)§7 at §f(54, 65, 9)§7."), "(s)§7 at §f(54, 65, 9)§7." + msg2);
    }
}
