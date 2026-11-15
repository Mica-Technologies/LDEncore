/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: aim a track head. */
public class AimLedFixturePacket implements IMessage {

    private BlockPos blockPos;
    private int pan;
    private int tilt;

    public AimLedFixturePacket() {
    }

    public AimLedFixturePacket(BlockPos blockPos, int pan, int tilt) {
        this.blockPos = blockPos;
        this.pan = pan;
        this.tilt = tilt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        pan = buf.readShort();
        tilt = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeShort(pan);
        buf.writeShort(tilt);
    }

    public static class Handler implements IMessageHandler<AimLedFixturePacket, IMessage> {

        @Override
        public IMessage onMessage(AimLedFixturePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityLedFixture tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityLedFixture.class);
                if (tile != null && tile.isAimed()) {
                    // setAim wraps the pan and clamps the tilt.
                    tile.setAim(message.pan, message.tilt);
                }
            });
            return null;
        }
    }
}
