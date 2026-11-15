/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * An LED house fixture: a DMX-controlled RGB light that can also act as an ordinary room light
 * (see {@link TileEntityLedFixture} for the modes).
 *
 * The body is an ordinary baked model, rotated by the blockstate; only the lens is drawn by the
 * tile renderer, because its colour follows DMX and re-baking a chunk for every colour change
 * would be far dearer than a few quads a frame. Subclasses describe their shape once, in the
 * model's own unrotated coordinates, plus the blockstate rotation for each state; the lens, the
 * selection box and the collision box are all derived from those, so they match the model.
 *
 * Fixtures whose {@link #getRunAxis} is not null join with identical neighbours along that axis
 * into a run, which shares one DMX address and one mode.
 */
public abstract class BlockLedFixture extends Block {

    private final Map<IBlockState, AxisAlignedBB[]> lensCache = new HashMap<>();
    private final Map<IBlockState, AxisAlignedBB> shapeCache = new HashMap<>();

    protected BlockLedFixture() {
        super(Material.IRON);
        setSoundType(SoundType.METAL);
    }

    /** The blockstate rotation of this state's model, {x, y} in degrees. */
    protected abstract int[] getRotation(IBlockState state);

    /** The lens boxes in the unrotated model's coordinates. */
    protected abstract AxisAlignedBB[] getModelLens(IBlockState state);

    /** The whole fixture in the unrotated model's coordinates, for the selection box. */
    protected abstract AxisAlignedBB getModelShape(IBlockState state);

    /** The axis this fixture joins its neighbours along, or null if it stands alone. */
    @Nullable
    public EnumFacing.Axis getRunAxis(IBlockState state) {
        return null;
    }

    /** Whether two states belong to the same run (same block, same orientation). */
    public boolean sameRun(IBlockState state, IBlockState other) {
        return state == other;
    }

    /**
     * Whether the fixture at this position keeps a floor light while in Room Light mode. A run
     * places one under every other segment: one under each would be twice the light updates for
     * no light anyone could see.
     */
    public boolean placesFloorLight(IBlockState state, BlockPos pos) {
        EnumFacing.Axis axis = getRunAxis(state);
        if (axis == null || axis == EnumFacing.Axis.Y) {
            return true;
        }
        int along = axis == EnumFacing.Axis.X ? pos.getX() : pos.getZ();
        return (along & 1) == 0;
    }

    /** The lens boxes as drawn, in block coordinates. */
    public AxisAlignedBB[] getLensBoxes(IBlockState state) {
        return lensCache.computeIfAbsent(state, s -> {
            int[] rotation = getRotation(s);
            AxisAlignedBB[] model = getModelLens(s);
            AxisAlignedBB[] out = new AxisAlignedBB[model.length];
            for (int i = 0; i < model.length; i++) {
                out[i] = LedGeometry.rotate(model[i], rotation[0], rotation[1]);
            }
            return out;
        });
    }

    private AxisAlignedBB getShape(IBlockState state) {
        return shapeCache.computeIfAbsent(state, s -> {
            int[] rotation = getRotation(s);
            return LedGeometry.rotate(getModelShape(s), rotation[0], rotation[1]);
        });
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getShape(state.getActualState(source, pos));
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityLedFixture();
    }

    @Nullable
    private static TileEntityLedFixture tile(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityLedFixture ? (TileEntityLedFixture) tile : null;
    }

    /**
     * Full brightness in Room Light mode, none otherwise. In show mode the fixture glows only in
     * its render: following the dimmer here would recalculate the light around every segment on
     * every step of a chase.
     */
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntityLedFixture tile = tile(world, pos);
        return tile != null && tile.isRoomLit() ? 15 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(TheatricalMod.instance, TheatricalGuiHandler.GUI_LED_FIXTURE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntityLedFixture tile = tile(world, pos);
        if (tile != null && !world.isRemote) {
            tile.adoptNeighbourSettings();
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        TileEntityLedFixture tile = tile(world, pos);
        if (tile != null) {
            tile.onNeighbourChanged();
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityLedFixture tile = tile(world, pos);
        if (tile != null) {
            tile.onRemoved();
        }
        super.breakBlock(world, pos, state);
    }
}
