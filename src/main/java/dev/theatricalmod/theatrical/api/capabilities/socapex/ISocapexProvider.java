/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/ISocapexProvider.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: scanDevices is new. Upstream only ever looked for receivers from
 * inside updateDevices, which a dimmer rack calls after its power check, so an unpowered
 * rack never knew what was connected to it and rejected every patch the player made.
 */
package dev.theatricalmod.theatrical.api.capabilities.socapex;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * The dimmer-rack side of a socapex multicore: six dimmed channels, patched onto the
 * sockets of the receivers (distros) found down the cable.
 */
public interface ISocapexProvider {

    void updateDevices(World world, BlockPos controllerPos);

    /**
     * Makes sure the receiver list is populated, without moving any power. Patching is
     * configuration, so it has to work on a rack that has never been energised.
     */
    void scanDevices(World world, BlockPos controllerPos);

    void refreshDevices();

    int[] receiveSocapex(int[] channels, boolean simulate);

    int[] extractSocapex(int[] channels, boolean simulate);

    boolean canReceive(int channel);

    boolean canExtract(int channel);

    SocapexPatch[] getPatch(int channel);

    void patch(int dmxChannel, ISocapexReceiver receiver, int receiverSocket, int patchSocket);

    void removePatch(int dmxChannel, int patchSocket);

    boolean hasPatch(ISocapexReceiver receiver);

    int[] getChannelsForReceiver(ISocapexReceiver receiver);

    List<ISocapexReceiver> getDevices(World world, BlockPos controller);

    int[] getPatchedCables(ISocapexReceiver socapexReceiver);

    String getIdentifier(BlockPos pos);
}
