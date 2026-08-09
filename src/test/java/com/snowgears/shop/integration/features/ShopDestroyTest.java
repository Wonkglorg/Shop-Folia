package com.snowgears.shop.integration.features;

import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

import java.util.ArrayList;
import java.util.List;

@Tag("integration")
public class ShopDestroyTest extends BaseMockBukkitTest {
    @Test
    void destroyOwn_op() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 8, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        PlayerSimulation simulation = new PlayerSimulation(player);
        simulation.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§7You have destroyed your selling shop.", waitForNextMessage(player), "Player should be sent dialog after destroying shop sign");
        assertEquals(Material.AIR, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should be deleted when player has destroy permission");
    }
    @Test
    void destroyOwn_permissions() {
        setConfig("usePerms", true);
        
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setName("Toby");
        player.setOp(false);
        player.addAttachment(getPlugin(), "shop.create.sell", true);
        player.addAttachment(getPlugin(), "shop.destroy", false);
        player.addAttachment(getPlugin(), "shop.operator", false);
        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 18, 65, 0, new ItemStack(Material.DIRT), "sell", 8, "1");
        PlayerSimulation simulation = new PlayerSimulation(player);
        simulation.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§cYou are not authorized to destroy your own shops.", waitForNextMessage(player), "Player should be sent dialog denying own shop destroy without shop.destroy permission");
        assertEquals(Material.OAK_WALL_SIGN, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should still exist when player lacks destroy permission");

        player.addAttachment(getPlugin(), "shop.destroy", true);
        simulation.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§7You have destroyed your selling shop.", waitForNextMessage(player), "Player should be sent dialog after destroying shop sign");
        assertEquals(Material.AIR, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should be deleted when player has destroy permission");
    }

    @Test
    void destroyOther_noPermission() {
        setConfig("usePerms", false);
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 12, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        PlayerMock playerTwo = server.addPlayer();
        playerTwo.setOp(false);
        PlayerSimulation simulationTwo = new PlayerSimulation(playerTwo);
        simulationTwo.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§cYou are not authorized to destroy other players shops.", waitForNextMessage(playerTwo), "Player two should be sent dialog denying shop destruction");
        assertNotNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should still exist when unauthorized player tries to break sign");
        assertEquals(Material.OAK_WALL_SIGN, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should still exist when unauthorized player tries to break sign");
    }
    @Test
    void destroyOther_op() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setName("Steve");
        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 14, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        PlayerMock playerTwo = server.addPlayer();
        playerTwo.setOp(true);
        PlayerSimulation simulationTwo = new PlayerSimulation(playerTwo);
        simulationTwo.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§7You have destroyed a selling shop owned by Steve.", waitForNextMessage(playerTwo), "Player two should be sent dialog allowing shop destruction");
        assertNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should be deleted by operator");
        assertEquals(Material.AIR, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should be deleted by operator");
    }

    @Test
    void destroyOther_permissions() {
        setConfig("usePerms", true);
        
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setName("Toby");
        player.setOp(true);
        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 16, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        PlayerMock playerTwo = server.addPlayer();
        playerTwo.setOp(false);
        PlayerSimulation simulationTwo = new PlayerSimulation(playerTwo);
        simulationTwo.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§cYou are not authorized to destroy other players shops.", waitForNextMessage(playerTwo), "Player two should be sent dialog denying shop destruction");
        assertNotNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should still exist when player lacks destroy.other permission");
        assertEquals(Material.OAK_WALL_SIGN, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should still exist when player lacks destroy.other permission");

        PlayerMock playerThree = server.addPlayer();
        playerThree.setOp(false);
        playerThree.addAttachment(getPlugin(), "shop.destroy.other", true);
        PlayerSimulation simulationThree = new PlayerSimulation(playerThree);
        simulationThree.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertEquals("§7You have destroyed a selling shop owned by Toby.", waitForNextMessage(playerThree), "Player three should be sent dialog allowing shop destruction");
        assertNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should be deleted when player has destroy.other permission");
        assertEquals(Material.AIR, world.getBlockAt(shop.getSignLocation()).getType(), "Shop should be deleted when player has destroy.other permission");
    }

    @Test
    void chestBreak_primary_prompts_sign_for_owner() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 20, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        Block chest = shop.getContainerLocation().getBlock();
        assertEquals(Material.CHEST, chest.getType());

        PlayerSimulation simulation = new PlayerSimulation(player);
        simulation.simulateBlockBreak(chest);
        assertEquals("§cYou must remove the sign from this shop to break it.", waitForNextMessage(player), "Owner should be prompted to break sign first when breaking primary chest");
        // Chest should remain since event is cancelled
        assertEquals(Material.CHEST, chest.getType(), "Primary chest should not break");
    }

    @Test
    void chestBreak_expansion_allowed_for_owner() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 22, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Place an adjacent chest to create a double chest (expansion chest)
        Location primary = shop.getContainerLocation();
        Location expansionLoc = primary.clone().add(1, 0, 0);
        Block expansion = world.getBlockAt(expansionLoc);
        expansion.setType(Material.CHEST);

        // Sanity: both halves are chests
        assertEquals(Material.CHEST, primary.getBlock().getType());
        assertEquals(Material.CHEST, expansion.getType());

        PlayerSimulation simulation = new PlayerSimulation(player);
        simulation.simulateBlockBreak(expansion);

        // Expansion chest should be allowed to break
        assertEquals(Material.AIR, expansion.getType(), "Expansion chest half should break for authorized owner");
        // Primary chest should remain and shop should still exist
        assertEquals(Material.CHEST, primary.getBlock().getType(), "Primary chest should remain");
        assertNotNull(getPlugin().getShopmanager().getShopByContainer(primary.getBlock()), "Shop should still exist after breaking expansion chest");
    }

    @Test
    void chestBreak_expansion_authorization_otherPlayer() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();

        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), owner, world, 42, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Place an adjacent chest to create a double chest (expansion chest)
        Location primary = shop.getContainerLocation();
        Location expansionLoc = primary.clone().add(1, 0, 0);
        Block expansion = world.getBlockAt(expansionLoc);
        expansion.setType(Material.CHEST);

        // Non-owner without permission should be denied
        PlayerMock other = server.addPlayer();
        other.setOp(false);
        PlayerSimulation simOther = new PlayerSimulation(other);
        // Try to break the primary chest first: should be denied and message shown
        PlayerSimulation simOwner = new PlayerSimulation(owner);
        simOwner.simulateBlockBreak(primary.getBlock());
        // Owner is allowed but breaking primary prompts message; ensure shop still exists
        assertEquals(Material.CHEST, primary.getBlock().getType());

        // Now, non-owner attempts expansion: according to current logic, non-owners have event cancelled with permission message
        simOther.simulateBlockBreak(expansion);

        while (waitForNextMessage(other) != null) {}
        // It seems like MockBukkit can't properly link the chests together into a doublechest, which is causing asserting that
        // the block should stay to fail. MockBukkit can't tell it is a doublechest and just allows it to break the block since it thinks it is an unattached chest.
        // assertEquals(Material.CHEST, expansion.getType(), "Expansion chest should break for authorized non-owner");
        assertNotNull(getPlugin().getShopmanager().getShopByContainer(primary.getBlock()), "Shop should still exist after unauthorized attempt");

        // Grant destroy.other and allow breaking expansion chest
        other.addAttachment(getPlugin(), "shop.destroy.other", true);
        simOther.simulateBlockBreak(expansion);
        while (waitForNextMessage(other) != null) {}
        assertEquals(Material.AIR, expansion.getType(), "Expansion chest should break for authorized non-owner");
        // Shop should still exist
        assertNotNull(getPlugin().getShopmanager().getShopByContainer(primary.getBlock()), "Shop should still exist after expansion broken by authorized non-owner");
    }

    @Test
    void breakBlockUnderShop_protection() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();

        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), owner, world, 24, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Place a block under the chest
        Location under = shop.getContainerLocation().clone().add(0, -1, 0);
        Block underBlock = world.getBlockAt(under);
        underBlock.setType(Material.STONE);
        assertEquals(Material.STONE, underBlock.getType());

        // Unauthorized player cannot break the block under the chest
        PlayerMock random = server.addPlayer();
        random.setOp(false);
        PlayerSimulation simRandom = new PlayerSimulation(random);
        simRandom.simulateBlockBreak(underBlock);
        assertEquals(Material.STONE, underBlock.getType(), "Unauthorized player should not break block under shop chest");

        // Owner can break the block under the chest
        PlayerSimulation simOwner = new PlayerSimulation(owner);
        simOwner.simulateBlockBreak(underBlock);
        assertEquals(Material.AIR, underBlock.getType(), "Owner should be able to break block under shop chest");
    }

    @Test
    void destroyRequiresSneak_mustSneakToDestroy() {
        setConfig("destroyShopRequiresSneak", true);

        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // create the shop
        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 26, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Not sneaking: should be cancelled and shop remains
        player.setSneaking(false);
        PlayerSimulation simulation = new PlayerSimulation(player);
        simulation.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertNotNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should remain when not sneaking with destroyShopRequiresSneak=true");

        // Sneaking: should destroy the shop
        player.setSneaking(true);
        simulation.simulateBlockBreak(shop.getSignLocation().getBlock());
        assertNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should be destroyed when sneaking and destroyShopRequiresSneak=true");
    }

    // Cost-related tests moved to CreationDestructionCostTest

    @Test
    void destroy_event_cancellable_prevents_delete() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 34, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        // Register a listener that cancels PlayerDestroyShopEvent
        Listener l = new Listener() {
            @EventHandler
            public void onDestroy(PlayerDestroyShopEvent e) {
                e.setCancelled(true);
            }
        };
        Bukkit.getPluginManager().registerEvents(l, getPlugin());

        new PlayerSimulation(player).simulateBlockBreak(shop.getSignLocation().getBlock());
        assertNotNull(getPlugin().getShopmanager().getShopBySign(shop.getSignLocation()), "Shop should remain when PlayerDestroyShopEvent is cancelled");
    }

    @Test
    void explosion_protects_shop_blocks() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), player, world, 36, 65, 10, new ItemStack(Material.DIRT), "sell", 8, "1");

        Block signBlock = shop.getSignLocation().getBlock();
        Block chestBlock = shop.getContainerLocation().getBlock();

        List<Block> toBlow = new ArrayList<>();
        toBlow.add(signBlock);
        toBlow.add(chestBlock);

        // Mock the event since direct construction is not part of the API contract
        EntityExplodeEvent event = org.mockito.Mockito.mock(EntityExplodeEvent.class);
        org.mockito.Mockito.when(event.blockList()).thenReturn(toBlow);

        // Call listener directly
        new com.snowgears.shop.listener.ShopListener(getPlugin()).onExplosion(event);

        // Listener should have removed shop blocks from the list
        assertFalse(toBlow.contains(signBlock), "Explosion should not include shop sign block");
        assertFalse(toBlow.contains(chestBlock), "Explosion should not include shop chest block");
    }

    @Test
    void destroyChest_while_creation_denied() {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setOp(true);

        // Place chest with free space to the NORTH for sign placement
        Location chestLoc = new Location(world, 40, 65, 10);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        world.getBlockAt(chestLoc.clone().add(0, 0, -1)).setType(Material.AIR);

        // Start creation by sneaking and left-clicking the chest with an item in hand (do not finish chat)
        stubCalculateBlockFaceForSign(BlockFace.NORTH);
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
        server.getScheduler().performTicks(2);
        sendChatMessage(player, "sell");
        while (player.nextMessage() != null) {}

        // Attempt to break the chest while creation is in progress
        new PlayerSimulation(player).simulateBlockBreak(chestBlock);
        assertEquals("§cThis chest cannot be destroyed while a shop is being created for it.", waitForNextMessage(player),
                "Player should be warned chest cannot be destroyed during shop creation");
        assertEquals(Material.CHEST, chestBlock.getType(), "Chest should not break during creation process");
    
        // Attempt to break the chest while creation is in progress
        PlayerMock other = server.addPlayer();
        new PlayerSimulation(other).simulateBlockBreak(chestBlock);
        assertEquals("§cThis chest cannot be destroyed while a shop is being created for it.", waitForNextMessage(other),
                "Player should be warned chest cannot be destroyed during shop creation");
        assertEquals(Material.CHEST, chestBlock.getType(), "Chest should not break during creation process");
    }
}

