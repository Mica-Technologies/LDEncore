/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/control/ConsoleGoPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the desk is resolved through PacketUtil;
 * the fade times are clamped, where a zero or negative one reached upstream's per-tick
 * division and threw; and the new cue state is sent back to the players watching the block
 * rather than only saved.
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

/** Client -> server: press Go on the lighting desk, with the fade times shown on screen. */
public class ConsoleGoPacket implements IMessage {

    /** An hour of fade is far beyond any use and keeps the per-tick division well away from zero. */
    private static final int MAX_FADE_TICKS = 72000;

    private BlockPos blockPos;
    private int fadeInTicks;
    private int fadeOutTicks;

    public ConsoleGoPacket() {
    }

    public ConsoleGoPacket(BlockPos blockPos, int fadeInTicks, int fadeOutTicks) {
        this.blockPos = blockPos;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        fadeInTicks = buf.readInt();
        fadeOutTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(fadeInTicks);
        buf.writeInt(fadeOutTicks);
    }

    public static class Handler implements IMessageHandler<ConsoleGoPacket, IMessage> {

        @Override
        public IMessage onMessage(ConsoleGoPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityBasicLightingControl desk = PacketUtil.getTile(ctx, message.blockPos, TileEntityBasicLightingControl.class);
                if (desk == null) {
                    return;
                }
                desk.setFadeInTicks(MathHelper.clamp(message.fadeInTicks, 0, MAX_FADE_TICKS));
                desk.setFadeOutTicks(MathHelper.clamp(message.fadeOutTicks, 0, MAX_FADE_TICKS));
                desk.clickButton();
                desk.sync();
            });
            return null;
        }
    }
}
