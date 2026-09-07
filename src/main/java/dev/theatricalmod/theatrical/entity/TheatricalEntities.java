/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * entity/TheatricalEntities.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: EntityEntryBuilder in RegistryEvent.Register<EntityEntry> in
 * place of an EntityType DeferredRegister. Tracking range 10 and update frequency 20 are
 * upstream's values.
 */
package dev.theatricalmod.theatrical.entity;

import dev.theatricalmod.theatrical.TheatricalMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
public final class TheatricalEntities {

    public static final ResourceLocation FALLING_LIGHT_ID = new ResourceLocation(TheatricalMod.MOD_ID, "falling_light");

    private TheatricalEntities() {
    }

    @SubscribeEvent
    public static void onRegisterEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(FallingLightEntity.class)
                .id(FALLING_LIGHT_ID, 0)
                .name(TheatricalMod.MOD_ID + ".falling_light")
                .tracker(10, 20, true)
                .build());
    }
}
