/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/widgets/ButtonFader.java (Theatrical Team, Apache License 2.0); the upstream
 * file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: extends 1.12's GuiButton rather than a 1.16 Widget, and draws with
 * Gui's textured-rect helpers rather than blit; the drag callback upstream held per widget is
 * gone, because 1.12 delivers drags to the screen (mouseClickMove) and the screen owns the
 * list of faders anyway. The value is clamped, which upstream did not do: dragging past the
 * ends of the track sent values outside 0-255.
 */
package dev.theatricalmod.theatrical.client.gui.widgets;

import dev.theatricalmod.theatrical.TheatricalMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** One channel fader on the lighting desk. Channel -1 is the grand master. */
public class ButtonFader extends GuiButton {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/lighting_console.png");

    public static final int WIDTH = 10;
    public static final int HEIGHT = 51;

    private final int channel;
    private int value;
    private boolean dragging = false;

    public ButtonFader(int id, int x, int y, int channel, int value) {
        super(id, x, y, WIDTH, HEIGHT, "");
        this.channel = channel;
        this.value = value;
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
        drawTexturedModalRect(x, y, 0, 126, WIDTH, HEIGHT);
        int handleY = (y + (height - 7)) - (int) ((this.value / 255F) * 50);
        drawTexturedModalRect(x + 1, handleY, 10, 126, 8, 11);
        GlStateManager.enableDepth();
    }

    public int getChannel() {
        return channel;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed) {
            this.value = calculateNewValue(mouseY);
            this.dragging = true;
        }
        return pressed;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
        super.mouseReleased(mouseX, mouseY);
    }

    /** The value the fader would take if the mouse were at this screen Y. */
    public int calculateNewValue(int mouseY) {
        return MathHelper.clamp((int) (((this.height - (mouseY - this.y)) / (float) this.height) * 255F), 0, 255);
    }

    public boolean isDragging() {
        return this.dragging;
    }
}
