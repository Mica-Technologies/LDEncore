/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.tiles.led;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.block.TheatricalBlocks;
import dev.theatricalmod.theatrical.block.led.BlockLedFixture;
import dev.theatricalmod.theatrical.block.light.BlockRoomIlluminator;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import dev.theatricalmod.theatrical.tiles.lights.IlluminatorQueue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An LED house fixture: RGB under DMX for a show, or a plain white room light, or either by
 * itself.
 *
 * <h3>Modes</h3>
 * <ul>
 * <li><b>Show</b>: colour and brightness come from DMX. The fixture glows in its render but
 *     casts no world light, so a chase costs nothing in light updates.</li>
 * <li><b>Room</b>: a plain warm-white light. The fixture gives off full block light, and keeps a
 *     floor light block (see {@link BlockRoomIlluminator}) on the floor beneath it.</li>
 * <li><b>Auto</b>: Room while the House channel is at 128 or above, while the fixture (or any
 *     segment of its run) has a redstone signal, or when no DMX has reached it for a few seconds;
 *     Show otherwise. A desk cue or time trigger can therefore bring the house lights up when the
 *     show ends.</li>
 * </ul>
 *
 * <h3>Channels</h3>
 * From the start address: Red, Green, Blue, Dimmer, House. A run in pixel mode adds a Red, Green,
 * Blue triple for each further segment, in order from the run's west or north end (or its bottom,
 * for an upright strip); the first segment uses the first three channels and every segment shares
 * the Dimmer and House. A channel that would fall past 511 reads as zero.
 *
 * <h3>Runs</h3>
 * Segments that join (see {@link BlockLedFixture#getRunAxis}) share one address, mode and pixel
 * setting: changing any segment changes the run, and a segment placed against a run takes the
 * run's settings. Each segment works out its own place in the run from the blocks around it, so
 * splitting or joining a run needs no bookkeeping. DMX itself needs none either: the cable walk
 * passes through every receiver, so one cable into any segment reaches the whole run.
 */
public class TileEntityLedFixture extends TileEntityTheatricalBase implements ITickable, IAcceptsCable {

    public enum Mode {
        SHOW, ROOM, AUTO;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        static Mode byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : AUTO;
        }
    }

    public static final int CH_RED = 0;
    public static final int CH_GREEN = 1;
    public static final int CH_BLUE = 2;
    public static final int CH_DIMMER = 3;
    public static final int CH_HOUSE = 4;
    /** Channels a fixture or run uses outside pixel mode. */
    public static final int BASE_CHANNELS = 5;

    /** The colour of Room Light: a warm white, about 3500 K. */
    public static final int ROOM_COLOUR = 0xFFE8CC;

    /** Longest run a segment will look along, each way. */
    public static final int MAX_RUN_WALK = 128;
    /** How often a segment re-reads its place in the run, in ticks. */
    private static final int RUN_RECHECK_TICKS = 20;
    /** How often the server re-decides the mode, in ticks. */
    private static final int DECIDE_TICKS = 10;
    /** How often a room-lit fixture checks its floor light is still there, in ticks. */
    private static final int HEAL_TICKS = 100;

    private int address = 0;
    private Mode mode = Mode.AUTO;
    private boolean pixel = false;
    /** Whether the fixture is acting as a room light now. Decided by the server, synced. */
    private boolean roomLit = false;
    /** The floor light block this fixture placed, if any. */
    @Nullable
    private BlockPos floorLight;
    /** Whether this block itself has a redstone signal. */
    private boolean powered = false;

    // Not saved: worked out again after a load.
    private int runIndex = 0;
    private int runLength = 1;
    private long runCheckedAt = Long.MIN_VALUE;
    private long lastSignal = Long.MIN_VALUE;
    private int ticks = 0;
    private boolean firstTick = true;

    private final DMXReceiver receiver = new DMXReceiver(BASE_CHANNELS, 0) {
        @Override
        public void receiveDMXValues(byte[] data, World world, BlockPos pos) {
            super.receiveDMXValues(data, world, pos);
            if (TileEntityLedFixture.this.world != null) {
                lastSignal = TileEntityLedFixture.this.world.getTotalWorldTime();
            }
        }
    };

    // ---------------------------------------------------------------- settings

    public int getAddress() {
        return address;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isPixel() {
        return pixel;
    }

    public boolean isRoomLit() {
        return roomLit;
    }

    /** Whether this fixture can form runs, and so whether pixel mode means anything. */
    public boolean canRun() {
        BlockLedFixture block = block();
        return block != null && block.getRunAxis(world.getBlockState(pos)) != null;
    }

    public int getRunLength() {
        refreshRunIfStale();
        return runLength;
    }

    public int getRunIndex() {
        refreshRunIfStale();
        return runIndex;
    }

    /** How many channels this fixture's run uses from its address. */
    public int getFootprint() {
        refreshRunIfStale();
        return pixel ? BASE_CHANNELS + 3 * (runLength - 1) : BASE_CHANNELS;
    }

    private void setSettings(int address, Mode mode, boolean pixel) {
        this.address = Math.max(0, Math.min(DMXUniverse.CHANNELS - 1, address));
        this.mode = mode;
        this.pixel = pixel;
        updateReceiver();
        sync();
    }

    /** Applies settings to this fixture and every segment of its run. Server side. */
    public void applyToRun(int address, Mode mode, boolean pixel) {
        for (TileEntityLedFixture member : runMembers()) {
            member.setSettings(address, mode, pixel);
            member.decideNow();
        }
    }

    /**
     * Takes the settings of a run this fixture was just placed against (or, for a fixture that
     * does not form runs, of an identical fixture beside it), so extending a run or a row of
     * panels needs no setting up. Placing a segment between two runs joins them, under the
     * settings of the first one found.
     */
    public void adoptNeighbourSettings() {
        BlockLedFixture block = block();
        if (block == null) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        EnumFacing.Axis axis = block.getRunAxis(state);
        for (EnumFacing direction : EnumFacing.values()) {
            if (axis != null && direction.getAxis() != axis) {
                continue;
            }
            BlockPos other = pos.offset(direction);
            IBlockState otherState = world.getBlockState(other);
            boolean match = axis != null ? block.sameRun(state, otherState) : otherState.getBlock() == block;
            TileEntity tile = world.getTileEntity(other);
            if (match && tile instanceof TileEntityLedFixture) {
                TileEntityLedFixture source = (TileEntityLedFixture) tile;
                if (axis != null) {
                    invalidateRun();
                    applyToRun(source.address, source.mode, source.pixel);
                } else {
                    setSettings(source.address, source.mode, source.pixel);
                }
                return;
            }
        }
    }

    // ---------------------------------------------------------------- runs

    @Nullable
    private BlockLedFixture block() {
        if (world == null) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockLedFixture ? (BlockLedFixture) state.getBlock() : null;
    }

    public void invalidateRun() {
        runCheckedAt = Long.MIN_VALUE;
    }

    private void refreshRunIfStale() {
        if (world == null) {
            return;
        }
        long now = world.getTotalWorldTime();
        if (now - runCheckedAt < RUN_RECHECK_TICKS && runCheckedAt != Long.MIN_VALUE) {
            return;
        }
        runCheckedAt = now;
        int oldIndex = runIndex;
        int oldLength = runLength;
        BlockLedFixture block = block();
        IBlockState state = world.getBlockState(pos);
        EnumFacing.Axis axis = block == null ? null : block.getRunAxis(state);
        if (axis == null) {
            runIndex = 0;
            runLength = 1;
        } else {
            int back = walk(block, state, EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis));
            int ahead = walk(block, state, EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis));
            runIndex = back;
            runLength = back + 1 + ahead;
        }
        if (runIndex != oldIndex || runLength != oldLength) {
            updateReceiver();
        }
    }

    private int walk(BlockLedFixture block, IBlockState state, EnumFacing direction) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos);
        while (count < MAX_RUN_WALK) {
            cursor.move(direction);
            if (!world.isBlockLoaded(cursor) || !block.sameRun(state, world.getBlockState(cursor))) {
                break;
            }
            count++;
        }
        return count;
    }

    /** This fixture and every other segment of its run. */
    private List<TileEntityLedFixture> runMembers() {
        List<TileEntityLedFixture> members = new ArrayList<>();
        members.add(this);
        BlockLedFixture block = block();
        if (block == null) {
            return members;
        }
        IBlockState state = world.getBlockState(pos);
        EnumFacing.Axis axis = block.getRunAxis(state);
        if (axis == null) {
            return members;
        }
        for (EnumFacing.AxisDirection sign : EnumFacing.AxisDirection.values()) {
            EnumFacing direction = EnumFacing.getFacingFromAxis(sign, axis);
            BlockPos cursor = pos;
            for (int i = 0; i < MAX_RUN_WALK; i++) {
                cursor = cursor.offset(direction);
                if (!world.isBlockLoaded(cursor) || !block.sameRun(state, world.getBlockState(cursor))) {
                    break;
                }
                TileEntity tile = world.getTileEntity(cursor);
                if (tile instanceof TileEntityLedFixture) {
                    members.add((TileEntityLedFixture) tile);
                }
            }
        }
        return members;
    }

    private void updateReceiver() {
        int count = Math.min(getFootprintUnchecked(), DMXUniverse.CHANNELS - address);
        receiver.setDMXStartPoint(address);
        receiver.setChannelCount(Math.max(1, count));
    }

    private int getFootprintUnchecked() {
        return pixel ? BASE_CHANNELS + 3 * (runLength - 1) : BASE_CHANNELS;
    }

    // ---------------------------------------------------------------- output

    private int channel(int offset) {
        return Byte.toUnsignedInt(receiver.getChannel(offset));
    }

    /**
     * The colour the lens shows now, as 0xRRGGBB with the dimmer already applied, or 0 for dark.
     */
    public int getOutputColour() {
        if (roomLit) {
            return ROOM_COLOUR;
        }
        int segment = pixel ? getRunIndex() : 0;
        int base = segment == 0 ? 0 : BASE_CHANNELS + 3 * (segment - 1);
        float dimmer = channel(CH_DIMMER) / 255F;
        int r = Math.round(channel(base + CH_RED) * dimmer);
        int g = Math.round(channel(base + CH_GREEN) * dimmer);
        int b = Math.round(channel(base + CH_BLUE) * dimmer);
        return (r << 16) | (g << 8) | b;
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        if (firstTick) {
            firstTick = false;
            // A fixture that has just loaded has heard nothing yet; give the console the grace
            // period before calling it gone.
            lastSignal = world.getTotalWorldTime();
            powered = world.isBlockPowered(pos);
            refreshRunIfStale();
        }
        if (++ticks % DECIDE_TICKS != 0) {
            return;
        }
        refreshRunIfStale();
        decideNow();
        if (roomLit && ticks % HEAL_TICKS == 0) {
            keepFloorLight();
        }
    }

    private void decideNow() {
        boolean want;
        switch (mode) {
            case ROOM:
                want = true;
                break;
            case SHOW:
                want = false;
                break;
            case AUTO:
            default:
                want = channel(CH_HOUSE) >= 128 || runPowered() || noSignal();
                break;
        }
        if (want != roomLit) {
            roomLit = want;
            sync();
            IlluminatorQueue.relight(world, pos);
            if (roomLit) {
                keepFloorLight();
            } else {
                removeFloorLight();
            }
        }
    }

    private boolean noSignal() {
        int seconds = TheatricalConfigHandler.ROOM_LIGHT.noSignalSeconds;
        return seconds > 0 && world.getTotalWorldTime() - lastSignal > seconds * 20L;
    }

    private boolean runPowered() {
        if (powered) {
            return true;
        }
        if (runLength <= 1) {
            return false;
        }
        for (TileEntityLedFixture member : runMembers()) {
            if (member.powered) {
                return true;
            }
        }
        return false;
    }

    public void onNeighbourChanged() {
        invalidateRun();
        if (world != null && !world.isRemote) {
            powered = world.isBlockPowered(pos);
        }
    }

    // ---------------------------------------------------------------- floor light

    /** The block id of City Super Mod's light-up air, which also lights a floor. Matched by name. */
    private static final ResourceLocation CSM_LIGHTUP_AIR = new ResourceLocation("csm", "lightupair");

    /**
     * Places this fixture's floor light, or moves it if the floor has changed. Scans straight
     * down for the lowest air above the first solid block, skipping the block just under the
     * fixture (the fixture's own light already covers that). If the column already holds another
     * fixture's floor light, or CSM's, the room there is lit already and nothing is placed.
     */
    private void keepFloorLight() {
        BlockLedFixture block = block();
        if (!TheatricalConfigHandler.ROOM_LIGHT.floorLight || block == null
                || !block.placesFloorLight(world.getBlockState(pos), pos)) {
            removeFloorLight();
            return;
        }
        BlockPos target = null;
        int depth = TheatricalConfigHandler.ROOM_LIGHT.floorLightDepth;
        for (int d = 1; d <= depth; d++) {
            BlockPos cell = pos.down(d);
            if (cell.getY() < 0 || !world.isBlockLoaded(cell)) {
                return;
            }
            if (cell.equals(floorLight)) {
                // Our own light counts as the air it stands in.
                if (d >= 2) {
                    target = cell;
                }
                continue;
            }
            IBlockState state = IlluminatorQueue.stateAt(world, cell);
            if (state.getBlock() instanceof BlockRoomIlluminator || CSM_LIGHTUP_AIR.equals(state.getBlock().getRegistryName())) {
                // Somebody else lights this column already.
                target = null;
                break;
            }
            if (!state.getBlock().isAir(state, world, cell)) {
                break;
            }
            if (d >= 2) {
                target = cell;
            }
        }
        if (floorLight != null && !floorLight.equals(target)) {
            removeFloorLight();
        }
        if (target != null && IlluminatorQueue.isFreeForRoomLight(world, target)) {
            IlluminatorQueue.set(world, target, TheatricalBlocks.ROOM_ILLUMINATOR.getDefaultState());
            if (!target.equals(floorLight)) {
                floorLight = target;
                markDirty();
            }
        }
    }

    private void removeFloorLight() {
        if (floorLight == null) {
            return;
        }
        if (IlluminatorQueue.stateAt(world, floorLight).getBlock() instanceof BlockRoomIlluminator) {
            IlluminatorQueue.set(world, floorLight, Blocks.AIR.getDefaultState());
        }
        floorLight = null;
        markDirty();
    }

    /** Called by the block when it is broken or replaced. */
    public void onRemoved() {
        if (world != null && !world.isRemote) {
            removeFloorLight();
        }
    }

    // ---------------------------------------------------------------- persistence and sync

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound nbt) {
        nbt = super.getNBT(nbt);
        nbt.setInteger("address", address);
        nbt.setByte("mode", (byte) mode.ordinal());
        nbt.setBoolean("pixel", pixel);
        nbt.setBoolean("roomLit", roomLit);
        if (floorLight != null) {
            nbt.setLong("floorLight", floorLight.toLong());
        }
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound nbt) {
        address = nbt.getInteger("address");
        mode = nbt.hasKey("mode") ? Mode.byOrdinal(nbt.getByte("mode")) : Mode.AUTO;
        pixel = nbt.getBoolean("pixel");
        roomLit = nbt.getBoolean("roomLit");
        floorLight = nbt.hasKey("floorLight") ? BlockPos.fromLong(nbt.getLong("floorLight")) : null;
        updateReceiver();
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        boolean wasLit = roomLit;
        super.onDataPacket(net, pkt);
        if (wasLit != roomLit && world != null) {
            // Clients work their light out for themselves; the server does not send it.
            world.checkLight(pos);
        }
    }

    @Override
    public void onLoad() {
        TheatricalMod.refreshDmxNetwork(world);
    }

    @Override
    public void invalidate() {
        TheatricalMod.refreshDmxNetwork(world);
        super.invalidate();
    }

    @Override
    public boolean hasFastRenderer() {
        return true;
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DMX};
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXReceiver.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXReceiver.CAP) {
            return DMXReceiver.CAP.cast(receiver);
        }
        return super.getCapability(capability, facing);
    }
}
