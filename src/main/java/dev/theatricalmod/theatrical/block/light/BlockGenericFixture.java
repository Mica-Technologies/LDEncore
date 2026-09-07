/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/light/BlockGenericFixture.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - the GUI opens through the mod's IGuiHandler (1.12) instead of NetworkHooks.openGui;
 *   - the positioner link is stored on the item as a plain position tag ("light": x/y/z);
 *   - onEntityWalk guards against a missing tile instead of casting blindly;
 *   - The One Probe overlay returns with the compat phase.
 */
package dev.theatricalmod.theatrical.block.light;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import dev.theatricalmod.theatrical.items.ItemPositioner;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityGenericFixture;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockGenericFixture extends BlockLight {

    public BlockGenericFixture(Fixture fixture) {
        super(Material.ANVIL, fixture);
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityGenericFixture && ((TileEntityGenericFixture) tile).getIntensity() > 0
                && !entity.isImmuneToFire() && entity instanceof EntityLivingBase) {
            entity.attackEntityFrom(DamageSource.HOT_FLOOR, 1.0F);
        }
        super.onEntityWalk(world, pos, entity);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityGenericFixture)) {
            return false;
        }
        TileEntityGenericFixture fixture = (TileEntityGenericFixture) tileEntity;
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.getItem() instanceof ItemPositioner) {
            if (fixture.getTrackingEntity() != null) {
                fixture.setTrackingEntity(null);
                ItemPositioner.clearLinkedLight(heldItem);
            } else {
                fixture.setTrackingEntity(player);
                ItemPositioner.setLinkedLight(heldItem, pos);
            }
            return true;
        }
        player.openGui(TheatricalMod.instance, TheatricalGuiHandler.GUI_GENERIC_FIXTURE, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
