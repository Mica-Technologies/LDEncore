/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/lights/TileEntityGenericFixture.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 capability plumbing; the GUI title and container come with the GUI phase;
 *   - BUG FIX: the remote positioner aimed the light with the wrong pan on two of the four
 *     facings (it combined the bearing to the player and the block's facing with the wrong
 *     signs, which only cancelled out for lights facing along one axis). The pan is now
 *     the difference between the block's facing angle and the yaw of the direction to the
 *     player, using the same yaw convention doRayTrace uses to turn pan back into a
 *     direction, and it measures from the block's centre.
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.power.ITheatricalPowerStorage;
import dev.theatricalmod.theatrical.api.capabilities.power.TheatricalPower;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.block.BlockHangable;
import dev.theatricalmod.theatrical.block.light.BlockGenericFixture;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The tungsten fixture: dimmed by the power it is fed, aimed by hand or by the positioner.
 */
public class TileEntityGenericFixture extends TileEntityFixture implements ITheatricalPowerStorage, IAcceptsCable {

    public int lastPower = 0;
    public int energyUsage, energyCost;

    private int power;
    private static final int MAX_RECEIVE = 255;
    private static final int MAX_EXTRACT = 255;

    private Entity trackingEntity;

    @Override
    public void setFixture(Fixture fixture) {
        super.setFixture(fixture);
        if (fixture != null) {
            energyCost = fixture.getEnergyUse();
            energyUsage = fixture.getEnergyUseTimer();
        }
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound compound) {
        NBTTagCompound tag = super.getNBT(compound);
        tag.setInteger("lastPower", lastPower);
        tag.setInteger("power", power);
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        super.readNBT(compound);
        if (compound.hasKey("lastPower")) {
            lastPower = compound.getInteger("lastPower");
        }
        if (compound.hasKey("power")) {
            power = compound.getInteger("power");
        }
    }

    @Override
    public float getMaxLightDistance() {
        return 20;
    }

    @Override
    public Class<? extends Block> getBlock() {
        return BlockGenericFixture.class;
    }

    @Override
    public boolean shouldTrace() {
        return power > 0;
    }

    @Override
    public boolean isUpsideDown() {
        return false;
    }

    @Override
    public float getBeamWidth() {
        return 0.15F;
    }

    @Override
    public float[] getBeamStartPosition() {
        if (getFixture() == null) {
            return new float[3];
        }
        return getFixture().getBeamStartPosition();
    }

    public void setTrackingEntity(Entity trackingEntity) {
        this.trackingEntity = trackingEntity;
    }

    public Entity getTrackingEntity() {
        return trackingEntity;
    }

    @Override
    public float getIntensity() {
        return lastPower;
    }

    /** Aims the fixture at the tracked entity: tilt from height and range, pan from bearing. */
    private void trackEntity() {
        double centreX = pos.getX() + 0.5D;
        double centreZ = pos.getZ() + 0.5D;
        double dx = trackingEntity.posX - centreX;
        double dz = trackingEntity.posZ - centreZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double height = pos.getY() - (trackingEntity.posY + trackingEntity.getEyeHeight());
        if (horizontal > 0.001D) {
            setTilt(-(int) Math.toDegrees(Math.atan(height / horizontal)));
        }
        // doRayTrace turns a pan back into a direction as yaw = facing - pan, with the
        // Minecraft yaw convention (x = -sin(yaw), z = cos(yaw)); invert that here.
        IBlockState state = getBlockState();
        EnumFacing facing = state.getPropertyKeys().contains(BlockHangable.FACING) ? state.getValue(BlockHangable.FACING) : EnumFacing.NORTH;
        double yawToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        int pan = (int) Math.round(facing.getHorizontalAngle() - yawToTarget);
        pan = (int) MathHelper.wrapDegrees(pan);
        setPan(pan);
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote) {
            prevPan = getPan();
            prevTilt = getTilt();
            prevFocus = getFocus();
            if (trackingEntity != null) {
                if (trackingEntity.isDead) {
                    trackingEntity = null;
                } else {
                    trackEntity();
                }
            }
            if (power != lastPower) {
                lastPower = power;
                sync();
            }
            if (power > 0) {
                int energyExtracted = Math.min(power, Math.min(MAX_EXTRACT, this.energyCost));
                power -= energyExtracted;
            }
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int energyReceived = Math.min(MAX_RECEIVE, maxReceive);
        if (!simulate) {
            power = energyReceived;
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        int energyExtracted = Math.min(power, Math.min(MAX_EXTRACT, maxExtract));
        if (!simulate) {
            power = energyExtracted;
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
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == TheatricalPower.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TheatricalPower.CAP) {
            return TheatricalPower.CAP.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DIMMED_POWER};
    }
}
