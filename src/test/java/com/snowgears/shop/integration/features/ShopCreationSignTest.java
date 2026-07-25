package com.snowgears.shop.integration.features;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.CreationWord;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import com.snowgears.shop.util.ShopMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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

import java.util.ArrayList;
import java.util.List;

@Tag("integration")
public class ShopCreationSignTest extends BaseMockBukkitTest {

    @Test
    void createUsingSign() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();

        // Arrange: world & player
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.setOp(true);

        // Ensure sign creation enabled; display is disabled and economy mocked by base
        setConfig("allowCreateMethodSign", true);

        // Place the wall sign and chest behind it
        Location signLoc = new Location(world, 5, 65, 5);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_WALL_SIGN);
        WallSign data = (WallSign) signBlock.getBlockData();
        data.setFacing(BlockFace.NORTH); // chest on WEST
        world.setBlockData(signBlock.getLocation(), data); // Setting the sign direction does not seem to be working with MockBukkit, the default is NORTH though.
        Location chestLoc = signBlock.getRelative(BlockFace.NORTH.getOppositeFace()).getLocation();
        
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);

        // Fire SignChangeEvent with proper lines
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(Shop.getPlugin().getSettingsConfig().getCreationWord(CreationWord.SHOP)));
        lines.add(Component.text("1"));      // amount
        lines.add(Component.text("10"));     // price
        lines.add(Component.text(Shop.getPlugin().getSettingsConfig().getCreationWord(CreationWord.SELL)));
        SignChangeEvent signEvent = new SignChangeEvent(signBlock, (Player) player, lines, Side.FRONT);
        server.getPluginManager().callEvent(signEvent);

        // Assert player is sent dialog to set up shop
        assertEquals("§6Now just hit the sign with the item you want to sell to other players!", waitForNextMessage(player));
        assertNull(player.nextMessage());

        // Assert shop created and registered
        AbstractShop shop = plugin.getShopHandler().getShop(signLoc);
        assertNotNull(shop, "Shop should be created via sign event");
        assertFalse(shop.isInitialized(), "Shop should not be initialized yet");

        // Initialize via left-click on sign with an item in hand
        ItemStack toInit = new ItemStack(Material.DIAMOND);
        player.getInventory().setItemInMainHand(toInit);
        PlayerInteractEvent initEvent = new PlayerInteractEvent(
                player,
                Action.LEFT_CLICK_BLOCK,
                toInit,
                signBlock,
                BlockFace.WEST,
                EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(initEvent);

        assertTrue(waitForNextMessage(player).contains("§eYou have created a shop that sells §6"));
        assertNull(player.nextMessage());

        assertTrue(shop.isInitialized(), "Shop should be initialized after left-click with item");
    }
}

