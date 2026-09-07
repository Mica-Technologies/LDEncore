/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/light/BlockMovingLight.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 createTileEntity signature only.
 */
package dev.theatricalmod.theatrical.block.light;

import dev.theatricalmod.theatrical.fixtures.TheatricalFixtures;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockMovingLight extends BlockIntelligentFixture {

    public BlockMovingLight() {
        super(TheatricalFixtures.MOVING_LIGHT);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        TileEntityIntelligentFixture tile = new TileEntityIntelligentFixture();
        tile.setFixture(TheatricalFixtures.MOVING_LIGHT);
        return tile;
    }
}
