/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/TheatricalClient.java (Theatrical Team, Apache License 2.0); the upstream file
 * targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the client @SidedProxy. So far it only registers item models
 * (1.12 needs ModelLoader.setCustomModelResourceLocation per item; 1.16 finds them by
 * name) and the cutout render layers. The renderers, screens, texture stitching and the
 * Art-Net polling come with their phases of the port.
 */
package dev.theatricalmod.theatrical.client;

import dev.theatricalmod.theatrical.TheatricalCommon;
import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.block.TheatricalBlocks;
import dev.theatricalmod.theatrical.items.TheatricalItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collections;

@Mod.EventBusSubscriber(modid = TheatricalMod.MOD_ID, value = Side.CLIENT)
public class TheatricalClient extends TheatricalCommon {

    @Override
    public World getClientWorld() {
        return Minecraft.getMinecraft().world;
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (Item item : TheatricalItems.ALL_ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
        // The illuminator is invisible and has no blockstate file; tell the model loader not
        // to look for one, or it logs a missing model for each of its sixteen light levels.
        ModelLoader.setCustomStateMapper(TheatricalBlocks.ILLUMINATOR, block -> Collections.emptyMap());
    }
}
