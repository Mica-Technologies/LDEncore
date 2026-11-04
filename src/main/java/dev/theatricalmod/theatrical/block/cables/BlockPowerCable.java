/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/cables/BlockPowerCable.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: upstream gave this cable the DIMMED_POWER type and a dimmed-power
 * tile, so it was a second dimmed power cable and no Forge Energy reached a moving head
 * through it. It is now the POWER type with a real FE conduit tile (TileEntityPowerCable,
 * PowerGrid): it connects to truss, IWB and any block that takes or gives FE, and carries
 * energy to the moving heads and dimmer racks on its run. The One Probe overlay returns
 * with the compat phase.
 */
package dev.theatricalmod.theatrical.block.cables;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.block.rigging.BlockIWB;
import dev.theatricalmod.theatrical.block.rigging.BlockTruss;
import dev.theatricalmod.theatrical.tiles.power.PowerGrid;
import dev.theatricalmod.theatrical.tiles.power.TileEntityPowerCable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nullable;

public class BlockPowerCable extends BlockCable {

    public BlockPowerCable() {
        super(CableType.POWER);
    }

    @Override
    public boolean canConnectTo(IBlockAccess world, BlockPos neighbourPos, EnumFacing direction) {
        Block block = world.getBlockState(neighbourPos).getBlock();
        if (block == this || block instanceof BlockTruss || block instanceof BlockIWB) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(neighbourPos);
        return tileEntity != null && tileEntity.hasCapability(CapabilityEnergy.ENERGY, direction.getOpposite());
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

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityPowerCable();
    }
}
