/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * A track light: a small aimable head on its own piece of ceiling track. Its blockstate model is
 * the track and the adapter; the head itself (yoke, can and lens) is drawn by the tile renderer,
 * turned to the aim the tile holds, and its room light lands where it points rather than straight
 * down. It stands alone rather than joining a run.
 */
public class BlockLedTrackHead extends BlockLedCeilingLinear {

    public BlockLedTrackHead() {
        super(false, LENS_SMOOTH, LedGeometry.px(6.5, 8, 0, 9.5, 16, 16), LedGeometry.px(6.75, 7.75, 6.75, 9.25, 8, 9.25));
    }

    @Override
    public boolean isAimable() {
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(AXIS, BlockLedTrack.trackAxisFor(world, pos, placer));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileEntityLedFixture) {
            // Start pointed the way the player faces, tilted 30 degrees, which lights the space
            // they are looking into. Minecraft's yaw counts from south; the aim counts from north.
            int pan = Math.round(MathHelper.wrapDegrees(placer.rotationYaw + 180F) / 15F) * 15;
            ((TileEntityLedFixture) tile).setAim(pan, 30);
        }
    }
}
