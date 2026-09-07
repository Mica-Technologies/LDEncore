/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/power/TileEntityDimmerRack.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 capability and lifecycle plumbing;
 *   - BUG FIX: the rack read its six DMX channels at (start address + i) from a receiver
 *     whose values are already relative to the start address, so any address other than 0
 *     read past the end and the rack put out nothing. It now reads channel i;
 *   - the rack looks for the distros wired to it every tick, before the power check rather
 *     than after it. Upstream scanned only once it had energy, so a rack that had never been
 *     powered had no idea what was connected and silently rejected every patch made in its
 *     screen -- which is the one thing you do to a rack before energising it.
 */
package dev.theatricalmod.theatrical.tiles.power;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexProvider;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Six-channel dimmer rack: DMX in, Forge Energy in, six socapex channels out, patched onto
 * the sockets of the distros found down the socapex cable.
 */
public class TileEntityDimmerRack extends TileEntityTheatricalBase implements ITickable, IEnergyStorage, IAcceptsCable {

    public static final int CHANNELS = 6;

    private final DMXReceiver dmxReceiver = new DMXReceiver(CHANNELS, 0);
    private final SocapexProvider socapexProvider = new SocapexProvider();

    private int power;

    public DMXReceiver getDmxReceiver() {
        return dmxReceiver;
    }

    public SocapexProvider getSocapexProvider() {
        return socapexProvider;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXReceiver.CAP || capability == SocapexProvider.CAP || capability == CapabilityEnergy.ENERGY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXReceiver.CAP) {
            return DMXReceiver.CAP.cast(dmxReceiver);
        }
        if (capability == SocapexProvider.CAP) {
            return SocapexProvider.CAP.cast(socapexProvider);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        dmxReceiver.setDMXStartPoint(compound.getInteger("dmxStart"));
        if (compound.hasKey("provider")) {
            socapexProvider.deserializeNBT(compound.getCompoundTag("provider"));
        }
        if (compound.hasKey("power")) {
            power = compound.getInteger("power");
        }
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound compound) {
        compound = super.getNBT(compound);
        compound.setInteger("dmxStart", dmxReceiver.getStartPoint());
        compound.setTag("provider", socapexProvider.serializeNBT());
        compound.setInteger("power", power);
        return compound;
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DIMMED_POWER, CableType.SOCAPEX, CableType.DMX};
    }

    @Override
    public void invalidate() {
        TheatricalMod.refreshNetworks(world);
        super.invalidate();
    }

    @Override
    public void validate() {
        TheatricalMod.refreshNetworks(world);
        super.validate();
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        // Always know what is wired up: patching happens before the rack is ever energised.
        socapexProvider.scanDevices(world, pos);

        int totalPower = 0;
        int[] powerChannels = new int[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            int val = dmxReceiver.getChannel(i) & 0xFF;
            totalPower += val;
            powerChannels[i] = val;
        }
        if (getEnergyStored() < 1) {
            return;
        }
        socapexProvider.receiveSocapex(powerChannels, false);
        socapexProvider.updateDevices(world, pos);
        extractEnergy(totalPower, false);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int energyReceived = Math.min(Integer.MAX_VALUE, maxReceive);
        if (!simulate) {
            power += energyReceived;
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(power, Math.min(1000, maxExtract));
        if (!simulate) {
            power -= energyExtracted;
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return power;
    }

    @Override
    public int getMaxEnergyStored() {
        return 1;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return getEnergyStored() < getMaxEnergyStored();
    }

    public int getDmxStart() {
        return dmxReceiver.getStartPoint();
    }
}
