package com.snowgears.shop.integration.features;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@Tag("integration")
public class ShopCreationChestTest extends BaseMockBukkitTest {

    @Test
    void createUsingChestFlow_withChatSteps() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        // create the shop
        AbstractShop shop = createShop(player, world, 10, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");
        assertEquals(Material.DIRT, shop.getItemStack().getType());
    }

    AbstractShop createShop(PlayerMock player, World world, int x, int y, int z, ItemStack item, String type, int amount, String price) {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        return createShop(server, plugin, player, world, new Location(world, x, y, z), item, type, amount, price);
    }
    static AbstractShop createShop(ServerMock server, Shop plugin, PlayerMock player, World world, int x, int y, int z, ItemStack item, String type, int amount, String price) {
        return createShop(server, plugin, player, world, new Location(world, x, y, z), item, type, amount, price);
    }
    static AbstractShop createShop(ServerMock server, Shop plugin, PlayerMock player, World world, Location chestLoc, ItemStack item, String type, int amount, String price) {

        player.setOp(true);

        // Enable chest creation
        setConfig("allowCreateMethodChest", true);

        // Place chest with free space to the NORTH for sign placement
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        // Ensure the block to the NORTH is air so sign can be placed
        world.getBlockAt(chestLoc.clone().add(0, 0, -1)).setType(Material.AIR);
        // Stub sign-face calculation to avoid MockBukkit material checks
        stubCalculateBlockFaceForSign(BlockFace.NORTH);

        // Start creation by sneaking and left-clicking the chest with an item in hand
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(item);
        PlayerInteractEvent startCreate = new PlayerInteractEvent(
                player,
                Action.LEFT_CLICK_BLOCK,
                item,
                chestBlock,
                BlockFace.NORTH,
                EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(startCreate);
        server.getScheduler().performTicks(20);
        String msg = waitForNextMessage(player);
        assertEquals("§eTo set up your shop, please type your responses in chat when prompted.", msg, "Player should be sent dialog to set up shop");
        msg = waitForNextMessage(player);
        assertTrue(msg.contains("§eEnter in chat what to do with §a"), "Player should be sent dialog for shop type");
        assertTrue(msg.contains("(s)§b §7("), "Player should be sent dialog for shop type");
        msg = player.nextMessage();
        if (msg != null) {
            assertEquals("§7(Adding §eadmin §7will make the shop unlimited stock.)", msg, "Player should be sent admin dialog if player is op");
        }
        assertEquals(null, player.nextMessage(), "Player should not have any more messages");

        // Provide chat inputs to finish the creation process
        // Step 1: Shop type (SELL). Use the localized creation word to ensure parsing works.
        sendChatMessage(player, type);
        // Step 2: Item amount
        msg = waitForNextMessage(player);
        assertTrue(msg.contains("§eEnter in chat the amount of §a"), "Player should be sent dialog for amount");
        assertTrue(msg.contains("(s)§b §eyou want to sell per transaction."), "Player should be sent dialog for amount");
        sendChatMessage(player, amount + "");
        // Step 3: Price
        msg = waitForNextMessage(player);
        assertTrue(msg.contains("§eEnter in chat the price you will sell §a"), "Player should be sent dialog for price");
        sendChatMessage(player, price);
        msg = waitForNextMessage(player);
        assertTrue(msg.contains("§eYou have created a shop that sells §6"), "Player should be sent dialog for successfully setup shop");

        // Assert: shop created and initialized (attached to the chest)
        AbstractShop created = plugin.getShopHandler().getShopByChest(chestBlock);
        assertNotNull(created, "Shop should be created via chest flow");
        assertTrue(created.isInitialized(), "Shop should be initialized after chat steps");
        assertNotNull(created.getItemStack(), "Shop item should be set");
        assertEquals(null, player.nextMessage(), "Player should not have any more messages");
        assertEquals(item.getType(), created.getItemStack().getType());
        // wait for all async tasks to complete
        server.getScheduler().waitAsyncTasksFinished();
        return created;
    }

    @Test
    void preventCreationOnExistingChest_interactEarlyReturn() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Arrange: an existing shop on a chest
        AbstractShop existing = createShop(server, getPlugin(), player, world, 50, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");
        assertNotNull(existing);

        // Act: attempt to start a new creation on the same chest via interact
        player.setSneaking(true);
        ItemStack item = new ItemStack(Material.COBBLESTONE);
        player.getInventory().setItemInMainHand(item);
        PlayerInteractEvent attempt = new PlayerInteractEvent(
                player,
                Action.LEFT_CLICK_BLOCK,
                item,
                world.getBlockAt(existing.getChestLocation()),
                BlockFace.NORTH,
                EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(attempt);

        // Assert: no creation prompts/messages should be sent
        server.getScheduler().performTicks(5);
        assertNull(player.nextMessage(), "No creation messages should be sent when interacting with an existing shop chest");
    }

    @Test
    void creationTimesOutAfter30Seconds_withoutFinishing() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setOp(true);

        // Enable chest creation
        setConfig("allowCreateMethodChest", true);

        // Place chest with free space to the NORTH for sign placement
        Location chestLoc = new Location(world, 52, 65, 10);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        world.getBlockAt(chestLoc.clone().add(0, 0, -1)).setType(Material.AIR);
        stubCalculateBlockFaceForSign(BlockFace.NORTH);

        // Start creation by sneaking and left-clicking the chest with an item in hand
        player.setSneaking(true);
        ItemStack item = new ItemStack(Material.DIRT);
        player.getInventory().setItemInMainHand(item);
        PlayerInteractEvent startCreate = new PlayerInteractEvent(
                player,
                Action.LEFT_CLICK_BLOCK,
                item,
                chestBlock,
                BlockFace.NORTH,
                EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(startCreate);

        // Drain initial creation messages
        while (player.nextMessage() != null) {}

        // Ensure any async tasks triggered by scheduler are processed (timeout check is an async task)
        server.getScheduler().waitAsyncTasksFinished();

        assertEquals("§7The shop you started to create at (52, 65, 10) has timed out.", player.nextMessage(), "Player should be sent timeout message");

        // Ensure no shop was created/registered for chest
        assertNull(getPlugin().getShopHandler().getShopByChest(chestBlock), "Shop should not be created when creation times out");
    }
}

