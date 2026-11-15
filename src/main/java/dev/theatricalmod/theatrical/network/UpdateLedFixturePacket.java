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

/** Client -> server: set an LED fixture's (and its run's) address, mode and pixel mode. */
public class UpdateLedFixturePacket implements IMessage {

    private BlockPos blockPos;
    private int address;
    private int mode;
    private boolean pixel;

    public UpdateLedFixturePacket() {
    }

    public UpdateLedFixturePacket(BlockPos blockPos, int address, TileEntityLedFixture.Mode mode, boolean pixel) {
        this.blockPos = blockPos;
        this.address = address;
        this.mode = mode.ordinal();
        this.pixel = pixel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        address = buf.readShort();
        mode = buf.readByte();
        pixel = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeShort(address);
        buf.writeByte(mode);
        buf.writeBoolean(pixel);
    }

    public static class Handler implements IMessageHandler<UpdateLedFixturePacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateLedFixturePacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityLedFixture tile = PacketUtil.getTile(ctx, message.blockPos, TileEntityLedFixture.class);
                if (tile == null) {
                    return;
                }
                TileEntityLedFixture.Mode[] modes = TileEntityLedFixture.Mode.values();
                if (message.mode < 0 || message.mode >= modes.length) {
                    return;
                }
                // The setter clamps the address to the universe.
                tile.applyToRun(message.address, modes[message.mode], message.pixel && tile.canRun());
            });
            return null;
        }
    }
}
