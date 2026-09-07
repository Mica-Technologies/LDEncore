/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * TheatricalConfigHandler.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: Forge 1.12's annotation config (@Config) replaces ForgeConfigSpec.
 * Upstream split the options into a common and a client file; 1.12 has one file,
 * config/theatrical.cfg, with the same two categories and the same defaults.
 */
package dev.theatricalmod.theatrical;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = TheatricalMod.MOD_ID, name = TheatricalMod.MOD_ID)
@Config.LangKey("config.theatrical.title")
public final class TheatricalConfigHandler {

    @Config.Name("fixtures")
    @Config.Comment("Fixture behaviour")
    public static final Fixtures FIXTURES = new Fixtures();

    @Config.Name("rendering")
    @Config.Comment("Client-side rendering")
    public static final Rendering RENDERING = new Rendering();

    private TheatricalConfigHandler() {
    }

    public static final class Fixtures {
        @Config.Comment("Set this to false to prevent lights emitting actual light")
        public boolean emitLight = true;

        @Config.Comment("Set this to false to prevent moving lights consuming power")
        public boolean consumePower = true;
    }

    public static final class Rendering {
        @Config.Comment("Opacity of rendered light beams. Set this to 0 to disable light beams")
        @Config.RangeDouble(min = 0, max = 1)
        public double lightBeamOpacity = 0.4;
    }

    @Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
    private static final class Sync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (TheatricalMod.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(TheatricalMod.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
