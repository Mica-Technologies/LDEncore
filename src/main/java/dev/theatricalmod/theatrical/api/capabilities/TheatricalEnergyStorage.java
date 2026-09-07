/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/TheatricalEnergyStorage.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: NBT API names only.
 */
package dev.theatricalmod.theatrical.api.capabilities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.EnergyStorage;

/**
 * Forge Energy storage that can be saved. Used where a block exposes ordinary FE.
 */
public class TheatricalEnergyStorage extends EnergyStorage implements INBTSerializable<NBTTagCompound> {

    public TheatricalEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("energy", getEnergyStored());
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        setEnergy(nbt.getInteger("energy"));
    }
}
