/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/TheatricalClient.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the client @SidedProxy. It registers item models (1.12 needs
 * ModelLoader.setCustomModelResourceLocation per item; 1.16 finds them by name) and applies
 * a DMX provider's universe when the server sends it, binds the tile-entity and entity
 * renderers, and declares which blocks render on the cutout layer (1.16 sets that through
 * RenderTypeLookup; 1.12 asks the block). Texture stitching and the baking of the fixture part
 * models live in client/model/FixtureModels, because 1.12 has no addSpecialModel. The Art-Net
 * polling comes with its phase of the port.
 */
package dev.theatricalmod.theatrical.client;

import dev.theatricalmod.theatrical.TheatricalCommon;
import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
import dev.theatricalmod.theatrical.block.TheatricalBlocks;
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import dev.theatricalmod.theatrical.client.gui.container.ContainerArtNetInterface;
import dev.theatricalmod.theatrical.client.gui.container.ContainerBasicLightingConsole;
import dev.theatricalmod.theatrical.client.gui.container.ContainerDMXRedstoneInterface;
import dev.theatricalmod.theatrical.client.gui.container.ContainerDimmerRack;
import dev.theatricalmod.theatrical.client.gui.container.ContainerGenericFixture;
import dev.theatricalmod.theatrical.client.gui.container.ContainerIntelligentFixture;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenArtNetInterface;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenBasicLightingConsole;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenDMXRedstoneInterface;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenDimmerRack;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenGenericFixture;
import dev.theatricalmod.theatrical.client.gui.screen.ScreenIntelligentFixture;
import dev.theatricalmod.theatrical.client.tile.TileEntityRendererBasicLightingDesk;
import dev.theatricalmod.theatrical.entity.FallingLightEntity;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityGenericFixture;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import dev.theatricalmod.theatrical.network.SendArtNetToServerPacket;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import ch.bildspur.artnet.ArtNetClient;
import dev.theatricalmod.theatrical.items.TheatricalItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID, value = Side.CLIENT)
public class TheatricalClient extends TheatricalCommon {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGenericFixture.class, new TileEntityFixtureRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityIntelligentFixture.class, new TileEntityFixtureRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBasicLightingControl.class, new TileEntityRendererBasicLightingDesk());
        RenderingRegistry.registerEntityRenderingHandler(FallingLightEntity.class, FallingLightRenderer::new);
    }

    /**
     * How often the owner's client reads its Art-Net input, in ticks.
     *
     * CHANGED FROM UPSTREAM: upstream read and sent every other tick, ten packets a second per
     * interface, whether or not anything had changed. The read still happens at that rate --
     * Art-Net is a live feed and a lighting cue should not visibly lag -- but an unchanged
     * universe is only sent once a second rather than ten times, which is the common case
     * between cues. It is still sent on that slow beat rather than not at all, because the
     * server pushes a universe down the cables only when one arrives, and a fixture wired up
     * while the feed is holding still would otherwise never be given its values.
     */
    private static final int ARTNET_POLL_TICKS = 2;
    /** How long an unchanged universe may go unsent, in milliseconds. */
    private static final long ARTNET_RESEND_INTERVAL_MS = 1000L;

    private final java.util.Map<BlockPos, byte[]> lastArtNetSent = new java.util.HashMap<>();
    private final java.util.Map<BlockPos, Long> lastArtNetSendTime = new java.util.HashMap<>();
    private final java.util.Map<BlockPos, Integer> artNetTicks = new java.util.HashMap<>();
    /** When a universe with anything in it last arrived, per interface. Drives the status line. */
    private static final java.util.Map<BlockPos, Long> LAST_ARTNET_DATA = new java.util.HashMap<>();

    /**
     * How long ago this interface last saw a non-empty Art-Net universe, in milliseconds, or -1
     * if it never has. Fork-authored: upstream showed nothing at all, and "is it even arriving"
     * is the first thing anyone setting up Art-Net needs to know.
     */
    public static long millisSinceArtNetData(BlockPos pos) {
        Long last = LAST_ARTNET_DATA.get(pos);
        return last == null ? -1L : System.currentTimeMillis() - last;
    }

    /**
     * Reads Art-Net on the owner's client and forwards it to the server.
     *
     * CHANGED FROM UPSTREAM: upstream compared the interface's owner against
     * Minecraft.getSession().getPlayerID() through a UUID adapter that throws on a malformed
     * id; this compares against the player entity's own UUID. Upstream also ran this from the
     * tile's tick in common code, which named a client-only class on the server.
     */
    @Override
    public void pollArtNet(TileEntityArtNetInterface tile) {
        UUID owner = tile.getPlayer();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (owner == null || player == null || !owner.equals(player.getUniqueID())) {
            return;
        }
        BlockPos pos = tile.getPos();
        int ticks = artNetTicks.getOrDefault(pos, 0) + 1;
        if (ticks < ARTNET_POLL_TICKS) {
            artNetTicks.put(pos, ticks);
            return;
        }
        artNetTicks.put(pos, 0);

        ArtNetClient client = TheatricalMod.getArtNetManager().getClient(tile.getIp());
        if (client == null) {
            return;
        }
        byte[] data;
        try {
            data = client.readDmxData(tile.getSubnet(), tile.getUniverse());
        } catch (Exception e) {
            // A read can fail on a socket that has just gone away; the next tick tries again.
            return;
        }
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (byte b : data) {
            if (b != 0) {
                LAST_ARTNET_DATA.put(pos, now);
                break;
            }
        }
        Long lastSent = lastArtNetSendTime.get(pos);
        boolean unchanged = Arrays.equals(data, lastArtNetSent.get(pos));
        if (unchanged && lastSent != null && now - lastSent < ARTNET_RESEND_INTERVAL_MS) {
            return;
        }
        lastArtNetSent.put(pos, data.clone());
        lastArtNetSendTime.put(pos, now);
        TheatricalNetworkHandler.MAIN.sendToServer(new SendArtNetToServerPacket(pos, data));
    }

    @Override
    public World getClientWorld() {
        return Minecraft.getMinecraft().world;
    }

    /**
     * How often a provider re-walks its cable run on the client, in milliseconds.
     *
     * CHANGED FROM UPSTREAM: upstream called refreshDevices() on every universe packet, which
     * threw the cached device list away and walked the whole DMX cable run again -- for a
     * provider updating every tick, twenty full network walks a second per provider, on the
     * render thread. Nothing on the client invalidates that cache when a cable is added or
     * removed, which is why upstream refreshed unconditionally; this keeps that correctness by
     * rescanning on a timer instead, so rewiring still shows up promptly while a steady stream
     * of DMX no longer re-walks anything.
     */
    private static final long DEVICE_RESCAN_INTERVAL_MS = 1000L;

    private final java.util.Map<BlockPos, Long> lastDeviceScan = new java.util.HashMap<>();

    @Override
    public void handleProviderDMXUpdate(BlockPos pos, byte[] data) {
        World world = getClientWorld();
        if (world == null || data == null || !world.isBlockLoaded(pos)) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null || !tile.hasCapability(DMXProvider.CAP, null)) {
            return;
        }
        IDMXProvider provider = tile.getCapability(DMXProvider.CAP, null);
        if (provider == null) {
            return;
        }
        provider.getUniverse(world).setDmxChannels(data);

        long now = System.currentTimeMillis();
        Long last = lastDeviceScan.get(pos);
        if (last == null || now - last >= DEVICE_RESCAN_INTERVAL_MS) {
            lastDeviceScan.put(pos, now);
            provider.refreshDevices();
        }
        // Pushes the universe into every receiver on the run, scanning first if it has to.
        provider.updateDevices(world, pos);
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        switch (id) {
            case TheatricalGuiHandler.GUI_GENERIC_FIXTURE:
                return new ScreenGenericFixture(new ContainerGenericFixture(world, pos));
            case TheatricalGuiHandler.GUI_INTELLIGENT_FIXTURE:
                return new ScreenIntelligentFixture(new ContainerIntelligentFixture(world, pos));
            case TheatricalGuiHandler.GUI_DIMMER_RACK:
                return new ScreenDimmerRack(new ContainerDimmerRack(world, pos));
            case TheatricalGuiHandler.GUI_ARTNET_INTERFACE:
                return new ScreenArtNetInterface(new ContainerArtNetInterface(world, pos));
            case TheatricalGuiHandler.GUI_BASIC_LIGHTING_CONSOLE:
                return new ScreenBasicLightingConsole(new ContainerBasicLightingConsole(world, pos));
            case TheatricalGuiHandler.GUI_DMX_REDSTONE_INTERFACE:
                return new ScreenDMXRedstoneInterface(new ContainerDMXRedstoneInterface(world, pos));
            default:
                return null;
        }
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (Item item : TheatricalItems.ALL_ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
        // The illuminator is invisible and has no blockstate file; tell the model loader not
        // to look for one, or it logs a missing model for each of its sixteen light levels.
        ModelLoader.setCustomStateMapper(TheatricalBlocks.ILLUMINATOR, block -> Collections.emptyMap());
    }
}
