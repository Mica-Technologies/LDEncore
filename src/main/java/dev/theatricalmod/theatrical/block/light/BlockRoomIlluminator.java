/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.light;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The invisible full-brightness block a fixture in Room Light mode keeps just above the floor
 * beneath it, so a room lit by ceiling fixtures is bright where people stand and not only up at
 * the ceiling. The same idea as City Super Mod's light-up air, built separately so LDEncore does
 * not depend on CSM.
 *
 * It is deliberately not a {@link BlockIlluminator}: a moving head's beam treats any
 * illuminator as its own to re-level and remove, and must never take a room light's block.
 *
 * Like the beam illuminator it uses a non-solid material rather than air, so that
 * {@code isAirBlock} is false. That is also what lets it sit beside CSM: CSM's lights only ever
 * place into air, and LDEncore only ever places into air, so neither replaces the other's block.
 */
public class BlockRoomIlluminator extends Block {

    /**
     * Whether an aimed fixture (a track head) placed this one. Fixtures that light straight down
     * own by column and aimed ones by the end of their path; telling the two kinds apart is what
     * stops each from taking the other's light for its own when an aimed light lands in another
     * fixture's column.
     */
    public static final PropertyBool AIMED = PropertyBool.create("aimed");

    public BlockRoomIlluminator() {
        super(Material.CIRCUITS);
        setLightLevel(1F);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(AIMED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AIMED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AIMED, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AIMED) ? 1 : 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
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
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }
}
