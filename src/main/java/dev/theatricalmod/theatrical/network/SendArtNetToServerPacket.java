/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/SendArtNetToServerPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 IMessage plumbing;
 *   - the payload length is bounded to one DMX universe on read, so a client cannot make the
 *     server allocate an arbitrary array;
 *   - the owner and operator checks upstream did in its proxy are done here, and the
 *     interface is resolved through PacketUtil like every other packet. This is the one
 *     packet a client sends continuously, so it deliberately does not force a block update.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/** Client -> server: a universe the owner's client received over Art-Net. */
public class SendArtNetToServerPacket implements IMessage {

    private BlockPos blockPos;
    private byte[] data;

    public SendArtNetToServerPacket() {
    }

    public SendArtNetToServerPacket(BlockPos blockPos, byte[] data) {
        this.blockPos = blockPos;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        int length = Math.min(buf.readInt(), DMXUniverse.CHANNELS);
        if (length < 0 || length > buf.readableBytes()) {
            data = new byte[0];
            return;
        }
        data = new byte[length];
        buf.readBytes(data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    public static class Handler implements IMessageHandler<SendArtNetToServerPacket, IMessage> {

        @Override
        public IMessage onMessage(SendArtNetToServerPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityArtNetInterface tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityArtNetInterface.class);
                if (tile == null || message.data.length == 0) {
                    return;
                }
                EntityPlayerMP sender = ctx.getServerHandler().player;
                UUID owner = tile.getPlayer();
                // Only the player who placed the interface may feed it, and only if they are
                // an operator: the data comes from software outside the game, so this is the
                // one input the server cannot otherwise vouch for.
                if (owner == null || !owner.equals(sender.getUniqueID())) {
                    return;
                }
                if (sender.server == null || !sender.server.getPlayerList().canSendCommands(sender.getGameProfile())) {
                    return;
                }
                tile.update(message.data);
            });
            return null;
        }
    }
}
