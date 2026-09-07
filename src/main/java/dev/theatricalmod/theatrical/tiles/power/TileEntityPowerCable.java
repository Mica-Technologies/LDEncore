/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/power/TileEntityPowerCable.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability plumbing. Note that upstream never creates this
 * tile (both power cable blocks create the dimmed variant) and it stores no energy; it is
 * ported for completeness and registered under upstream's id.
 */
package dev.theatricalmod.theatrical.tiles.power;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class TileEntityPowerCable extends TileEntity implements ITickable, IEnergyStorage {

    private int ticksSinceLastSend = 0;

    private final ArrayList<EnumFacing> sendingFace = new ArrayList<>();

    public boolean isConnected(EnumFacing direction) {
        TileEntity tileEntity = world.getTileEntity(pos.offset(direction));
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity instanceof TileEntityPowerCable) {
            return true;
        }
        if (tileEntity.hasCapability(CapabilityEnergy.ENERGY, direction.getOpposite())) {
            return true;
        }
        if (tileEntity instanceof IAcceptsCable && canAcceptPower((IAcceptsCable) tileEntity, direction.getOpposite())) {
            return true;
        }
        return tileEntity.hasCapability(DMXReceiver.CAP, direction.getOpposite()) || tileEntity.hasCapability(DMXProvider.CAP, direction.getOpposite());
    }

    public boolean canAcceptPower(IAcceptsCable cable, EnumFacing side) {
        return Arrays.stream(cable.getAcceptedCables(side)).anyMatch(cableType -> cableType == CableType.POWER);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return facing == null || isConnected(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && (facing == null || isConnected(facing))) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        ticksSinceLastSend++;
        if (ticksSinceLastSend >= 10) {
            sendingFace.clear();
            ticksSinceLastSend = 0;
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
