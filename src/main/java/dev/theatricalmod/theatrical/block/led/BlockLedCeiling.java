/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A ceiling LED fixture with no orientation (a downlight, a flat panel, a high-bay): it sits at
 * the top of its block, under a ceiling, a truss or a suspension cable, and stands alone rather
 * than joining a run. Its shape is given in model coordinates, as the model JSON writes it.
 */
public class BlockLedCeiling extends BlockLedFixture {

    private final AxisAlignedBB shape;
    private final AxisAlignedBB[] lens;
    private final ResourceLocation lensTexture;

    public BlockLedCeiling(ResourceLocation lensTexture, AxisAlignedBB shape, AxisAlignedBB... lens) {
        this.lensTexture = lensTexture;
        this.shape = shape;
        this.lens = lens;
    }

    @Override
    protected int[] getRotation(IBlockState state) {
        return new int[]{0, 0};
    }

    @Override
    protected AxisAlignedBB[] getModelLens(IBlockState state) {
        return lens;
    }

    @Override
    protected AxisAlignedBB getModelShape(IBlockState state) {
        return shape;
    }

    @Override
    public ResourceLocation getLensTexture() {
        return lensTexture;
    }
}
