/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/BlockHangable.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 block state plumbing (metadata mapping for FACING + BROKEN, createBlockState);
 *   - the "broken" flag travels on the dropped item's NBT ("broken" boolean) and is read
 *     back on placement. Upstream relied on 1.16's BlockStateTag mechanism, which 1.12 does
 *     not have, and dropped the item by hand in creative mode to attach it; here the normal
 *     drop path carries it and creative mode drops nothing, like any other block;
 *   - falling is driven by neighborChanged -> scheduled updateTick, the 1.12 shape of
 *     updatePostPlacement -> tick.
 */
package dev.theatricalmod.theatrical.block;

import dev.theatricalmod.theatrical.api.ISupport;
import dev.theatricalmod.theatrical.entity.FallingLightEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * A block that hangs from an {@link ISupport} above it and falls (as a
 * {@link FallingLightEntity}) when that support goes away, landing broken.
 */
public class BlockHangable extends BlockHorizontal {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool BROKEN = PropertyBool.create("broken");

    /** NBT key on the item stack that carries the broken flag between drop and placement. */
    public static final String BROKEN_TAG = "broken";

    protected BlockHangable(Material material) {
        super(material);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(BROKEN, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, BROKEN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(BROKEN, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(BROKEN) ? 4 : 0);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (isBrokenStack(stack)) {
            tooltip.add(TextFormatting.RED + "Broken!");
        }
    }

    public static boolean isBrokenStack(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(BROKEN_TAG);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = new ItemStack(this);
        if (state.getValue(BROKEN)) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean(BROKEN_TAG, true);
            stack.setTagCompound(tag);
        }
        drops.add(stack);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        boolean broken = isBrokenStack(placer.getHeldItem(hand));
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing()).withProperty(BROKEN, broken);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return isHanging(world, pos);
    }

    /** Whether the block may stay where it is; overridden by blocks that can also stand. */
    public boolean isValidPosition(IBlockState state, World world, BlockPos pos) {
        return isHanging(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos fromPos) {
        world.scheduleUpdate(pos, this, 3);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        if (!state.getValue(BROKEN) && !isValidPosition(state, world, pos)) {
            FallingLightEntity falling = new FallingLightEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
            world.spawnEntity(falling);
            // Block removal is handled in the first tick of the entity, as vanilla does it.
        }
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    public boolean isHanging(IBlockAccess world, BlockPos pos) {
        BlockPos up = pos.up();
        return !world.isAirBlock(up) && world.getBlockState(up).getBlock() instanceof ISupport;
    }
}
