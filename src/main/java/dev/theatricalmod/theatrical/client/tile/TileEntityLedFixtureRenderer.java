/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.client.tile;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.block.led.BlockLedFixture;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.block.Block;
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
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

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

    /** A lens that is off: a pale grey diffuser, lit by the world like any block. */
    private static final int OFF_COLOUR = 0x9A9A9A;
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Each lens texture's sprite, filled when the atlas is stitched. */
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        SPRITES.clear();
        // Lens textures are reached by no block model, so they are stitched here.
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof BlockLedFixture) {
                ResourceLocation texture = ((BlockLedFixture) block).getLensTexture();
                if (!SPRITES.containsKey(texture)) {
                    SPRITES.put(texture, event.getMap().registerSprite(texture));
                }
            }
        }
    }

    @Override
    public void renderTileEntityFast(TileEntityLedFixture te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
        World world = te.getWorld();
        IBlockState state = world.getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockLedFixture)) {
            return;
        }
        BlockLedFixture fixture = (BlockLedFixture) state.getBlock();
        TextureAtlasSprite sprite = SPRITES.get(fixture.getLensTexture());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
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
        if (fixture.isAimable()) {
            renderTrackHead(te, x, y, z, buffer, sprite, r, g, b, sky, block);
            return;
        }
        for (AxisAlignedBB box : fixture.getLensBoxes(state)) {
            AxisAlignedBB bb = box.offset(x, y, z);
            cuboid(buffer, corners(bb), sprite, r, g, b, sky, block);
        }
    }

    private static final ResourceLocation HEAD_TEXTURE = new ResourceLocation(TheatricalMod.MOD_ID, "blocks/led/housing_dark");
    private static final double PX = 1D / 16D;

    /**
     * A track head: a short yoke under the track adapter, and a can with the lens at its mouth,
     * turned to the tile's aim. The can is drawn pointing straight down, tilted about X towards
     * north, then turned about Y by the pan (clockwise from north), which is the same direction
     * the tile's room light is cast along.
     */
    private static void renderTrackHead(TileEntityLedFixture te, double x, double y, double z, BufferBuilder buffer,
                                        TextureAtlasSprite lens, int r, int g, int b, int sky, int block) {
        TextureAtlasSprite body = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(HEAD_TEXTURE.toString());
        int worldLight = te.getWorld().getCombinedLight(te.getPos(), 0);
        int wSky = (worldLight >> 16) & 0xFFFF;
        int wBlock = worldLight & 0xFFFF;
        double pan = Math.toRadians(te.getAimPan());
        double tilt = Math.toRadians(te.getAimTilt());
        // The yoke hangs from the adapter at y = 14 px and does not tilt.
        double cx = x + 0.5D;
        double cz = z + 0.5D;
        double yokeTop = y + 14 * PX;
        cuboid(buffer, corners(new AxisAlignedBB(cx - 0.5 * PX, yokeTop - 1.5 * PX, cz - 0.5 * PX,
                cx + 0.5 * PX, yokeTop, cz + 0.5 * PX)), body, 200, 200, 200, wSky, wBlock);
        double pivotY = yokeTop - 1.5 * PX;
        cuboid(buffer, aimed(-1.5, -5, -1.5, 1.5, 0.5, 1.5, cx, pivotY, cz, tilt, pan), body, 200, 200, 200, wSky, wBlock);
        cuboid(buffer, aimed(-1.25, -5.25, -1.25, 1.25, -5, 1.25, cx, pivotY, cz, tilt, pan), lens, r, g, b, sky, block);
    }

    /** The eight corners of a box given in pixels about a pivot, tilted then panned, in render space. */
    private static double[][] aimed(double x0, double y0, double z0, double x1, double y1, double z1,
                                    double px, double py, double pz, double tilt, double pan) {
        double[][] out = new double[8][];
        for (int i = 0; i < 8; i++) {
            double vx = ((i & 1) != 0 ? x1 : x0) * PX;
            double vy = ((i & 2) != 0 ? y1 : y0) * PX;
            double vz = ((i & 4) != 0 ? z1 : z0) * PX;
            // Tilt about X: straight down leans towards north.
            double ty = vy * Math.cos(tilt) - vz * Math.sin(tilt);
            double tz = vy * Math.sin(tilt) + vz * Math.cos(tilt);
            // Pan about Y, clockwise from north.
            double rx = vx * Math.cos(pan) - tz * Math.sin(pan);
            double rz = vx * Math.sin(pan) + tz * Math.cos(pan);
            out[i] = new double[]{px + rx, py + ty, pz + rz};
        }
        return out;
    }

    /** The eight corners of a box; index bit 1 = max X, bit 2 = max Y, bit 4 = max Z. */
    private static double[][] corners(AxisAlignedBB bb) {
        double[][] out = new double[8][];
        for (int i = 0; i < 8; i++) {
            out[i] = new double[]{(i & 1) != 0 ? bb.maxX : bb.minX, (i & 2) != 0 ? bb.maxY : bb.minY, (i & 4) != 0 ? bb.maxZ : bb.minZ};
        }
        return out;
    }

    /**
     * Emits the six faces of a box from its corners, each wound anticlockwise as seen from
     * outside. A rotation keeps the winding, so a turned box draws the same way.
     */
    private static void cuboid(BufferBuilder buffer, double[][] c, TextureAtlasSprite sprite, int r, int g, int b, int sky, int block) {
        quad(buffer, sprite, r, g, b, sky, block, c[4], c[0], c[1], c[5]); // down
        quad(buffer, sprite, r, g, b, sky, block, c[2], c[6], c[7], c[3]); // up
        quad(buffer, sprite, r, g, b, sky, block, c[3], c[1], c[0], c[2]); // north
        quad(buffer, sprite, r, g, b, sky, block, c[6], c[4], c[5], c[7]); // south
        quad(buffer, sprite, r, g, b, sky, block, c[2], c[0], c[4], c[6]); // west
        quad(buffer, sprite, r, g, b, sky, block, c[7], c[5], c[1], c[3]); // east
    }

    private static void quad(BufferBuilder buffer, TextureAtlasSprite sprite, int r, int g, int b, int sky, int block,
                             double[] a, double[] bb, double[] c, double[] d) {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        buffer.pos(a[0], a[1], a[2]).color(r, g, b, 255).tex(u0, v0).lightmap(sky, block).endVertex();
        buffer.pos(bb[0], bb[1], bb[2]).color(r, g, b, 255).tex(u0, v1).lightmap(sky, block).endVertex();
        buffer.pos(c[0], c[1], c[2]).color(r, g, b, 255).tex(u1, v1).lightmap(sky, block).endVertex();
        buffer.pos(d[0], d[1], d[2]).color(r, g, b, 255).tex(u1, v0).lightmap(sky, block).endVertex();
    }
}
