/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/power/TileEntityDimmedPowerCable.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 NBT, sync and capability plumbing; the stored power is
 * actually written to NBT (upstream read "power" but never wrote it, so a cable always
 * reloaded empty).
 */
package dev.theatricalmod.theatrical.tiles.power;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.power.ITheatricalPowerStorage;
import dev.theatricalmod.theatrical.api.capabilities.power.TheatricalPower;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dimmed power cable: holds up to 255 units and pushes them each tick towards neighbours
 * holding less, remembering for ten ticks which faces it sent to so power does not slosh
 * straight back.
 */
public class TileEntityDimmedPowerCable extends TileEntity implements ITheatricalPowerStorage, ITickable {

    public int power = 0;
    private static final int TRANSFER_RATE = 6000;

    private int ticksSinceLastSend = 0;

    private final ArrayList<EnumFacing> sendingFace = new ArrayList<>();

    public NBTTagCompound writeNBT(NBTTagCompound nbt) {
        nbt.setInteger("power", power);
        return nbt;
    }

    public void readNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("power")) {
            power = nbt.getInteger("power");
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        readNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return super.writeToNBT(writeNBT(compound));
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, writeNBT(new NBTTagCompound()));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeNBT(super.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readNBT(pkt.getNbtCompound());
    }

    public boolean isConnected(EnumFacing direction) {
        TileEntity tileEntity = world.getTileEntity(pos.offset(direction));
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity instanceof TileEntityDimmedPowerCable) {
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
        return Arrays.stream(cable.getAcceptedCables(side)).anyMatch(cableType -> cableType == CableType.DIMMED_POWER);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == TheatricalPower.CAP) {
            return facing == null || isConnected(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TheatricalPower.CAP && (facing == null || isConnected(facing))) {
            return TheatricalPower.CAP.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int energyReceived = Math.min(getMaxEnergyStored() - getEnergyStored(), Math.min(TRANSFER_RATE, maxReceive));
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
        int energyExtracted = Math.min(getEnergyStored(), Math.min(TRANSFER_RATE, maxExtract));
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
        return 255;
    }

    @Override
    public boolean canExtract() {
        return getEnergyStored() != 0;
    }

    @Override
    public boolean canReceive() {
        return getMaxEnergyStored() != getEnergyStored();
    }

    public boolean canReceiveFromFace(EnumFacing facing) {
        if (sendingFace.contains(facing)) {
            return false;
        }
        return canReceive();
    }

    public void doEnergyTransfer() {
        if (!canExtract()) {
            return;
        }
        ArrayList<ITheatricalPowerStorage> acceptors = new ArrayList<>();
        for (EnumFacing face : EnumFacing.values()) {
            BlockPos newPos = pos.offset(face);
            TileEntity tile = world.getTileEntity(newPos);
            if (tile == null) {
                continue;
            } else if (tile instanceof TileEntityDimmedPowerCable) {
                TileEntityDimmedPowerCable cable = (TileEntityDimmedPowerCable) tile;
                if (power > cable.power && cable.canReceiveFromFace(face.getOpposite())) {
                    acceptors.add(cable);
                    if (!sendingFace.contains(face)) {
                        sendingFace.add(face);
                    }
                }
            } else if (tile.hasCapability(TheatricalPower.CAP, face.getOpposite())) {
                ITheatricalPowerStorage storage = tile.getCapability(TheatricalPower.CAP, face.getOpposite());
                if (storage != null && storage.canReceive()) {
                    acceptors.add(storage);
                }
            }
        }
        for (ITheatricalPowerStorage acceptor : acceptors) {
            int drain = Math.min(power, TRANSFER_RATE);
            if (drain > 0 && acceptor.receiveEnergy(drain, true) > 0) {
                int move = acceptor.receiveEnergy(drain, false);
                extractEnergy(move, false);
            }
        }
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
        doEnergyTransfer();
    }
}
