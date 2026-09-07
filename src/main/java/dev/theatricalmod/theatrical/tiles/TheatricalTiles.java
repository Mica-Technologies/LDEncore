/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/TheatricalTiles.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 registers tile entity classes by name
 * (GameRegistry.registerTileEntity) rather than TileEntityTypes tied to blocks. The ids
 * are upstream's. This is also where the fixture types learn which tile they create
 * (FixtureType.setTileFactory), which upstream hard-wired in the enum.
 */
package dev.theatricalmod.theatrical.tiles;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.FixtureType;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityDMXRedstoneInterface;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityGenericFixture;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import dev.theatricalmod.theatrical.tiles.power.TileEntityDimmedPowerCable;
import dev.theatricalmod.theatrical.tiles.power.TileEntityDimmerRack;
import dev.theatricalmod.theatrical.tiles.power.TileEntityPowerCable;
import dev.theatricalmod.theatrical.tiles.power.TileEntitySocapexDistribution;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class TheatricalTiles {

    private TheatricalTiles() {
    }

    private static void register(Class<? extends TileEntity> clazz, String name) {
        GameRegistry.registerTileEntity(clazz, new ResourceLocation(TheatricalMod.MOD_ID, name));
    }

    /** Called from the block registry event. */
    public static void register() {
        register(TileEntityIntelligentFixture.class, "moving_light");
        register(TileEntityDimmedPowerCable.class, "dimmed_power_cable");
        register(TileEntityPowerCable.class, "power_cable");
        register(TileEntityArtNetInterface.class, "artnet_interface");
        register(TileEntityTestDMX.class, "test_dmx");
        register(TileEntityGenericFixture.class, "generic_light");
        register(TileEntityDimmerRack.class, "dimmer_rack");
        register(TileEntitySocapexDistribution.class, "socapex_distribution");
        register(TileEntityCable.class, "cable");
        register(TileEntityBasicLightingControl.class, "basic_lighting_desk");
        register(TileEntityDMXRedstoneInterface.class, "redstone_interface");

        FixtureType.INTELLIGENT.setTileFactory(TileEntityIntelligentFixture::new);
        FixtureType.TUNGSTEN.setTileFactory(TileEntityGenericFixture::new);
    }
}
