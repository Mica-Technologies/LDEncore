/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/dmx/WorldDMXNetwork.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's ICapabilityProvider (hasCapability/getCapability, no LazyOptional);
 *   - the provider hands out itself as the capability instance. Upstream created a second,
 *     separate WorldDMXNetwork via CAP.getDefaultInstance(), so the object attached to the
 *     world and the object the mod ticked were different; one object is what was intended;
 *   - isInvalid() is 1.12's name for isRemoved().
 */
package dev.theatricalmod.theatrical.api.capabilities.dmx;

import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
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

/**
 * Per-world registry of DMX providers. When something flags a refresh (a cable or device
 * placed or broken), every provider forgets its cached receiver list and re-walks its cables
 * on the next tick.
 */
public class WorldDMXNetwork implements ICapabilityProvider {

    @CapabilityInject(WorldDMXNetwork.class)
    public static Capability<WorldDMXNetwork> CAP = null;

    private final HashMap<IDMXProvider, BlockPos> providerList = new HashMap<>();
    private boolean refresh = true;

    public WorldDMXNetwork() {
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public void tick(World world) {
        if (refresh) {
            providerList.clear();
            // Copy first: a provider's refresh can add or remove tiles while we iterate.
            List<TileEntity> tiles = new ArrayList<>(world.loadedTileEntityList);
            for (TileEntity tileEntity : tiles) {
                if (!tileEntity.isInvalid() && tileEntity.hasCapability(DMXProvider.CAP, null)) {
                    IDMXProvider provider = tileEntity.getCapability(DMXProvider.CAP, null);
                    if (provider != null) {
                        providerList.put(provider, tileEntity.getPos());
                    }
                }
            }

            for (IDMXProvider provider : providerList.keySet()) {
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
