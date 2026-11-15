/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.client.tile;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.block.led.BlockLedFixture;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.animation.FastTESR;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws an LED fixture's lens in its current colour, at full brightness while it is lit.
 *
 * A FastTESR, so the lenses of every LED fixture in view go into one batched buffer per frame
 * rather than one draw call each: a room of a few hundred strip segments costs a few thousand
 * quads, with no state changes between them. The rest of the fixture is its baked block model.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID, value = Side.CLIENT)
public class TileEntityLedFixtureRenderer extends FastTESR<TileEntityLedFixture> {

    public static final ResourceLocation LENS_TEXTURE = new ResourceLocation(TheatricalMod.MOD_ID, "blocks/led/lens");

    /** A lens that is off: a pale grey diffuser, lit by the world like any block. */
    private static final int OFF_COLOUR = 0x9A9A9A;
    private static final int FULL_BRIGHT = 0xF000F0;

    private static TextureAtlasSprite lensSprite;

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        lensSprite = event.getMap().registerSprite(LENS_TEXTURE);
    }

    @Override
    public void renderTileEntityFast(TileEntityLedFixture te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
        World world = te.getWorld();
        IBlockState state = world.getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockLedFixture)) {
            return;
        }
        TextureAtlasSprite sprite = lensSprite != null ? lensSprite
                : Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        int colour = te.getOutputColour();
        int light;
        if (colour == 0) {
            colour = OFF_COLOUR;
            light = world.getCombinedLight(te.getPos(), 0);
        } else {
            light = FULL_BRIGHT;
        }
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        int sky = (light >> 16) & 0xFFFF;
        int block = light & 0xFFFF;
        for (AxisAlignedBB box : ((BlockLedFixture) state.getBlock()).getLensBoxes(state)) {
            box(buffer, box.offset(x, y, z), sprite, r, g, b, sky, block);
        }
    }

    /** Emits the six faces of a box, each wound anticlockwise as seen from outside. */
    private static void box(BufferBuilder buffer, AxisAlignedBB bb, TextureAtlasSprite sprite, int r, int g, int b, int sky, int block) {
        double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ;
        double x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        // Down
        quad(buffer, sprite, r, g, b, sky, block, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        // Up
        quad(buffer, sprite, r, g, b, sky, block, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        // North
        quad(buffer, sprite, r, g, b, sky, block, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        // South
        quad(buffer, sprite, r, g, b, sky, block, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        // West
        quad(buffer, sprite, r, g, b, sky, block, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        // East
        quad(buffer, sprite, r, g, b, sky, block, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void quad(BufferBuilder buffer, TextureAtlasSprite sprite, int r, int g, int b, int sky, int block,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        buffer.pos(ax, ay, az).color(r, g, b, 255).tex(u0, v0).lightmap(sky, block).endVertex();
        buffer.pos(bx, by, bz).color(r, g, b, 255).tex(u0, v1).lightmap(sky, block).endVertex();
        buffer.pos(cx, cy, cz).color(r, g, b, 255).tex(u1, v1).lightmap(sky, block).endVertex();
        buffer.pos(dx, dy, dz).color(r, g, b, 255).tex(u1, v0).lightmap(sky, block).endVertex();
    }
}
