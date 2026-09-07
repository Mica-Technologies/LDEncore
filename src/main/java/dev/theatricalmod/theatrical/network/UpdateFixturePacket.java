/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/UpdateFixturePacket.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 IMessage plumbing; the target is resolved through PacketUtil;
 * pan and tilt are clamped to the range the screen's own sliders allow.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.tiles.lights.TileEntityGenericFixture;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: aim a generic fixture. */
public class UpdateFixturePacket implements IMessage {

    private BlockPos blockPos;
    private int tilt;
    private int pan;

    public UpdateFixturePacket() {
    }

    public UpdateFixturePacket(BlockPos blockPos, int tilt, int pan) {
        this.blockPos = blockPos;
        this.tilt = tilt;
        this.pan = pan;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        tilt = buf.readInt();
        pan = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(tilt);
        buf.writeInt(pan);
    }

    public static class Handler implements IMessageHandler<UpdateFixturePacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateFixturePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityGenericFixture tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityGenericFixture.class);
                if (tile == null) {
                    return;
                }
                tile.setPan(MathHelper.clamp(message.pan, -180, 180));
                tile.setTilt(MathHelper.clamp(message.tilt, -180, 180));
            });
            return null;
        }
    }
}
