/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/lights/TileEntityIntelligentFixture.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability plumbing; the DMX channels are read straight off
 * the receiver this tile owns instead of through a sided capability lookup on itself; the
 * GUI title and container come with the GUI phase. Fork addition: a Show/Room/Auto mode
 * (screen and redstone, no extra DMX channel); in Room the head points away from its mount
 * at full open white.
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.api.ChannelType;
import dev.theatricalmod.theatrical.api.capabilities.TheatricalEnergyStorage;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.api.fixtures.IRGB;
import dev.theatricalmod.theatrical.block.light.BlockIntelligentFixture;
import dev.theatricalmod.theatrical.block.light.BlockMovingLight;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The moving head: every attribute comes off DMX, power comes in as Forge Energy.
 */
public class TileEntityIntelligentFixture extends TileEntityFixtureDMXAcceptor implements IRGB {

    private final TheatricalEnergyStorage energyStorage = new TheatricalEnergyStorage(2000, 2000);

    /**
     * Fork addition: Room Light. Show (the default, so existing rigs are unchanged) follows DMX;
     * Room points the head straight away from its mount (down when hung) at full open white;
     * Auto is Room while the head has a redstone signal or has heard no DMX for a few seconds.
     * There is no House channel: adding one would move every channel after it and break the
     * patch of every rig already built.
     */
    private TileEntityLedFixture.Mode roomMode = TileEntityLedFixture.Mode.SHOW;
    private boolean roomLit = false;
    private int roomTicks = 0;

    public TileEntityLedFixture.Mode getRoomMode() {
        return roomMode;
    }

    public void setRoomMode(TileEntityLedFixture.Mode mode) {
        this.roomMode = mode;
        decideRoom();
        sync();
    }

    public boolean isRoomLit() {
        return roomLit;
    }

    private boolean room() {
        return roomLit && isPowered();
    }

    private void decideRoom() {
        boolean want;
        switch (roomMode) {
            case ROOM:
                want = true;
                break;
            case AUTO:
                int seconds = TheatricalConfigHandler.ROOM_LIGHT.noSignalSeconds;
                boolean noSignal = seconds > 0 && world.getTotalWorldTime() - lastSignal > seconds * 20L;
                want = world.isBlockPowered(pos) || noSignal;
                break;
            case SHOW:
            default:
                want = false;
                break;
        }
        if (want != roomLit) {
            roomLit = want;
            sync();
        }
    }

    @Override
    public void setFixture(Fixture fixture) {
        super.setFixture(fixture);
        if (fixture != null) {
            this.getIdmxReceiver().setChannelCount(fixture.getChannelCount());
        }
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound compound) {
        NBTTagCompound tag = super.getNBT(compound);
        tag.setTag("energy", energyStorage.serializeNBT());
        tag.setByte("roomMode", (byte) roomMode.ordinal());
        tag.setBoolean("roomLit", roomLit);
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        super.readNBT(compound);
        if (compound.hasKey("energy")) {
            this.energyStorage.deserializeNBT(compound.getCompoundTag("energy"));
        }
        int mode = compound.getByte("roomMode");
        roomMode = mode >= 0 && mode < TileEntityLedFixture.Mode.values().length
                ? TileEntityLedFixture.Mode.values()[mode] : TileEntityLedFixture.Mode.SHOW;
        roomLit = compound.getBoolean("roomLit");
    }

    @Override
    public float getMaxLightDistance() {
        return 50;
    }

    @Override
    public Class<? extends Block> getBlock() {
        return BlockMovingLight.class;
    }

    @Override
    public boolean shouldTrace() {
        return isPowered() && aimChanged();
    }

    private boolean isHangingState() {
        IBlockState state = getBlockState();
        return state.getPropertyKeys().contains(BlockIntelligentFixture.HANGING) && state.getValue(BlockIntelligentFixture.HANGING);
    }

    @Override
    public boolean isUpsideDown() {
        return isHangingState();
    }

    @Override
    public float getDefaultRotation() {
        return 90F;
    }

    @Override
    public float getBeamWidth() {
        return 0.15F;
    }

    @Override
    public float[] getBeamStartPosition() {
        return new float[]{0.5F, 0.5F, 0.15F};
    }

    private boolean hasChannels() {
        return getFixture() != null && getFixture().getChannelsDefinition() != null;
    }

    private int channel(ChannelType type) {
        return Byte.toUnsignedInt(getIdmxReceiver().getChannel(getFixture().getChannelsDefinition().getChannel(type)));
    }

    @Override
    public int getColorHex() {
        return (getRed() << 16) | (getGreen() << 8) | getBlue();
    }

    public int getRed() {
        if (room()) {
            return 255;
        }
        return hasChannels() && isPowered() ? channel(ChannelType.RED) : 0;
    }

    public int getGreen() {
        if (room()) {
            return 244;
        }
        return hasChannels() && isPowered() ? channel(ChannelType.GREEN) : 0;
    }

    public int getBlue() {
        if (room()) {
            return 229;
        }
        return hasChannels() && isPowered() ? channel(ChannelType.BLUE) : 0;
    }

    @Override
    public int getPan() {
        if (room()) {
            return 0;
        }
        if (hasChannels() && isPowered()) {
            return (int) ((channel(ChannelType.PAN) * 360) / 255F);
        }
        return prevPan;
    }

    @Override
    public int getTilt() {
        if (room()) {
            // Tilt 0 plus the head's built-in 90 points the beam straight away from the mount.
            return 0;
        }
        if (hasChannels() && isPowered()) {
            return (int) ((channel(ChannelType.TILT) * 180) / 255F) - 90;
        }
        return prevTilt;
    }

    @Override
    public int getFocus() {
        if (room()) {
            return 25;
        }
        if (hasChannels()) {
            return (int) ((channel(ChannelType.FOCUS) * 50) / 255F);
        }
        return prevFocus;
    }

    @Override
    public float getIntensity() {
        if (room()) {
            return 255;
        }
        if (hasChannels() && isPowered()) {
            return channel(ChannelType.INTENSITY);
        }
        return 0;
    }

    @Override
    public int getExtraTilt() {
        return 90;
    }

    public boolean isPowered() {
        if (!TheatricalConfigHandler.FIXTURES.consumePower) {
            return true;
        }
        return getFixture() != null && energyStorage.getEnergyStored() >= getFixture().getEnergyUse();
    }

    @Override
    public void update() {
        if (getFixture() == null) {
            return;
        }
        if (!world.isRemote) {
            if (lastSignal == Long.MIN_VALUE) {
                // Just loaded: give the console its grace period before calling it gone.
                lastSignal = world.getTotalWorldTime();
            }
            if (++roomTicks % 10 == 0) {
                decideRoom();
            }
        }
        if (isPowered()) {
            if (TheatricalConfigHandler.FIXTURES.consumePower) {
                energyStorage.extractEnergy(getFixture().getEnergyUse(), false);
            }
            super.update();
        }
        prevPan = getPan();
        prevTilt = getTilt();
        prevFocus = getFocus();
    }

    @Override
    public float getRayTraceRotation() {
        if (getFixture() != null) {
            return isHangingState() ? 0 : getFixture().getRayTraceRotation();
        }
        return 0;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return super.getCapability(capability, facing);
    }
}
