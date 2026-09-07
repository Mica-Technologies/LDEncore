/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/CableSide.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: NBT API names only (NBTTagCompound, setIntArray/hasKey). readNBT
 * also fills slots the tag does not cover with NONE instead of leaving them null, which
 * would have thrown on the first getIndex() call.
 */
package dev.theatricalmod.theatrical.api;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;

/**
 * The set of cable types occupying one face of a block: up to five, one per slot.
 */
public class CableSide {

    public static final int SLOTS = 5;

    private CableType[] types;

    public CableSide() {
        this.types = new CableType[SLOTS];
        Arrays.fill(this.types, CableType.NONE);
    }

    public CableType[] getTypes() {
        return types;
    }

    public void setTypes(CableType[] types) {
        this.types = types;
    }

    public void setSlotToType(CableType toType, int slot) {
        if (!hasTypeInSlot(slot)) {
            types[slot] = toType;
        }
    }

    public int getTotalTypes() {
        int count = 0;
        for (CableType type : types) {
            if (type.getIndex() != CableType.NONE.getIndex()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasTypeInSlot(int i) {
        return types[i].getIndex() != CableType.NONE.getIndex();
    }

    public boolean hasAnyType(CableType[] types) {
        for (CableType type : types) {
            if (type != CableType.NONE) {
                if (this.hasType(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getSlotForType(CableType type) {
        for (int i = 0; i < SLOTS; i++) {
            if (types[i].getIndex() == type.getIndex()) {
                return i;
            }
        }
        return -1;
    }

    public CableType getFirstType() {
        for (CableType type : types) {
            if (type != CableType.NONE) {
                return type;
            }
        }
        return null;
    }

    public boolean hasType(CableType type) {
        for (CableType type1 : types) {
            if (type1.getIndex() == type.getIndex()) {
                return true;
            }
        }
        return false;
    }

    public boolean addType(CableType type) {
        if (!hasType(type)) {
            int availableSlot = -1;
            for (int i = 0; i < types.length; i++) {
                if (types[i].getIndex() == CableType.NONE.getIndex()) {
                    availableSlot = i;
                    break;
                }
            }
            if (availableSlot != -1) {
                types[availableSlot] = type;
                return true;
            }
        }
        return false;
    }

    public boolean removeType(CableType type) {
        if (hasType(type)) {
            int foundSlot = -1;
            for (int i = 0; i < types.length; i++) {
                if (types[i].getIndex() == type.getIndex()) {
                    foundSlot = i;
                    break;
                }
            }
            if (foundSlot != -1) {
                types[foundSlot] = CableType.NONE;
                return true;
            }
        }
        return false;
    }

    public NBTTagCompound getNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        int[] indices = new int[SLOTS];
        for (int i = 0; i < this.types.length; i++) {
            indices[i] = this.types[i].getIndex();
        }
        tag.setIntArray("types", indices);
        return tag;
    }

    public static CableSide readNBT(NBTTagCompound tag) {
        CableSide side = new CableSide();
        CableType[] cableTypes = new CableType[SLOTS];
        Arrays.fill(cableTypes, CableType.NONE);
        if (tag.hasKey("types")) {
            int[] indices = tag.getIntArray("types");
            for (int i = 0; i < Math.min(indices.length, SLOTS); i++) {
                cableTypes[i] = CableType.byIndex(indices[i]);
            }
        }
        side.setTypes(cableTypes);
        return side;
    }
}
