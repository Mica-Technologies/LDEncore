/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/light/BlockIntelligentFixture.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - HANGING is the block's own PropertyBool and is mapped into metadata bit 3 (1.12 has
 *     no shared BlockStateProperties.HANGING);
 *   - canPlaceBlockAt has no state on 1.12, so placement allows either mounting and the
 *     state-aware check lives in isValidPosition, which the fall check uses;
 *   - the GUI opens through the mod's IGuiHandler.
 */
package dev.theatricalmod.theatrical.block.light;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockIntelligentFixture extends BlockLight {

    public static final PropertyBool HANGING = PropertyBool.create("hanging");

    public BlockIntelligentFixture(Fixture fixture) {
        super(Material.ANVIL, fixture);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(BROKEN, false).withProperty(HANGING, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, BROKEN, HANGING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(HANGING, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return super.getMetaFromState(state) | (state.getValue(HANGING) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        boolean hanging = facing == EnumFacing.DOWN || isHanging(world, pos);
        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(HANGING, hanging);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return isHanging(world, pos) || !world.isAirBlock(pos.down());
    }

    @Override
    public boolean isValidPosition(IBlockState state, World world, BlockPos pos) {
        if (state.getValue(HANGING)) {
            return isHanging(world, pos);
        }
        return !world.isAirBlock(pos.down());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(TheatricalMod.instance, TheatricalGuiHandler.GUI_INTELLIGENT_FIXTURE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
