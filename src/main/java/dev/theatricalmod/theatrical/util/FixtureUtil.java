/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * util/FixtureUtil.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.util;

import dev.theatricalmod.theatrical.api.fixtures.GelType;
import dev.theatricalmod.theatrical.api.fixtures.IGelable;
import dev.theatricalmod.theatrical.api.fixtures.IRGB;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityFixture;

public final class FixtureUtil {

    private FixtureUtil() {
    }

    /** The colour a fixture is currently making: its gel, its RGB mix, or white. */
    public static int getColorFromTile(TileEntityFixture tile) {
        if (tile instanceof IGelable) {
            return ((IGelable) tile).getGel().getHex();
        } else if (tile instanceof IRGB) {
            return ((IRGB) tile).getColorHex();
        }
        return GelType.CLEAR.getHex();
    }
}
