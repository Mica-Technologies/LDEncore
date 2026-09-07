/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/ISupport.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: Direction -> EnumFacing.
 */
package dev.theatricalmod.theatrical.api;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block a fixture can hang from (truss, bar). Supplies the translation the fixture's model
 * needs so it sits on the support rather than floating at the block origin.
 */
public interface ISupport {

    float[] getLightTransforms(World world, BlockPos pos, EnumFacing facing);

}
