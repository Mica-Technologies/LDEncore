/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenGenericFixture.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 GuiContainer and Forge's 1.12 GuiSlider; the sliders are
 * labelled, which upstream's were not; and a moved slider is sent to the server at most once
 * per tick rather than on every drag event. Upstream sent a packet per pixel of travel,
 * which is a few hundred packets for one sweep of a slider.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.client.gui.container.ContainerGenericFixture;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.UpdateFixturePacket;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiSlider;

public class ScreenGenericFixture extends GuiContainer implements GuiSlider.ISlider {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/blank.png");

    private static final int SLIDER_TILT = 0;
    private static final int SLIDER_PAN = 1;

    private final ContainerGenericFixture container;

    private GuiSlider tiltSlider;
    private GuiSlider panSlider;

    private int tilt;
    private int pan;
    private int sentTilt;
    private int sentPan;

    public ScreenGenericFixture(ContainerGenericFixture container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 126;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.tilt = container.blockEntity == null ? 0 : container.blockEntity.getTilt();
        this.pan = container.blockEntity == null ? 0 : container.blockEntity.getPan();
        this.sentTilt = this.tilt;
        this.sentPan = this.pan;
        this.tiltSlider = new GuiSlider(SLIDER_TILT, guiLeft + 13, guiTop + 35, 150, 20,
                "Tilt: ", "", -180, 180, this.tilt, false, true, this);
        this.panSlider = new GuiSlider(SLIDER_PAN, guiLeft + 13, guiTop + 65, 150, 20,
                "Pan: ", "", -180, 180, this.pan, false, true, this);
        this.buttonList.add(this.tiltSlider);
        this.buttonList.add(this.panSlider);
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        if (slider.id == SLIDER_PAN) {
            this.pan = slider.getValueInt();
        } else if (slider.id == SLIDER_TILT) {
            this.tilt = slider.getValueInt();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Coalesce a drag into at most one packet per tick.
        if (container.blockEntity != null && (pan != sentPan || tilt != sentTilt)) {
            sentPan = pan;
            sentTilt = tilt;
            TheatricalNetworkHandler.MAIN.sendToServer(new UpdateFixturePacket(container.blockEntity.getPos(), tilt, pan));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = "Generic Fixture";
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
