/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * TheatricalMod.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12.2 @Mod lifecycle (FMLPreInitialization/Initialization/PostInitialization events)
 *     in place of the 1.16 constructor + mod-bus setup;
 *   - @SidedProxy (TheatricalCommon / client.TheatricalClient) in place of DistExecutor;
 *   - blocks, items, tiles and entities register from their own @EventBusSubscriber classes
 *     rather than DeferredRegisters owned here; the fixture registry is created by
 *     TheatricalFixtures in RegistryEvent.NewRegistry;
 *   - the world capabilities are attached with 1.12's AttachCapabilitiesEvent<World> and
 *     ticked from WorldTickEvent exactly as upstream did, minus LazyOptional;
 *   - the creative tab is labelled LDEncore (see the lang file), the mod's own name;
 *   - the Art-Net manager, network channel and The One Probe IMC arrive with their phases.
 */
package dev.theatricalmod.theatrical;

import dev.theatricalmod.theatrical.api.capabilities.WorldSocapexNetwork;
import dev.theatricalmod.theatrical.api.capabilities.dmx.WorldDMXNetwork;
import dev.theatricalmod.theatrical.artnet.ArtNetManager;
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
import dev.theatricalmod.theatrical.client.gui.TheatricalGuiHandler;
import dev.theatricalmod.theatrical.items.TheatricalItems;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.util.CapabilityStorageProvider;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class TheatricalMod {

    public static final String MOD_ID = Tags.MODID;

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    private static final String COMMON_PROXY = "dev.theatricalmod.theatrical.TheatricalCommon";
    private static final String CLIENT_PROXY = "dev.theatricalmod.theatrical.client.TheatricalClient";

    private static final ResourceLocation DMX_NETWORK_ID = new ResourceLocation(MOD_ID, "dmx_world_network");
    private static final ResourceLocation SOCAPEX_NETWORK_ID = new ResourceLocation(MOD_ID, "socapex_network");

    /** The creative tab. Its label comes from the lang key {@code itemGroup.theatrical}. */
    public static final CreativeTabs THEATRICAL_TAB = new CreativeTabs(MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(TheatricalItems.DIMMER_RACK);
        }
    };

    @Mod.Instance
    public static TheatricalMod instance;

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static TheatricalCommon proxy;

    /**
     * The Art-Net clients, one per address an interface listens on.
     *
     * CHANGED FROM UPSTREAM: upstream created this on the common proxy and so ran an Art-Net
     * receive socket on a dedicated server as well, where nothing ever read from it. Art-Net
     * only ever reaches the game through the owning player's client, so the manager is only
     * created there.
     */
    private static ArtNetManager artNetManager;

    /** Never null: a server that never polls simply gets a manager holding no clients. */
    public static ArtNetManager getArtNetManager() {
        if (artNetManager == null) {
            artNetManager = new ArtNetManager();
        }
        return artNetManager;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initialising {} {} (Forge 1.12.2 port of Theatrical)", Tags.MODNAME, Tags.VERSION);
        registerCapabilities();
        TheatricalNetworkHandler.init();
        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new TheatricalGuiHandler());
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    /**
     * Closes every Art-Net socket when the game stops.
     *
     * CHANGED FROM UPSTREAM: upstream shut the clients down when the server stopped, which on
     * a single-player world leaked every socket on the way back to the main menu, since the
     * client keeps running. This also clears the failed-address list, so a player who fixes
     * their network setup and rejoins is not still refused.
     */
    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        getArtNetManager().shutdownAll();
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

    /** Flags both world networks for a re-walk on the next tick. Server worlds only. */
    public static void refreshNetworks(World world) {
        if (world == null || world.isRemote) {
            return;
        }
        if (world.hasCapability(WorldDMXNetwork.CAP, null)) {
            WorldDMXNetwork dmx = world.getCapability(WorldDMXNetwork.CAP, null);
            if (dmx != null) {
                dmx.setRefresh(true);
            }
        }
        if (world.hasCapability(WorldSocapexNetwork.CAP, null)) {
            WorldSocapexNetwork socapex = world.getCapability(WorldSocapexNetwork.CAP, null);
            if (socapex != null) {
                socapex.setRefresh(true);
            }
        }
    }

    /** Flags only the DMX network for a re-walk. */
    public static void refreshDmxNetwork(World world) {
        if (world == null || world.isRemote || !world.hasCapability(WorldDMXNetwork.CAP, null)) {
            return;
        }
        WorldDMXNetwork dmx = world.getCapability(WorldDMXNetwork.CAP, null);
        if (dmx != null) {
            dmx.setRefresh(true);
        }
    }

    /** Flags only the socapex network for a re-walk. */
    public static void refreshSocapexNetwork(World world) {
        if (world == null || world.isRemote || !world.hasCapability(WorldSocapexNetwork.CAP, null)) {
            return;
        }
        WorldSocapexNetwork socapex = world.getCapability(WorldSocapexNetwork.CAP, null);
        if (socapex != null) {
            socapex.setRefresh(true);
        }
    }
}
