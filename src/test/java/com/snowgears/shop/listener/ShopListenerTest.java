package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

/**
 * Very small unit-tests that exercise the build-limit calculation logic inside
 * {@link ShopListener#getBuildLimit(Player)}.
 *
 * Because these tests only need to validate the numeric result, we mock the
 * Bukkit/Spigot types with Mockito. Mockito-inline is declared as a test
 * dependency in the module POM to allow mocking of the final
 * {@link PermissionAttachmentInfo} class.
 */
class ShopListenerTest {

    private static PermissionAttachmentInfo mockPermission(String node) {
        PermissionAttachmentInfo info = Mockito.mock(PermissionAttachmentInfo.class);
        Mockito.when(info.getPermission()).thenReturn(node);
        return info;
    }

    private static ShopListener listenerWithPerms(boolean usePerms) {
        Shop plugin = Mockito.mock(Shop.class);
        Mockito.when(plugin.usePerms()).thenReturn(usePerms);
        return new ShopListener(plugin);
    }

    @Test
    @DisplayName("Use highest base limit")
    void useHighestBaseLimit() {
        ShopListener listener = listenerWithPerms(true);

        Player player = Mockito.mock(Player.class);
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(mockPermission("shop.buildlimit.2"));
        perms.add(mockPermission("shop.buildlimit.5"));
        perms.add(mockPermission("shop.other.permission")); // malformed permission, ignored
        perms.add(mockPermission("shop.buildlimit.#"));     // malformed permission, ignored
        perms.add(mockPermission("shop.buildlimit.rawr"));  // malformed permission, ignored
        perms.add(mockPermission("shop.buildlimit.10"));
        perms.add(mockPermission("shop.buildlimit.7"));
        Mockito.when(player.getEffectivePermissions()).thenReturn(perms);

        assertEquals(10, listener.getBuildLimit(player));
    }

    @Test
    @DisplayName("Base 10 with extras 2+3 results in 15")
    void baseAndExtrasAreAdded() {
        ShopListener listener = listenerWithPerms(true);

        Player player = Mockito.mock(Player.class);
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(mockPermission("shop.buildlimitextra.2"));
        perms.add(mockPermission("shop.buildlimit.10"));
        perms.add(mockPermission("shop.buildlimitextra.3"));
        Mockito.when(player.getEffectivePermissions()).thenReturn(perms);

        assertEquals(15, listener.getBuildLimit(player));
    }

    @Test
    @DisplayName("Extras without base falls back to default base 10 000")
    void extrasOnlyGetsDefaultBase() {
        ShopListener listener = listenerWithPerms(true);

        Player player = Mockito.mock(Player.class);
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(mockPermission("shop.buildlimitextra.5"));
        Mockito.when(player.getEffectivePermissions()).thenReturn(perms);

        // default (10000) without extras
        assertEquals(10000, listener.getBuildLimit(player));
    }

    @Test
    @DisplayName("Permissions disabled returns hard-coded limit")
    void permissionsDisabledReturnsDefault() {
        ShopListener listener = listenerWithPerms(false);

        Player player = Mockito.mock(Player.class);
        // Even if some permission nodes exist, they are ignored when perms are off
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(mockPermission("shop.buildlimit.1"));
        Mockito.when(player.getEffectivePermissions()).thenReturn(perms);

        assertEquals(10000, listener.getBuildLimit(player));
    }
}
