/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/TileEntityTestDMX.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 capability and lifecycle plumbing (ITickable, onLoad,
 * invalidate); the client sync goes through TheatricalNetworkHandler.
 */
package dev.theatricalmod.theatrical.tiles;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * Debug block: a DMX provider that fills its universe with random values once a second.
 */
public class TileEntityTestDMX extends TileEntity implements ITickable {

    private final IDMXProvider idmxProvider = new DMXProvider(new DMXUniverse());
    private final Random random = new Random();

    private int ticks = 0;

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        ticks++;
        if (ticks >= 20) {
            byte[] data = generateRandomDMX();
            this.idmxProvider.getUniverse(world).setDmxChannels(data);
            TheatricalNetworkHandler.sendProviderUniverse(world, pos, data);
            sendDMXSignal();
            ticks = 0;
        }
    }

    public byte[] generateRandomDMX() {
        byte[] dmx = new byte[DMXUniverse.CHANNELS];
        random.nextBytes(dmx);
        return dmx;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXProvider.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXProvider.CAP) {
            return DMXProvider.CAP.cast(idmxProvider);
        }
        return super.getCapability(capability, facing);
    }

    public void sendDMXSignal() {
        idmxProvider.updateDevices(world, pos);
    }

    @Override
    public void invalidate() {
        TheatricalMod.refreshDmxNetwork(world);
        super.invalidate();
    }

    @Override
    public void onLoad() {
        TheatricalMod.refreshDmxNetwork(world);
    }
}
