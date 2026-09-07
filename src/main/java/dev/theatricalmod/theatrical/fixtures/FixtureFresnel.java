/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * fixtures/FixtureFresnel.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: none beyond the port. Model locations are the same "block/..."
 * paths on both versions (they name models, not textures).
 */
package dev.theatricalmod.theatrical.fixtures;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.ChannelsDefinition;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.api.fixtures.FixtureType;
import dev.theatricalmod.theatrical.api.fixtures.HangableType;
import net.minecraft.util.ResourceLocation;

/**
 * The tungsten fresnel: a hook-hung generic light with no DMX footprint of its own (it is
 * dimmed by the power it is fed).
 */
public class FixtureFresnel extends Fixture {

    public static final ResourceLocation ID = new ResourceLocation(TheatricalMod.MOD_ID, "fresnel_fixture");

    public FixtureFresnel() {
        super(ID, FixtureType.TUNGSTEN, HangableType.HOOK_BAR,
            new ResourceLocation(TheatricalMod.MOD_ID, "block/fresnel/fresnel_hook_bar"), new ResourceLocation(TheatricalMod.MOD_ID, "block/fresnel/fresnel_hook"), new ResourceLocation(TheatricalMod.MOD_ID, "block/fresnel/fresnel_body_only"),
            new ResourceLocation(TheatricalMod.MOD_ID, "block/fresnel/fresnel_handle_only"),
            new float[]{0.5F, 0.3F, 0.39F}, new float[]{0.5F, 0, 0.41F}, new float[]{0.5F, 0.24F, 0.1F}, 0, 0.25F, 0, 25, 0,
            255, 0, 0, new ChannelsDefinition());
    }
}
