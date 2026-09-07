/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/ChangeDimmerPatchPacket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 IMessage plumbing, and the patch travels as an NBT tag through ByteBufUtils;
 *   - the rack is resolved through PacketUtil, and the channel and socket numbers are
 *     range-checked before they index the provider's arrays. Upstream passed both straight
 *     through, so a malformed packet indexed out of bounds inside the tick loop;
 *   - the receiver's facing is read from whichever "facing" property its block has, matching
 *     the socapex provider.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexProvider;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexPatch;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexProvider;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexReceiver;
import dev.theatricalmod.theatrical.tiles.power.TileEntityDimmerRack;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/** Client -> server: patch or unpatch one dimmer channel onto a distro socket. */
public class ChangeDimmerPatchPacket implements IMessage {

    private BlockPos blockPos;
    private int channel;
    private int socketNumber;
    private SocapexPatch patch;

    public ChangeDimmerPatchPacket() {
    }

    public ChangeDimmerPatchPacket(BlockPos blockPos, int channel, int socketNumber, SocapexPatch patch) {
        this.blockPos = blockPos;
        this.channel = channel;
        this.socketNumber = socketNumber;
        this.patch = patch;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = BlockPos.fromLong(buf.readLong());
        channel = buf.readInt();
        socketNumber = buf.readInt();
        patch = new SocapexPatch();
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag != null) {
            patch.deserialize(tag);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(blockPos.toLong());
        buf.writeInt(channel);
        buf.writeInt(socketNumber);
        ByteBufUtils.writeTag(buf, patch.serialize());
    }

    public static class Handler implements IMessageHandler<ChangeDimmerPatchPacket, IMessage> {

        @Override
        public IMessage onMessage(ChangeDimmerPatchPacket message, MessageContext ctx) {
            PacketUtil.onServerThread(ctx, () -> {
                TileEntityDimmerRack rack = PacketUtil.getTile(ctx, message.blockPos, TileEntityDimmerRack.class);
                if (rack == null) {
                    return;
                }
                if (message.channel < 0 || message.channel >= SocapexProvider.CHANNELS) {
                    return;
                }
                if (message.socketNumber < 1 || message.socketNumber > SocapexProvider.PATCHES_PER_CHANNEL) {
                    return;
                }
                ISocapexProvider provider = rack.getSocapexProvider();
                BlockPos receiverPos = message.patch.getReceiver();
                if (receiverPos == null) {
                    provider.removePatch(message.channel, message.socketNumber);
                    rack.sync();
                    return;
                }
                if (message.patch.getReceiverSocket() < 0 || message.patch.getReceiverSocket() >= SocapexReceiver.CHANNELS) {
                    return;
                }
                TileEntity receiverTile = rack.getWorld().getTileEntity(receiverPos);
                if (receiverTile == null) {
                    return;
                }
                EnumFacing facing = facingOf(rack.getWorld().getBlockState(receiverPos));
                if (!receiverTile.hasCapability(SocapexReceiver.CAP, facing)) {
                    return;
                }
                ISocapexReceiver receiver = receiverTile.getCapability(SocapexReceiver.CAP, facing);
                if (receiver != null) {
                    provider.patch(message.channel, receiver, message.patch.getReceiverSocket(), message.socketNumber);
                    rack.sync();
                }
            });
            return null;
        }

        @Nullable
        private static EnumFacing facingOf(IBlockState state) {
            for (IProperty<?> property : state.getPropertyKeys()) {
                if ("facing".equals(property.getName()) && property.getValueClass() == EnumFacing.class) {
                    return (EnumFacing) state.getValue(property);
                }
            }
            return null;
        }
    }
}
