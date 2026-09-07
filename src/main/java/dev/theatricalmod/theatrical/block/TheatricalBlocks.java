/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/TheatricalBlocks.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: eager static instances registered from RegistryEvent.Register
 * instead of a DeferredRegister; the shared block "properties" become the prepare() helper
 * (anvil material, hardness 3, pickaxe). The tile entities register here too, because
 * 1.12 needs them registered by name before any world loads.
 */
package dev.theatricalmod.theatrical.block;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.block.cables.BlockCable;
import dev.theatricalmod.theatrical.block.cables.BlockDimmedPowerCable;
import dev.theatricalmod.theatrical.block.cables.BlockPowerCable;
import dev.theatricalmod.theatrical.block.control.BlockBasicLightingControl;
import dev.theatricalmod.theatrical.block.interfaces.BlockArtNetInterface;
import dev.theatricalmod.theatrical.block.interfaces.BlockDMXRedstoneInterface;
import dev.theatricalmod.theatrical.block.light.BlockGenericFixture;
import dev.theatricalmod.theatrical.block.light.BlockIlluminator;
import dev.theatricalmod.theatrical.block.light.BlockMovingLight;
import dev.theatricalmod.theatrical.block.power.BlockDimmerRack;
import dev.theatricalmod.theatrical.block.power.BlockSocapexDistribution;
import dev.theatricalmod.theatrical.block.rigging.BlockIWB;
import dev.theatricalmod.theatrical.block.rigging.BlockTruss;
import dev.theatricalmod.theatrical.block.test.BlockTestDMX;
import dev.theatricalmod.theatrical.fixtures.TheatricalFixtures;
import dev.theatricalmod.theatrical.tiles.TheatricalTiles;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
public final class TheatricalBlocks {

    public static final float HARDNESS = 3F;
    public static final float RESISTANCE = 3F;

    public static final BlockTruss TRUSS = prepare(new BlockTruss(), "truss");
    public static final BlockMovingLight MOVING_LIGHT = prepare(new BlockMovingLight(), "moving_light");
    public static final BlockGenericFixture GENERIC_LIGHT = prepare(new BlockGenericFixture(TheatricalFixtures.FRESNEL), "generic_light");
    public static final BlockCable DMX_CABLE = prepare(new BlockCable(CableType.DMX), "dmx_cable");
    public static final BlockCable SOCAPEX_CABLE = prepare(new BlockCable(CableType.SOCAPEX), "socapex_cable");
    public static final BlockDimmedPowerCable DIMMED_POWER_CABLE = prepare(new BlockDimmedPowerCable(), "dimmed_power_cable");
    public static final BlockPowerCable POWER_CABLE = prepare(new BlockPowerCable(), "power_cable");
    public static final BlockIlluminator ILLUMINATOR = prepare(new BlockIlluminator(), "illuminator");
    public static final BlockArtNetInterface ARTNET_INTERFACE = prepare(new BlockArtNetInterface(), "artnet_interface");
    public static final BlockIWB IWB = prepare(new BlockIWB(), "iwb");
    public static final BlockTestDMX TEST_DMX = prepare(new BlockTestDMX(), "test_dmx");
    public static final BlockDimmerRack DIMMER_RACK = prepare(new BlockDimmerRack(), "dimmer_rack");
    public static final BlockSocapexDistribution SOCAPEX_DISTRIBUTION = prepare(new BlockSocapexDistribution(), "socapex_distribution");
    public static final BlockBasicLightingControl BASIC_LIGHTING_DESK = prepare(new BlockBasicLightingControl(), "basic_lighting_desk");
    public static final BlockDMXRedstoneInterface DMX_REDSTONE_INTERFACE = prepare(new BlockDMXRedstoneInterface(), "redstone_interface");

    /** Every block, in registration order. The illuminator has no item and no creative tab. */
    public static final Block[] ALL_BLOCKS = {
            TRUSS, MOVING_LIGHT, GENERIC_LIGHT, DMX_CABLE, SOCAPEX_CABLE, DIMMED_POWER_CABLE, POWER_CABLE,
            ILLUMINATOR, ARTNET_INTERFACE, IWB, TEST_DMX, DIMMER_RACK, SOCAPEX_DISTRIBUTION,
            BASIC_LIGHTING_DESK, DMX_REDSTONE_INTERFACE
    };

    private TheatricalBlocks() {
    }

    /** Applies upstream's shared block properties: registry name, lang key, tab, anvil-like toughness. */
    private static <T extends Block> T prepare(T block, String name) {
        block.setRegistryName(TheatricalMod.MOD_ID, name);
        block.setTranslationKey(TheatricalMod.MOD_ID + "." + name);
        block.setHardness(HARDNESS);
        block.setResistance(RESISTANCE);
        block.setHarvestLevel("pickaxe", 0);
        if (!(block instanceof BlockIlluminator)) {
            block.setCreativeTab(TheatricalMod.THEATRICAL_TAB);
        }
        return block;
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(ALL_BLOCKS);
        TheatricalTiles.register();
    }
}
