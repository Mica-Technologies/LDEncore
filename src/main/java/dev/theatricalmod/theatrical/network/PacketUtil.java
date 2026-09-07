/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 * Fork-authored file, Apache License 2.0.
 *
 * Upstream's packet handlers took the block position a client sent and acted on whatever
 * tile was there, with no check that the sender was anywhere near it or that the tile was
 * the kind the packet is about. Any client could retune a stranger's dimmer rack from
 * across the world, or crash the handler by naming a block of the wrong type. Every
 * server-bound packet in this fork resolves its target through here instead.
 */
package dev.theatricalmod.theatrical.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public final class PacketUtil {

    /**
     * How far a player may be from a block and still act on it, squared. The same 8 blocks
     * vanilla allows a player with a container open.
     */
    public static final double REACH_SQ = 64.0D;

    private PacketUtil() {
    }

    /**
     * The tile the sender named, if the chunk is loaded, the sender is within reach of it,
     * and it is of the expected type. Null otherwise, and callers do nothing in that case.
     */
    @Nullable
    public static <T extends TileEntity> T getTile(MessageContext ctx, BlockPos pos, Class<T> type) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null) {
            return null;
        }
        World world = player.getServerWorld();
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        if (player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > REACH_SQ) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        return type.isInstance(tile) ? type.cast(tile) : null;
    }

    /** Runs the task on the server thread that owns the sender's world. */
    public static void onServerThread(MessageContext ctx, Runnable task) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player != null) {
            player.getServerWorld().addScheduledTask(task);
        }
    }
}
