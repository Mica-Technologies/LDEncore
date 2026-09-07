/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/WorldSocapexNetwork.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the same three changes as WorldDMXNetwork -- 1.12's
 * ICapabilityProvider, the provider is its own capability instance, isInvalid().
 */
package dev.theatricalmod.theatrical.api.capabilities;

import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexProvider;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-world registry of socapex providers (dimmer racks), refreshed on demand the same way
 * as {@link dev.theatricalmod.theatrical.api.capabilities.dmx.WorldDMXNetwork}.
 */
public class WorldSocapexNetwork implements ICapabilityProvider {

    @CapabilityInject(WorldSocapexNetwork.class)
    public static Capability<WorldSocapexNetwork> CAP = null;

    private final HashMap<BlockPos, ISocapexProvider> panelList = new HashMap<>();
    private boolean refresh = true;

    public WorldSocapexNetwork() {
    }

    public void updateDevices(World world) {
        for (Map.Entry<BlockPos, ISocapexProvider> entry : panelList.entrySet()) {
            entry.getValue().updateDevices(world, entry.getKey());
        }
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public void tick(World world) {
        if (refresh) {
            panelList.clear();
            List<TileEntity> tiles = new ArrayList<>(world.loadedTileEntityList);
            for (TileEntity tileEntity : tiles) {
                if (!tileEntity.isInvalid() && tileEntity.hasCapability(SocapexProvider.CAP, null)) {
                    ISocapexProvider provider = tileEntity.getCapability(SocapexProvider.CAP, null);
                    if (provider != null) {
                        panelList.put(tileEntity.getPos(), provider);
                    }
                }
            }

            for (ISocapexProvider provider : panelList.values()) {
                provider.refreshDevices();
            }
            refresh = false;
        }
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CAP;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CAP ? CAP.cast(this) : null;
    }

}
