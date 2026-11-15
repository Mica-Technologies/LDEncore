/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: set a moving head's Room Light mode (Show, Room or Auto). */
public class SetRoomModePacket implements IMessage {

    private BlockPos blockPos;
    private int mode;

    public SetRoomModePacket() {
    }

    public SetRoomModePacket(BlockPos blockPos, TileEntityLedFixture.Mode mode) {
        this.blockPos = blockPos;
        this.mode = mode.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        mode = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeByte(mode);
    }

    public static class Handler implements IMessageHandler<SetRoomModePacket, IMessage> {

        @Override
        public IMessage onMessage(SetRoomModePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityIntelligentFixture tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityIntelligentFixture.class);
                TileEntityLedFixture.Mode[] modes = TileEntityLedFixture.Mode.values();
                if (tile != null && message.mode >= 0 && message.mode < modes.length) {
                    tile.setRoomMode(modes[message.mode]);
                }
            });
            return null;
        }
    }
}
