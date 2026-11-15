/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A wall LED fixture (a sconce): it fixes to the face of a wall and faces out from it. Its model
 * is drawn on the north wall facing south; FACING turns it by y in the blockstate. Stands alone.
 */
public class BlockLedWall extends BlockLedFixture {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private final AxisAlignedBB shape;
    private final AxisAlignedBB[] lens;
    private final ResourceLocation lensTexture;

    public BlockLedWall(ResourceLocation lensTexture, AxisAlignedBB shape, AxisAlignedBB... lens) {
        this.lensTexture = lensTexture;
        this.shape = shape;
        this.lens = lens;
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    /** Must match the blockstate JSON: south 0, west 90, north 180, east 270. */
    @Override
    protected int[] getRotation(IBlockState state) {
        switch (state.getValue(FACING)) {
            case WEST:
                return new int[]{0, 90};
            case NORTH:
                return new int[]{0, 180};
            case EAST:
                return new int[]{0, 270};
            case SOUTH:
            default:
                return new int[]{0, 0};
        }
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

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        // On a wall, face out of it; placed on a floor or ceiling, face the player.
        EnumFacing facing = side.getAxis().isHorizontal() ? side : placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(FACING, facing);
    }
}
