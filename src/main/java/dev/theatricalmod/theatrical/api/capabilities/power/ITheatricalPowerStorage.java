/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/power/ITheatricalPowerStorage.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port (no Minecraft API involved).
 */
package dev.theatricalmod.theatrical.api.capabilities.power;

/**
 * The mod's own power capability: the "mains" that dimmer racks and distros push into
 * fixtures. Deliberately not Forge Energy, so ordinary FE machines cannot be wired into a
 * dimmer.
 */
public interface ITheatricalPowerStorage {

    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();

}
