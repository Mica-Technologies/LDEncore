/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/fixtures/IFixtureModelProvider.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port.
 */
package dev.theatricalmod.theatrical.api.fixtures;

import net.minecraft.util.ResourceLocation;

/**
 * Everything the renderer needs to draw a fixture: which model parts, where they pivot,
 * where the beam leaves the body.
 */
public interface IFixtureModelProvider {

    HangableType getHangType();

    ResourceLocation getStaticModel();

    ResourceLocation getTiltModel();

    ResourceLocation getPanModel();

    float[] getTiltRotationPosition();

    float[] getPanRotationPosition();

    float getDefaultRotation();

    float[] getBeamStartPosition();

    float getBeamWidth();

    float getRayTraceRotation();
}
