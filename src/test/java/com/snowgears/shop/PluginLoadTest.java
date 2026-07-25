package com.snowgears.shop;

import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Basic sanity check to verify the plugin can be loaded and enabled inside a MockBukkit environment.
 * <p>
 * This ensures that MockBukkit is correctly wired into the build and that the plugin’s lifecycle
 * methods do not immediately throw exceptions when run on a mock server.
 */
class PluginLoadTest extends BaseMockBukkitTest {
    @Test
    void pluginShouldEnable() {
        assertNotNull(getPlugin(), "Plugin should be loaded by MockBukkit");
        assertTrue(getPlugin().isEnabled(), "Plugin should be enabled inside MockBukkit environment");
    }
}