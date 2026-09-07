/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/IAcceptsCable.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: Direction -> EnumFacing.
 */
package dev.theatricalmod.theatrical.api;

import net.minecraft.util.EnumFacing;

/**
 * Implemented by blocks (usually their tile entities) that cables may plug into.
 */
public interface IAcceptsCable {

    CableType[] getAcceptedCables(EnumFacing side);

}
