/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A ceiling LED fixture with a length: a batten, a linear pendant, a troffer. It runs north-south
 * or east-west, sits at the top of its block (under a ceiling, a truss or a suspension cable) and,
 * if {@code joins}, joins identical fixtures end to end into a run.
 *
 * The model is drawn running north-south; AXIS = x turns it by y = 90 in the blockstate.
 */
public class BlockLedCeilingLinear extends BlockLedFixture {

    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class,
            EnumFacing.Axis.X, EnumFacing.Axis.Z);

    private final boolean joins;
    private final AxisAlignedBB shape;
    private final AxisAlignedBB[] lens;
    private final ResourceLocation lensTexture;

    public BlockLedCeilingLinear(boolean joins, ResourceLocation lensTexture, AxisAlignedBB shape, AxisAlignedBB... lens) {
        this.joins = joins;
        this.lensTexture = lensTexture;
        this.shape = shape;
        this.lens = lens;
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Z));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AXIS, (meta & 1) != 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS) == EnumFacing.Axis.X ? 1 : 0;
    }

    /** Must match the blockstate JSON: axis=x is the model turned by y = 90. */
    @Override
    protected int[] getRotation(IBlockState state) {
        return new int[]{0, state.getValue(AXIS) == EnumFacing.Axis.X ? 90 : 0};
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

    @Nullable
    @Override
    public EnumFacing.Axis getRunAxis(IBlockState state) {
        return joins ? state.getValue(AXIS) : null;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (joins) {
            // Line up with a run this one is placed against, so a run extends the way it goes.
            for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
                IBlockState neighbour = world.getBlockState(pos.offset(direction));
                if (neighbour.getBlock() == this && neighbour.getValue(AXIS) == direction.getAxis()) {
                    return neighbour;
                }
            }
        }
        return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
    }
}
