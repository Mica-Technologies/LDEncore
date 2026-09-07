/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 * Fork-authored file, Apache License 2.0.
 *
 * Upstream's 1.16 code opens screens through NetworkHooks.openGui and per-tile
 * INamedContainerProvider implementations. 1.12 routes every GUI through one IGuiHandler
 * keyed by an integer id, so the blocks open their GUIs through this class. The containers
 * and screens themselves are ported with the GUI phase; until then every id opens nothing.
 */
package dev.theatricalmod.theatrical.client.gui;

import net.minecraft.entity.player.EntityPlayer;
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
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
