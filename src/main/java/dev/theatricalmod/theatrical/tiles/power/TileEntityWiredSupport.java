/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.tiles.power;

/**
 * The tile behind a truss or IWB block, which carries Forge Energy to the fixtures hung
 * from it. It does not tick: a source has to push into the bar, or feed it through a power
 * cable, which also pulls from storage. Created lazily, so truss placed before this
 * existed gains one the first time anything asks for it.
 */
public class TileEntityWiredSupport extends TileEntityPowerConduit {
}
