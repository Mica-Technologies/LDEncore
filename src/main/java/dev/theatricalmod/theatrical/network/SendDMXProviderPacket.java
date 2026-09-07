/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/SendDMXProviderPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the payload length is bounded to one
 * universe on read; the client-side work goes through the proxy, so no client-only type is
 * named from a class the dedicated server loads.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server -> client: a provider's whole universe, so the client's copy can show it. */
public class SendDMXProviderPacket implements IMessage {

    private BlockPos blockPos;
    private byte[] data;

    public SendDMXProviderPacket() {
    }

    public SendDMXProviderPacket(BlockPos blockPos, byte[] data) {
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

    public static class Handler implements IMessageHandler<SendDMXProviderPacket, IMessage> {

        @Override
        public IMessage onMessage(SendDMXProviderPacket message, MessageContext ctx) {
            TheatricalMod.proxy.handleProviderDMXUpdate(message.blockPos, message.data);
            return null;
        }
    }
}
