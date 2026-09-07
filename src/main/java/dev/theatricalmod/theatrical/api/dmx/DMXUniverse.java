/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/dmx/DMXUniverse.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: none beyond the port (no Minecraft API involved).
 */
package dev.theatricalmod.theatrical.api.dmx;

import java.util.UUID;

/**
 * One DMX universe: 512 channels of 8-bit values, as bytes.
 */
public class DMXUniverse {

    public static final int CHANNELS = 512;

    private final UUID uuid;
    private byte[] dmxChannels;

    public DMXUniverse() {
        this.uuid = UUID.randomUUID();
        this.dmxChannels = new byte[CHANNELS];
    }

    public byte[] getDMXChannels() {
        return dmxChannels;
    }

    public int getChannel(int index) {
        if (index > CHANNELS - 1 || index < 0) {
            throw new DMXValueOutOfBoundsException("There are only 512 channels in this universe");
        }
        return dmxChannels[index];
    }

    public void setChannel(int index, byte value) {
        dmxChannels[index] = value;
    }

    public void setDmxChannels(byte[] data) {
        dmxChannels = data;
    }

    public UUID getUuid() {
        return uuid;
    }
}
