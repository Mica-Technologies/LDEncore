/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/ChannelsDefinition.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: none beyond the port. commons-lang3 ships with 1.12.2 as it does
 * with 1.16, so StringUtils stays.
 */
package dev.theatricalmod.theatrical.api;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps the roles a fixture responds to (intensity, colour, pan, tilt...) onto DMX channel
 * offsets relative to the fixture's start address.
 */
public class ChannelsDefinition {

    private final Map<ChannelType, Integer> channels;

    public ChannelsDefinition() {
        this.channels = new HashMap<>();
    }

    public void setChannel(ChannelType type, int channel) {
        channels.remove(type);
        channels.put(type, channel);
    }

    public int getChannel(ChannelType type) {
        return channels.getOrDefault(type, 0);
    }

    @Override
    public String toString() {
        // Sort into ascending order of channel
        StringBuilder string = new StringBuilder();
        channels.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(entry -> string.append(String.format("#%d: %s", entry.getValue(), StringUtils.capitalize(entry.getKey().name().toLowerCase()))));
        return string.toString();
    }
}
