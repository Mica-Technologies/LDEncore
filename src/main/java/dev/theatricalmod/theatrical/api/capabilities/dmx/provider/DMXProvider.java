/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/dmx/provider/DMXProvider.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - cable runs are followed through the api's ICable interface instead of
 *     `instanceof BlockCable`, so this package no longer depends on the block package;
 *   - capability lookups use 1.12's hasCapability/getCapability pair (no LazyOptional);
 *   - serializeNBT returns an empty tag rather than null (the provider carries no saved
 *     state; the universe is owned by whoever created it).
 */
package dev.theatricalmod.theatrical.api.capabilities.dmx.provider;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.ICable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;

public class DMXProvider implements IDMXProvider, INBTSerializable<NBTTagCompound> {

    @CapabilityInject(IDMXProvider.class)
    public static Capability<IDMXProvider> CAP = null;

    private DMXUniverse dmxUniverse;
    private HashSet<BlockPos> devices = null;

    public DMXProvider() {
    }

    public DMXProvider(DMXUniverse dmxUniverse) {
        this.dmxUniverse = dmxUniverse;
    }

    @Override
    public byte[] sendDMXValues(DMXUniverse dmxUniverse) {
        return dmxUniverse.getDMXChannels();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
    }

    @Override
    public DMXUniverse getUniverse(World world) {
        return dmxUniverse;
    }

    /**
     * Walks outward from {@code pos}: along DMX cables, and through any receiver it finds
     * (receivers pass DMX on, so a fixture can daisy-chain to the next).
     */
    public void addToList(HashSet<BlockPos> scanned, World world, BlockPos pos, EnumFacing facing, HashSet<BlockPos> scannedCable) {
        IBlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof ICable && ((ICable) blockState.getBlock()).getCableType() == CableType.DMX) {
            scannedCable.add(pos);
            ICable cable = (ICable) blockState.getBlock();
            for (EnumFacing direction : EnumFacing.values()) {
                if (direction != facing) {
                    if (cable.canConnect(world, pos, direction) && !scannedCable.contains(pos.offset(direction))) {
                        addToList(scanned, world, pos.offset(direction), direction.getOpposite(), scannedCable);
                    }
                }
            }
        } else {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null && tileEntity.hasCapability(DMXReceiver.CAP, facing)) {
                if (scanned.add(pos)) {
                    for (EnumFacing facing1 : EnumFacing.values()) {
                        if (facing1 != facing) {
                            addToList(scanned, world, pos.offset(facing1), facing1.getOpposite(), scannedCable);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updateDevices(World world, BlockPos controllerPos) {
        if (devices == null) {
            HashSet<BlockPos> receivers = new HashSet<>();
            HashSet<BlockPos> scannedCable = new HashSet<>();
            for (EnumFacing facing : EnumFacing.values()) {
                addToList(receivers, world, controllerPos.offset(facing), facing.getOpposite(), scannedCable);
            }
            scannedCable.clear();
            devices = new HashSet<>(receivers);
        }
        for (BlockPos receiver : devices) {
            IBlockState blockState = world.getBlockState(receiver);
            if (blockState.getBlock() instanceof ICable) {
                continue;
            }
            TileEntity tile = world.getTileEntity(receiver);
            if (tile != null && tile.hasCapability(DMXReceiver.CAP, null)) {
                IDMXReceiver idmxReceiver = tile.getCapability(DMXReceiver.CAP, null);
                if (idmxReceiver != null) {
                    idmxReceiver.receiveDMXValues(dmxUniverse.getDMXChannels(), world, receiver);
                }
            }
        }
    }

    @Override
    public void refreshDevices() {
        devices = null;
    }
}
