/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/lights/TileEntityFixture.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - extends TileEntityTheatricalBase for the NBT/sync plumbing instead of repeating it;
 *   - the ray trace uses 1.12's World.rayTraceBlocks (no entity context, so the fake player
 *     upstream created is gone);
 *   - the redstone particle upstream spawned at the beam end every trace is dropped: it was
 *     a no-op on a 1.16 server world but on 1.12 a server-side spawnParticle is broadcast
 *     to every client;
 *   - the trace/illuminator behaviour is otherwise upstream's, deliberately, so the port
 *     can be compared against the 1.16 reference before the performance redesign.
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.api.fixtures.HangableType;
import dev.theatricalmod.theatrical.api.fixtures.IFixture;
import dev.theatricalmod.theatrical.api.fixtures.IFixtureModelProvider;
import dev.theatricalmod.theatrical.block.BlockHangable;
import dev.theatricalmod.theatrical.block.TheatricalBlocks;
import dev.theatricalmod.theatrical.block.light.BlockIlluminator;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * The base fixture tile: owns pan/tilt/focus, traces the beam every five ticks and keeps an
 * {@link BlockIlluminator} at the point where the beam lands.
 */
public abstract class TileEntityFixture extends TileEntityTheatricalBase implements IFixture, ITickable, IFixtureModelProvider {

    private Fixture fixture;
    private double distance = 0;
    private int pan, tilt = 0;
    private int focus = 6;

    private long timer = 0;

    /** Ticks between beam traces. Upstream's cadence, named. */
    private static final int TRACE_INTERVAL = 5;
    /** A pan or tilt no real aim can take, so the first tick always counts as a change. */
    private static final int NEVER_TRACED = Integer.MIN_VALUE;

    public int prevTilt, prevPan, prevFocus = 0;

    /**
     * The aim the beam was last traced at.
     *
     * CHANGED FROM UPSTREAM: upstream asked whether prevPan and prevTilt differed from the
     * current aim, but those two are the renderer's interpolation state and are overwritten at
     * the top of every tick, so the comparison did not mean what it reads as: for a fixture
     * whose aim is derived (a moving light reading DMX) it was true on every tick, and for one
     * whose aim is a plain field it could never be true at all. These record what was actually
     * traced, which is the question being asked.
     */
    private int tracedPan = NEVER_TRACED;
    private int tracedTilt = NEVER_TRACED;

    /** Whether the beam can have moved since it was last traced. */
    protected final boolean aimChanged() {
        return getLightBlock() == null || tracedPan != getPan() || tracedTilt != getTilt();
    }

    private BlockPos lightBlock;

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public BlockPos getLightBlock() {
        return lightBlock;
    }

    public void setLightBlock(BlockPos lightBlock) {
        this.lightBlock = lightBlock;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    public IBlockState getBlockState() {
        return world.getBlockState(pos);
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound compound) {
        compound = super.getNBT(compound);
        compound.setInteger("pan", this.pan);
        compound.setInteger("tilt", this.tilt);
        compound.setInteger("focus", this.focus);
        compound.setInteger("prevPan", prevPan);
        compound.setInteger("prevTilt", prevTilt);
        compound.setInteger("prevFocus", prevFocus);
        compound.setLong("timer", timer);
        compound.setDouble("distance", distance);
        if (lightBlock != null) {
            compound.setLong("lightBlock", lightBlock.toLong());
        }
        if (getFixture() != null && getFixture().getRegistryName() != null) {
            compound.setString("fixture_type", getFixture().getRegistryName().toString());
        }
        return compound;
    }

    @Override
    public void readNBT(NBTTagCompound compound) {
        pan = compound.getInteger("pan");
        tilt = compound.getInteger("tilt");
        focus = compound.getInteger("focus");
        prevPan = compound.getInteger("prevPan");
        prevTilt = compound.getInteger("prevTilt");
        prevFocus = compound.getInteger("prevFocus");
        timer = compound.getLong("timer");
        distance = compound.getDouble("distance");
        lightBlock = compound.hasKey("lightBlock") ? BlockPos.fromLong(compound.getLong("lightBlock")) : null;
        if (compound.hasKey("fixture_type")) {
            setFixture(Fixture.getRegistry().getValue(new ResourceLocation(compound.getString("fixture_type"))));
        } else {
            setFixture(null);
        }
    }

    public int getExtraTilt() {
        return 0;
    }

    public final Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = MathHelper.cos(f1);
        float f3 = MathHelper.sin(f1);
        float f4 = MathHelper.cos(f);
        float f5 = MathHelper.sin(f);
        return new Vec3d(f3 * f4, -f5, f2 * f4);
    }

    @Override
    public boolean emitsLight() {
        IBlockState state = getBlockState();
        boolean broken = state.getPropertyKeys().contains(BlockHangable.BROKEN) && state.getValue(BlockHangable.BROKEN);
        return !broken && TheatricalConfigHandler.FIXTURES.emitLight;
    }

    private EnumFacing getFacing() {
        IBlockState state = getBlockState();
        if (state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return state.getValue(BlockHorizontal.FACING);
        }
        return EnumFacing.NORTH;
    }

    /**
     * Traces the beam from the fixture along its pan/tilt and records where it lands as the
     * illuminator position. Returns the beam length.
     */
    public double doRayTrace() {
        EnumFacing direction = getFacing();
        float lookingAngle = direction.getHorizontalAngle();
        lookingAngle = (isUpsideDown() ? lookingAngle + getPan() : lookingAngle - getPan());
        lookingAngle = lookingAngle % 360;

        float tilt = getTilt() + getExtraTilt();
        if (!isUpsideDown()) {
            tilt = -tilt;
        }

        Vec3d look = getVectorForRotation(tilt, lookingAngle);
        double distance = getMaxLightDistance();
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d startVec = look.scale(0.9F).add(center).add(0, 0.01, 0);
        Vec3d endVec = look.scale(distance).add(center);

        RayTraceResult result = world.rayTraceBlocks(startVec, endVec, false, true, false);
        BlockPos lightPos;
        EnumFacing hitFace = null;
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
            distance = result.hitVec.distanceTo(startVec);
            hitFace = result.sideHit;
            if (!result.getBlockPos().equals(pos)) {
                lightPos = result.getBlockPos().offset(result.sideHit);
            } else {
                lightPos = new BlockPos(result.hitVec);
            }
        } else {
            lightPos = new BlockPos(endVec);
        }
        if (lightPos.equals(pos)) {
            return distance;
        }
        if (!world.isAirBlock(lightPos) && !(world.getBlockState(lightPos).getBlock() instanceof BlockIlluminator) && hitFace != null) {
            lightPos = lightPos.offset(hitFace);
        }
        distance = new Vec3d(lightPos.getX(), lightPos.getY(), lightPos.getZ()).distanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        if (lightPos.equals(lightBlock)) {
            return distance;
        }
        if (lightBlock != null && !lightBlock.equals(lightPos) && world.getBlockState(lightBlock).getBlock() instanceof BlockIlluminator) {
            world.setBlockState(lightBlock, Blocks.AIR.getDefaultState());
        }
        Block target = world.getBlockState(lightPos).getBlock();
        if (!world.isAirBlock(lightPos) && !(target instanceof BlockIlluminator)) {
            return distance;
        }
        setLightBlock(lightPos);
        return distance;
    }

    public double getDistance() {
        return distance;
    }

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

    public int getFocus() {
        return focus;
    }

    public void setPan(int pan) {
        this.pan = pan;
        sync();
    }

    public void setTilt(int tilt) {
        this.tilt = tilt;
        sync();
    }

    public void setFocus(int focus) {
        this.focus = focus;
        sync();
    }

    @Override
    public HangableType getHangType() {
        if (fixture != null) {
            return fixture.getHangableType();
        }
        return HangableType.NONE;
    }

    @Override
    public ResourceLocation getStaticModel() {
        if (fixture != null) {
            Block block = getBlockState().getBlock();
            if (block instanceof BlockHangable && ((BlockHangable) block).isHanging(world, pos)) {
                return getFixture().getHookedModelLocation();
            }
            return getFixture().getStaticModelLocation();
        }
        return null;
    }

    @Override
    public ResourceLocation getTiltModel() {
        return fixture != null ? getFixture().getTiltModelLocation() : null;
    }

    @Override
    public ResourceLocation getPanModel() {
        return fixture != null ? getFixture().getPanModelLocation() : null;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return getFixture() == null ? new float[]{0, 0, 0} : getFixture().getTiltRotationPosition();
    }

    @Override
    public float[] getPanRotationPosition() {
        return getFixture() == null ? new float[]{0, 0, 0} : getFixture().getPanRotationPosition();
    }

    @Override
    public float getDefaultRotation() {
        return getFixture() == null ? 0 : getFixture().getDefaultRotation();
    }

    @Override
    public float[] getBeamStartPosition() {
        return getFixture() == null ? new float[]{} : getFixture().getBeamStartPosition();
    }

    @Override
    public float getBeamWidth() {
        return getFixture() == null ? 0 : getFixture().getBeamWidth();
    }

    @Override
    public float getRayTraceRotation() {
        return getFixture() == null ? 0 : getFixture().getRayTraceRotation();
    }

    /** Removes the illuminator this fixture placed, if it is still there. */
    protected void removeLightBlock() {
        if (lightBlock != null && world != null) {
            if (!world.isAirBlock(lightBlock) && world.getBlockState(lightBlock).getBlock() instanceof BlockIlluminator) {
                world.setBlockState(lightBlock, Blocks.AIR.getDefaultState());
            }
            lightBlock = null;
        }
    }

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote) {
            removeLightBlock();
        }
        super.invalidate();
    }

    @Override
    public void update() {
        prevFocus = focus;
        prevPan = pan;
        prevTilt = tilt;
        if (world.isRemote) {
            return;
        }
        timer++;
        if (timer < TRACE_INTERVAL) {
            return;
        }
        timer = 0;

        if (shouldTrace()) {
            tracedPan = getPan();
            tracedTilt = getTilt();
            double traced = doRayTrace();
            // Only tell the clients when the beam actually moved. Upstream sent a block update
            // on every trace, which for a lit generic fixture was four a second forever, each
            // one a re-render and a tile sync for every player in range.
            if (traced != this.distance) {
                this.distance = traced;
                world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 3);
            }
        }
        updateIlluminator();
    }

    /**
     * Keeps the invisible light block in step with the fixture's brightness.
     *
     * CHANGED FROM UPSTREAM: this ran inside the shouldTrace() gate, so a fixture that dimmed
     * without moving never revised its light level -- a moving light faded to nothing left the
     * room as bright as it had been at full. It now runs whenever the fixture ticks, and still
     * writes a block only when the light value it wants differs from the one already there.
     */
    private void updateIlluminator() {
        if (lightBlock == null || !emitsLight()) {
            return;
        }
        int lightval = MathHelper.clamp((int) ((getIntensity() / 255F) * 15F), 0, 15);
        IBlockState existing = world.getBlockState(lightBlock);
        if (world.isAirBlock(lightBlock) || !(existing.getBlock() instanceof BlockIlluminator)) {
            world.setBlockState(lightBlock, TheatricalBlocks.ILLUMINATOR.getDefaultState()
                    .withProperty(BlockIlluminator.LIGHT_VALUE, lightval), 3);
        } else if (existing.getValue(BlockIlluminator.LIGHT_VALUE) != lightval) {
            world.setBlockState(lightBlock, existing.withProperty(BlockIlluminator.LIGHT_VALUE, lightval), 3);
        }
    }
}
