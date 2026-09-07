/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/widgets/ButtonPlug.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 GuiButton, GlStateManager and Gui's scaled textured-rect
 * helper (the dimmer rack sheet is 512x512, so the source region has to be scaled down
 * explicitly rather than assumed to be 1:1); the press callback is gone because 1.12
 * dispatches presses to the screen's actionPerformed.
 */
package dev.theatricalmod.theatrical.client.gui.widgets;

import dev.theatricalmod.theatrical.TheatricalMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** One socket on the distro currently shown in the dimmer rack's right-hand panel. */
public class ButtonPlug extends GuiButton {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/dimmer_rack.png");

    private final int plugNumber;
    private final String identifier;
    private boolean active;

    public ButtonPlug(int id, int x, int y, int plugNumber, String identifier, boolean active) {
        super(id, x, y, 14, 12, "");
        this.plugNumber = plugNumber;
        this.identifier = identifier;
        this.active = active;
    }

    public int getPlugNumber() {
        return plugNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        this.hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        GlStateManager.disableDepth();
        drawScaledCustomSizeModalRect(x, y, 250, 0, 19, 17, width, height, 512, 512);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 4, y + 5, 0);
        GlStateManager.scale(0.5F, 0.5F, 1F);
        mc.fontRenderer.drawString(identifier + plugNumber, 0, 0, 0xFFFFFF);
        GlStateManager.popMatrix();
        if (hovered) {
            drawRect(x, y, x + width, y + height, -2130706433);
        }
        if (active) {
            drawRect(x, y, x + width, y + height, 0x6666FF66);
        }
        GlStateManager.enableDepth();
    }
}
