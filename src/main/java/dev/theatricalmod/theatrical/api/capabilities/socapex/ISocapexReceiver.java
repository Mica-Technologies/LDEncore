/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/ISocapexReceiver.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.api.capabilities.socapex;

import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * The far end of a socapex multicore: a distro whose sockets feed individual fixtures.
 */
public interface ISocapexReceiver {

    int[] receiveSocapex(int[] channels, boolean simulate);

    int[] extractSocapex(int[] channels, boolean simulate);

    int getEnergyStored(int channel);

    int getMaxEnergyStored(int channel);

    boolean canExtract(int channel);

    boolean canReceive(int channel);

    BlockPos getReceiverPos();

    List<BlockPos> getDevices();

    int getTotalChannels();
}
