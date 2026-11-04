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
 *
 * It also keeps each part model compiled into an OpenGL display list. 1.12's
 * renderModelBrightnessColor issues one tessellator draw per quad, so a moving head (three parts,
 * hundreds of quads) cost hundreds of draw calls a frame; replaying a list is one. The lists are
 * released and rebuilt whenever the models are re-baked, since a resource reload moves the UVs.
 */
package dev.theatricalmod.theatrical.client.model;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
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
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Bakes and holds the fixture part models the fixture renderers draw. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID, value = Side.CLIENT)
public final class FixtureModels {

    private static final Map<ResourceLocation, IBakedModel> BAKED = new HashMap<>();
    /** A compiled display list per baked part, built the first time the part is drawn. */
    private static final Map<IBakedModel, Integer> LISTS = new IdentityHashMap<>();

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

    /**
     * Draws a part model in its own local coordinates at full brightness and white, from its
     * display list (compiled on first use). The caller sets the lightmap, the texture and the
     * transforms; none of that is recorded in the list, so one list serves every fixture.
     */
    public static void draw(IBakedModel model) {
        Integer list = LISTS.get(model);
        if (list == null) {
            int id = GLAllocation.generateDisplayLists(1);
            if (id == 0) {
                // No list could be allocated (the driver is out of names): draw it the slow way.
                drawDirect(model);
                return;
            }
            GlStateManager.glNewList(id, GL11.GL_COMPILE);
            drawDirect(model);
            GlStateManager.glEndList();
            LISTS.put(model, id);
            list = id;
        }
        GlStateManager.callList(list);
    }

    private static void drawDirect(IBakedModel model) {
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelBrightnessColor(model, 1F, 1F, 1F, 1F);
    }

    private static void releaseLists() {
        for (int id : LISTS.values()) {
            GLAllocation.deleteDisplayLists(id);
        }
        LISTS.clear();
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
        releaseLists();
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
