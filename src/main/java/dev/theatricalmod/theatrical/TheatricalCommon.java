/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * TheatricalCommon.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: this is the @SidedProxy base. Upstream's class was a proxy in all
 * but name (its packet handlers took a network Context); those handlers return with the
 * network package. The client override lives in client.TheatricalClient.
 */
package dev.theatricalmod.theatrical;

import dev.theatricalmod.theatrical.tiles.interfaces.TileEntityArtNetInterface;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Server-side (and shared) proxy. Anything the client must do differently is overridden in
 * {@code client.TheatricalClient}.
 */
public class TheatricalCommon {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /** The client's current world; null on a dedicated server. */
    public World getClientWorld() {
        return null;
    }

    /**
     * Called every tick by an Art-Net interface tile on the logical client. The client proxy
     * reads the Art-Net client here and forwards data to the server; the server has nothing
     * to do.
     */
    public void pollArtNet(TileEntityArtNetInterface tile) {
    }
}
