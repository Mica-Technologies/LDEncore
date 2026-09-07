/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/control/UpdateConsoleFaderPacket.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the desk is resolved through PacketUtil;
 * the fader index is range-checked, where upstream indexed the fader array with whatever
 * number arrived and a malformed packet threw inside the tick loop; and the moved fader is
 * sent back to the players watching the block rather than only saved.
 */
package dev.theatricalmod.theatrical.network.control;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: move one fader, or the grand master (fader -1). */
public class UpdateConsoleFaderPacket implements IMessage {

    private BlockPos blockPos;
    private int fader;
    private int value;

    public UpdateConsoleFaderPacket() {
    }

    public UpdateConsoleFaderPacket(BlockPos blockPos, int fader, int value) {
        this.blockPos = blockPos;
        this.fader = fader;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        fader = buf.readInt();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(fader);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<UpdateConsoleFaderPacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateConsoleFaderPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityBasicLightingControl desk = PacketUtil.getTile(ctx, message.blockPos, TileEntityBasicLightingControl.class);
                if (desk == null) {
                    return;
                }
                if (message.fader != -1 && (message.fader < 0 || message.fader >= TileEntityBasicLightingControl.FADERS)) {
                    return;
                }
                desk.setFader(message.fader, MathHelper.clamp(message.value, 0, 255));
                desk.sync();
            });
            return null;
        }
    }
}
