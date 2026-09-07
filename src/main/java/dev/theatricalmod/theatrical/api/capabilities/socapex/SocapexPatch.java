/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/capabilities/socapex/SocapexPatch.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: NBT API names only.
 */
package dev.theatricalmod.theatrical.api.capabilities.socapex;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * One patch: a dimmer channel wired to socket {@code receiverSocket} of the receiver at
 * {@code receiver}.
 */
public class SocapexPatch {

    private BlockPos receiver;
    private int receiverSocket;

    public SocapexPatch() {
    }

    public SocapexPatch(BlockPos pos, int receiverSocket) {
        this.receiver = pos;
        this.receiverSocket = receiverSocket;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        if (receiver != null) {
            tag.setInteger("receiver", receiverSocket);
            tag.setInteger("x", receiver.getX());
            tag.setInteger("y", receiver.getY());
            tag.setInteger("z", receiver.getZ());
        }
        return tag;
    }

    public void deserialize(NBTTagCompound tag) {
        if (tag.hasKey("x")) {
            this.receiver = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        }
        this.receiverSocket = tag.getInteger("receiver");
    }

    public BlockPos getReceiver() {
        return receiver;
    }

    public int getReceiverSocket() {
        return receiverSocket;
    }
}
