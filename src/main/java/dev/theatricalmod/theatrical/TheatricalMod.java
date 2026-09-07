/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * TheatricalMod.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12.2 @Mod lifecycle (FMLPreInitialization/Initialization/PostInitialization events)
 *     in place of the 1.16 constructor + mod-bus setup; registration, network and Art-Net
 *     wiring is added phase by phase as it is ported;
 *   - the fixture registry is created by TheatricalFixtures in RegistryEvent.NewRegistry
 *     rather than from here;
 *   - the world capabilities are attached with 1.12's AttachCapabilitiesEvent<World> and
 *     ticked from WorldTickEvent exactly as upstream did, minus LazyOptional.
 */
package dev.theatricalmod.theatrical;

import dev.theatricalmod.theatrical.api.capabilities.WorldSocapexNetwork;
import dev.theatricalmod.theatrical.api.capabilities.dmx.WorldDMXNetwork;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.power.ITheatricalPowerStorage;
import dev.theatricalmod.theatrical.api.capabilities.power.TheatricalPower;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexProvider;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexProvider;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexReceiver;
import dev.theatricalmod.theatrical.util.CapabilityStorageProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class TheatricalMod {

    public static final String MOD_ID = Tags.MODID;

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    private static final ResourceLocation DMX_NETWORK_ID = new ResourceLocation(MOD_ID, "dmx_world_network");
    private static final ResourceLocation SOCAPEX_NETWORK_ID = new ResourceLocation(MOD_ID, "socapex_network");

    @Mod.Instance
    public static TheatricalMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initialising {} {} (Forge 1.12.2 port of Theatrical)", Tags.MODNAME, Tags.VERSION);
        registerCapabilities();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }

    private void registerCapabilities() {
        CapabilityManager.INSTANCE.register(IDMXProvider.class, new CapabilityStorageProvider<>(), DMXProvider::new);
        CapabilityManager.INSTANCE.register(IDMXReceiver.class, new CapabilityStorageProvider<>(), DMXReceiver::new);
        CapabilityManager.INSTANCE.register(WorldDMXNetwork.class, new CapabilityStorageProvider<>(), WorldDMXNetwork::new);

        CapabilityManager.INSTANCE.register(ISocapexReceiver.class, new CapabilityStorageProvider<>(), SocapexReceiver::new);
        CapabilityManager.INSTANCE.register(ISocapexProvider.class, new CapabilityStorageProvider<>(), SocapexProvider::new);
        CapabilityManager.INSTANCE.register(WorldSocapexNetwork.class, new CapabilityStorageProvider<>(), WorldSocapexNetwork::new);

        CapabilityManager.INSTANCE.register(ITheatricalPowerStorage.class, new CapabilityStorageProvider<>(), TheatricalPower::new);
    }

    /**
     * Every server-side world gets its own DMX and socapex networks. Client worlds get none:
     * the networks are walked on the server and their results reach the client as block
     * entity state.
     */
    @SubscribeEvent
    public void attachWorldCapabilities(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();
        if (world == null || world.isRemote) {
            return;
        }
        event.addCapability(DMX_NETWORK_ID, new WorldDMXNetwork());
        event.addCapability(SOCAPEX_NETWORK_ID, new WorldSocapexNetwork());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        World world = event.world;
        if (world.hasCapability(WorldDMXNetwork.CAP, null)) {
            WorldDMXNetwork dmxNetwork = world.getCapability(WorldDMXNetwork.CAP, null);
            if (dmxNetwork != null) {
                dmxNetwork.tick(world);
            }
        }
        if (world.hasCapability(WorldSocapexNetwork.CAP, null)) {
            WorldSocapexNetwork socapexNetwork = world.getCapability(WorldSocapexNetwork.CAP, null);
            if (socapexNetwork != null) {
                socapexNetwork.tick(world);
            }
        }
    }
}
