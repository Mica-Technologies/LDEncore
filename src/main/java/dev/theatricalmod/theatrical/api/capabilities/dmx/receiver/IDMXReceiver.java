/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/dmx/receiver/IDMXReceiver.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.api.capabilities.dmx.receiver;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Something addressed on the DMX universe: a fixture, a dimmer rack, a redstone interface.
 */
public interface IDMXReceiver {

    int getChannelCount();

    int getStartPoint();

    void receiveDMXValues(byte[] data, World world, BlockPos pos);

    byte getChannel(int index);

    void updateChannel(int index, byte value);

    void setDMXStartPoint(int dmxStartPoint);

    void setChannelCount(int channelCount);
}
