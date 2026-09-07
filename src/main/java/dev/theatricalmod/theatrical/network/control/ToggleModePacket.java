/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/control/ToggleModePacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the desk is resolved through PacketUtil,
 * and the new mode is sent back to the players watching the block rather than only saved.
 */
package dev.theatricalmod.theatrical.network.control;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: switch the lighting desk between program and run mode. */
public class ToggleModePacket implements IMessage {

    private BlockPos blockPos;

    public ToggleModePacket() {
    }

    public ToggleModePacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
    }

    public static class Handler implements IMessageHandler<ToggleModePacket, IMessage> {

        @Override
        public IMessage onMessage(ToggleModePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityBasicLightingControl desk = PacketUtil.getTile(ctx, message.blockPos, TileEntityBasicLightingControl.class);
                if (desk != null) {
                    desk.toggleMode();
                    desk.sync();
                }
            });
            return null;
        }
    }
}
