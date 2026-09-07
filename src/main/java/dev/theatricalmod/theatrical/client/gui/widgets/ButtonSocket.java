/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/widgets/ButtonSocket.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 GuiButton and GlStateManager; the press callback is gone
 * because 1.12 dispatches presses to the screen's actionPerformed; the container reference
 * upstream held on the widget is gone with it, since nothing here read it.
 */
package dev.theatricalmod.theatrical.client.gui.widgets;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/** One of the two sockets on a dimmer channel in the rack's left-hand panel. */
public class ButtonSocket extends GuiButton {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/dimmer_rack.png");

    private final int channelNumber;
    private final boolean secondSocket;
    private final SocapexPatch patch;
    private final String patchIdentifier;

    public ButtonSocket(int id, int x, int y, int channelNumber, boolean secondSocket) {
        this(id, x, y, channelNumber, secondSocket, null, "");
    }

    public ButtonSocket(int id, int x, int y, int channelNumber, boolean secondSocket, @Nullable SocapexPatch patch, String patchIdentifier) {
        super(id, x, y, 12, 12, "");
        this.channelNumber = channelNumber;
        this.secondSocket = secondSocket;
        this.patch = patch;
        this.patchIdentifier = patchIdentifier;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public boolean isSecondSocket() {
        return secondSocket;
    }

    public boolean isPatched() {
        return patch != null && patch.getReceiver() != null;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        this.hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawScaledCustomSizeModalRect(x, y, 269, 0, 17, 17, 12, 12, 512, 512);
        if (isPatched()) {
            drawScaledCustomSizeModalRect(x, y, 250, 0, 19, 17, 13, 13, 512, 512);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 5, y + 7, 0);
            GlStateManager.scale(0.7F, 0.7F, 1F);
            mc.fontRenderer.drawString(patchIdentifier + (patch.getReceiverSocket() + 1), 0, 0, 0xFFFFFF);
            GlStateManager.popMatrix();
        }
        if (hovered) {
            drawRect(x, y, x + width, y + height, -2130706433);
        }
    }
}
