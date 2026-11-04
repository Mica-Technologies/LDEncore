/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.tiles.lights;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.block.light.BlockIlluminator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Spreads the world-light side of fixture beams over time.
 *
 * Every time a beam lands somewhere new, its invisible illuminator block is removed from the old
 * spot and placed at the new one, and each of those writes makes vanilla recompute the light of
 * everything up to fifteen blocks around. That recalculation is almost all of what a lit rig
 * costs the server: 64 moving heads re-aimed together took one tick to 199 ms. Writes therefore
 * go through this queue, which applies them at the end of the world tick within a time budget
 * ({@code fixtures.lightUpdateBudgetMillis}). Several writes to one block before it is applied
 * collapse into the last, in the position of the first, so a beam sweeping across a stage costs
 * one write per block it finally settles on rather than one per block it passed.
 *
 * Applying a whole chunk section's writes together was tried, to save clients section rebuilds:
 * it gave clients nothing measurable and let one tick run to 26 ms, so writes are applied one at
 * a time against the budget. What keeps a client smooth under a heavily moving rig is Forge's
 * {@code alwaysSetupTerrainOffThread}, which moves those rebuilds off the render thread.
 *
 * Only the world light lags under load. The visible beam is drawn from the fixture's own aim and
 * is never delayed.
 *
 * Server worlds only; on a client world (which never runs this logic) writes go straight through.
 */
public final class IlluminatorQueue {

    private static final Map<World, Map<BlockPos, IBlockState>> PENDING = new WeakHashMap<>();

    private IlluminatorQueue() {
    }

    /** The state a position will have once the queue has caught up. */
    public static IBlockState stateAt(World world, BlockPos pos) {
        Map<BlockPos, IBlockState> pending = PENDING.get(world);
        if (pending != null) {
            IBlockState queued = pending.get(pos);
            if (queued != null) {
                return queued;
            }
        }
        return world.getBlockState(pos);
    }

    /** Whether the position will hold air, or an illuminator, once the queue has caught up. */
    public static boolean isFreeForLight(World world, BlockPos pos) {
        IBlockState state = stateAt(world, pos);
        return state.getBlock() instanceof BlockIlluminator || state.getBlock().isAir(state, world, pos);
    }

    /** Asks for an illuminator write (or its removal, with air). */
    public static void set(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            world.setBlockState(pos, state, 3);
            return;
        }
        Map<BlockPos, IBlockState> pending = PENDING.computeIfAbsent(world, w -> new LinkedHashMap<>());
        if (!pending.containsKey(pos) && world.getBlockState(pos) == state) {
            return;
        }
        // put() on an existing key keeps its place in the order: first asked, first served.
        pending.put(pos.toImmutable(), state);
    }

    /** Applies queued writes until this tick's budget is spent. Called at the end of each world tick. */
    public static void drain(World world) {
        Map<BlockPos, IBlockState> pending = PENDING.get(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        long budgetNanos = (long) (TheatricalConfigHandler.FIXTURES.lightUpdateBudgetMillis * 1_000_000D);
        long start = System.nanoTime();
        Iterator<Map.Entry<BlockPos, IBlockState>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, IBlockState> entry = it.next();
            it.remove();
            apply(world, entry.getKey(), entry.getValue());
            if (System.nanoTime() - start >= budgetNanos) {
                break;
            }
        }
    }

    private static void apply(World world, BlockPos pos, IBlockState wanted) {
        // Never load a chunk for a light, and never replace anything but air or an illuminator:
        // a player may have built into the spot since the write was asked for.
        if (!world.isBlockLoaded(pos)) {
            return;
        }
        IBlockState current = world.getBlockState(pos);
        boolean currentFree = current.getBlock() instanceof BlockIlluminator || current.getBlock().isAir(current, world, pos);
        if (!currentFree || current == wanted) {
            return;
        }
        world.setBlockState(pos, wanted, 3);
    }

    /** How many writes are waiting, for diagnostics. */
    public static int pendingCount(World world) {
        Map<BlockPos, IBlockState> pending = PENDING.get(world);
        return pending == null ? 0 : pending.size();
    }
}
