/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/dmx/receiver/DMXReceiver.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - NBT API names;
 *   - the start address and channel count are actually saved (upstream's serializeNBT
 *     returned null and the owning tile saved the address itself; doing it here too costs
 *     nothing and lets the capability stand alone);
 *   - receiveDMXValues clamps the copy to the end of the universe instead of throwing when
 *     a fixture is addressed within the last few channels;
 *   - updateChannel's off-by-one (it refused to write the last channel) is fixed.
 */
package dev.theatricalmod.theatrical.api.capabilities.dmx.receiver;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Arrays;

public class DMXReceiver implements IDMXReceiver, INBTSerializable<NBTTagCompound> {

    @CapabilityInject(IDMXReceiver.class)
    public static Capability<IDMXReceiver> CAP = null;

    private int dmxStartPoint;
    private int dmxChannels;
    private byte[] dmxValues;

    public DMXReceiver() {
        this(0, 0);
    }

    public DMXReceiver(int dmxChannels, int dmxStartPoint) {
        this.dmxChannels = dmxChannels;
        this.dmxStartPoint = dmxStartPoint;
        this.dmxValues = new byte[dmxChannels];
    }

    @Override
    public int getChannelCount() {
        return dmxChannels;
    }

    @Override
    public int getStartPoint() {
        return dmxStartPoint;
    }

    public byte getDMXChannel(int channel) {
        return dmxValues[channel];
    }

    @Override
    public void receiveDMXValues(byte[] data, World world, BlockPos pos) {
        if (data.length > this.dmxStartPoint) {
            int end = Math.min(data.length, this.dmxStartPoint + this.dmxChannels);
            byte[] values = Arrays.copyOfRange(data, this.dmxStartPoint, end);
            this.dmxValues = values.length == this.dmxChannels ? values : Arrays.copyOf(values, this.dmxChannels);
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dmxStart", dmxStartPoint);
        tag.setInteger("dmxChannels", dmxChannels);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("dmxStart")) {
            dmxStartPoint = nbt.getInteger("dmxStart");
        }
        if (nbt.hasKey("dmxChannels")) {
            setChannelCount(nbt.getInteger("dmxChannels"));
        }
    }

    @Override
    public void setDMXStartPoint(int dmxStartPoint) {
        this.dmxStartPoint = dmxStartPoint;
    }

    @Override
    public void setChannelCount(int channelCount) {
        this.dmxChannels = channelCount;
        this.dmxValues = Arrays.copyOf(dmxValues, channelCount);
    }

    @Override
    public byte getChannel(int index) {
        if (index < 0 || dmxValues.length < (index + 1)) {
            return 0;
        }
        return dmxValues[index];
    }

    @Override
    public void updateChannel(int index, byte value) {
        if (index >= 0 && index < this.dmxValues.length) {
            this.dmxValues[index] = value;
        }
    }

    public byte[] getDmxValues() {
        return dmxValues;
    }
}
