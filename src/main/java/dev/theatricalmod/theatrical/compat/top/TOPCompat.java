/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * compat/top/TOPCompat.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 registers a probe provider by sending an inter-mod message
 * naming a class, rather than 1.16's InterModComms with a supplier, so the provider is named
 * by string here and nothing outside this package ever mentions The One Probe.
 */
package dev.theatricalmod.theatrical.compat.top;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

/** Registers the in-world overlay, when The One Probe is installed. */
public final class TOPCompat {

    private static final String TOP_MOD_ID = "theoneprobe";

    private TOPCompat() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(TOP_MOD_ID);
    }

    public static void register() {
        if (!isLoaded()) {
            return;
        }
        FMLInterModComms.sendFunctionMessage(TOP_MOD_ID, "getTheOneProbe",
                TOPInfoProvider.class.getName());
    }
}
