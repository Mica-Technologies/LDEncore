/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * items/ItemWrench.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 item API. Rotation goes through Block.rotateBlock, which is
 * 1.12's way of asking a block to turn on the clicked face; upstream rotated the state
 * directly.
 */
package dev.theatricalmod.theatrical.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemWrench extends Item {

    public ItemWrench() {
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        EnumFacing axis = player.isSneaking() ? facing.getOpposite() : facing;
        if (world.getBlockState(pos).getBlock().rotateBlock(world, pos, axis)) {
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }
}
