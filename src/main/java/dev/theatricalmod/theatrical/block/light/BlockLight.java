/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * block/light/BlockLight.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 signatures; the tile comes from the fixture type's
 * registered factory (see api.fixtures.FixtureType).
 */
package dev.theatricalmod.theatrical.block.light;

import dev.theatricalmod.theatrical.api.fixtures.Fixture;
import dev.theatricalmod.theatrical.block.BlockHangable;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityFixture;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockLight extends BlockHangable {

    protected final Fixture fixture;

    protected BlockLight(Material material, Fixture fixture) {
        super(material);
        this.fixture = fixture;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (fixture != null && flag.isAdvanced()) {
            String[] channels = fixture.getChannelsDefinition().toString().split("#");
            for (String channel : channels) {
                if (!channel.isEmpty()) {
                    tooltip.add(channel);
                }
            }
        }
        super.addInformation(stack, world, tooltip, flag);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        TileEntity tile = this.fixture.getFixtureType().getTileClass().get();
        if (tile instanceof TileEntityFixture) {
            ((TileEntityFixture) tile).setFixture(fixture);
        }
        return tile;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getAmbientOcclusionLightValue(IBlockState state) {
        return 1.0F;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    public Fixture getFixture() {
        return fixture;
    }
}
