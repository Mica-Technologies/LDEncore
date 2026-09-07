/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/dmx/provider/IDMXProvider.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.api.capabilities.dmx.provider;

import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Something that owns a DMX universe and pushes it down the cables to the receivers it
 * finds: an Art-Net interface, a lighting desk, a redstone interface.
 */
public interface IDMXProvider {

    byte[] sendDMXValues(DMXUniverse dmxUniverse);

    DMXUniverse getUniverse(World world);

    void updateDevices(World world, BlockPos controllerPos);

    void refreshDevices();
}
