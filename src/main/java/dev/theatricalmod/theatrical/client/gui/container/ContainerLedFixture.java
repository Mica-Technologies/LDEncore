/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.client.gui.container;

import dev.theatricalmod.theatrical.network.PacketUtil;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** LED fixture screen: address, mode and pixel mode. */
public class ContainerLedFixture extends Container {

    @Nullable
    public final TileEntityLedFixture blockEntity;
    protected final BlockPos pos;

    public ContainerLedFixture(World world, BlockPos pos) {
        this.pos = pos;
        TileEntity tile = world.getTileEntity(pos);
        this.blockEntity = tile instanceof TileEntityLedFixture ? (TileEntityLedFixture) tile : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return blockEntity != null && !blockEntity.isInvalid()
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= PacketUtil.REACH_SQ;
    }
}
