/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenBasicLightingConsole.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 GuiContainer, GuiButton and GuiTextField, with drags delivered through
 *     mouseClickMove rather than a per-widget callback;
 *   - a dragged fader is sent to the server at most once per tick instead of on every drag
 *     event, which upstream turned into hundreds of packets per sweep;
 *   - Go no longer throws when a fade time field is empty or holds something unparsable:
 *     the fields accept digits only and an empty one reads as zero. Upstream parsed both
 *     unconditionally inside the click handler.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.client.gui.container.ContainerBasicLightingConsole;
import dev.theatricalmod.theatrical.client.gui.widgets.ButtonFader;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.control.ConsoleGoPacket;
import dev.theatricalmod.theatrical.network.control.MoveStepPacket;
import dev.theatricalmod.theatrical.network.control.ToggleModePacket;
import dev.theatricalmod.theatrical.network.control.UpdateConsoleFaderPacket;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScreenBasicLightingConsole extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/lighting_console.png");

    private static final int BUTTON_BACK = 100;
    private static final int BUTTON_FORWARD = 101;
    private static final int BUTTON_GO = 102;
    private static final int BUTTON_MODE = 103;

    /** Fader widgets carry ids 0..FADERS, with the grand master last. */
    private static final int GRAND_MASTER_ID = TileEntityBasicLightingControl.FADERS;

    private final ContainerBasicLightingConsole container;
    private final List<ButtonFader> faders = new ArrayList<>();

    private GuiTextField fadeInTime;
    private GuiTextField fadeOutTime;

    /** Fader value waiting to be sent, and the one last sent, so a drag costs one packet a tick. */
    private int pendingChannel = Integer.MIN_VALUE;
    private int pendingValue;

    public ScreenBasicLightingConsole(ContainerBasicLightingConsole container) {
        super(container);
        this.container = container;
        this.xSize = 244;
        this.ySize = 126;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.faders.clear();

        TileEntityBasicLightingControl desk = container.blockEntity;
        byte[] values = desk == null ? new byte[TileEntityBasicLightingControl.FADERS] : desk.getFaders();
        for (int i = 0; i < values.length; i++) {
            int baseY = guiTop + 7;
            if (i >= 6) {
                baseY += (i / 6) * 61;
            }
            int faderNumber = i - ((i / 6) * 6);
            ButtonFader fader = new ButtonFader(i, guiLeft + 7 + (faderNumber * 20), baseY, i, Byte.toUnsignedInt(values[i]));
            this.buttonList.add(fader);
            this.faders.add(fader);
        }
        ButtonFader master = new ButtonFader(GRAND_MASTER_ID, guiLeft + 184, guiTop + 7, -1,
                desk == null ? 255 : Byte.toUnsignedInt(desk.getGrandMaster()));
        this.buttonList.add(master);
        this.faders.add(master);

        this.buttonList.add(new GuiButton(BUTTON_BACK, guiLeft + 130, guiTop + 20, 15, 20, "<-"));
        this.buttonList.add(new GuiButton(BUTTON_FORWARD, guiLeft + 145, guiTop + 20, 15, 20, "->"));
        this.buttonList.add(new GuiButton(BUTTON_GO, guiLeft + 130, guiTop + 100, 20, 20, "Go"));
        this.buttonList.add(new GuiButton(BUTTON_MODE, guiLeft + 155, guiTop + 100, 30, 20, "Mode"));

        this.fadeInTime = makeTimeField(0, guiLeft + 170, guiTop + 60, desk == null ? 0 : desk.getFadeInTicks());
        this.fadeOutTime = makeTimeField(1, guiLeft + 170, guiTop + 75, desk == null ? 0 : desk.getFadeOutTicks());
    }

    private GuiTextField makeTimeField(int id, int x, int y, int value) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, 20, 10);
        field.setEnableBackgroundDrawing(true);
        field.setTextColor(-1);
        field.setDisabledTextColour(-1);
        field.setMaxStringLength(5);
        field.setValidator(text -> text != null && text.chars().allMatch(Character::isDigit));
        field.setText(Integer.toString(value));
        return field;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private int readTicks(GuiTextField field) {
        String text = field.getText();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (container.blockEntity == null) {
            return;
        }
        if (button instanceof ButtonFader) {
            // The widget computed its own value when it was pressed.
            ButtonFader fader = (ButtonFader) button;
            queueFader(fader.getChannel(), fader.getValue());
            return;
        }
        switch (button.id) {
            case BUTTON_BACK:
                TheatricalNetworkHandler.MAIN.sendToServer(new MoveStepPacket(container.blockEntity.getPos(), false));
                return;
            case BUTTON_FORWARD:
                TheatricalNetworkHandler.MAIN.sendToServer(new MoveStepPacket(container.blockEntity.getPos(), true));
                return;
            case BUTTON_GO:
                TheatricalNetworkHandler.MAIN.sendToServer(new ConsoleGoPacket(container.blockEntity.getPos(),
                        readTicks(fadeInTime), readTicks(fadeOutTime)));
                return;
            case BUTTON_MODE:
                TheatricalNetworkHandler.MAIN.sendToServer(new ToggleModePacket(container.blockEntity.getPos()));
                return;
            default:
                break;
        }
    }

    private void queueFader(int channel, int value) {
        this.pendingChannel = channel;
        this.pendingValue = value;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (ButtonFader fader : faders) {
            if (fader.isDragging()) {
                int value = fader.calculateNewValue(mouseY);
                fader.setValue(value);
                queueFader(fader.getChannel(), value);
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.fadeInTime.mouseClicked(mouseX, mouseY, mouseButton);
        this.fadeOutTime.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (ButtonFader fader : faders) {
            fader.mouseReleased(mouseX, mouseY);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.fadeInTime.textboxKeyTyped(typedChar, keyCode) || this.fadeOutTime.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.fadeInTime.updateCursorCounter();
        this.fadeOutTime.updateCursorCounter();

        if (pendingChannel != Integer.MIN_VALUE && container.blockEntity != null) {
            TheatricalNetworkHandler.MAIN.sendToServer(
                    new UpdateConsoleFaderPacket(container.blockEntity.getPos(), pendingChannel, pendingValue));
            pendingChannel = Integer.MIN_VALUE;
        }

        // Follow the desk's own values, so a second player moving a fader shows here too.
        TileEntityBasicLightingControl desk = container.blockEntity;
        if (desk != null) {
            byte[] values = desk.getFaders();
            for (ButtonFader fader : faders) {
                if (fader.isDragging()) {
                    continue;
                }
                int channel = fader.getChannel();
                if (channel == -1) {
                    fader.setValue(Byte.toUnsignedInt(desk.getGrandMaster()));
                } else if (channel < values.length) {
                    fader.setValue(Byte.toUnsignedInt(values[channel]));
                }
            }
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
        TileEntityBasicLightingControl desk = container.blockEntity;
        if (desk == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        fontRenderer.drawString(desk.isRunMode() ? "Run Mode" : "Program Mode", 165, 5, 0x404040);
        fontRenderer.drawString("Step " + desk.getCurrentStep(), 165, 15, 0x404040);
        fontRenderer.drawString("Cues", 265, 5, 0x404040);
        for (int key : desk.getStoredSteps().keySet()) {
            fontRenderer.drawString("Cue - " + key, 260, 15 + (10 * key), 0x404040);
        }
        fontRenderer.drawString("Fade In", 165, 77, 0x404040);
        fontRenderer.drawString("Fade Out", 165, 95, 0x404040);
        GlStateManager.popMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.fadeInTime.drawTextBox();
        this.fadeOutTime.drawTextBox();
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
