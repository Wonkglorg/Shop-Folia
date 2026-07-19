package com.snowgears.shop.hook;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

class PlotSquaredHookListenerTest {

    @Test
    void isShopSignInsidePlot_trueWhenSignIsInRegionAndWorldMatches() {
        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(10, 255, 10)
        );

        World world = mock(World.class);
        when(world.getName()).thenReturn("plotworld");
        Location signLocation = new Location(world, 5, 64, 5);

        assertTrue(PlotSquaredHookListener.isShopSignInsidePlotRegions("plotworld", Set.of(region), signLocation));
    }

    @Test
    void isShopSignInsidePlot_falseWhenWorldDoesNotMatch() {
        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(10, 255, 10)
        );

        World otherWorld = mock(World.class);
        when(otherWorld.getName()).thenReturn("otherworld");
        Location signLocation = new Location(otherWorld, 5, 64, 5);

        assertFalse(PlotSquaredHookListener.isShopSignInsidePlotRegions("plotworld", Set.of(region), signLocation));
    }

    @Test
    void isShopSignInsidePlot_falseWhenOutsideAllRegions() {
        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(10, 255, 10)
        );

        World world = mock(World.class);
        when(world.getName()).thenReturn("plotworld");
        Location signLocation = new Location(world, 100, 64, 100);

        assertFalse(PlotSquaredHookListener.isShopSignInsidePlotRegions("plotworld", Set.of(region), signLocation));
    }
}


