/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/fixtures/CustomFixture.java (Theatrical Team, Apache License 2.0).
 *
 * CHANGED FROM UPSTREAM: none beyond the port (plain data holder, no Minecraft API).
 */
package dev.theatricalmod.theatrical.api.fixtures;

import dev.theatricalmod.theatrical.api.ChannelsDefinition;

/**
 * A fixture definition as it would be read from a data file. Nothing loads these yet on
 * either line; kept so the shape is ready when a JSON fixture loader comes back.
 */
public class CustomFixture {

    private String name;
    private FixtureType fixtureType;
    private HangableType hangableType;
    private String staticModel;
    private String hookedModel;
    private String tiltModel;
    private String panModel;
    private float[] tiltRotation;
    private float[] panRotation;
    private float[] beamStartPosition;
    private float defaultRotation;
    private float beamWidth;
    private float rayTraceRotation;
    private float maxLightDistance;
    private int maxEnergy;
    private int energyUse;
    private int energyUseTimer;
    private int channelCount;
    private ChannelsDefinition channelsDefinition;

    public String getName() {
        return name;
    }

    public FixtureType getFixtureType() {
        return fixtureType;
    }

    public HangableType getHangableType() {
        return hangableType;
    }

    public String getStaticModel() {
        return staticModel;
    }

    public String getHookedModel() {
        return hookedModel;
    }

    public String getTiltModel() {
        return tiltModel;
    }

    public String getPanModel() {
        return panModel;
    }

    public float[] getTiltRotation() {
        return tiltRotation;
    }

    public float[] getPanRotation() {
        return panRotation;
    }

    public float[] getBeamStartPosition() {
        return beamStartPosition;
    }

    public float getDefaultRotation() {
        return defaultRotation;
    }

    public float getBeamWidth() {
        return beamWidth;
    }

    public float getRayTraceRotation() {
        return rayTraceRotation;
    }

    public float getMaxLightDistance() {
        return maxLightDistance;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getEnergyUse() {
        return energyUse;
    }

    public int getEnergyUseTimer() {
        return energyUseTimer;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public ChannelsDefinition getChannelsDefinition() {
        return channelsDefinition;
    }
}
