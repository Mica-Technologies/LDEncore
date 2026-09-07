/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/lights/TileEntityIntelligentFixture.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability plumbing; the DMX channels are read straight off
 * the receiver this tile owns instead of through a sided capability lookup on itself; the
 * GUI title and container come with the GUI phase.
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.api.ChannelType;
import dev.theatricalmod.theatrical.api.capabilities.TheatricalEnergyStorage;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.api.fixtures.IRGB;
import dev.theatricalmod.theatrical.block.light.BlockIntelligentFixture;
import dev.theatricalmod.theatrical.block.light.BlockMovingLight;
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
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        super.readNBT(compound);
        if (compound.hasKey("energy")) {
            this.energyStorage.deserializeNBT(compound.getCompoundTag("energy"));
        }
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
        if (isPowered()) {
            return this.getLightBlock() == null || prevPan != getPan() || prevTilt != getTilt();
        }
        return false;
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
        return hasChannels() && isPowered() ? channel(ChannelType.RED) : 0;
    }

    public int getGreen() {
        return hasChannels() && isPowered() ? channel(ChannelType.GREEN) : 0;
    }

    public int getBlue() {
        return hasChannels() && isPowered() ? channel(ChannelType.BLUE) : 0;
    }

    @Override
    public int getPan() {
        if (hasChannels() && isPowered()) {
            return (int) ((channel(ChannelType.PAN) * 360) / 255F);
        }
        return prevPan;
    }

    @Override
    public int getTilt() {
        if (hasChannels() && isPowered()) {
            return (int) ((channel(ChannelType.TILT) * 180) / 255F) - 90;
        }
        return prevTilt;
    }

    @Override
    public int getFocus() {
        if (hasChannels()) {
            return (int) ((channel(ChannelType.FOCUS) * 50) / 255F);
        }
        return prevFocus;
    }

    @Override
    public float getIntensity() {
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
