/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies; there is no upstream file this
 * corresponds to.
 *
 * CHANGED FROM UPSTREAM: this class exists only because 1.12 has no equivalent of 1.16's
 * ModelLoader.addSpecialModel. Upstream simply asks the model manager for a fixture's pan,
 * tilt and static models by name, because on 1.16 those models were registered for baking by
 * one call. On 1.12 the model manager only knows models reachable from a blockstate or an item
 * model, and a fixture's parts are reachable from neither, so this loads and bakes them itself
 * during ModelBakeEvent and hands the results to the renderers. The textures they reference are
 * registered during TextureStitchEvent, which is upstream's textureStitch listener translated.
 */
package dev.theatricalmod.theatrical.client.model;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Bakes and holds the fixture part models the fixture renderers draw. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID, value = Side.CLIENT)
public final class FixtureModels {

    private static final Map<ResourceLocation, IBakedModel> BAKED = new HashMap<>();

    private FixtureModels() {
    }

    /**
     * The baked model for one of a fixture's parts, or null if it could not be loaded. Callers
     * must tolerate null: a resource pack can remove a model, and a fixture added by another
     * mod may not ship every part.
     */
    @Nullable
    public static IBakedModel get(@Nullable ResourceLocation location) {
        return location == null ? null : BAKED.get(location);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        if (Fixture.getRegistry() == null) {
            return;
        }
        for (Fixture fixture : Fixture.getRegistry()) {
            for (ResourceLocation texture : fixture.getTextures()) {
                if (texture != null) {
                    event.getMap().registerSprite(texture);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        BAKED.clear();
        if (Fixture.getRegistry() == null) {
            return;
        }
        for (Fixture fixture : Fixture.getRegistry()) {
            bake(fixture.getStaticModelLocation());
            bake(fixture.getHookedModelLocation());
            bake(fixture.getPanModelLocation());
            bake(fixture.getTiltModelLocation());
        }
        TheatricalMod.LOGGER.info("Baked {} fixture part models", BAKED.size());
    }

    private static void bake(@Nullable ResourceLocation location) {
        if (location == null || BAKED.containsKey(location)) {
            return;
        }
        try {
            IModel model = ModelLoaderRegistry.getModel(location);
            BAKED.put(location, model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
                    ModelLoader.defaultTextureGetter()));
        } catch (Exception e) {
            // A missing part model costs that piece of the fixture, not the game.
            TheatricalMod.LOGGER.error("Could not bake fixture model {}", location, e);
        }
    }
}
