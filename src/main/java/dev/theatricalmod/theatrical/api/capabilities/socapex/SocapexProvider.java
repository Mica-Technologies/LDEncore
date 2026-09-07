/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/SocapexProvider.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - cable runs are followed through the api's ICable interface instead of
 *     `instanceof BlockCable`, so this package no longer depends on the block package;
 *   - 1.12 capability lookups (hasCapability/getCapability), NBT API names;
 *   - the receiver's facing is read from whichever "facing" property its block state has,
 *     rather than from one specific shared property instance, which threw for any block
 *     that declared its own;
 *   - the loop guard upstream added in its final commit (do not re-enter a cable already
 *     scanned) is kept: without it a ring of socapex cable overflows the stack;
 *   - patch() no longer NPEs when called before the first device scan;
 *   - scanDevices() is new, so a rack can find what is wired to it before it has power.
 *     Upstream scanned only from updateDevices, which the rack calls after its power check,
 *     so patching a rack that had never been energised silently did nothing.
 */
package dev.theatricalmod.theatrical.api.capabilities.socapex;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.ICable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SocapexProvider implements ISocapexProvider, INBTSerializable<NBTTagCompound> {

    public static final String[] IDENTIFIERS = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    /** Dimmed channels a rack puts out. */
    public static final int CHANNELS = 6;
    /** Sockets on a receiver. */
    public static final int RECEIVER_SOCKETS = 8;
    /** Two receiver sockets may hang off one dimmer channel. */
    public static final int PATCHES_PER_CHANNEL = 2;

    @CapabilityInject(ISocapexProvider.class)
    public static Capability<ISocapexProvider> CAP = null;

    private int lastIdentifier = -1;
    private HashMap<EnumFacing, BlockPos> devices = null;
    private final int[] channels = new int[CHANNELS];
    private final HashMap<Integer, SocapexPatch[]> patch = new HashMap<>();

    /**
     * Walks outward from {@code pos} along socapex cables until it reaches a receiver, and
     * records the first receiver found per side of the controller.
     */
    public void addToList(HashMap<EnumFacing, BlockPos> scanned, World world, BlockPos pos, EnumFacing facing, EnumFacing connectionSide, HashSet<BlockPos> scannedCable) {
        IBlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof ICable && ((ICable) blockState.getBlock()).getCableType() == CableType.SOCAPEX) {
            scannedCable.add(pos);
            ICable cable = (ICable) blockState.getBlock();
            for (EnumFacing direction : EnumFacing.values()) {
                if (direction != facing) {
                    if (cable.canConnect(world, pos, direction) && !scannedCable.contains(pos.offset(direction))) {
                        addToList(scanned, world, pos.offset(direction), direction.getOpposite(), connectionSide, scannedCable);
                    }
                }
            }
        } else {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null && tileEntity.hasCapability(SocapexReceiver.CAP, facing)) {
                if (!scanned.containsKey(connectionSide)) {
                    scanned.put(connectionSide, pos);
                }
            }
        }
    }

    private void scan(World world, BlockPos controllerPos) {
        HashMap<EnumFacing, BlockPos> receivers = new HashMap<>();
        HashSet<BlockPos> scannedCable = new HashSet<>();
        for (EnumFacing facing : EnumFacing.values()) {
            addToList(receivers, world, controllerPos.offset(facing), facing.getOpposite(), facing, scannedCable);
        }
        scannedCable.clear();
        devices = new HashMap<>(receivers);
    }

    @Override
    public void scanDevices(World world, BlockPos controllerPos) {
        if (devices != null) {
            return;
        }
        if (world.isRemote) {
            devices = new HashMap<>();
            return;
        }
        scan(world, controllerPos);
    }

    @Override
    public void updateDevices(World world, BlockPos controllerPos) {
        scanDevices(world, controllerPos);
        for (EnumFacing direction : devices.keySet()) {
            BlockPos receiverPos = devices.get(direction);
            IBlockState blockState = world.getBlockState(receiverPos);
            if (blockState.getBlock() instanceof ICable) {
                continue;
            }
            TileEntity tile = world.getTileEntity(receiverPos);
            if (tile == null) {
                continue;
            }
            EnumFacing receiverFacing = facingOf(blockState);
            if (tile.hasCapability(SocapexReceiver.CAP, receiverFacing)) {
                ISocapexReceiver receiver = tile.getCapability(SocapexReceiver.CAP, receiverFacing);
                if (receiver != null && hasPatch(receiver)) {
                    int[] drain = receiver.receiveSocapex(getChannelsForReceiver(receiver), false);
                    extractSocapex(drain, false);
                }
            }
        }
    }

    @Override
    public void refreshDevices() {
        devices = null;
    }

    @Override
    public int[] receiveSocapex(int[] channels, boolean simulate) {
        int[] energyReceived = new int[RECEIVER_SOCKETS];
        for (int i = 0; i < Math.min(channels.length, CHANNELS); i++) {
            if (channels[i] > this.channels[i] && !canReceive(i)) {
                energyReceived[i] = 0;
                continue;
            }
            energyReceived[i] = channels[i];
            if (!simulate) {
                this.channels[i] = energyReceived[i];
            }
        }
        return energyReceived;
    }

    @Override
    public int[] extractSocapex(int[] channels, boolean simulate) {
        int[] energyExtracted = new int[RECEIVER_SOCKETS];
        for (int i = 0; i < Math.min(channels.length, CHANNELS); i++) {
            if (!canExtract(i)) {
                energyExtracted[i] = 0;
                continue;
            }
            energyExtracted[i] = Math.min(this.channels[i], Math.min(1000, channels[i]));
            if (!simulate) {
                this.channels[i] -= energyExtracted[i];
            }
        }
        return energyExtracted;
    }

    @Override
    public boolean canReceive(int channel) {
        if (channel >= channels.length) {
            return false;
        }
        return channels[channel] < 255;
    }

    @Override
    public boolean canExtract(int channel) {
        if (channel >= channels.length) {
            return false;
        }
        return channels[channel] > 0;
    }

    @Override
    public SocapexPatch[] getPatch(int channel) {
        if (channel >= channels.length) {
            return null;
        }
        return patch.get(channel);
    }

    @Override
    public void patch(int dmxChannel, ISocapexReceiver receiver, int receiverSocket, int patchSocket) {
        if (devices == null || !devices.containsValue(receiver.getReceiverPos())) {
            return;
        }
        SocapexPatch[] patches;
        if (patch.containsKey(dmxChannel)) {
            patches = patch.get(dmxChannel);
        } else {
            patches = new SocapexPatch[PATCHES_PER_CHANNEL];
        }
        patches[patchSocket - 1] = new SocapexPatch(receiver.getReceiverPos(), receiverSocket);
        patch.put(dmxChannel, patches);
    }

    @Override
    public boolean hasPatch(ISocapexReceiver receiver) {
        for (SocapexPatch[] patches : patch.values()) {
            for (SocapexPatch socapexPatch : patches) {
                if (socapexPatch != null && receiver.getReceiverPos().equals(socapexPatch.getReceiver())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void removePatch(int dmxChannel, int patchSocket) {
        if (patch.containsKey(dmxChannel)) {
            patch.get(dmxChannel)[patchSocket - 1] = new SocapexPatch();
        }
    }

    @Override
    public int[] getChannelsForReceiver(ISocapexReceiver receiver) {
        int[] channels = new int[RECEIVER_SOCKETS];
        if (receiver == null) {
            return channels;
        }
        for (Map.Entry<Integer, SocapexPatch[]> entry : patch.entrySet()) {
            SocapexPatch[] patches = entry.getValue();
            if (patches == null) {
                continue;
            }
            for (SocapexPatch socapexPatch : patches) {
                if (socapexPatch != null && receiver.getReceiverPos().equals(socapexPatch.getReceiver())) {
                    channels[socapexPatch.getReceiverSocket()] = this.channels[entry.getKey()];
                }
            }
        }
        return channels;
    }

    @Override
    public int[] getPatchedCables(ISocapexReceiver socapexReceiver) {
        if (socapexReceiver == null) {
            return new int[0];
        }
        int[] channels = new int[socapexReceiver.getTotalChannels()];
        for (SocapexPatch[] patches : patch.values()) {
            if (patches == null) {
                continue;
            }
            for (SocapexPatch socapexPatch : patches) {
                if (socapexPatch != null && socapexReceiver.getReceiverPos().equals(socapexPatch.getReceiver())) {
                    channels[socapexPatch.getReceiverSocket()] = 1;
                }
            }
        }
        return channels;
    }

    @Override
    public String getIdentifier(BlockPos pos) {
        if (devices == null) {
            return "";
        }
        for (Map.Entry<EnumFacing, BlockPos> entry : devices.entrySet()) {
            if (entry.getValue().equals(pos)) {
                return entry.getKey().getName();
            }
        }
        return "";
    }

    @Override
    public List<ISocapexReceiver> getDevices(World world, BlockPos controller) {
        List<ISocapexReceiver> receivers = new ArrayList<>();
        if (world.isRemote || devices == null) {
            scan(world, controller);
            updateDevices(world, controller);
        }
        for (BlockPos pos : devices.values()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity == null) {
                continue;
            }
            EnumFacing facing = facingOf(world.getBlockState(pos));
            if (tileEntity.hasCapability(SocapexReceiver.CAP, facing)) {
                ISocapexReceiver receiver = tileEntity.getCapability(SocapexReceiver.CAP, facing);
                if (receiver != null) {
                    receivers.add(receiver);
                }
            }
        }
        return receivers;
    }

    /**
     * The block's "facing" property, whichever property instance it declared it with; null
     * if it has none.
     */
    @Nullable
    private static EnumFacing facingOf(IBlockState state) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if ("facing".equals(property.getName()) && property.getValueClass() == EnumFacing.class) {
                return (EnumFacing) state.getValue(property);
            }
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("lastIdentifier", lastIdentifier);
        NBTTagCompound patchTag = new NBTTagCompound();
        for (Map.Entry<Integer, SocapexPatch[]> entry : patch.entrySet()) {
            SocapexPatch[] patches = entry.getValue();
            if (patches == null) {
                continue;
            }
            NBTTagCompound channelTag = new NBTTagCompound();
            for (int socket = 0; socket < PATCHES_PER_CHANNEL; socket++) {
                if (patches[socket] != null) {
                    channelTag.setTag("socket_" + socket, patches[socket].serialize());
                }
            }
            patchTag.setTag("patch_" + entry.getKey(), channelTag);
        }
        tag.setTag("patch", patchTag);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("lastIdentifier")) {
            lastIdentifier = nbt.getInteger("lastIdentifier");
        }
        if (nbt.hasKey("patch")) {
            patch.clear();
            NBTTagCompound patchTag = nbt.getCompoundTag("patch");
            for (int i = 0; i < CHANNELS; i++) {
                if (patchTag.hasKey("patch_" + i)) {
                    NBTTagCompound channelTag = patchTag.getCompoundTag("patch_" + i);
                    SocapexPatch[] patches = new SocapexPatch[PATCHES_PER_CHANNEL];
                    for (int socket = 0; socket < PATCHES_PER_CHANNEL; socket++) {
                        if (channelTag.hasKey("socket_" + socket)) {
                            SocapexPatch socapexPatch = new SocapexPatch();
                            socapexPatch.deserialize(channelTag.getCompoundTag("socket_" + socket));
                            patches[socket] = socapexPatch;
                        }
                    }
                    patch.put(i, patches);
                }
            }
        }
    }
}
