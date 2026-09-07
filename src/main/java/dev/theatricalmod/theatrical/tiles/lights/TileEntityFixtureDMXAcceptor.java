/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/lights/TileEntityFixtureDMXAcceptor.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability and lifecycle plumbing (onLoad/invalidate for the
 * network refresh hooks).
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A fixture that is addressed on the DMX universe.
 */
public abstract class TileEntityFixtureDMXAcceptor extends TileEntityFixture implements IAcceptsCable {

    private final DMXReceiver idmxReceiver = new DMXReceiver(0, 0);

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
        idmxReceiver.setChannelCount(compound.getInteger("channelCount"));
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
}
