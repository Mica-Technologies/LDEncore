/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/SocapexReceiver.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - NBT API names (NBTUtil.createPosTag/getPosFromTag);
 *   - serializeNBT no longer throws when the receiver has no position yet (the no-arg
 *     constructor is what the capability system uses for defaults).
 */
package dev.theatricalmod.theatrical.api.capabilities.socapex;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

public class SocapexReceiver implements ISocapexReceiver, INBTSerializable<NBTTagCompound> {

    public static final int CHANNELS = 8;

    @CapabilityInject(ISocapexReceiver.class)
    public static Capability<ISocapexReceiver> CAP = null;

    private int[] channels;
    private BlockPos pos;

    private List<BlockPos> blockPosList = new ArrayList<>();

    public SocapexReceiver() {
        this.channels = new int[CHANNELS];
    }

    public SocapexReceiver(BlockPos pos) {
        this.channels = new int[CHANNELS];
        this.pos = pos;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int i = 0; i < channels.length; i++) {
            tag.setInteger("channel_" + i, channels[i]);
        }
        if (pos != null) {
            tag.setTag("pos", NBTUtil.createPosTag(pos));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        int[] channels = new int[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            if (nbt.hasKey("channel_" + i)) {
                channels[i] = nbt.getInteger("channel_" + i);
            }
        }
        if (nbt.hasKey("pos")) {
            pos = NBTUtil.getPosFromTag(nbt.getCompoundTag("pos"));
        }
        this.channels = channels;
    }

    @Override
    public int[] receiveSocapex(int[] channels, boolean simulate) {
        int[] energyReceived = new int[CHANNELS];
        for (int i = 0; i < Math.min(channels.length, CHANNELS); i++) {
            if (!canReceive(i)) {
                energyReceived[i] = 0;
                continue;
            }
            energyReceived[i] = Math.min(255 - this.channels[i], Math.min(1000, channels[i]));
            if (!simulate) {
                this.channels[i] = energyReceived[i];
            }
        }
        return energyReceived;
    }

    @Override
    public int[] extractSocapex(int[] channels, boolean simulate) {
        int[] energyExtracted = new int[CHANNELS];
        for (int i = 0; i < Math.min(channels.length, CHANNELS); i++) {
            if (!canExtract(i)) {
                energyExtracted[i] = 0;
                continue;
            }
            energyExtracted[i] = Math.min(this.channels[i], Math.min(1000, channels[i]));
            if (!simulate) {
                this.channels[i] -= energyExtracted[i];
            }
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored(int channel) {
        return this.channels[channel];
    }

    @Override
    public int getMaxEnergyStored(int channel) {
        return 255;
    }

    @Override
    public boolean canExtract(int channel) {
        return true;
    }

    @Override
    public boolean canReceive(int channel) {
        return true;
    }

    @Override
    public BlockPos getReceiverPos() {
        return pos;
    }

    public void setBlockPosList(List<BlockPos> blockPosList) {
        this.blockPosList = blockPosList;
    }

    @Override
    public List<BlockPos> getDevices() {
        return blockPosList;
    }

    @Override
    public int getTotalChannels() {
        return 0;
    }
}
