/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/tile/TileEntityRendererBasicLightingDesk.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's TileEntitySpecialRenderer: the MatrixStack becomes GlStateManager, and the fader
 *     travels, the guide lines and the two labels are emitted through a Tessellator and the
 *     1.12 FontRenderer instead of RenderType buffers. The layout numbers are upstream's.
 *   - The fader travel is fixed. Upstream computed it as (value / 255) * 3 in integer
 *     arithmetic, so every fader except a full one evaluated to zero and the physical faders on
 *     the desk model never moved. The division is done in floating point here, which is plainly
 *     what was meant, and the faders now track the desk.
 *   - The second row of faders is offset by one row rather than by the row index, which is
 *     upstream's formula read literally; with twelve faders the two agree, but upstream's
 *     spaced a third row twice as far as the second.
 *   - The labels are drawn only within a few blocks of the camera. Text is by far the most
 *     expensive thing here and is unreadable at range anyway.
 */
package dev.theatricalmod.theatrical.client.tile;

import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

public class TileEntityRendererBasicLightingDesk extends TileEntitySpecialRenderer<TileEntityBasicLightingControl> {

    /** How many faders sit in one row on the desk. */
    private static final int FADERS_PER_ROW = 6;
    /** Distance beyond which the two labels are not worth drawing, squared. */
    private static final double LABEL_RANGE_SQ = 16D * 16D;

    private static final float FADER_HEIGHT = 0.4F / 16F;
    private static final float FADER_WIDTH = 0.6F / 16F;
    /** How far a fader slides between 0 and full, in model units. */
    private static final double FADER_TRAVEL = 3D;
    /**
     * The height the faders, their slots and the labels are drawn at, in model units.
     *
     * CHANGED FROM UPSTREAM: upstream drew all of them at exactly 3, but the desk model's top
     * face sits at 3.008, so everything was a hair inside the desk and z-fought with it. The
     * labels were invisible from directly above and flickered from any other angle. A fraction
     * higher puts them on the surface instead.
     */
    private static final double SURFACE_Y = 3.1D;

    @Override
    public void render(TileEntityBasicLightingControl desk, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        if (desk.getWorld() == null) {
            return;
        }
        IBlockState state = desk.getWorld().getBlockState(desk.getPos());
        if (!state.getProperties().containsKey(BlockHorizontal.FACING)) {
            return;
        }
        EnumFacing facing = state.getValue(BlockHorizontal.FACING);
        if (facing.getAxis() == EnumFacing.Axis.X) {
            facing = facing.getOpposite();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.5D, 0.5D, 0.5D);
        GlStateManager.rotate(facing.getHorizontalAngle(), 0F, 1F, 0F);
        GlStateManager.translate(-0.5D, -0.5D, -0.5D);

        byte[] faders = desk.getFaders();
        double startX = 1.5D;

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);

        for (int i = 0; i < faders.length; i++) {
            renderLine(startX + (i % FADERS_PER_ROW) * 1.2D, rowBaseY(i));
        }
        renderLine(14.5D, 5.4D);

        for (int i = 0; i < faders.length; i++) {
            renderFader(startX + (i % FADERS_PER_ROW) * 1.2D, rowBaseY(i), travel(faders[i]));
        }
        renderFader(14.5D, 5.4D, travel(desk.getGrandMaster()));

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();

        if (this.rendererDispatcher.entityX != 0 || this.rendererDispatcher.entityZ != 0) {
            double dx = this.rendererDispatcher.entityX - (desk.getPos().getX() + 0.5D);
            double dy = this.rendererDispatcher.entityY - (desk.getPos().getY() + 0.5D);
            double dz = this.rendererDispatcher.entityZ - (desk.getPos().getZ() + 0.5D);
            if (dx * dx + dy * dy + dz * dz <= LABEL_RANGE_SQ) {
                renderLabel(10.7D, 9.3D, 0.005F, "Step: " + desk.getCurrentStep());
                renderLabel(10.4D, 8.3D, 0.003F, desk.isRunMode() ? "Run mode" : "Program mode");
            }
        }

        GlStateManager.popMatrix();
    }

    /** The model's Z offset of the row fader {@code index} sits in. */
    private static double rowBaseY(int index) {
        return 5.4D + (index / FADERS_PER_ROW) * 7D;
    }

    /** How far a fader at this value has slid from its zero position, in model units. */
    private static double travel(byte value) {
        return -((Byte.toUnsignedInt(value) / 255D) * FADER_TRAVEL);
    }

    private void renderLabel(double x, double z, float scale, String text) {
        FontRenderer font = this.getFontRenderer();
        if (font == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x / 16D, SURFACE_Y / 16D, z / 16D);
        GlStateManager.scale(scale, -scale, scale);
        GlStateManager.rotate(90F, 1F, 0F, 0F);
        GlStateManager.disableLighting();
        font.drawString(text, 0, 0, 0x000000);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /** The slot a fader travels along, drawn as a single line on the desk surface. */
    private void renderLine(double x, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x / 16D, SURFACE_Y / 16D, z / 16D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0D, 0D, 0D).color(0, 0, 0, 255).endVertex();
        buffer.pos(0D, 0D, -(FADER_TRAVEL / 16D)).color(0, 0, 0, 255).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    /** The fader cap: a small black box standing on the desk surface. */
    private void renderFader(double x, double baseZ, double travel) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((x / 16D) - FADER_WIDTH / 2D, SURFACE_Y / 16D, (baseZ + travel) / 16D);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float w = FADER_WIDTH;
        float h = FADER_HEIGHT;

        // right
        buffer.pos(w, h, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, h, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, 0, 0).color(0, 0, 0, 255).endVertex();
        // front
        buffer.pos(0, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, h, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, h, w).color(0, 0, 0, 255).endVertex();
        // left
        buffer.pos(0, 0, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, h, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, h, 0).color(0, 0, 0, 255).endVertex();
        // back
        buffer.pos(0, h, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, h, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, 0, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, 0, 0).color(0, 0, 0, 255).endVertex();
        // bottom
        buffer.pos(w, 0, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, 0, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, 0, 0).color(0, 0, 0, 255).endVertex();
        // top
        buffer.pos(0, h, 0).color(0, 0, 0, 255).endVertex();
        buffer.pos(0, h, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, h, w).color(0, 0, 0, 255).endVertex();
        buffer.pos(w, h, 0).color(0, 0, 0, 255).endVertex();

        tessellator.draw();
        GlStateManager.popMatrix();
    }
}
