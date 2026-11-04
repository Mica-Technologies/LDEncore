/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/TileEntityTheatricalBase.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12's NBT and sync plumbing (readFromNBT/writeToNBT,
 * SPacketUpdateTileEntity). Subclasses only override readNBT/getNBT, exactly as upstream.
 * The tile survives a change of its block's state (shouldRefresh); on 1.12 Forge would otherwise
 * replace it, losing everything it held.
 */
package dev.theatricalmod.theatrical.tiles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/**
 * Base tile: subclasses implement {@link #readNBT} and {@link #getNBT}, and the same data
 * is used for saving and for syncing to the client.
 */
public class TileEntityTheatricalBase extends TileEntity {

    /**
     * Keeps this tile when only the block's state changes (its facing, a powered flag).
     * Forge's default for a modded tile is to replace it on any state change, which wiped a
     * lighting desk's cues on every redstone pulse and a rack's patch when it was turned
     * with the wrench.
     */
    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                 net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public void readNBT(NBTTagCompound nbt) {
    }

    public NBTTagCompound getNBT(@Nullable NBTTagCompound nbt) {
        return nbt == null ? new NBTTagCompound() : nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        readNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return getNBT(super.writeToNBT(compound));
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getNBT(null));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return getNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readNBT(pkt.getNbtCompound());
    }

    /** Re-sends this tile's sync data to clients and marks the chunk dirty. */
    public void sync() {
        markDirty();
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }
}
