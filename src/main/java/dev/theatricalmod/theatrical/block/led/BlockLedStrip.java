/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A thin RGB LED strip that sticks to any face of a block: floor, ceiling or wall. Strips side by
 * side along their length join into one run.
 *
 * FACING is the way the light faces (away from the surface it is stuck to). ALONG picks which of
 * the two directions in that surface the strip runs: on a floor or ceiling, false runs north-south
 * and true east-west; on a wall, false runs level and true runs up and down. Both together are
 * twelve states, which fit the four metadata bits.
 */
public class BlockLedStrip extends BlockLedFixture {

    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool ALONG = PropertyBool.create("along");

    // The model runs along Z ("led_strip") or X ("led_strip_x"), stuck to the floor.
    private static final AxisAlignedBB[] LENS_Z = {LedGeometry.px(6.5, 0.5, 0, 9.5, 1, 16)};
    private static final AxisAlignedBB[] LENS_X = {LedGeometry.px(0, 0.5, 6.5, 16, 1, 9.5)};
    private static final AxisAlignedBB SHAPE_Z = LedGeometry.px(5, 0, 0, 11, 1, 16);
    private static final AxisAlignedBB SHAPE_X = LedGeometry.px(0, 0, 5, 16, 1, 11);

    public BlockLedStrip() {
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN).withProperty(ALONG, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ALONG);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(ALONG, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(ALONG) ? 8 : 0);
    }

    /**
     * Whether this state uses the model that runs along X. Floor and ceiling: ALONG means
     * east-west. Walls: the level strip is the X model (which a wall's x=90 rotation keeps level)
     * and the upright one is the Z model (which it turns upright).
     */
    private static boolean usesXModel(IBlockState state) {
        boolean along = state.getValue(ALONG);
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.Y ? along : !along;
    }

    /** Must match blockstates/led_strip.json. */
    @Override
    protected int[] getRotation(IBlockState state) {
        switch (state.getValue(FACING)) {
            case UP:
                return new int[]{0, 0};
            case DOWN:
                return new int[]{180, 0};
            case NORTH:
                return new int[]{90, 0};
            case EAST:
                return new int[]{90, 90};
            case SOUTH:
                return new int[]{90, 180};
            case WEST:
            default:
                return new int[]{90, 270};
        }
    }

    @Override
    protected AxisAlignedBB[] getModelLens(IBlockState state) {
        return usesXModel(state) ? LENS_X : LENS_Z;
    }

    @Override
    protected AxisAlignedBB getModelShape(IBlockState state) {
        return usesXModel(state) ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public EnumFacing.Axis getRunAxis(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        boolean along = state.getValue(ALONG);
        switch (facing.getAxis()) {
            case Y:
                return along ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
            case Z:
                return along ? EnumFacing.Axis.Y : EnumFacing.Axis.X;
            case X:
            default:
                return along ? EnumFacing.Axis.Y : EnumFacing.Axis.Z;
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        // The light faces away from the face that was clicked.
        EnumFacing facing = side;
        // Line up with a strip already on this surface, so a run extends the way it is going.
        for (EnumFacing direction : EnumFacing.values()) {
            if (direction.getAxis() == facing.getAxis()) {
                continue;
            }
            IBlockState neighbour = world.getBlockState(pos.offset(direction));
            if (neighbour.getBlock() == this && neighbour.getValue(FACING) == facing
                    && getRunAxis(neighbour) == direction.getAxis()) {
                return neighbour;
            }
        }
        boolean along;
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            // Floor or ceiling: run the way the player is looking.
            along = placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.X;
        } else {
            // A wall: level, unless the player is looking well up or down it.
            along = Math.abs(placer.rotationPitch) > 50F;
        }
        return getDefaultState().withProperty(FACING, facing).withProperty(ALONG, along);
    }
}
