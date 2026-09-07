/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/container/ContainerDimmerRack.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 container plumbing and a real canInteractWith; the receiver
 * list is re-read on demand rather than captured once in the constructor, so patching a
 * distro that was connected after the screen opened works; every accessor tolerates a
 * missing rack instead of dereferencing null.
 *
 * The lookup is cached for a second, because on the client it walks the whole socapex cable
 * run every time it is asked and the screen asks once per rendered frame.
 */
package dev.theatricalmod.theatrical.client.gui.container;

import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexPatch;
import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.power.TileEntityDimmerRack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Dimmer rack screen: six channels, each patchable onto two distro sockets. */
public class ContainerDimmerRack extends Container {

    /** How long a client-side device scan is reused, in ticks. */
    private static final long DEVICE_CACHE_TICKS = 20L;

    public final TileEntityDimmerRack dimmerRack;
    protected final World world;
    protected final BlockPos pos;

    private List<ISocapexReceiver> cachedDevices = Collections.emptyList();
    private long cachedAtTick = Long.MIN_VALUE;

    public ContainerDimmerRack(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        TileEntity tile = world.getTileEntity(pos);
        this.dimmerRack = tile instanceof TileEntityDimmerRack ? (TileEntityDimmerRack) tile : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return dimmerRack != null && !dimmerRack.isInvalid()
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= PacketUtil.REACH_SQ;
    }

    /** The distros this rack can currently see down its socapex cable. */
    public List<ISocapexReceiver> getDevices() {
        if (dimmerRack == null) {
            return Collections.emptyList();
        }
        long now = world.getTotalWorldTime();
        if (cachedAtTick != Long.MIN_VALUE && now - cachedAtTick < DEVICE_CACHE_TICKS) {
            return cachedDevices;
        }
        cachedAtTick = now;
        List<ISocapexReceiver> devices = dimmerRack.getSocapexProvider().getDevices(world, pos);
        cachedDevices = devices == null ? Collections.emptyList() : devices;
        return cachedDevices;
    }

    /** Which of a receiver's sockets already have a channel patched to them. */
    public int[] getChannelsForReceiver(ISocapexReceiver receiver) {
        if (dimmerRack == null || receiver == null) {
            return new int[0];
        }
        return dimmerRack.getSocapexProvider().getPatchedCables(receiver);
    }

    @Nullable
    public SocapexPatch[] getPatch(int channel) {
        return dimmerRack == null ? null : dimmerRack.getSocapexProvider().getPatch(channel);
    }

    public String getIdentifier(BlockPos receiverPos) {
        return dimmerRack == null ? "" : dimmerRack.getSocapexProvider().getIdentifier(receiverPos);
    }
}
