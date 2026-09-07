/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/fixtures/IFixture.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.api.fixtures;

import net.minecraft.block.Block;

/**
 * The runtime side of a fixture: what its tile entity reports about the light it is making.
 */
public interface IFixture {

    float getIntensity();

    float getMaxLightDistance();

    Class<? extends Block> getBlock();

    boolean shouldTrace();

    boolean emitsLight();

    boolean isUpsideDown();

}
