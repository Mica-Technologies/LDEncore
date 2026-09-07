/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/control/MoveStepPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the desk is resolved through PacketUtil,
 * and the new step is sent back to the players watching the block rather than only saved.
 */
package dev.theatricalmod.theatrical.network.control;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: step the lighting desk's cue list forwards or back. */
public class MoveStepPacket implements IMessage {

    private BlockPos blockPos;
    private boolean isForwards;

    public MoveStepPacket() {
    }

    public MoveStepPacket(BlockPos blockPos, boolean isForwards) {
        this.blockPos = blockPos;
        this.isForwards = isForwards;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        isForwards = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeBoolean(isForwards);
    }

    public static class Handler implements IMessageHandler<MoveStepPacket, IMessage> {

        @Override
        public IMessage onMessage(MoveStepPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityBasicLightingControl desk = PacketUtil.getTile(ctx, message.blockPos, TileEntityBasicLightingControl.class);
                if (desk == null) {
                    return;
                }
                if (message.isForwards) {
                    desk.moveForward();
                } else {
                    desk.moveBack();
                }
                desk.sync();
            });
            return null;
        }
    }
}
