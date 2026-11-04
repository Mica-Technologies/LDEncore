/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.network.control;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: flip one of the lighting desk's playback options from its screen. */
public class DeskOptionPacket implements IMessage {

    public static final int LOOP = 0;
    public static final int AUTO_START = 1;
    public static final int STOP = 2;

    private BlockPos blockPos;
    private int option;

    public DeskOptionPacket() {
    }

    public DeskOptionPacket(BlockPos blockPos, int option) {
        this.blockPos = blockPos;
        this.option = option;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        option = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeByte(option);
    }

    public static class Handler implements IMessageHandler<DeskOptionPacket, IMessage> {

        @Override
        public IMessage onMessage(DeskOptionPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityBasicLightingControl desk = PacketUtil.getTile(ctx, message.blockPos, TileEntityBasicLightingControl.class);
                if (desk == null) {
                    return;
                }
                switch (message.option) {
                    case LOOP:
                        desk.setLoop(!desk.isLoop());
                        break;
                    case AUTO_START:
                        desk.setAutoStart(!desk.isAutoStart());
                        break;
                    case STOP:
                        desk.stop();
                        break;
                    default:
                        return;
                }
                desk.sync();
            });
            return null;
        }
    }
}
