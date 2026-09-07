/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/fixtures/Fixture.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's registry API: IForgeRegistryEntry.Impl, and the registry is created in
 *     RegistryEvent.NewRegistry (see createRegistry's caller) rather than by posting a
 *     Register event by hand;
 *   - the baked model handles (IBakedModel fields and their accessors) are gone from this
 *     class. IBakedModel is a client-only class, and a dedicated server has no such class to
 *     load; the client keeps its baked models in its own cache keyed by fixture.
 */
package dev.theatricalmod.theatrical.api.fixtures;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.ChannelsDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

/**
 * A kind of light: its models, pivots, beam geometry, power draw and DMX footprint. Registry
 * entries, so other mods can add fixtures.
 */
public class Fixture extends IForgeRegistryEntry.Impl<Fixture> {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(TheatricalMod.MOD_ID, "fixtures");

    private static ForgeRegistry<Fixture> REGISTRY;

    /**
     * Creates the fixture registry. Must run during {@code RegistryEvent.NewRegistry}, before
     * any {@code RegistryEvent.Register} fires.
     */
    public static void createRegistry() {
        if (REGISTRY == null) {
            IForgeRegistry<Fixture> registry = new RegistryBuilder<Fixture>()
                    .setName(REGISTRY_NAME)
                    .setType(Fixture.class)
                    .create();
            REGISTRY = (ForgeRegistry<Fixture>) registry;
        }
    }

    public static ForgeRegistry<Fixture> getRegistry() {
        return REGISTRY;
    }

    private final ResourceLocation name;
    private final FixtureType fixtureType;
    private final HangableType hangableType;
    private final ResourceLocation staticModelLocation;
    private final ResourceLocation hookedModelLocation;
    private final ResourceLocation tiltModelLocation;
    private final ResourceLocation panModelLocation;
    private final ResourceLocation[] textures;
    private final float[] tiltRotationPosition;
    private final float[] panRotationPosition;
    private final float[] beamStartPosition;
    private final float defaultRotation;
    private final float beamWidth;
    private final float rayTraceRotation;
    private final float maxLightDistance;
    private final int maxEnergy;
    private final int energyUse;
    private final int energyUseTimer;
    private final int channelCount;
    private final ChannelsDefinition channelsDefinition;

    /**
     * An instance of a fixture
     *
     * @param name Name of Fixture
     * @param fixtureType The Type of Fixture
     * @param hangableType How the fixture hangs
     * @param staticModelLocation The location of the static model
     * @param hookedModelLocation The location of the hooked model
     * @param tiltModelLocation The location of the model that tilts
     * @param panModelLocation The Location of the model that pans
     * @param tiltRotationPosition The middle of the tilt rotation area
     * @param panRotationPosition The middle of the pan rotation area
     * @param beamStartPosition The location of where the beam starts
     * @param defaultRotation The default rotation of the model
     * @param beamWidth The width of the beam
     * @param rayTraceRotation Any extra raytracing rotation
     * @param maxLightDistance How far the beam reaches
     * @param maxEnergy Power buffer size
     * @param energyUse Power drawn every energyUseTimer ticks
     * @param energyUseTimer Ticks between power draws
     * @param channelCount DMX channels the fixture occupies
     * @param channelsDefinition Which channel offset does what
     * @param textures Textures the fixture's models need registered
     */
    public Fixture(ResourceLocation name, FixtureType fixtureType, HangableType hangableType, ResourceLocation staticModelLocation, ResourceLocation hookedModelLocation, ResourceLocation tiltModelLocation, ResourceLocation panModelLocation, float[] tiltRotationPosition, float[] panRotationPosition, float[] beamStartPosition, float defaultRotation, float beamWidth, float rayTraceRotation, float maxLightDistance, int maxEnergy, int energyUse, int energyUseTimer, int channelCount, ChannelsDefinition channelsDefinition, ResourceLocation... textures) {
        this.name = name;
        this.setRegistryName(name);
        this.fixtureType = fixtureType;
        this.hangableType = hangableType;
        this.staticModelLocation = staticModelLocation;
        this.hookedModelLocation = hookedModelLocation;
        this.tiltModelLocation = tiltModelLocation;
        this.panModelLocation = panModelLocation;
        this.tiltRotationPosition = tiltRotationPosition;
        this.panRotationPosition = panRotationPosition;
        this.beamStartPosition = beamStartPosition;
        this.defaultRotation = defaultRotation;
        this.beamWidth = beamWidth;
        this.rayTraceRotation = rayTraceRotation;
        this.maxLightDistance = maxLightDistance;
        this.energyUse = energyUse;
        this.energyUseTimer = energyUseTimer;
        this.channelCount = channelCount;
        this.channelsDefinition = channelsDefinition;
        this.maxEnergy = maxEnergy;
        this.textures = textures;
    }

    public ResourceLocation getName() {
        return name;
    }

    public HangableType getHangableType() {
        return hangableType;
    }

    public ResourceLocation getStaticModelLocation() {
        return staticModelLocation;
    }

    public ResourceLocation getTiltModelLocation() {
        return tiltModelLocation;
    }

    public ResourceLocation getPanModelLocation() {
        return panModelLocation;
    }

    public ResourceLocation getHookedModelLocation() {
        return hookedModelLocation;
    }

    public float[] getTiltRotationPosition() {
        return tiltRotationPosition;
    }

    public float[] getPanRotationPosition() {
        return panRotationPosition;
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

    public FixtureType getFixtureType() {
        return fixtureType;
    }

    public float getMaxLightDistance() {
        return maxLightDistance;
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

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public ResourceLocation[] getTextures() {
        return textures;
    }
}
