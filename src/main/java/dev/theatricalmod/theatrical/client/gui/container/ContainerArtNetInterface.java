/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/container/ContainerArtNetInterface.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 containers take no ContainerType and are built by the mod's
 * IGuiHandler; canInteractWith checks the player is still in reach of the block and that the
 * tile is still there, where upstream always answered true and left a screen usable after
 * its block was broken.
 */
package dev.theatricalmod.theatrical.client.gui.container;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Art-Net interface screen: universe and listen address. */
public class ContainerArtNetInterface extends Container {

    public final TileEntityArtNetInterface blockEntity;
    protected final World world;
    protected final BlockPos pos;

    public ContainerArtNetInterface(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.blockEntity = asTile(world, pos);
    }

    @Nullable
    private static TileEntityArtNetInterface asTile(World world, BlockPos pos) {
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityArtNetInterface ? (TileEntityArtNetInterface) tile : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return blockEntity != null && !blockEntity.isInvalid()
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= PacketUtil.REACH_SQ;
    }
}
