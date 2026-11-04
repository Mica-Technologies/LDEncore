/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/power/TileEntityPowerCable.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: upstream never created this tile and it stored and passed no
 * energy, so the power cable carried nothing. It is now the power cable's tile and a real
 * Forge Energy conduit: energy pushed into it goes straight to the moving heads and dimmer
 * racks on its network (see PowerGrid), and every tick it also pulls from any storage or
 * generator beside it that lets energy be taken.
 */
package dev.theatricalmod.theatrical.tiles.power;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class TileEntityPowerCable extends TileEntityPowerConduit implements ITickable {

    /** The most FE a cable asks one neighbour for per tick; the network's demand is the real limit. */
    private static final int PULL_MAX = 1_000_000;

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        for (EnumFacing face : EnumFacing.values()) {
            BlockPos next = pos.offset(face);
            if (!world.isBlockLoaded(next)) {
                continue;
            }
            TileEntity tile = world.getTileEntity(next);
            if (tile == null || tile instanceof TileEntityPowerConduit || PowerGrid.isConsumer(tile)
                    || !tile.hasCapability(CapabilityEnergy.ENERGY, face.getOpposite())) {
                continue;
            }
            IEnergyStorage source = tile.getCapability(CapabilityEnergy.ENERGY, face.getOpposite());
            if (source == null || !source.canExtract()) {
                continue;
            }
            int available = source.extractEnergy(PULL_MAX, true);
            if (available <= 0) {
                continue;
            }
            int wanted = PowerGrid.deliver(world, pos, available, true);
            if (wanted <= 0) {
                return;
            }
            PowerGrid.deliver(world, pos, source.extractEnergy(wanted, false), false);
        }
    }
}
