/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/cables/BlockPowerCable.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the block is otherwise upstream's; note that upstream gives this
 * "power" cable the DIMMED_POWER cable type and a dimmed-power tile, i.e. it behaves as a
 * second dimmed power cable. That is preserved. The One Probe overlay returns with the
 * compat phase.
 */
package dev.theatricalmod.theatrical.block.cables;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.tiles.power.TileEntityDimmedPowerCable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockPowerCable extends BlockCable {

    public BlockPowerCable() {
        super(CableType.DIMMED_POWER);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityDimmedPowerCable();
    }
}
