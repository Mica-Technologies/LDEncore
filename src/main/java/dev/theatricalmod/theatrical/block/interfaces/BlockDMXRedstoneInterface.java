/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/interfaces/BlockDMXRedstoneInterface.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 signatures; the GUI opens through the mod's IGuiHandler;
 * getWeakPower checks the tile's type instead of casting blindly.
 */
package dev.theatricalmod.theatrical.block.interfaces;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityDMXRedstoneInterface;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockDMXRedstoneInterface extends Block {

    public BlockDMXRedstoneInterface() {
        super(Material.ANVIL);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityDMXRedstoneInterface();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(TheatricalMod.instance, TheatricalGuiHandler.GUI_DMX_REDSTONE_INTERFACE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityDMXRedstoneInterface) {
            return ((TileEntityDMXRedstoneInterface) tile).getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }
}
