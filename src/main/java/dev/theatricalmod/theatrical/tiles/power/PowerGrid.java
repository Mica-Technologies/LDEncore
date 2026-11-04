/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.tiles.power;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.block.cables.BlockPowerCable;
import dev.theatricalmod.theatrical.block.rigging.BlockIWB;
import dev.theatricalmod.theatrical.block.rigging.BlockTruss;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Forge Energy over power cable, truss and IWB.
 *
 * <p>Every touching run of those blocks is one network. Energy pushed into any of them (or
 * pulled by a power cable from a storage block beside it) goes straight to the LDEncore
 * consumers touching the network: moving heads first, then dimmer racks. Nothing is stored
 * on the way and nothing is lost, so the source's output is the only limit. Networks never
 * deliver to other mods' blocks, which keeps the grid from becoming a general-purpose
 * conduit and rules out loops through someone else's storage.
 *
 * <p>Networks are found by a walk the first time energy arrives and cached per world. Any
 * change to a conduit or to a block beside one, and any chunk load or unload the network
 * touches, drops the cache; the next delivery walks again.
 */
@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
public final class PowerGrid {

    /** The most conduit blocks one network walk visits, so a city's worth of truss cannot stall a tick. */
    public static final int MAX_CONDUITS = 4096;

    private static final Map<World, Map<BlockPos, Net>> GRIDS = new WeakHashMap<>();

    private PowerGrid() {
    }

    private static final class Net {
        final List<TileEntity> fixtures = new ArrayList<>();
        final List<TileEntity> racks = new ArrayList<>();
        /** Chunks holding this network's conduits or consumers. */
        final Set<Long> chunks = new HashSet<>();
        /** Unloaded chunks the walk stopped at: loading one may extend the network. */
        final Set<Long> frontier = new HashSet<>();
        int rotate;
    }

    public static boolean isConduit(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockPowerCable || block instanceof BlockTruss || block instanceof BlockIWB;
    }

    /** Whether the tile is one the grid feeds (and so must never be pulled from). */
    public static boolean isConsumer(TileEntity tile) {
        return tile instanceof TileEntityIntelligentFixture || tile instanceof TileEntityDimmerRack;
    }

    /** Forget every network in the world; called whenever a conduit or its surroundings change. */
    public static void invalidate(World world) {
        if (world != null && !world.isRemote) {
            GRIDS.remove(world);
        }
    }

    /**
     * Hands up to {@code amount} FE to the consumers on the network at {@code from} and
     * returns how much they took.
     */
    public static int deliver(World world, BlockPos from, int amount, boolean simulate) {
        if (world == null || world.isRemote || amount <= 0) {
            return 0;
        }
        Net net = netAt(world, from);
        if (net == null) {
            return 0;
        }
        int left = offer(world, net, net.fixtures, amount, simulate);
        if (left > 0) {
            left = offer(world, net, net.racks, left, simulate);
        }
        if (!simulate) {
            net.rotate++;
        }
        return amount - left;
    }

    private static int offer(World world, Net net, List<TileEntity> consumers, int left, boolean simulate) {
        int n = consumers.size();
        if (n == 0) {
            return left;
        }
        // Start one further along each time, so no consumer is always last in line.
        int start = Math.floorMod(net.rotate, n);
        for (int i = 0; i < n && left > 0; i++) {
            TileEntity tile = consumers.get((start + i) % n);
            if (tile.isInvalid()) {
                invalidate(world);
                continue;
            }
            IEnergyStorage storage = tile.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage != null && storage.canReceive()) {
                left -= storage.receiveEnergy(left, simulate);
            }
        }
        return left;
    }

    private static Net netAt(World world, BlockPos pos) {
        Map<BlockPos, Net> grid = GRIDS.computeIfAbsent(world, w -> new HashMap<>());
        Net net = grid.get(pos);
        if (net == null && isConduit(world.getBlockState(pos))) {
            net = walk(world, pos, grid);
        }
        return net;
    }

    private static Net walk(World world, BlockPos start, Map<BlockPos, Net> grid) {
        Net net = new Net();
        Set<TileEntity> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        start = start.toImmutable();
        queue.add(start);
        grid.put(start, net);
        int visited = 0;
        while (!queue.isEmpty() && visited < MAX_CONDUITS) {
            BlockPos pos = queue.poll();
            visited++;
            net.chunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            for (EnumFacing face : EnumFacing.values()) {
                BlockPos next = pos.offset(face);
                if (!world.isBlockLoaded(next)) {
                    net.frontier.add(ChunkPos.asLong(next.getX() >> 4, next.getZ() >> 4));
                    continue;
                }
                IBlockState state = world.getBlockState(next);
                if (isConduit(state)) {
                    if (!grid.containsKey(next)) {
                        grid.put(next, net);
                        queue.add(next);
                    }
                    continue;
                }
                if (!state.getBlock().hasTileEntity(state)) {
                    continue;
                }
                TileEntity tile = world.getTileEntity(next);
                if (tile != null && seen.add(tile)) {
                    if (tile instanceof TileEntityIntelligentFixture) {
                        net.fixtures.add(tile);
                    } else if (tile instanceof TileEntityDimmerRack) {
                        net.racks.add(tile);
                    } else {
                        continue;
                    }
                    net.chunks.add(ChunkPos.asLong(next.getX() >> 4, next.getZ() >> 4));
                }
            }
        }
        return net;
    }

    private static void onChunkChange(World world, long chunk, boolean loaded) {
        Map<BlockPos, Net> grid = GRIDS.get(world);
        if (grid == null) {
            return;
        }
        for (Net net : new HashSet<>(grid.values())) {
            if (loaded ? net.frontier.contains(chunk) : net.chunks.contains(chunk)) {
                GRIDS.remove(world);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getWorld().isRemote) {
            onChunkChange(event.getWorld(), ChunkPos.asLong(event.getChunk().x, event.getChunk().z), true);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            onChunkChange(event.getWorld(), ChunkPos.asLong(event.getChunk().x, event.getChunk().z), false);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        GRIDS.remove(event.getWorld());
    }
}
