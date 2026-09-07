/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * fixtures/TheatricalFixtures.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - this class now owns the registry lifecycle: it creates the fixture registry in
 *     RegistryEvent.NewRegistry and registers the built-in fixtures in
 *     RegistryEvent.Register, where upstream posted a Register event by hand from the mod
 *     constructor and registered from a listener there;
 *   - the FRESNSEL typo is corrected to FRESNEL.
 */
package dev.theatricalmod.theatrical.fixtures;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The fixtures this mod ships, and the registry they live in.
 */
@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
public final class TheatricalFixtures {

    public static final Fixture MOVING_LIGHT = new MovingLightFixture();
    public static final Fixture FRESNEL = new FixtureFresnel();

    private TheatricalFixtures() {
    }

    @SubscribeEvent
    public static void onNewRegistry(RegistryEvent.NewRegistry event) {
        Fixture.createRegistry();
    }

    @SubscribeEvent
    public static void onRegisterFixtures(RegistryEvent.Register<Fixture> event) {
        // Registry names are applied here, inside the event, so the active mod container is
        // ours when Forge checks the namespace prefix.
        event.getRegistry().register(MOVING_LIGHT.setRegistryName(MOVING_LIGHT.getName()));
        event.getRegistry().register(FRESNEL.setRegistryName(FRESNEL.getName()));
        TheatricalMod.LOGGER.info("Registered {} fixtures", event.getRegistry().getKeys().size());
    }
}
