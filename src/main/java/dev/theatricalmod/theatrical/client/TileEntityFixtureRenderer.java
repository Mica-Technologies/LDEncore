/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/TileEntityFixtureRenderer.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's TileEntitySpecialRenderer, so every MatrixStack push/translate/rotate becomes the
 *     equivalent GlStateManager call and the beam is emitted through a Tessellator instead of
 *     an IVertexBuilder from a RenderType buffer. The transform order and the beam geometry
 *     are upstream's.
 *   - Part models come from our own FixtureModels cache rather than the model manager, for the
 *     reason given in that class, and they are drawn with renderModelBrightnessColor, which
 *     emits a model in its own local coordinates and so composes with the pan and tilt
 *     rotations. Upstream's renderModel bakes the block position into the vertices, which
 *     1.12 cannot combine with GL transforms.
 *   - The beam opacity is read per frame instead of once when the renderer is constructed, so
 *     changing it in the config screen takes effect without a restart. Upstream captured it in
 *     a field initialiser, which on 1.16 also ran before the config was loaded.
 *   - A fixture whose models failed to load is skipped rather than throwing once per frame.
 */
package dev.theatricalmod.theatrical.client;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.api.ISupport;
import dev.theatricalmod.theatrical.api.fixtures.HangableType;
import dev.theatricalmod.theatrical.block.BlockHangable;
import dev.theatricalmod.theatrical.block.light.BlockIntelligentFixture;
import dev.theatricalmod.theatrical.client.model.FixtureModels;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityFixture;
import dev.theatricalmod.theatrical.util.FixtureUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class TileEntityFixtureRenderer extends TileEntitySpecialRenderer<TileEntityFixture> {

    /** The beam reaches past the block it is drawn from, so it must not be culled by chunk. */
    @Override
    public boolean isGlobalRenderer(TileEntityFixture te) {
        return true;
    }

    @Override
    public void render(TileEntityFixture fixture, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        World world = fixture.getWorld();
        if (world == null) {
            return;
        }
        IBlockState state = fixture.getBlockState();
        if (!(state.getBlock() instanceof BlockHangable)) {
            return;
        }

        boolean isFlipped = state.getBlock() instanceof BlockIntelligentFixture
                && state.getValue(BlockIntelligentFixture.HANGING);
        boolean isHanging = ((BlockHangable) state.getBlock()).isHanging(world, fixture.getPos());

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        renderLight(fixture, state, state.getValue(BlockHangable.FACING), partialTicks, isFlipped, isHanging);

        float intensity = fixture.getIntensity();
        double opacity = TheatricalConfigHandler.RENDERING.lightBeamOpacity;
        if (intensity > 0 && opacity > 0) {
            float[] beamStart = fixture.getBeamStartPosition();
            if (beamStart.length >= 3) {
                GlStateManager.translate(0.5D, 0.5D, 0.5D);
                GlStateManager.rotate(fixture.getDefaultRotation(), 1F, 0F, 0F);
                GlStateManager.translate(-0.5D, -0.5D, -0.5D);
                GlStateManager.translate(beamStart[0], beamStart[1], beamStart[2]);
                renderLightBeam(fixture, (float) ((intensity * opacity) / 255D), fixture.getBeamWidth(),
                        (float) fixture.getDistance(), FixtureUtil.getColorFromTile(fixture));
            }
        }
        GlStateManager.popMatrix();
    }

    private void renderLight(TileEntityFixture fixture, IBlockState state, EnumFacing facing,
                             float partialTicks, boolean isFlipped, boolean isHanging) {
        IBakedModel staticModel = FixtureModels.get(fixture.getStaticModel());
        IBakedModel panModel = FixtureModels.get(fixture.getPanModel());
        IBakedModel tiltModel = FixtureModels.get(fixture.getTiltModel());
        if (staticModel == null && panModel == null && tiltModel == null) {
            return;
        }

        World world = fixture.getWorld();
        BlockPos pos = fixture.getPos();
        HangableType hangType = fixture.getHangType();

        if (hangType == HangableType.BRACE_BAR && isHanging) {
            GlStateManager.translate(0D, 0.175D, 0D);
        }
        if (hangType == HangableType.HOOK_BAR && isHanging) {
            GlStateManager.translate(0D, 0.05D, 0D);
        }
        GlStateManager.translate(0.5D, 0D, 0.5D);
        if (facing.getAxis() == EnumFacing.Axis.Z) {
            GlStateManager.rotate(facing.getOpposite().getHorizontalAngle(), 0F, 1F, 0F);
        } else {
            GlStateManager.rotate(facing.getHorizontalAngle(), 0F, 1F, 0F);
        }
        GlStateManager.translate(-0.5D, 0D, -0.5D);

        if (hangType == HangableType.BRACE_BAR && isHanging) {
            IBlockState above = world.getBlockState(pos.offset(EnumFacing.UP));
            if (above.getBlock() instanceof ISupport) {
                float[] transforms = ((ISupport) above.getBlock()).getLightTransforms(world, pos, facing);
                GlStateManager.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                GlStateManager.translate(0D, 0.19D, 0D);
            }
        }
        if (isFlipped) {
            GlStateManager.translate(0.5D, 0.5D, 0.5D);
            GlStateManager.rotate(180F, 0F, 0F, 1F);
            GlStateManager.translate(-0.5D, -0.5D, -0.5D);
        }
        if (hangType == HangableType.BRACE_BAR && isHanging) {
            GlStateManager.translate(0D, 0.19D, 0D);
        }

        beginModels(world, pos);
        drawModel(state, staticModel);
        if (hangType == HangableType.BRACE_BAR && isHanging) {
            GlStateManager.translate(0D, 0.19D, 0D);
        }

        float[] pans = fixture.getPanRotationPosition();
        GlStateManager.translate(pans[0], pans[1], pans[2]);
        GlStateManager.rotate(fixture.prevPan + (fixture.getPan() - fixture.prevPan) * partialTicks, 0F, 1F, 0F);
        GlStateManager.translate(-pans[0], -pans[1], -pans[2]);
        drawModel(state, panModel);

        float[] tilts = fixture.getTiltRotationPosition();
        GlStateManager.translate(tilts[0], tilts[1], tilts[2]);
        GlStateManager.rotate(fixture.prevTilt + (fixture.getTilt() - fixture.prevTilt) * partialTicks, 1F, 0F, 0F);
        GlStateManager.translate(-tilts[0], -tilts[1], -tilts[2]);
        drawModel(state, tiltModel);
        endModels();
    }

    /**
     * Sets the block atlas, the fixture's own light level and cutout-friendly blending up once
     * for all three parts. The parts are drawn in local coordinates, so the lightmap has to be
     * set by hand -- the vertex format the model renderer uses on 1.12 carries no light value.
     */
    private void beginModels(World world, BlockPos pos) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int light = world.getCombinedLight(pos, 0);
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
    }

    private void endModels() {
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
    }

    private void drawModel(IBlockState state, IBakedModel model) {
        if (model == null) {
            return;
        }
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelBrightnessColor(state, model, 1F, 1F, 1F, 1F);
    }

    /**
     * Upstream's beam: four quads forming a tapering box, opaque at the lens and transparent at
     * the far end, with the taper set by the fixture's focus.
     */
    private void renderLightBeam(TileEntityFixture fixture, float alpha, float beamSize, float length, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (Math.min(alpha, 1F) * 255F);
        float end = beamSize * fixture.getFocus();

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(end, end, -length).color(r, g, b, 0).endVertex();
        buffer.pos(beamSize, beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(beamSize, -beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(end, -end, -length).color(r, g, b, 0).endVertex();

        buffer.pos(-end, -end, -length).color(r, g, b, 0).endVertex();
        buffer.pos(-beamSize, -beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(-beamSize, beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(-end, end, -length).color(r, g, b, 0).endVertex();

        buffer.pos(-end, end, -length).color(r, g, b, 0).endVertex();
        buffer.pos(-beamSize, beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(beamSize, beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(end, end, -length).color(r, g, b, 0).endVertex();

        buffer.pos(end, -end, -length).color(r, g, b, 0).endVertex();
        buffer.pos(beamSize, -beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(-beamSize, -beamSize, 0).color(r, g, b, a).endVertex();
        buffer.pos(-end, -end, -length).color(r, g, b, 0).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }
}
