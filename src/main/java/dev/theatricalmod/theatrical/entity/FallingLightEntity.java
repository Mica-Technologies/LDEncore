/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * entity/FallingLightEntity.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 entity API (onUpdate, fall, setDead, motion fields);
 *   - EntityFallingBlock keeps its block state private on 1.12, so the state received in
 *     the spawn packet is kept here and getBlock() is overridden to serve it on the client;
 *   - the helmet check reads the wearer's head slot (1.12 stacks do not know their slot);
 *   - the turtle-egg crack sound does not exist on 1.12, so only the glass break plays.
 */
package dev.theatricalmod.theatrical.entity;

import dev.theatricalmod.theatrical.block.BlockHangable;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A fixture whose support was removed, falling. Lands as the same block with its BROKEN flag
 * set, and hurts whatever it lands on.
 */
public class FallingLightEntity extends EntityFallingBlock implements IEntityAdditionalSpawnData {

    private IBlockState spawnState;

    public FallingLightEntity(World world) {
        super(world);
    }

    public FallingLightEntity(World world, double x, double y, double z, IBlockState fallingBlockState) {
        super(world, x, y, z, fallingBlockState);
    }

    @Nullable
    @Override
    public IBlockState getBlock() {
        IBlockState state = super.getBlock();
        return state != null ? state : spawnState;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        int i = MathHelper.ceil(distance - 1.0F);
        if (i > 0) {
            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox());
            for (Entity entity : list) {
                int damage = 4;
                int maxDamage = 60;
                if (entity instanceof EntityLivingBase && !((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()) {
                    damage /= 2;
                    maxDamage /= 2;
                }
                entity.attackEntityFrom(DamageSource.FALLING_BLOCK, (float) Math.min(MathHelper.floor((float) i * damage), maxDamage));
            }
        }
    }

    @Override
    public void onUpdate() {
        IBlockState fallTile = getBlock();
        if (fallTile == null) {
            setDead();
            return;
        }
        Block block = fallTile.getBlock();
        BlockPos currentPos;
        // Called every tick to increment the fall time, and also removes the block on the first tick
        if (this.fallTime++ == 0) {
            currentPos = new BlockPos(this);
            if (this.world.getBlockState(currentPos).getBlock() == block) {
                this.world.setBlockToAir(currentPos);
            } else if (!this.world.isRemote) {
                this.setDead();
                return;
            }
        }
        // Do motion
        if (!this.hasNoGravity()) {
            this.motionY -= 0.04D;
        }
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        // If on ground, handle turning back into a block
        if (!this.world.isRemote) {
            currentPos = new BlockPos(this);
            if (this.onGround) {
                IBlockState landedOn = this.world.getBlockState(currentPos);
                this.setDead();
                if (landedOn.getBlock().isReplaceable(this.world, currentPos)) {
                    if (this.world.setBlockState(currentPos, fallTile, 3)) {
                        this.world.playEvent(2001, currentPos, Block.getStateId(fallTile));
                        this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 1);
                        if (fallTile.getPropertyKeys().contains(BlockHangable.BROKEN)) {
                            this.world.setBlockState(currentPos, fallTile.withProperty(BlockHangable.BROKEN, true), 3);
                        }
                        if (this.tileEntityData != null && block.hasTileEntity(fallTile)) {
                            TileEntity tileentity = this.world.getTileEntity(currentPos);
                            if (tileentity != null) {
                                NBTTagCompound compound = tileentity.writeToNBT(new NBTTagCompound());
                                for (String key : this.tileEntityData.getKeySet()) {
                                    NBTBase nbt = this.tileEntityData.getTag(key);
                                    if (!"x".equals(key) && !"y".equals(key) && !"z".equals(key)) {
                                        compound.setTag(key, nbt.copy());
                                    }
                                }
                                tileentity.readFromNBT(compound);
                                tileentity.markDirty();
                            }
                        }
                    }
                }
            } else if ((this.fallTime > 100 && (currentPos.getY() < 1 || currentPos.getY() > 256)) || this.fallTime > 600) {
                this.setDead();
            }
        }
        // Copied from vanilla
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        IBlockState state = getBlock();
        buffer.writeInt(state != null ? Block.getStateId(state) : 0);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        int id = additionalData.readInt();
        this.spawnState = id != 0 ? Block.getStateById(id) : null;
    }
}
