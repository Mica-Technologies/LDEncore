/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/TileEntityCable.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: invalidate() is 1.12's remove(); the cable type is saved so it
 * survives a reload (upstream set it only from createTileEntity, so a loaded cable never
 * flagged the right network).
 */
package dev.theatricalmod.theatrical.tiles;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * The tile behind DMX and socapex cables. It carries no state of consequence; it exists so
 * placing or breaking a cable flags the matching world network for a re-walk.
 */
public class TileEntityCable extends TileEntity {

    private CableType type = CableType.NONE;

    public void setCableType(CableType cableType) {
        type = cableType;
    }

    public CableType getCableType() {
        return type;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("cableType")) {
            type = CableType.byIndex(compound.getInteger("cableType"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("cableType", type.getIndex());
        return super.writeToNBT(compound);
    }

    @Override
    public void validate() {
        super.validate();
        updateWorldNetworks();
    }

    @Override
    public void invalidate() {
        updateWorldNetworks();
        super.invalidate();
    }

    private void updateWorldNetworks() {
        if (!hasWorld()) {
            return;
        }
        if (type == CableType.SOCAPEX) {
            TheatricalMod.refreshSocapexNetwork(world);
        } else if (type == CableType.DMX) {
            TheatricalMod.refreshDmxNetwork(world);
        }
    }
}
