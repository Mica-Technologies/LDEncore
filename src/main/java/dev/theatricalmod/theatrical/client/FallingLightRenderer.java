/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/FallingLightRenderer.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12's Render<T>, so the MatrixStack becomes GlStateManager and the
 * parts are drawn through our FixtureModels cache with renderModelBrightnessColor, for the
 * reasons given in TileEntityFixtureRenderer. The fixture comes from the falling block's own
 * state rather than from the block class, so a light that fell from a block whose fixture was
 * later unregistered simply does not draw instead of throwing every frame.
 */
package dev.theatricalmod.theatrical.client;

import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.block.BlockHangable;
import dev.theatricalmod.theatrical.block.light.BlockLight;
import dev.theatricalmod.theatrical.client.model.FixtureModels;
import dev.theatricalmod.theatrical.entity.FallingLightEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class FallingLightRenderer extends Render<FallingLightEntity> {

    public FallingLightRenderer(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(FallingLightEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        IBlockState state = entity.getBlock();
        World world = entity.getEntityWorld();
        if (state == null || world == null || !(state.getBlock() instanceof BlockLight)) {
            return;
        }
        Fixture fixture = ((BlockLight) state.getBlock()).getFixture();
        if (fixture == null) {
            return;
        }
        IBakedModel staticModel = FixtureModels.get(fixture.getStaticModelLocation());
        IBakedModel panModel = FixtureModels.get(fixture.getPanModelLocation());
        IBakedModel tiltModel = FixtureModels.get(fixture.getTiltModelLocation());
        if (staticModel == null && panModel == null && tiltModel == null) {
            return;
        }

        BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.5D, 0D, 0.5D);
        EnumFacing facing = state.getProperties().containsKey(BlockHangable.FACING)
                ? state.getValue(BlockHangable.FACING)
                : EnumFacing.NORTH;
        if (facing.getAxis() == EnumFacing.Axis.Z) {
            GlStateManager.rotate(facing.getOpposite().getHorizontalAngle(), 0F, 1F, 0F);
        } else {
            GlStateManager.rotate(facing.getHorizontalAngle(), 0F, 1F, 0F);
        }
        GlStateManager.translate(-0.5D, 0D, -0.5D);
        GlStateManager.translate(-0.5D, 0D, -0.5D);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int light = world.getCombinedLight(pos, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);

        draw(state, staticModel);
        draw(state, panModel);
        draw(state, tiltModel);

        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void draw(IBlockState state, @Nullable IBakedModel model) {
        if (model == null) {
            return;
        }
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelBrightnessColor(state, model, 1F, 1F, 1F, 1F);
    }

    @Override
    protected ResourceLocation getEntityTexture(FallingLightEntity entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
