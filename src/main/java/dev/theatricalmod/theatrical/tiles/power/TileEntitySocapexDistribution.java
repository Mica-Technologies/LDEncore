/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/power/TileEntitySocapexDistribution.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability plumbing; the facing comes from
 * BlockDirectional.FACING; the socket-to-side mapping is derived from the facing on demand
 * rather than cached when the position is set (the block state is not always available at
 * that moment on 1.12); the channels are saved to NBT.
 */
package dev.theatricalmod.theatrical.tiles.power;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.power.ITheatricalPowerStorage;
import dev.theatricalmod.theatrical.api.capabilities.power.TheatricalPower;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexReceiver;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Socapex distro: the multicore comes in on the facing side; each of the other five sides
 * is a socket that feeds dimmed power to whatever is there.
 */
public class TileEntitySocapexDistribution extends TileEntity implements IAcceptsCable, ISocapexReceiver, ITickable {

    public static final int CHANNELS = 6;
    public static final int SOCKETS = 5;

    private int[] channels = new int[CHANNELS];

    public EnumFacing getFacing() {
        IBlockState state = world != null ? world.getBlockState(pos) : null;
        if (state != null && state.getPropertyKeys().contains(BlockDirectional.FACING)) {
            return state.getValue(BlockDirectional.FACING);
        }
        return EnumFacing.NORTH;
    }

    /** Socket index (0-4) of a side, or -1 for the input side. */
    public int getDirectionalIndex(EnumFacing direction) {
        EnumFacing facing = getFacing();
        int i = 0;
        for (EnumFacing candidate : EnumFacing.values()) {
            if (candidate == facing) {
                continue;
            }
            if (candidate == direction) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        if (side == getFacing()) {
            return new CableType[]{CableType.SOCAPEX};
        }
        return new CableType[]{CableType.DIMMED_POWER};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == SocapexReceiver.CAP) {
            return facing == getFacing();
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == SocapexReceiver.CAP) {
            return facing == getFacing() ? SocapexReceiver.CAP.cast(this) : null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("channels")) {
            int[] saved = compound.getIntArray("channels");
            if (saved.length == CHANNELS) {
                channels = saved.clone();
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setIntArray("channels", channels);
        return super.writeToNBT(compound);
    }

    @Override
    public int[] receiveSocapex(int[] incoming, boolean simulate) {
        int[] energyReceived = new int[SocapexReceiver.CHANNELS];
        for (int i = 0; i < Math.min(incoming.length, CHANNELS); i++) {
            if (!canReceive(i)) {
                energyReceived[i] = 0;
                continue;
            }
            energyReceived[i] = Math.min(255 - this.channels[i], Math.min(1000, incoming[i]));
            if (!simulate) {
                this.channels[i] += energyReceived[i];
            }
        }
        return energyReceived;
    }

    @Override
    public int[] extractSocapex(int[] channels, boolean simulate) {
        return new int[CHANNELS];
    }

    @Override
    public int getEnergyStored(int channel) {
        return channel >= 0 && channel < CHANNELS ? channels[channel] : 0;
    }

    @Override
    public int getMaxEnergyStored(int channel) {
        return 255;
    }

    @Override
    public boolean canExtract(int channel) {
        return false;
    }

    @Override
    public boolean canReceive(int channel) {
        if (channel == getDirectionalIndex(getFacing())) {
            return false;
        }
        return channel >= 0 && channel <= this.channels.length - 1;
    }

    @Override
    public List<BlockPos> getDevices() {
        return Collections.emptyList();
    }

    @Override
    public int getTotalChannels() {
        return SOCKETS;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        EnumFacing facing = getFacing();
        for (EnumFacing direction : EnumFacing.values()) {
            if (direction == facing) {
                continue;
            }
            TileEntity tileEntity = world.getTileEntity(pos.offset(direction));
            if (tileEntity == null || !tileEntity.hasCapability(TheatricalPower.CAP, direction.getOpposite())) {
                continue;
            }
            ITheatricalPowerStorage storage = tileEntity.getCapability(TheatricalPower.CAP, direction.getOpposite());
            if (storage == null) {
                continue;
            }
            int index = getDirectionalIndex(direction);
            if (storage.getEnergyStored() > getEnergyStored(index)) {
                continue;
            }
            if (storage.receiveEnergy(255, true) > 0) {
                int amount = storage.receiveEnergy(getEnergyStored(index), false);
                channels[index] = channels[index] - amount;
            }
        }
    }

    @Override
    public BlockPos getReceiverPos() {
        return getPos();
    }
}
