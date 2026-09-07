/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * network/TheatricalNetworkHandler.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12's SimpleNetworkWrapper in place of the 1.16 SimpleChannel, with an
 *     IMessage/IMessageHandler pair per packet;
 *   - the discriminator ids are upstream's, so the two lines stay comparable;
 *   - a provider's universe is sent to the players tracking that block rather than to
 *     everyone in the dimension. Upstream broadcast a 512-byte array to every player in the
 *     world several times a second, whether or not they could see the block.
 */
package dev.theatricalmod.theatrical.network;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.network.control.ConsoleGoPacket;
import dev.theatricalmod.theatrical.network.control.MoveStepPacket;
import dev.theatricalmod.theatrical.network.control.ToggleModePacket;
import dev.theatricalmod.theatrical.network.control.UpdateConsoleFaderPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class TheatricalNetworkHandler {

    public static SimpleNetworkWrapper MAIN;

    private TheatricalNetworkHandler() {
    }

    public static void init() {
        MAIN = NetworkRegistry.INSTANCE.newSimpleChannel(TheatricalMod.MOD_ID);

        MAIN.registerMessage(SendDMXProviderPacket.Handler.class, SendDMXProviderPacket.class, 2, Side.CLIENT);
        MAIN.registerMessage(UpdateDMXAddressPacket.Handler.class, UpdateDMXAddressPacket.class, 3, Side.SERVER);
        MAIN.registerMessage(UpdateArtNetInterfacePacket.Handler.class, UpdateArtNetInterfacePacket.class, 4, Side.SERVER);
        MAIN.registerMessage(UpdateFixturePacket.Handler.class, UpdateFixturePacket.class, 5, Side.SERVER);
        MAIN.registerMessage(ChangeDimmerPatchPacket.Handler.class, ChangeDimmerPatchPacket.class, 6, Side.SERVER);
        MAIN.registerMessage(UpdateConsoleFaderPacket.Handler.class, UpdateConsoleFaderPacket.class, 7, Side.SERVER);
        MAIN.registerMessage(ConsoleGoPacket.Handler.class, ConsoleGoPacket.class, 8, Side.SERVER);
        MAIN.registerMessage(MoveStepPacket.Handler.class, MoveStepPacket.class, 9, Side.SERVER);
        MAIN.registerMessage(ToggleModePacket.Handler.class, ToggleModePacket.class, 10, Side.SERVER);
        MAIN.registerMessage(SendArtNetToServerPacket.Handler.class, SendArtNetToServerPacket.class, 11, Side.SERVER);
    }

    /**
     * Sends a DMX provider's whole universe to every player currently tracking that block,
     * so their copy of the provider tile can show it.
     */
    public static void sendProviderUniverse(World world, BlockPos pos, byte[] data) {
        if (world == null || world.isRemote || MAIN == null) {
            return;
        }
        SendDMXProviderPacket packet = new SendDMXProviderPacket(pos, data);
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && isTracking((EntityPlayerMP) player, world, pos)) {
                MAIN.sendTo(packet, (EntityPlayerMP) player);
            }
        }
    }

    /** Whether the player has the chunk containing this block loaded on their client. */
    private static boolean isTracking(EntityPlayerMP player, World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        return ((WorldServer) world).getPlayerChunkMap().isPlayerWatchingChunk(player, pos.getX() >> 4, pos.getZ() >> 4);
    }
}
