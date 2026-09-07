/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * items/TheatricalItems.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: eager static instances registered from RegistryEvent.Register
 * instead of a DeferredRegister; ItemBlock in place of BlockItem; the cog is registered
 * as "gearIron" in the ore dictionary, which is what upstream's forge:gears/iron tag did.
 */
package dev.theatricalmod.theatrical.items;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.block.TheatricalBlocks;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID)
public final class TheatricalItems {

    private static final List<Item> REGISTERED = new ArrayList<>();

    /*
     * Blocks
     */
    public static final Item TRUSS = blockItem(TheatricalBlocks.TRUSS);
    public static final Item ARTNET_INTERFACE = blockItem(TheatricalBlocks.ARTNET_INTERFACE);
    public static final Item DMX_CABLE = blockItem(TheatricalBlocks.DMX_CABLE);
    public static final Item SOCAPEX_CABLE = blockItem(TheatricalBlocks.SOCAPEX_CABLE);
    public static final Item DIMMED_POWER_CABLE = blockItem(TheatricalBlocks.DIMMED_POWER_CABLE);
    public static final Item POWER_CABLE = blockItem(TheatricalBlocks.POWER_CABLE);
    public static final Item IWB = blockItem(TheatricalBlocks.IWB);
    public static final Item TEST_DMX = blockItem(TheatricalBlocks.TEST_DMX);
    public static final Item MOVING_LIGHT = blockItem(TheatricalBlocks.MOVING_LIGHT);
    public static final Item GENERIC_LIGHT = blockItem(TheatricalBlocks.GENERIC_LIGHT);
    public static final Item DIMMER_RACK = blockItem(TheatricalBlocks.DIMMER_RACK);
    public static final Item SOCAPEX_DISTRIBUTION = blockItem(TheatricalBlocks.SOCAPEX_DISTRIBUTION);
    public static final Item BASIC_LIGHTING_DESK = blockItem(TheatricalBlocks.BASIC_LIGHTING_DESK);
    public static final Item DMX_REDSTONE_INTERFACE = blockItem(TheatricalBlocks.DMX_REDSTONE_INTERFACE);

    /*
     * Useful items
     */
    public static final Item POSITIONER = prepare(new ItemPositioner(), "positioner");
    public static final Item WRENCH = prepare(new ItemWrench(), "wrench");

    /*
     * Recipe ingredients
     */
    public static final Item BULB = prepare(new Item(), "bulb");
    public static final Item COG = prepare(new Item(), "cog");
    public static final Item MOTOR = prepare(new Item(), "motor");
    public static final Item LED = prepare(new Item(), "led");

    /** Every item, in registration order; the client registers a model for each. */
    public static final List<Item> ALL_ITEMS = Collections.unmodifiableList(REGISTERED);

    private TheatricalItems() {
    }

    private static Item blockItem(Block block) {
        Item item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        REGISTERED.add(item);
        return item;
    }

    private static <T extends Item> T prepare(T item, String name) {
        item.setRegistryName(TheatricalMod.MOD_ID, name);
        item.setTranslationKey(TheatricalMod.MOD_ID + "." + name);
        item.setCreativeTab(TheatricalMod.THEATRICAL_TAB);
        REGISTERED.add(item);
        return item;
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (Item item : REGISTERED) {
            event.getRegistry().register(item);
        }
        OreDictionary.registerOre("gearIron", COG);
    }
}
