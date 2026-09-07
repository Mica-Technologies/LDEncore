/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/power/TheatricalPower.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: NBT API names only.
 */
package dev.theatricalmod.theatrical.api.capabilities.power;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

public class TheatricalPower implements ITheatricalPowerStorage, INBTSerializable<NBTTagCompound> {

    @CapabilityInject(ITheatricalPowerStorage.class)
    public static Capability<ITheatricalPowerStorage> CAP = null;

    private int power;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public TheatricalPower() {
        this(100, 100, 100, 0);
    }

    public TheatricalPower(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public TheatricalPower(int capacity, int maxReceive, int maxExtract, int power) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.power = Math.max(0, Math.min(capacity, power));
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("power", power);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        power = nbt.getInteger("power");
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }

        int energyReceived = Math.min(capacity - power, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            power += energyReceived;
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }

        int energyExtracted = Math.min(power, Math.min(this.maxExtract, maxExtract));
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
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }
}
