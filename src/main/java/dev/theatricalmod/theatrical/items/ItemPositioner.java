/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * items/ItemPositioner.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 item API; the linked light's position is kept under a
 * "light" sub-tag with the helpers below, which the generic fixture block also uses.
 */
package dev.theatricalmod.theatrical.items;

import dev.theatricalmod.theatrical.tiles.lights.TileEntityGenericFixture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Remote light positioner: right-click a generic light with it to have that light follow
 * you; sneak-right-click in the air to release it.
 */
public class ItemPositioner extends Item {

    private static final String LIGHT_TAG = "light";

    public ItemPositioner() {
        setMaxStackSize(1);
    }

    public static void setLinkedLight(ItemStack stack, BlockPos pos) {
        stack.setTagInfo(LIGHT_TAG, NBTUtil.createPosTag(pos));
    }

    @Nullable
    public static BlockPos getLinkedLight(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(LIGHT_TAG)) {
            return null;
        }
        return NBTUtil.getPosFromTag(tag.getCompoundTag(LIGHT_TAG));
    }

    public static void clearLinkedLight(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(LIGHT_TAG);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player.isSneaking()) {
            BlockPos lightPos = getLinkedLight(stack);
            if (lightPos != null) {
                TileEntity tile = world.getTileEntity(lightPos);
                if (tile instanceof TileEntityGenericFixture) {
                    ((TileEntityGenericFixture) tile).setTrackingEntity(null);
                }
                clearLinkedLight(stack);
            }
        }
        return super.onItemRightClick(world, player, hand);
    }
}
