/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/UpdateDMXAddressPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12's IMessage/IMessageHandler pair in place of the 1.16
 * SimpleChannel functional form; the target is resolved through PacketUtil, so a client
 * cannot readdress a block it is nowhere near; the address is clamped to a real DMX
 * address rather than trusted.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: set a DMX receiver's start address. */
public class UpdateDMXAddressPacket implements IMessage {

    private BlockPos blockPos;
    private int address;

    public UpdateDMXAddressPacket() {
    }

    public UpdateDMXAddressPacket(BlockPos blockPos, int address) {
        this.blockPos = blockPos;
        this.address = address;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        address = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(address);
    }

    public static class Handler implements IMessageHandler<UpdateDMXAddressPacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateDMXAddressPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntity tile = PacketUtil.getTile(ctx, message.blockPos, TileEntity.class);
                if (tile == null || !tile.hasCapability(DMXReceiver.CAP, null)) {
                    return;
                }
                IDMXReceiver receiver = tile.getCapability(DMXReceiver.CAP, null);
                if (receiver != null) {
                    receiver.setDMXStartPoint(MathHelper.clamp(message.address, 0, DMXUniverse.CHANNELS - 1));
                    tile.markDirty();
                    tile.getWorld().notifyBlockUpdate(message.blockPos, tile.getWorld().getBlockState(message.blockPos),
                            tile.getWorld().getBlockState(message.blockPos), 3);
                }
            });
            return null;
        }
    }
}
