/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * util/CapabilityStorageProvider.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: Capability.IStorage on 1.12 works with NBTBase and EnumFacing.
 */
package dev.theatricalmod.theatrical.util;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * Generic capability storage: delegates to the instance if it is {@link INBTSerializable}.
 */
public class CapabilityStorageProvider<T> implements IStorage<T> {

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side) {
        return instance instanceof INBTSerializable ? ((INBTSerializable<NBTBase>) instance).serializeNBT() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {
        if (nbt != null && instance instanceof INBTSerializable) {
            ((INBTSerializable<NBTBase>) instance).deserializeNBT(nbt);
        }
    }
}
