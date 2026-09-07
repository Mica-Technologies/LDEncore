/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/interfaces/TileEntityDMXRedstoneInterface.java (Theatrical Team, Apache License
 * 2.0); the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 NBT, capability and lifecycle plumbing; the GUI comes with
 * the GUI phase.
 */
package dev.theatricalmod.theatrical.tiles.interfaces;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One DMX channel out as a redstone level 0-15.
 */
public class TileEntityDMXRedstoneInterface extends TileEntityTheatricalBase implements ITickable, IAcceptsCable {

    private final DMXReceiver idmxReceiver = new DMXReceiver(1, 0);
    private int ticks, prevSignal = 0;

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound compound) {
        NBTTagCompound tag = super.getNBT(compound);
        tag.setInteger("channelCount", idmxReceiver.getChannelCount());
        tag.setInteger("channelStartPoint", idmxReceiver.getStartPoint());
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        super.readNBT(compound);
        idmxReceiver.setChannelCount(Math.max(1, compound.getInteger("channelCount")));
        idmxReceiver.setDMXStartPoint(compound.getInteger("channelStartPoint"));
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXReceiver.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXReceiver.CAP) {
            return DMXReceiver.CAP.cast(idmxReceiver);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidate() {
        TheatricalMod.refreshDmxNetwork(world);
        super.invalidate();
    }

    @Override
    public void onLoad() {
        TheatricalMod.refreshDmxNetwork(world);
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DMX, CableType.DIMMED_POWER};
    }

    public IDMXReceiver getIdmxReceiver() {
        return idmxReceiver;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        ticks++;
        if (ticks >= 10) {
            if (getRedstoneSignal() != prevSignal) {
                prevSignal = getRedstoneSignal();
                world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock(), false);
            }
            ticks = 0;
        }
    }

    public int getRedstoneSignal() {
        return (int) ((Byte.toUnsignedInt(idmxReceiver.getChannel(0)) * 15) / 255F);
    }
}
