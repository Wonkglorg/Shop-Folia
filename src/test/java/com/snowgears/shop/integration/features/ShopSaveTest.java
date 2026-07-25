package com.snowgears.shop.integration.features;

import com.snowgears.shop.Shop;
import com.snowgears.shop.config.PlayerShopsConfig;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mockStatic;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Tag("integration")
public class ShopSaveTest extends BaseMockBukkitTest {

    @Test
    void saveShops_happyPath_writesYamlAndNoBackup() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Arrange: create an initialized shop for the player
        ItemStack item = new ItemStack(Material.DIRT);
        AbstractShop shop = ShopCreationChestTest.createShop(
                server,
                plugin,
                player,
                world,
                new Location(world, 10, 65, 10),
                item,
                "sell",
                8,
                "1"
        );
        assertNotNull(shop, "Shop should be created for the player");
        assertTrue(shop.isInitialized(), "Shop should be initialized after creation flow");

        // Act: explicitly save the player's shops
        int shopsSaved = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
        assertEquals(1, shopsSaved, "Exactly one shop should be saved");

        // Assert: file exists, has content, and no leftover .bak remains
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        assertTrue(playerFile.exists(), "Player YAML file should exist after saving");
        assertTrue(playerFile.length() > 0, "Player YAML file should not be empty");

        File backupFile = new File(dataDir, player.getUniqueId().toString() + ".yml.bak");
        assertFalse(backupFile.exists(), "No backup file should remain after successful save");

        // Optionally verify that the YAML contains a section for the player's UUID without deserializing Bukkit objects
        String yaml = assertDoesNotThrow(() -> Files.readString(playerFile.toPath()), "Should be able to read saved YAML as text");
        assertTrue(yaml.contains("shops:"), "YAML should contain the shops root key");
        assertTrue(yaml.contains(player.getUniqueId().toString()), "YAML should contain the player's UUID");
    }

    @Test
    void createTempFileFails_preservesExistingFile_andReturnsError() throws Exception {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Create one shop so saveShops attempts to write
        ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 40, 65, 40), new ItemStack(Material.DIRT), "sell", 1, "1");

        // Overwrite target with sentinel original content to verify preservation on failure
        File dataDir = new File(plugin.getDataFolder(), "Data");
        assertTrue(dataDir.mkdirs() || dataDir.exists());
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        Files.writeString(playerFile.toPath(), "ORIGINAL_CONTENT");
        String original = Files.readString(playerFile.toPath());

        // Mock Files.createTempFile to throw, other Files calls should pass through
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, invocation -> {
            if (invocation.getMethod().getName().equals("createTempFile")) {
                throw new IOException("Simulated: createTempFile failed");
            }
            return invocation.callRealMethod();
        })) {
            int result = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
            assertEquals(-2, result, "Should return -2 when temp file cannot be created, but file was left untouched (and/or restored successfully from backup)");
        }

        // Assert the existing file is unchanged and no .bak exists
        assertTrue(playerFile.exists());
        assertEquals(original, Files.readString(playerFile.toPath()));
        assertFalse(Files.exists(playerFile.toPath().resolveSibling(playerFile.getName() + ".bak")));
    }

    @Test
    void yamlSaveThrows_preservesExistingFile_andReturnsError() throws Exception {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Create one shop so saveShops attempts to write
        ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 41, 65, 41), new ItemStack(Material.DIRT), "sell", 1, "1");

        // Overwrite target with sentinel original content to verify preservation on failure
        File dataDir = new File(plugin.getDataFolder(), "Data");
        assertTrue(dataDir.mkdirs() || dataDir.exists());
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        Files.writeString(playerFile.toPath(), "ORIGINAL_CONTENT");
        String original = Files.readString(playerFile.toPath());

        // Allow createTempFile to succeed but force YamlConfiguration.save to throw
        try (MockedConstruction<org.bukkit.configuration.file.YamlConfiguration> yamlMock =
                     Mockito.mockConstruction(org.bukkit.configuration.file.YamlConfiguration.class,
                             (mock, context) -> {
                                 Mockito.doThrow(new IOException("Simulated: yaml save failed"))
                                         .when(mock).save(Mockito.any(File.class));
                             })) {
            int result = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
            assertEquals(-2, result, "Should return -2 when YAML save fails, but file was left untouched (and/or restored successfully from backup)");
        }

        // Assert the existing file is unchanged and no .bak exists
        assertTrue(playerFile.exists());
        assertEquals(original, Files.readString(playerFile.toPath()));
        assertFalse(Files.exists(playerFile.toPath().resolveSibling(playerFile.getName() + ".bak")));
    }

    @Test
    void totalDataLoss_disablesPlugin_andReturnsMinusFive() throws Exception {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Arrange: create a shop so saveShops will attempt to write
        ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 42, 65, 42), new ItemStack(Material.DIRT), "sell", 1, "1");

        // Pre-compute target and backup paths
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        Path target = playerFile.toPath();
        Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");

        // Ensure starting state has no files
        Files.deleteIfExists(target);
        Files.deleteIfExists(backup);

        // Mock: ATOMIC_MOVE fails → fallback; fallback temp->target move fails; no backup exists; final check sees both missing
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, invocation -> {
            String name = invocation.getMethod().getName();
            if (name.equals("createTempFile")) {
                return invocation.callRealMethod();
            }
            if (name.equals("move")) {
                Object[] args = invocation.getArguments();
                // If atomic move is requested, fail to force fallback
                for (Object o : args) {
                    if (o == StandardCopyOption.ATOMIC_MOVE) {
                        throw new IOException("Simulated: atomic move unsupported");
                    }
                }
                // In fallback, fail temp -> target move to trigger restore branch
                Path src = (Path) args[0];
                Path dst = (Path) args[1];
                if (src.getFileName().toString().endsWith(".tmp") && dst.equals(target)) {
                    throw new IOException("Simulated: fallback move failed");
                }
                return invocation.callRealMethod();
            }
            if (name.equals("exists")) {
                Path p = (Path) invocation.getArguments()[0];
                if (p.equals(target) || p.equals(backup)) {
                    return false; // simulate total loss: neither exists
                }
                return invocation.callRealMethod();
            }
            if (name.equals("deleteIfExists")) {
                return invocation.callRealMethod();
            }
            return invocation.callRealMethod();
        })) {
            int result = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
            assertEquals(-5, result, "Should return -5 on total data loss");
            server.getScheduler().waitAsyncEventsFinished();
            server.getScheduler().waitAsyncTasksFinished();
            assertFalse(plugin.isEnabled(), "Plugin should be disabled to prevent cascading data loss");
        }

        // Final state: both files should be absent
        assertFalse(Files.exists(target));
        assertFalse(Files.exists(backup));
    }
    @Test
    void saveShops_yamlContainsExpectedStructure_forMultipleShops() {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        PlayerMock player2 = server.addPlayer();

        // Arrange: create two initialized shops for the player
        AbstractShop shop1 = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 20, 65, 20), new ItemStack(Material.DIRT), "sell", 4, "2"
        );
        // Create a shop for the second player to make sure they are saved seperately
        AbstractShop player2Shop = ShopCreationChestTest.createShop(
                server, plugin, player2, world,
                new Location(world, 19, 65, 19), new ItemStack(Material.OAK_LOG), "sell", 4, "1"
        );

        AbstractShop shop2 = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 21, 65, 21), new ItemStack(Material.STONE), "sell", 8, "3"
        );
        // Make sure the shops are both created
        assertTrue(shop1.isInitialized() && shop2.isInitialized() && player2Shop.isInitialized());
        assertEquals(2, plugin.getShopHandler().getNumberOfShops(player.getUniqueId()), "Two shops should exist for the player");
        assertEquals(1, plugin.getShopHandler().getNumberOfShops(player2.getUniqueId()), "One shop should exist for the second player");

        // Assert: YAML structure contains both shop indexes and expected keys
        // Shop will have been saved automatically when created, so no need to explicitly trigger the save function.
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        File player2File = new File(dataDir, player2.getUniqueId().toString() + ".yml");
        assertTrue(playerFile.exists());
        assertTrue(player2File.exists());
        String yamlText = assertDoesNotThrow(() -> Files.readString(playerFile.toPath()));
        String yamlText2 = assertDoesNotThrow(() -> Files.readString(player2File.toPath()));
        String playerUuid = player.getUniqueId().toString();
        String player2Uuid = player2.getUniqueId().toString();

        // Parse YAML safely using SnakeYAML (no Bukkit deserialization)
        @SuppressWarnings("unchecked")
        var root1 = (java.util.Map<String, Object>) new Yaml().load(yamlText);
        @SuppressWarnings("unchecked")
        var root2 = (java.util.Map<String, Object>) new Yaml().load(yamlText2);

        // shops map exists
        @SuppressWarnings("unchecked")
        var shops1 = (java.util.Map<String, Object>) getAtPath(root1, "shops");
        @SuppressWarnings("unchecked")
        var shops2 = (java.util.Map<String, Object>) getAtPath(root2, "shops");
        assertNotNull(shops1, "shops should exist in player1 YAML");
        assertNotNull(shops2, "shops should exist in player2 YAML");
        assertTrue(shops1.containsKey(playerUuid), "player1 file should have player1 UUID");
        assertFalse(shops1.containsKey(player2Uuid), "player1 file should not have player2 UUID");
        assertTrue(shops2.containsKey(player2Uuid), "player2 file should have their UUID");
        assertFalse(shops2.containsKey(playerUuid), "player2 file should not have player1 UUID");

        // Player1 shop 1 assertions
        assertEquals(4, getAtPath(root1, "shops." + playerUuid + ".1.amount"));
        assertEquals(2.0, ((Number) getAtPath(root1, "shops." + playerUuid + ".1.price")).doubleValue(), 0.0001);
        assertEquals("sell", getAtPath(root1, "shops." + playerUuid + ".1.type"));
        assertEquals("DIRT", getAtPath(root1, "shops." + playerUuid + ".1.item.type"));
        // Player1 shop 2 assertions
        assertEquals(8, getAtPath(root1, "shops." + playerUuid + ".2.amount"));
        assertEquals(3.0, ((Number) getAtPath(root1, "shops." + playerUuid + ".2.price")).doubleValue(), 0.0001);
        assertEquals("sell", getAtPath(root1, "shops." + playerUuid + ".2.type"));
        assertEquals("STONE", getAtPath(root1, "shops." + playerUuid + ".2.item.type"));

        // Player2 only one shop (index 1)
        assertEquals(4, getAtPath(root2, "shops." + player2Uuid + ".1.amount"));
        assertEquals(1.0, ((Number) getAtPath(root2, "shops." + player2Uuid + ".1.price")).doubleValue(), 0.0001);
        assertEquals("sell", getAtPath(root2, "shops." + player2Uuid + ".1.type"));
        assertEquals("OAK_LOG", getAtPath(root2, "shops." + player2Uuid + ".1.item.type"));
    }

    @Test
    void createThenDeleteShop_andSave_removesPlayerSaveFile() throws Exception {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Create one shop
        AbstractShop shop = ShopCreationChestTest.createShop(
                server, plugin, player, world,
                new Location(world, 30, 65, 30), new ItemStack(Material.DIRT), "sell", 1, "1"
        );
        assertTrue(shop.isInitialized());

        // Save and verify file exists
        int savedFirst = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
        assertEquals(1, savedFirst);
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        assertTrue(playerFile.exists(), "Player save should exist after save");

        // Delete the shop by breaking the sign
        PlayerSimulation sim = new PlayerSimulation(player);
        sim.simulateBlockBreak(shop.getSignLocation().getBlock());
        // Ensure the shop is actually gone in handler
        assertNull(plugin.getShopHandler().getShop(shop.getSignLocation()), "Shop should be removed after breaking sign");

        // Save again; file should be removed because player has no shops now
        int savedSecond = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
        assertEquals(-1, savedSecond, "File should be deleted when player has no shops");
        assertFalse(playerFile.exists(), "Player save should be removed when player has no shops");
    }

    private static Object getAtPath(java.util.Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (current == null) return null;
            if (!(current instanceof java.util.Map)) return null;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) current;
            current = map.get(part);
        }
        return current;
    }
    @Test
    void saveShops_withNoShops_deletesExistingFile() throws IOException {
        Shop plugin = getPlugin();
        // Use a random UUID that has no shops registered in the handler
        UUID randomPlayer = UUID.randomUUID();

        // Pre-create a file as if it existed from a prior run
        File dataDir = new File(plugin.getDataFolder(), "Data");
        assertTrue(dataDir.mkdirs() || dataDir.exists(), "Data directory should exist");
        File playerFile = new File(dataDir, randomPlayer.toString() + ".yml");
        assertTrue(playerFile.createNewFile() || playerFile.exists(), "Precondition: player YAML should exist before save");
        assertTrue(playerFile.exists(), "Precondition: player YAML exists");

        // Act: saving for a player with no shops should delete the file
        int shopsSaved = PlayerShopsConfig.saveShops(randomPlayer, true);
        assertEquals(-1, shopsSaved, "No shops should be saved for a player with no shops");
        assertFalse(playerFile.exists(), "Player YAML file should be deleted when no shops exist");
    }


    @Test
    void atomicMoveUnsupported_fallbackSucceeds_andBackupRemoved() throws IOException {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Arrange: create a shop so saveShops writes a YAML
        ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 10, 65, 10), new ItemStack(Material.DIRT), "sell", 1, "5");

        // Pre-create an existing file to force backup path
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        assertTrue(dataDir.mkdirs() || dataDir.exists());
        Files.writeString(playerFile.toPath(), "OLD_DATA", StandardCharsets.UTF_8);
        assertTrue(playerFile.exists());

        // Simulate: ATOMIC_MOVE fails, other moves act normal
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, invocation -> {
            if (invocation.getMethod().getName().equals("move")) {
                Object[] args = invocation.getArguments();
                // args: source, target, options...
                for (Object o : args) {
                    if (o == StandardCopyOption.ATOMIC_MOVE) {
                        throw new IOException("Simulated: atomic move unsupported");
                    }
                }
            }
            return invocation.callRealMethod();
        })) {
            int savedCount = PlayerShopsConfig.saveShops(player.getUniqueId(), true);
            assertEquals(1, savedCount);
        }

        // Assert: final file exists and backup was removed
        Path target = playerFile.toPath();
        Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");
        assertTrue(Files.exists(target), "Target file should exist after fallback");
        assertFalse(Files.exists(backup), "Backup should be deleted after successful fallback");

        String content = Files.readString(target);
        assertTrue(content.contains("shops:"), "New YAML content should be present");
        assertFalse(content.contains("OLD_DATA"), "Old sentinel content should have been replaced");
    }

    @Test
    void fallbackMoveFails_backupRestored_andTargetPreserved() throws IOException {
        ServerMock server = getServer();
        Shop plugin = getPlugin();
        World world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // Arrange: create a shop so saveShops will attempt to write
        ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 12, 65, 12), new ItemStack(Material.DIRT), "sell", 1, "5");

        // Pre-create original target file with sentinel content
        File dataDir = new File(plugin.getDataFolder(), "Data");
        File playerFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        assertTrue(dataDir.mkdirs() || dataDir.exists());
        Files.writeString(playerFile.toPath(), "ORIGINAL_CONTENT", StandardCharsets.UTF_8);
        String original = Files.readString(playerFile.toPath());

        // Simulate: ATOMIC_MOVE fails; backup move succeeds; temp->target fails; restore backup succeeds
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, invocation -> {
            String name = invocation.getMethod().getName();
            if (name.equals("move")) {
                Object[] args = invocation.getArguments();
                Path source = (Path) args[0];
                Path target = (Path) args[1];
                for (Object o : args) {
                    if (o == StandardCopyOption.ATOMIC_MOVE) {
                        throw new IOException("Simulated: atomic move unsupported");
                    }
                }
                // temp -> target move should fail
                if (source.getFileName().toString().endsWith(".tmp") && target.getFileName().toString().endsWith(".yml")) {
                    throw new IOException("Simulated: failure moving temp into place");
                }
            }
            return invocation.callRealMethod();
        })) {
            // int saved = plugin.getShopHandler().saveShops(player.getUniqueId(), true);
            // assertEquals(-1, saved);
            ShopCreationChestTest.createShop(server, plugin, player, world, new Location(world, 10, 65, 12), new ItemStack(Material.COBBLESTONE), "sell", 64, "2");
        }

        // Assert: target restored to original content and no lingering .bak file
        Path target = playerFile.toPath();
        Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");
        assertTrue(Files.exists(target), "Target should exist after restore");
        assertFalse(Files.exists(backup), "Backup should not remain after restore");
        String finalContent = Files.readString(target);
        assertEquals(original, finalContent, "Original content should be restored on failure");
    }
}

