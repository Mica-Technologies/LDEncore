/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * compat/top/TOPInfoProvider.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's probe API: plain strings rather than text components, IBlockState, EntityPlayer.
 *   - The overlay lines are built here, from the block's capabilities, instead of through an
 *     ITOPInfoProvider interface that each block implemented. Upstream's arrangement put The
 *     One Probe's types into the signature of an interface that ordinary blocks implement, so
 *     those blocks carry a reference to a mod that need not be installed. Reading the
 *     capabilities keeps every mention of The One Probe inside this package, which is loaded
 *     only when it is present, and means a block gains the overlay by having the capability
 *     rather than by remembering to implement an interface.
 *   - Upstream showed a dimmer rack's DMX channels but not the address they start at, which is
 *     the number a player actually has to match on their desk, so the start address is shown
 *     too. A socapex receiver's channels are shown as well; upstream showed nothing for them.
 */
package dev.theatricalmod.theatrical.compat.top;

import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.power.ITheatricalPowerStorage;
import dev.theatricalmod.theatrical.api.capabilities.power.TheatricalPower;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexReceiver;
import dev.theatricalmod.theatrical.TheatricalMod;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.function.Function;

/** Adds LDEncore's power and DMX readouts to The One Probe's in-world overlay. */
public class TOPInfoProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {

    /** Beyond this many DMX channels the overlay becomes a wall of text, so it is summarised. */
    private static final int MAX_LISTED_CHANNELS = 8;

    @Override
    public String getID() {
        return TheatricalMod.MOD_ID + ":default";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world,
                             IBlockState state, IProbeHitData hit) {
        TileEntity tile = world.getTileEntity(hit.getPos());
        if (tile == null) {
            return;
        }
        addPower(info, tile);
        addDmx(info, tile, mode);
        addSocapex(info, tile);
    }

    private void addPower(IProbeInfo info, TileEntity tile) {
        if (!tile.hasCapability(TheatricalPower.CAP, null)) {
            return;
        }
        ITheatricalPowerStorage power = tile.getCapability(TheatricalPower.CAP, null);
        if (power != null) {
            info.text("Power: " + power.getEnergyStored() + " / " + power.getMaxEnergyStored());
        }
    }

    private void addDmx(IProbeInfo info, TileEntity tile, ProbeMode mode) {
        if (!tile.hasCapability(DMXReceiver.CAP, null)) {
            return;
        }
        IDMXReceiver receiver = tile.getCapability(DMXReceiver.CAP, null);
        if (receiver == null) {
            return;
        }
        int count = receiver.getChannelCount();
        info.text("DMX address: " + receiver.getStartPoint() + " (" + count + " channels)");
        // The individual values are only worth the space when the player asks for detail.
        if (mode != ProbeMode.EXTENDED) {
            return;
        }
        int shown = Math.min(count, MAX_LISTED_CHANNELS);
        for (int i = 0; i < shown; i++) {
            info.text("  #" + i + ": " + Byte.toUnsignedInt(receiver.getChannel(i)));
        }
        if (count > shown) {
            info.text("  ... and " + (count - shown) + " more");
        }
    }

    private void addSocapex(IProbeInfo info, TileEntity tile) {
        if (!tile.hasCapability(SocapexReceiver.CAP, null)) {
            return;
        }
        ISocapexReceiver receiver = tile.getCapability(SocapexReceiver.CAP, null);
        if (receiver == null) {
            return;
        }
        int live = 0;
        for (int i = 0; i < receiver.getTotalChannels(); i++) {
            if (receiver.getEnergyStored(i) > 0) {
                live++;
            }
        }
        info.text("Socapex: " + live + " of " + receiver.getTotalChannels() + " channels live");
    }

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(this);
        return null;
    }
}
