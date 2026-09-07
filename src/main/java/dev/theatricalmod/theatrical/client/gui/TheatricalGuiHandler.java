/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 * Fork-authored file, Apache License 2.0.
 *
 * Upstream's 1.16 code opens screens through NetworkHooks.openGui and per-tile
 * INamedContainerProvider implementations. 1.12 routes every GUI through one IGuiHandler
 * keyed by an integer id, so the blocks open their GUIs through this class.
 *
 * The client half is delegated to the proxy rather than built here: this class is loaded on
 * a dedicated server, and naming a GuiContainer subclass from it would drag client-only
 * types onto the server's classpath.
 */
package dev.theatricalmod.theatrical.client.gui;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.client.gui.container.ContainerArtNetInterface;
import dev.theatricalmod.theatrical.client.gui.container.ContainerBasicLightingConsole;
import dev.theatricalmod.theatrical.client.gui.container.ContainerDMXRedstoneInterface;
import dev.theatricalmod.theatrical.client.gui.container.ContainerDimmerRack;
import dev.theatricalmod.theatrical.client.gui.container.ContainerGenericFixture;
import dev.theatricalmod.theatrical.client.gui.container.ContainerIntelligentFixture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class TheatricalGuiHandler implements IGuiHandler {

    public static final int GUI_GENERIC_FIXTURE = 0;
    public static final int GUI_INTELLIGENT_FIXTURE = 1;
    public static final int GUI_DIMMER_RACK = 2;
    public static final int GUI_ARTNET_INTERFACE = 3;
    public static final int GUI_BASIC_LIGHTING_CONSOLE = 4;
    public static final int GUI_DMX_REDSTONE_INTERFACE = 5;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        switch (id) {
            case GUI_GENERIC_FIXTURE:
                return new ContainerGenericFixture(world, pos);
            case GUI_INTELLIGENT_FIXTURE:
                return new ContainerIntelligentFixture(world, pos);
            case GUI_DIMMER_RACK:
                return new ContainerDimmerRack(world, pos);
            case GUI_ARTNET_INTERFACE:
                return new ContainerArtNetInterface(world, pos);
            case GUI_BASIC_LIGHTING_CONSOLE:
                return new ContainerBasicLightingConsole(world, pos);
            case GUI_DMX_REDSTONE_INTERFACE:
                return new ContainerDMXRedstoneInterface(world, pos);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return TheatricalMod.proxy.getClientGuiElement(id, player, world, new BlockPos(x, y, z));
    }
}
