/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/TheatricalNetworkHandler.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: placeholder for the networking phase. The tiles already call the
 * two sync methods where upstream sent its packets, so that the packet layer can be dropped
 * in without touching them again; until then the methods do nothing, which only means the
 * client's copy of a provider's universe is not kept live (the server-side simulation is
 * unaffected).
 */
package dev.theatricalmod.theatrical.network;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TheatricalNetworkHandler {

    private TheatricalNetworkHandler() {
    }

    public static void init() {
    }

    /**
     * Sends a DMX provider's whole universe to every client in the world, so their copy of
     * the provider tile can show it.
     */
    public static void sendProviderUniverse(World world, BlockPos pos, byte[] data) {
    }

    /**
     * Sends a receiver's current channel values to every client in the world.
     */
    public static void sendReceiverValues(World world, BlockPos pos, byte[] data) {
    }
}
