/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/rigging/BlockTruss.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 has no waterlogging, so that property and its fluid ticks
 * are gone; the axis is mapped to metadata. The truss is a full-cube frame rendered on the
 * cutout layer.
 */
package dev.theatricalmod.theatrical.block.rigging;

import dev.theatricalmod.theatrical.api.ISupport;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTruss extends BlockRotatedPillar implements ISupport {

    public BlockTruss() {
        super(Material.ANVIL);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis axis = meta >= 0 && meta < EnumFacing.Axis.values().length ? EnumFacing.Axis.values()[meta] : EnumFacing.Axis.X;
        return getDefaultState().withProperty(AXIS, axis);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(AXIS, facing.getAxis());
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public float[] getLightTransforms(World world, BlockPos pos, EnumFacing facing) {
        return new float[]{0, 0F, 0};
    }
}
