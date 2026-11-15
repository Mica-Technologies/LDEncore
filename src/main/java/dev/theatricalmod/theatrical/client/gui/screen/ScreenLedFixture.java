/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.client.gui.container.ContainerLedFixture;
import dev.theatricalmod.theatrical.network.AimLedFixturePacket;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.UpdateLedFixturePacket;
import dev.theatricalmod.theatrical.tiles.led.TileEntityLedFixture;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * The LED fixture screen: DMX start address, mode (Show / Room / Auto) and, for fixtures that
 * form runs, pixel mode. Save applies all three to the whole run.
 */
public class ScreenLedFixture extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/blank.png");

    private static final int BUTTON_MODE = 0;
    private static final int BUTTON_PIXEL = 1;
    private static final int BUTTON_SAVE = 2;
    private static final int BUTTON_PAN_LEFT = 3;
    private static final int BUTTON_PAN_RIGHT = 4;
    private static final int BUTTON_TILT_DOWN = 5;
    private static final int BUTTON_TILT_UP = 6;
    /** Degrees an aim button moves a track head. */
    private static final int AIM_STEP = 15;

    private final ContainerLedFixture container;
    private GuiTextField address;
    private GuiButton modeButton;
    private GuiButton pixelButton;
    private TileEntityLedFixture.Mode mode = TileEntityLedFixture.Mode.AUTO;
    private boolean pixel;

    public ScreenLedFixture(ContainerLedFixture container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 126;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        TileEntityLedFixture tile = container.blockEntity;
        if (tile != null) {
            mode = tile.getMode();
            pixel = tile.isPixel();
        }
        address = new GuiTextField(0, fontRenderer, guiLeft + 20, guiTop + 32, 50, 20);
        address.setMaxStringLength(3);
        address.setValidator(text -> text != null && text.chars().allMatch(Character::isDigit));
        address.setText(Integer.toString(tile == null ? 0 : tile.getAddress()));
        address.setFocused(true);
        modeButton = new GuiButton(BUTTON_MODE, guiLeft + 80, guiTop + 32, 76, 20, "");
        pixelButton = new GuiButton(BUTTON_PIXEL, guiLeft + 20, guiTop + 56, 136, 20, "");
        pixelButton.visible = tile != null && tile.canRun();
        buttonList.add(modeButton);
        buttonList.add(pixelButton);
        buttonList.add(new GuiButton(BUTTON_SAVE, guiLeft + 38, guiTop + 100, 100, 20, "Save"));
        if (tile != null && tile.isAimed()) {
            // A track head aims instead of forming runs, so its aim takes the pixel button's row.
            // Aim applies at once; it does not wait for Save.
            buttonList.add(new GuiButton(BUTTON_PAN_LEFT, guiLeft + 20, guiTop + 56, 32, 20, "Pan<"));
            buttonList.add(new GuiButton(BUTTON_PAN_RIGHT, guiLeft + 54, guiTop + 56, 32, 20, "Pan>"));
            buttonList.add(new GuiButton(BUTTON_TILT_DOWN, guiLeft + 90, guiTop + 56, 32, 20, "Tilt-"));
            buttonList.add(new GuiButton(BUTTON_TILT_UP, guiLeft + 124, guiTop + 56, 32, 20, "Tilt+"));
        }
        updateLabels();
    }

    private void updateLabels() {
        modeButton.displayString = "Mode: " + modeName(mode);
        pixelButton.displayString = "Pixel per segment: " + (pixel ? "on" : "off");
    }

    private static String modeName(TileEntityLedFixture.Mode mode) {
        switch (mode) {
            case SHOW:
                return "Show";
            case ROOM:
                return "Room";
            case AUTO:
            default:
                return "Auto";
        }
    }

    private void aim(int panDelta, int tiltDelta) {
        TileEntityLedFixture tile = container.blockEntity;
        if (tile != null) {
            TheatricalNetworkHandler.MAIN.sendToServer(new AimLedFixturePacket(tile.getPos(),
                    tile.getAimPan() + panDelta, tile.getAimTilt() + tiltDelta));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BUTTON_PAN_LEFT:
                aim(-AIM_STEP, 0);
                return;
            case BUTTON_PAN_RIGHT:
                aim(AIM_STEP, 0);
                return;
            case BUTTON_TILT_DOWN:
                // Tilt is measured from straight down, so "down" means less of it.
                aim(0, -AIM_STEP);
                return;
            case BUTTON_TILT_UP:
                aim(0, AIM_STEP);
                return;
            case BUTTON_MODE:
                mode = mode.next();
                updateLabels();
                return;
            case BUTTON_PIXEL:
                pixel = !pixel;
                updateLabels();
                return;
            case BUTTON_SAVE:
                if (container.blockEntity != null) {
                    String text = address.getText();
                    int value = text.isEmpty() ? container.blockEntity.getAddress()
                            : Math.min(Integer.parseInt(text), DMXUniverse.CHANNELS - 1);
                    TheatricalNetworkHandler.MAIN.sendToServer(
                            new UpdateLedFixturePacket(container.blockEntity.getPos(), value, mode, pixel));
                }
                return;
            default:
                super.actionPerformed(button);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        address.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (address.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        address.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        TileEntityLedFixture tile = container.blockEntity;
        if (tile == null) {
            return;
        }
        String title = tile.getWorld().getBlockState(tile.getPos()).getBlock().getLocalizedName();
        fontRenderer.drawString(title, xSize / 2 - fontRenderer.getStringWidth(title) / 2, 6, 0x404040);
        fontRenderer.drawString("DMX address", 20, 21, 0x404040);
        int first = tile.getAddress();
        int last = Math.min(DMXUniverse.CHANNELS - 1, first + tile.getFootprint() - 1);
        // The order (R G B Dimmer House, then R G B per further pixel) is on the wiki; spelling it
        // out here does not fit the panel.
        String channels = "Uses channels " + first + "-" + last;
        if (tile.isAimed()) {
            String aim = "Aim: " + compass(tile.getAimPan()) + ", " + tile.getAimTilt() + " deg from down";
            fontRenderer.drawString(aim, 20, 79, 0x404040);
        } else if (tile.canRun()) {
            String run = "Run of " + tile.getRunLength() + ", this is segment " + (tile.getRunIndex() + 1);
            fontRenderer.drawString(run, 20, 79, 0x404040);
        }
        fontRenderer.drawString(channels, 20, 89, 0x404040);
        // Kept short: it shares a line with the address label.
        String now = tile.isRoomLit() ? "Room" : "Show";
        fontRenderer.drawString("Now: " + now, xSize - 20 - fontRenderer.getStringWidth("Now: " + now), 21, 0x404040);
    }

    /** A bearing as degrees with the nearest compass point, e.g. "45 NE". */
    private static String compass(int pan) {
        String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return pan + " " + points[Math.floorMod(Math.round(pan / 45F), 8)];
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        address.drawTextBox();
        renderHoveredToolTip(mouseX, mouseY);
    }
}
