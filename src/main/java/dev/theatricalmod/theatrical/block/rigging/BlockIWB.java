/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/rigging/BlockIWB.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 block state plumbing and bounding boxes; hanging a light on
 * the bar shrinks the held stack with ItemStack.shrink instead of setting the count by
 * hand. Fork addition: the bar is actually wired, carrying Forge Energy to the
 * fixtures hung from it (a TileEntityWiredSupport on the PowerGrid).
 */
package dev.theatricalmod.theatrical.block.rigging;

import dev.theatricalmod.theatrical.api.ISupport;
import dev.theatricalmod.theatrical.tiles.power.PowerGrid;
import dev.theatricalmod.theatrical.tiles.power.TileEntityWiredSupport;
import net.minecraft.tileentity.TileEntity;
import dev.theatricalmod.theatrical.block.BlockHangable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Internally Wired Bar: a thin horizontal bar fixtures hang from. Right-clicking it with a
 * hangable block hangs that block underneath.
 */
public class BlockIWB extends BlockHorizontal implements ISupport {

    private static final AxisAlignedBB Z_BOX = new AxisAlignedBB(0.35, 0, 0, 0.65, 0.2, 1);
    private static final AxisAlignedBB X_BOX = new AxisAlignedBB(0, 0, 0.4, 1, 0.2, 0.6);

    public BlockIWB() {
        super(Material.ANVIL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
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

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public float[] getLightTransforms(World world, BlockPos pos, EnumFacing facing) {
        return new float[]{0, 0.19F, 0};
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z ? Z_BOX : X_BOX;
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
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()) {
            Block block = getBlockFromItem(held.getItem());
            if (block instanceof BlockHangable) {
                BlockPos down = pos.down();
                if (!world.isAirBlock(down)) {
                    return false;
                }
                if (!world.isRemote) {
                    world.setBlockState(down, block.getDefaultState().withProperty(BlockHangable.FACING, player.getHorizontalFacing()));
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                }
                return true;
            }
        }
        return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    // Fork addition: the bar carries Forge Energy to the fixtures hung from it (PowerGrid).

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityWiredSupport();
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        PowerGrid.invalidate(world);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        PowerGrid.invalidate(world);
        super.breakBlock(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        PowerGrid.invalidate(world);
    }
}
