/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 * Fork-authored file, Apache License 2.0.
 *
 * Upstream's DMX and socapex providers walk cable runs by checking
 * `instanceof BlockCable` and calling the block directly, which makes the api package
 * depend on the block package. This interface is the part of a cable block the network
 * walkers actually need; the cable block implements it when the block package is ported.
 */
package dev.theatricalmod.theatrical.api;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block that carries a cable network: the DMX and socapex walkers follow it from block to
 * block as long as {@link #canConnect} says the run continues in that direction.
 */
public interface ICable {

    CableType getCableType();

    boolean canConnect(World world, BlockPos pos, EnumFacing side);

}
