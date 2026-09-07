/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/ISocapexProvider.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
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
