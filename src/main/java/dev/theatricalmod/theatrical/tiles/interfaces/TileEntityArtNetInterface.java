/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/interfaces/TileEntityArtNetInterface.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 NBT, capability and lifecycle plumbing; the client-side
 * Art-Net polling that upstream did inline (it referenced Minecraft's session from common
 * code) is delegated to the client proxy, which fills it in with the Art-Net phase; the
 * client sync goes through TheatricalNetworkHandler; the GUI comes with the GUI phase.
 */
package dev.theatricalmod.theatrical.tiles.interfaces;

import dev.theatricalmod.theatrical.TheatricalConfigHandler;
import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Art-Net interface: the player who placed it receives Art-Net on their client and forwards
 * it here; the tile then pushes it down the DMX cables as a provider.
 */
public class TileEntityArtNetInterface extends TileEntityTheatricalBase implements ITickable, IAcceptsCable {

    private final IDMXProvider idmxProvider = new DMXProvider(new DMXUniverse());

    private int subnet, universe = 0;
    private String ip = TheatricalConfigHandler.ARTNET.defaultListenAddress;
    private UUID player;

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public UUID getPlayer() {
        return player;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            TheatricalMod.proxy.pollArtNet(this);
        }
    }

    /** Server side: takes a universe received from the owner's client and sends it on. */
    public void update(byte[] data) {
        this.idmxProvider.getUniverse(world).setDmxChannels(data);
        TheatricalNetworkHandler.sendProviderUniverse(world, pos, data);
        sendDMXSignal();
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound tag) {
        tag = super.getNBT(tag);
        tag.setInteger("subnet", this.subnet);
        tag.setInteger("universe", this.universe);
        tag.setString("ip", this.ip);
        if (this.player != null) {
            tag.setString("owner", this.player.toString());
        }
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound tag) {
        subnet = tag.getInteger("subnet");
        universe = tag.getInteger("universe");
        ip = tag.getString("ip");
        if (tag.hasKey("owner")) {
            try {
                this.player = UUID.fromString(tag.getString("owner"));
            } catch (IllegalArgumentException ignored) {
                this.player = null;
            }
        }
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXProvider.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXProvider.CAP) {
            return DMXProvider.CAP.cast(idmxProvider);
        }
        return super.getCapability(capability, facing);
    }

    public void sendDMXSignal() {
        idmxProvider.updateDevices(world, pos);
    }

    public int getSubnet() {
        return subnet;
    }

    public int getUniverse() {
        return universe;
    }

    public void setSubnet(int subnet) {
        this.subnet = subnet;
    }

    public void setUniverse(int universe) {
        this.universe = universe;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public void invalidate() {
        TheatricalMod.refreshDmxNetwork(world);
        super.invalidate();
    }

    @Override
    public void onLoad() {
        TheatricalMod.refreshDmxNetwork(world);
    }

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DMX};
    }
}
