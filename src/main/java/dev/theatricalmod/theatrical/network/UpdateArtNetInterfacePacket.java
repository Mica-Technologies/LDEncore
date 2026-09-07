/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/UpdateArtNetInterfacePacket.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the target is resolved through PacketUtil;
 * the IP string is length-capped on the wire so a client cannot send an unbounded string.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: set an Art-Net interface's universe and listen address. */
public class UpdateArtNetInterfacePacket implements IMessage {

    /** Long enough for any IPv6 literal; short enough that it cannot be abused. */
    private static final int MAX_IP_LENGTH = 64;

    private BlockPos blockPos;
    private int universe;
    private String ipAddress;

    public UpdateArtNetInterfacePacket() {
    }

    public UpdateArtNetInterfacePacket(BlockPos blockPos, int universe, String ip) {
        this.blockPos = blockPos;
        this.universe = universe;
        this.ipAddress = ip;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        universe = buf.readInt();
        String read = ByteBufUtils.readUTF8String(buf);
        ipAddress = read.length() > MAX_IP_LENGTH ? read.substring(0, MAX_IP_LENGTH) : read;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(universe);
        ByteBufUtils.writeUTF8String(buf, ipAddress);
    }

    public static class Handler implements IMessageHandler<UpdateArtNetInterfacePacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateArtNetInterfacePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityArtNetInterface tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityArtNetInterface.class);
                if (tile == null) {
                    return;
                }
                tile.setUniverse(MathHelper.clamp(message.universe, 0, 32767));
                tile.setIp(message.ipAddress);
                if (tile.getPlayer() == null) {
                    tile.setPlayer(ctx.getServerHandler().player.getUniqueID());
                }
                tile.sync();
            });
            return null;
        }
    }
}
