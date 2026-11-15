/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A length of ceiling lighting track. Decorative: it carries nothing. Track heads sit on their own
 * piece of the same rail, so a run of track with heads along it reads as one continuous track.
 */
public class BlockLedTrack extends Block {

    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockLedCeilingLinear.AXIS;

    private static final AxisAlignedBB BOX_Z = LedGeometry.px(6.5, 15, 0, 9.5, 16, 16);
    private static final AxisAlignedBB BOX_X = LedGeometry.px(0, 15, 6.5, 16, 16, 9.5);

    public BlockLedTrack() {
        super(Material.IRON);
        setSoundType(SoundType.METAL);
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

    /** Lines up with track (or a track head) it is placed against, else runs the way the player faces. */
    public static EnumFacing.Axis trackAxisFor(World world, BlockPos pos, EntityLivingBase placer) {
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            IBlockState neighbour = world.getBlockState(pos.offset(direction));
            boolean track = neighbour.getBlock() instanceof BlockLedTrack || neighbour.getBlock() instanceof BlockLedTrackHead;
            if (track && neighbour.getValue(AXIS) == direction.getAxis()) {
                return direction.getAxis();
            }
        }
        return placer.getHorizontalFacing().getAxis();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(AXIS, trackAxisFor(world, pos, placer));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(AXIS) == EnumFacing.Axis.X ? BOX_X : BOX_Z;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
