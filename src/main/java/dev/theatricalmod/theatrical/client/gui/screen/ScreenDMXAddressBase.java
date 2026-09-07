/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 * Fork-authored file, Apache License 2.0.
 *
 * Upstream shipped ScreenIntelligentFixture and ScreenDMXRedstoneInterface as two files
 * that differ only in the label at the top: both are a DMX start address field over a Save
 * button. This is that screen once, and the two subclasses supply their titles.
 *
 * Behaviour differs from upstream in two places, both bugs there: pressing Save with the
 * field empty or holding something unparsable threw a NumberFormatException out of the
 * click handler, and an out-of-range address was silently dropped with no sign to the
 * player. Here the field only ever holds digits, an empty field means "no change", and the
 * address is clamped to the universe.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.DMXReceiver;
import dev.theatricalmod.theatrical.api.capabilities.dmx.receiver.IDMXReceiver;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.UpdateDMXAddressPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** A DMX start address field over a Save button, on the blank panel background. */
public abstract class ScreenDMXAddressBase extends GuiContainer {

    protected static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/blank.png");

    private static final int BUTTON_SAVE = 0;

    private final TileEntity tile;
    private final BlockPos pos;
    private GuiTextField dmxAddress;

    protected ScreenDMXAddressBase(Container container, TileEntity tile, BlockPos pos) {
        super(container);
        this.tile = tile;
        this.pos = pos;
        this.xSize = 176;
        this.ySize = 126;
    }

    /** Title drawn at the top of the panel. */
    protected abstract String getScreenTitle();

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.dmxAddress = new GuiTextField(0, this.fontRenderer, guiLeft + 40, guiTop + 50, 100, 20);
        this.dmxAddress.setEnableBackgroundDrawing(true);
        this.dmxAddress.setTextColor(-1);
        this.dmxAddress.setDisabledTextColour(-1);
        this.dmxAddress.setMaxStringLength(3);
        // Digits only, so nothing unparsable can ever reach the Save handler.
        this.dmxAddress.setValidator(text -> text != null && text.chars().allMatch(Character::isDigit));
        this.dmxAddress.setText(Integer.toString(currentAddress()));
        this.dmxAddress.setFocused(true);
        this.buttonList.add(new GuiButton(BUTTON_SAVE, guiLeft + 40, guiTop + 90, 100, 20, "Save"));
    }

    private int currentAddress() {
        if (tile != null && tile.hasCapability(DMXReceiver.CAP, null)) {
            IDMXReceiver receiver = tile.getCapability(DMXReceiver.CAP, null);
            if (receiver != null) {
                return receiver.getStartPoint();
            }
        }
        return 0;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.dmxAddress.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_SAVE) {
            String text = this.dmxAddress.getText();
            if (!text.isEmpty()) {
                int address = Math.min(Integer.parseInt(text), DMXUniverse.CHANNELS - 1);
                TheatricalNetworkHandler.MAIN.sendToServer(new UpdateDMXAddressPacket(pos, address));
            }
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.dmxAddress.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.dmxAddress.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = getScreenTitle();
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0x404040);
        String label = "DMX Start Address";
        fontRenderer.drawString(label, xSize / 2 - fontRenderer.getStringWidth(label) / 2, 16, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.dmxAddress.drawTextBox();
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
