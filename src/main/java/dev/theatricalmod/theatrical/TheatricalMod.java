/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * TheatricalMod.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12.2 @Mod lifecycle (FMLPreInitialization/Initialization/
 * PostInitialization events) in place of the 1.16 constructor + mod-bus setup; the
 * registration, capability, network and Art-Net wiring is added phase by phase as it
 * is ported.
 */
package dev.theatricalmod.theatrical;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class TheatricalMod {

    public static final String MOD_ID = Tags.MODID;

    public static Logger LOGGER;

    @Mod.Instance
    public static TheatricalMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        LOGGER.info("Initialising {} {} (Forge 1.12.2 port of Theatrical)", Tags.MODNAME, Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
