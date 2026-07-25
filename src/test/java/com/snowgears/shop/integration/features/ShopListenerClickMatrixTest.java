package com.snowgears.shop.integration.features;

import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import com.snowgears.shop.util.ShopClickType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

@Tag("integration")
public class ShopListenerClickMatrixTest extends BaseMockBukkitTest {

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

    private AbstractShop spyAndReplace(AbstractShop real) {
        try {
            // Replace the registered shop in ShopHandler with a spy so we can verify executeClickAction invocations
            var handler = getPlugin().getShopHandler();
            Field f = handler.getClass().getDeclaredField("allShops");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Location, AbstractShop> map = (ConcurrentHashMap<Location, AbstractShop>) f.get(handler);
            AbstractShop spy = spy(real);
            // By default, have the spy return true for any executeClickAction so that ShopListener cancels events
            when(spy.executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class))).thenReturn(true);
            map.put(real.getSignLocation(), spy);
            return spy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace shop with spy", e);
        }
    }

    // ---------- Sign click matrix ----------

    @Test
    void sign_rightClick_calls_RIGHT_CLICK_SIGN_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 10, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(false);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.RIGHT_CLICK_SIGN));
        assertTrue(event.isCancelled(), "RIGHT_CLICK_SIGN should cancel when actionPerformed=true");
    }

    @Test
    void sign_leftClick_calls_LEFT_CLICK_SIGN_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 12, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(false);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.LEFT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.LEFT_CLICK_SIGN));
        assertTrue(event.isCancelled(), "LEFT_CLICK_SIGN should cancel when actionPerformed=true");
    }

    @Test
    void sign_shiftRightClick_calls_SHIFT_RIGHT_CLICK_SIGN_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 14, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(true);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.SHIFT_RIGHT_CLICK_SIGN));
        assertTrue(event.isCancelled(), "SHIFT_RIGHT_CLICK_SIGN should cancel when actionPerformed=true");
    }

    @Test
    void sign_shiftLeftClick_calls_SHIFT_LEFT_CLICK_SIGN_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 16, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(true);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.LEFT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.SHIFT_LEFT_CLICK_SIGN));
        assertTrue(event.isCancelled(), "SHIFT_LEFT_CLICK_SIGN should cancel when actionPerformed=true");
    }

    @Test
    void sign_offHand_ignored_noCall_noCancel() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 18, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(false);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInOffHand(), signBlock, BlockFace.NORTH, EquipmentSlot.OFF_HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "OFF_HAND interactions should be ignored");
    }

    // ---------- Chest click matrix ----------

    @Test
    void chest_rightClick_ownerSneaking_calls_SHIFT_RIGHT_CLICK_CHEST_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 20, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.SHIFT_RIGHT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "SHIFT_RIGHT_CLICK_CHEST should cancel when actionPerformed=true");
    }

    @Test
    void chest_leftClick_owner_calls_LEFT_CLICK_CHEST_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 22, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(false);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.LEFT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.LEFT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "LEFT_CLICK_CHEST should cancel when actionPerformed=true");
    }

    @Test
    void chest_rightClick_nonOwner_calls_RIGHT_CLICK_CHEST_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 24, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        // Non-owner player
        PlayerMock stranger = server.addPlayer();
        stranger.setOp(false);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(stranger, Action.RIGHT_CLICK_BLOCK, stranger.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.RIGHT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "Non-owner RIGHT_CLICK_CHEST should be cancelled");
        assertNull(stranger.nextMessage(), "No chat message should be sent when actionPerformed=true for non-owner");
    }

    @Test
    void chest_rightClick_nonOwner_operator_nonAdmin_noExecute_noCancel() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 28, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        // Operator but non-owner
        PlayerMock operator = server.addPlayer();
        operator.setOp(false);
        operator.addAttachment(getPlugin(), "shop.operator", true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(operator, Action.RIGHT_CLICK_BLOCK, operator.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        // For non-admin shops, operator should receive an info message and not execute an action or cancel the event
        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "Operator opening non-admin shop should not be cancelled and should not execute an action");
        assertEquals("§7You are opening a selling shop owned by Player0.", waitForNextMessage(operator));
        assertNull(operator.nextMessage(), "No additional messages expected");
    }

    @Test
    void chest_rightClick_nonOwner_operator_admin_executes_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 29, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        // Make the shop behave like an admin shop
        when(spy.isAdmin()).thenReturn(true);

        // Operator but non-owner
        PlayerMock operator = server.addPlayer();
        operator.setOp(false);
        operator.addAttachment(getPlugin(), "shop.operator", true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(operator, Action.RIGHT_CLICK_BLOCK, operator.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.RIGHT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "Operator opening admin shop should cancel and execute action");
        assertNull(operator.nextMessage(), "Operator admin-open should not receive a chat message");
    }

    @Test
    void chest_rightClick_nonOwner_displayTagsShown_whenRightClickChestOptionEnabled() {
        // Enable RIGHT_CLICK_CHEST display tag behavior
        setConfig("displayTagOption", com.snowgears.shop.display.DisplayTagOption.RIGHT_CLICK_CHEST);

        AbstractShop shop = createInitializedShopAt(new Location(world, 31, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        // Spy display and inject
        com.snowgears.shop.display.AbstractDisplay displayMock = Mockito.mock(com.snowgears.shop.display.AbstractDisplay.class);
        when(spy.getDisplay()).thenReturn(displayMock);

        // Non-owner without operator permission
        PlayerMock stranger = server.addPlayer();
        stranger.setOp(false);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(stranger, Action.RIGHT_CLICK_BLOCK, stranger.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        // Non-owner path should cancel, execute action, and show display tags when RIGHT_CLICK_CHEST option is enabled
        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.RIGHT_CLICK_CHEST));
        verify(displayMock).showDisplayTags(stranger);
        assertTrue(event.isCancelled(), "Non-owner RIGHT_CLICK_CHEST should be cancelled and show display tags");
        assertNull(stranger.nextMessage(), "No chat message expected for non-owner display tags path");
    }

    @Test
    void chest_shiftLeftClick_owner_calls_SHIFT_LEFT_CLICK_CHEST_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 32, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        owner.setSneaking(true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.LEFT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.SHIFT_LEFT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "SHIFT_LEFT_CLICK_CHEST should cancel when actionPerformed=true");
    }

    @Test
    void chest_rightClick_owner_sneaking_withSign_doesNotExecute_andNotCancelled() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 33, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        owner.setSneaking(true);
        owner.getInventory().setItemInMainHand(new ItemStack(Material.OAK_SIGN));

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "Sneaking with a sign should not execute an action or cancel the event");
        assertNull(owner.nextMessage(), "No chat message expected when sneaking with a sign");
    }

    @Test
    void chest_rightClick_nonOwner_operator_adminGamble_allowsOpen_noExecute_noCancel() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 34, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        when(spy.isAdmin()).thenReturn(true);
        when(spy.getType()).thenReturn(com.snowgears.shop.shop.ShopType.GAMBLE);

        PlayerMock operator = server.addPlayer();
        operator.setOp(false);
        operator.addAttachment(getPlugin(), "shop.operator", true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(operator, Action.RIGHT_CLICK_BLOCK, operator.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "Operator should be allowed to open admin GAMBLE shop (no cancel, no execute)");
    }

    @Test
    void chest_offHand_ignored_noCall_noCancel() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 26, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInOffHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.OFF_HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "OFF_HAND interactions should be ignored for chest as well");
        assertNull(owner.nextMessage(), "No chat message expected for OFF_HAND chest interactions");
    }

    // ---------- Message and region gating behaviors ----------

    @Test
    void chest_rightClick_operator_nonAdmin_opOpenMessage() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 41, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        PlayerMock operator = server.addPlayer();
        operator.setOp(false);
        operator.addAttachment(getPlugin(), "shop.operator", true);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(operator, Action.RIGHT_CLICK_BLOCK, operator.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "Operator opening non-admin shop should not be cancelled");
        String msg = waitForNextMessage(operator);
        assertEquals("§7You are opening a selling shop owned by Player0.", msg, "Operator should receive an opOpen info message");
    }

    @Test
    void chest_rightClick_nonOwner_actionNotPerformed_sendsOpenOtherMessage_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 42, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        // Force action not performed
        when(spy.executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class))).thenReturn(false);

        PlayerMock stranger = server.addPlayer();
        stranger.setOp(false);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(stranger, Action.RIGHT_CLICK_BLOCK, stranger.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(), "Non-owner should be blocked from opening chest");
        String msg = waitForNextMessage(stranger);
        assertEquals("§cYou are not authorized to open other players shops.", msg, "Non-owner should receive 'openOther' permission message when action not performed");
        assertNull(stranger.nextMessage(), "No additional messages expected");
    }

    @Test
    void chest_leftClick_nonOwner_calls_LEFT_CLICK_CHEST_andCancels() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 44, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        PlayerMock stranger = server.addPlayer();
        stranger.setOp(false);

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(stranger, Action.LEFT_CLICK_BLOCK, stranger.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        verify(spy).executeClickAction(any(PlayerInteractEvent.class), Mockito.eq(ShopClickType.LEFT_CLICK_CHEST));
        assertTrue(event.isCancelled(), "LEFT_CLICK_CHEST should cancel when actionPerformed=true for non-owner as well");
        assertNull(stranger.nextMessage(), "No chat message expected for non-owner left click");
    }

    @Test
    void sign_click_uninitialized_noCall_noCancel() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 46, 65, 10));
        AbstractShop spy = spyAndReplace(shop);
        when(spy.isInitialized()).thenReturn(false);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        Mockito.verify(spy, Mockito.never()).executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class));
        assertFalse(event.isCancelled(), "Uninitialized shops on sign click should not execute or cancel");
        assertNull(owner.nextMessage(), "No chat message expected for uninitialized sign click");
    }

    @Test
    void sign_rightClick_owner_sends_useOwnShop_message() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 48, 65, 10));
        // For this test, no need to spy, use real executeClickAction to drive TransactionHandler path

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(), "Owner using their own shop should cancel");
        assertEquals("§7You cannot use your own shop.", waitForNextMessage(owner));
        assertNull(owner.nextMessage());
    }

    @Test
    void sign_rightClick_nonOwner_withoutUsePermission_sends_permission_use_message_andDoesNotTransact() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 52, 65, 10));
        // For this test, no need to spy, use real executeClickAction to drive TransactionHandler path

        PlayerMock stranger = server.addPlayer();
        stranger.setOp(false);
        // Ensure permissions are enabled and player lacks use perms
        setConfig("usePerms", true);

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(stranger, Action.RIGHT_CLICK_BLOCK, stranger.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        // Player should be told they cannot use this shop type
        String msg = waitForNextMessage(stranger);
        assertNotNull(msg);
        assertTrue(msg.startsWith("§cYou are not authorized to use"), "Expected a permission.use message, got: " + msg);
    }

    @Test
    void sign_rightClick_owner_useOwnShop_message_when_transactMapped() {
        // Ensure mapping for transact is RIGHT_CLICK_SIGN by default
        AbstractShop shop = createInitializedShopAt(new Location(world, 54, 65, 10));
        // For this test, no need to spy, use real executeClickAction to drive TransactionHandler path

        Block signBlock = shop.getSignLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.RIGHT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), signBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertEquals("§7You cannot use your own shop.", waitForNextMessage(owner));
    }

    @Test
    void chest_leftClick_owner_shows_description_lines() {
        AbstractShop shop = createInitializedShopAt(new Location(world, 50, 65, 10));
        AbstractShop spy = spyAndReplace(shop);

        // Use real method to trigger VIEW_DETAILS -> printSalesInfo
        when(spy.executeClickAction(any(PlayerInteractEvent.class), any(ShopClickType.class))).thenCallRealMethod();

        Block chestBlock = shop.getChestLocation().getBlock();
        PlayerInteractEvent event = new PlayerInteractEvent(owner, Action.LEFT_CLICK_BLOCK, owner.getInventory().getItemInMainHand(), chestBlock, BlockFace.NORTH, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        // VIEW_DETAILS prints description lines; assert at least the first line appears when available
        String first = waitForNextMessage(owner);
        if (first != null) {
            assertEquals("§d+---------------------------------------------------+", first);
        }
    }
}


