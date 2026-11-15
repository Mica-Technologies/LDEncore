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
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Aircraft cable that drops a ceiling LED fixture below its ceiling, truss or bar: stack as many
 * as the drop needs, one block each, and hang the fixture under the last. Purely decorative; it
 * carries nothing.
 *
 * It draws two wires along a linear fixture below it (batten, pendant, troffer), turned to match,
 * or one down the middle over anything else. That is worked out from the block below in
 * getActualState, through any further cables, so nothing is saved but the block itself.
 */
public class BlockSuspensionCable extends Block {

    public enum Shape implements IStringSerializable {
        CENTER, Z, X;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final PropertyEnum<Shape> SHAPE = PropertyEnum.create("shape", Shape.class);

    private static final AxisAlignedBB BOX = new AxisAlignedBB(0.375D, 0D, 0.375D, 0.625D, 1D, 0.625D);
    /** How far down a chain of cables is followed to find what hangs from it. */
    private static final int MAX_DROP = 32;

    public BlockSuspensionCable() {
        super(Material.IRON);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(SHAPE, Shape.CENTER));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SHAPE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        for (int i = 0; i < MAX_DROP && world.getBlockState(below).getBlock() == this; i++) {
            below = below.down();
        }
        IBlockState fixture = world.getBlockState(below);
        Shape shape = Shape.CENTER;
        if (fixture.getBlock() instanceof BlockLedCeilingLinear) {
            shape = fixture.getValue(BlockLedCeilingLinear.AXIS) == EnumFacing.Axis.X ? Shape.X : Shape.Z;
        }
        return state.withProperty(SHAPE, shape);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOX;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
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

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
