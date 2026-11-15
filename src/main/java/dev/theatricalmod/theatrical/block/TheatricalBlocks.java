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
 * 1.12 needs them registered by name before any world loads. Fork additions: the LED
 * fixtures and the room light's floor block.
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
import dev.theatricalmod.theatrical.block.led.BlockLedCeiling;
import dev.theatricalmod.theatrical.block.led.BlockLedCeilingLinear;
import dev.theatricalmod.theatrical.block.led.BlockLedFixture;
import dev.theatricalmod.theatrical.block.led.BlockLedStrip;
import dev.theatricalmod.theatrical.block.led.BlockLedTrack;
import dev.theatricalmod.theatrical.block.led.BlockLedTrackHead;
import dev.theatricalmod.theatrical.block.led.BlockLedWall;
import dev.theatricalmod.theatrical.block.led.BlockSuspensionCable;
import dev.theatricalmod.theatrical.block.light.BlockMovingLight;
import dev.theatricalmod.theatrical.block.light.BlockRoomIlluminator;
import dev.theatricalmod.theatrical.block.power.BlockDimmerRack;
import dev.theatricalmod.theatrical.block.power.BlockSocapexDistribution;
import dev.theatricalmod.theatrical.block.rigging.BlockIWB;
import dev.theatricalmod.theatrical.block.rigging.BlockTruss;
import dev.theatricalmod.theatrical.block.test.BlockTestDMX;
import dev.theatricalmod.theatrical.fixtures.TheatricalFixtures;
import dev.theatricalmod.theatrical.tiles.TheatricalTiles;
import net.minecraft.block.Block;
import static dev.theatricalmod.theatrical.block.led.LedGeometry.px;
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
    // Fork additions: LED house fixtures and the floor light they keep in Room Light mode.
    public static final BlockRoomIlluminator ROOM_ILLUMINATOR = prepare(new BlockRoomIlluminator(), "room_illuminator");
    public static final BlockLedStrip LED_STRIP = prepare(new BlockLedStrip(), "led_strip");
    // Model-pixel boxes; they must match models/block/led/*.json (the lens is drawn live, so it
    // is only in the item models).
    public static final BlockLedCeilingLinear LED_BATTEN = prepare(new BlockLedCeilingLinear(true, BlockLedFixture.LENS_DOTS,
            px(6, 13.25, 0, 10, 16, 16), px(6.5, 13.25, 0, 9.5, 14, 16)), "led_batten");
    public static final BlockLedCeilingLinear LED_LINEAR_PENDANT = prepare(new BlockLedCeilingLinear(true, BlockLedFixture.LENS_SMOOTH,
            px(5, 11, 0, 11, 16, 16), px(5.75, 11, 0, 10.25, 11.5, 16)), "led_linear_pendant");
    public static final BlockLedCeilingLinear LED_TROFFER = prepare(new BlockLedCeilingLinear(false, BlockLedFixture.LENS_SMOOTH,
            px(0, 14.5, 0, 16, 16, 16), px(3, 14.5, 1, 13, 15, 15)), "led_troffer");
    public static final BlockLedCeiling LED_PANEL = prepare(new BlockLedCeiling(BlockLedFixture.LENS_SMOOTH,
            px(0, 15, 0, 16, 16, 16), px(0.75, 15, 0.75, 15.25, 15.5, 15.25)), "led_panel");
    public static final BlockLedCeiling LED_DOWNLIGHT = prepare(new BlockLedCeiling(BlockLedFixture.LENS_SMOOTH,
            px(4, 15.25, 4, 12, 16, 12), px(5, 15.25, 5, 11, 15.5, 11)), "led_downlight");
    public static final BlockLedCeiling LED_HIGHBAY = prepare(new BlockLedCeiling(BlockLedFixture.LENS_SMOOTH,
            px(3, 10.5, 3, 13, 16, 13), px(3.5, 10.5, 3.5, 12.5, 11, 12.5)), "led_highbay");
    public static final BlockSuspensionCable SUSPENSION_CABLE = prepare(new BlockSuspensionCable(), "suspension_cable");
    // Sconces are modelled on the north wall, facing south.
    public static final BlockLedWall LED_SCONCE_SHELL = prepare(new BlockLedWall(BlockLedFixture.LENS_SMOOTH,
            px(4, 5, 0, 12, 11, 4.5), px(4.5, 10, 1, 11.5, 10.5, 4)), "led_sconce_shell");
    public static final BlockLedWall LED_SCONCE_VANITY = prepare(new BlockLedWall(BlockLedFixture.LENS_SMOOTH,
            px(1.5, 6.5, 0, 14.5, 9.5, 3.5), px(2.5, 6.5, 1.5, 13.5, 9.5, 3.5)), "led_sconce_vanity");
    public static final BlockLedWall LED_SCONCE_LANTERN = prepare(new BlockLedWall(BlockLedFixture.LENS_DOTS,
            px(5, 3, 0, 11, 13, 9), px(5.5, 4, 3.5, 10.5, 10, 8.5)), "led_sconce_lantern");
    public static final BlockLedTrack LED_TRACK = prepare(new BlockLedTrack(), "led_track");
    public static final BlockLedTrackHead LED_TRACK_HEAD = prepare(new BlockLedTrackHead(), "led_track_head");

    /** Every block, in registration order. The illuminator has no item and no creative tab. */
    public static final Block[] ALL_BLOCKS = {
            TRUSS, MOVING_LIGHT, GENERIC_LIGHT, DMX_CABLE, SOCAPEX_CABLE, DIMMED_POWER_CABLE, POWER_CABLE,
            ILLUMINATOR, ARTNET_INTERFACE, IWB, TEST_DMX, DIMMER_RACK, SOCAPEX_DISTRIBUTION,
            BASIC_LIGHTING_DESK, DMX_REDSTONE_INTERFACE, ROOM_ILLUMINATOR, LED_STRIP, LED_BATTEN,
            LED_LINEAR_PENDANT, LED_TROFFER, LED_PANEL, LED_DOWNLIGHT, LED_HIGHBAY, SUSPENSION_CABLE,
            LED_SCONCE_SHELL, LED_SCONCE_VANITY, LED_SCONCE_LANTERN, LED_TRACK, LED_TRACK_HEAD
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
        if (!(block instanceof BlockIlluminator) && !(block instanceof BlockRoomIlluminator)) {
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
