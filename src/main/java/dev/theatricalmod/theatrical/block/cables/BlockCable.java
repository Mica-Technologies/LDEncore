/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/cables/BlockCable.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 has no SixWayBlock; the six connection properties are computed in
 *     getActualState from the neighbours (they do not fit in metadata) and the collision /
 *     selection boxes are built from them;
 *   - implements api.ICable. ICable.canConnect(world, pos, side) answers "does the cable at
 *     pos continue towards side", which is what the network walkers ask; upstream's method
 *     took the neighbour's position and, as the walkers called it with the cable's own
 *     position, always answered yes;
 *   - getPickBlock returns the block's own item (1.12 has no BLOCK_TO_ITEM map).
 */
package dev.theatricalmod.theatrical.block.cables;

import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.ICable;
import dev.theatricalmod.theatrical.tiles.TileEntityCable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCable extends Block implements ICable {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    private static final PropertyBool[] BY_FACING = new PropertyBool[EnumFacing.values().length];

    static {
        BY_FACING[EnumFacing.DOWN.getIndex()] = DOWN;
        BY_FACING[EnumFacing.UP.getIndex()] = UP;
        BY_FACING[EnumFacing.NORTH.getIndex()] = NORTH;
        BY_FACING[EnumFacing.SOUTH.getIndex()] = SOUTH;
        BY_FACING[EnumFacing.WEST.getIndex()] = WEST;
        BY_FACING[EnumFacing.EAST.getIndex()] = EAST;
    }

    /** Half-width of the cable, in blocks (upstream's SixWayBlock apothem). */
    private static final double APOTHEM = 0.125D;
    private static final double MIN = 0.5D - APOTHEM;
    private static final double MAX = 0.5D + APOTHEM;

    private static final AxisAlignedBB CENTER = new AxisAlignedBB(MIN, MIN, MIN, MAX, MAX, MAX);
    private static final AxisAlignedBB[] ARMS = new AxisAlignedBB[EnumFacing.values().length];

    static {
        ARMS[EnumFacing.DOWN.getIndex()] = new AxisAlignedBB(MIN, 0, MIN, MAX, MIN, MAX);
        ARMS[EnumFacing.UP.getIndex()] = new AxisAlignedBB(MIN, MAX, MIN, MAX, 1, MAX);
        ARMS[EnumFacing.NORTH.getIndex()] = new AxisAlignedBB(MIN, MIN, 0, MAX, MAX, MIN);
        ARMS[EnumFacing.SOUTH.getIndex()] = new AxisAlignedBB(MIN, MIN, MAX, MAX, MAX, 1);
        ARMS[EnumFacing.WEST.getIndex()] = new AxisAlignedBB(0, MIN, MIN, MIN, MAX, MAX);
        ARMS[EnumFacing.EAST.getIndex()] = new AxisAlignedBB(MAX, MIN, MIN, 1, MAX, MAX);
    }

    private final CableType cableType;

    public BlockCable(CableType cableType) {
        super(Material.ANVIL);
        this.cableType = cableType;
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false)
                .withProperty(WEST, false).withProperty(UP, false).withProperty(DOWN, false));
    }

    public static PropertyBool propertyFor(EnumFacing facing) {
        return BY_FACING[facing.getIndex()];
    }

    @Override
    public CableType getCableType() {
        return this.cableType;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.values()) {
            state = state.withProperty(propertyFor(facing), canConnectTo(world, pos.offset(facing), facing));
        }
        return state;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean causesSuffocation(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getAmbientOcclusionLightValue(IBlockState state) {
        return 1.0F;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState actual = getActualState(state, world, pos);
        AxisAlignedBB box = CENTER;
        for (EnumFacing facing : EnumFacing.values()) {
            if (actual.getValue(propertyFor(facing))) {
                box = box.union(ARMS[facing.getIndex()]);
            }
        }
        return box;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        IBlockState actual = isActualState ? state : getActualState(state, world, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CENTER);
        for (EnumFacing facing : EnumFacing.values()) {
            if (actual.getValue(propertyFor(facing))) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, ARMS[facing.getIndex()]);
            }
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }

    public boolean containsType(CableType[] types) {
        for (CableType type : types) {
            if (type == this.cableType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this cable, when at the block next to {@code neighbourPos} in direction
     * {@code direction}, connects to whatever is at {@code neighbourPos}: another cable of
     * the same block, or a tile that accepts this cable type on that face.
     */
    public boolean canConnectTo(IBlockAccess world, BlockPos neighbourPos, EnumFacing direction) {
        Block block = world.getBlockState(neighbourPos).getBlock();
        TileEntity tileEntity = world.getTileEntity(neighbourPos);
        if (tileEntity instanceof IAcceptsCable) {
            return containsType(((IAcceptsCable) tileEntity).getAcceptedCables(direction.getOpposite()));
        }
        return block == this;
    }

    @Override
    public boolean canConnect(World world, BlockPos pos, EnumFacing side) {
        return canConnectTo(world, pos.offset(side), side);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        TileEntityCable tileEntityCable = new TileEntityCable();
        tileEntityCable.setCableType(cableType);
        return tileEntityCable;
    }
}
