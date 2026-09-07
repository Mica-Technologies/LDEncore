/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/fixtures/FixtureType.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: upstream's enum constants reference the fixture tile entity
 * classes directly, which makes the api package depend on the tiles package. Here the tile
 * factory is registered by the tiles package at startup (setTileFactory), so the api stays
 * self-contained and a type without a factory fails loudly rather than with a null.
 */
package dev.theatricalmod.theatrical.api.fixtures;

import net.minecraft.tileentity.TileEntity;

import java.util.function.Supplier;

public enum FixtureType {

    INTELLIGENT,
    TUNGSTEN;

    private Supplier<? extends TileEntity> tileFactory;

    /**
     * Wires the tile entity this type of fixture creates. Called once by the tiles package
     * during registration.
     */
    public void setTileFactory(Supplier<? extends TileEntity> tileFactory) {
        this.tileFactory = tileFactory;
    }

    public Supplier<? extends TileEntity> getTileClass() {
        if (tileFactory == null) {
            throw new IllegalStateException("No tile entity factory registered for fixture type " + name());
        }
        return this.tileFactory;
    }

    public boolean hasTileFactory() {
        return tileFactory != null;
    }
}
