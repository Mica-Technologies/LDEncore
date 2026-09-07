/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenArtNetInterface.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 GuiContainer, GuiTextField and GlStateManager; the universe
 * field only accepts digits so Save cannot throw on unparsable text; the screen also states
 * who owns the interface, that the address is the one on this player's own machine, whether a
 * socket is actually listening on it and how long ago Art-Net last arrived. Upstream showed
 * none of that, and "is anything reaching the game at all" is the first thing anyone wiring a
 * console up needs to know.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.artnet.ArtNetManager;
import dev.theatricalmod.theatrical.client.TheatricalClient;
import dev.theatricalmod.theatrical.client.gui.container.ContainerArtNetInterface;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.UpdateArtNetInterfacePacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class ScreenArtNetInterface extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/blank.png");

    private static final int BUTTON_SAVE = 0;

    private final ContainerArtNetInterface container;
    private GuiTextField universeField;
    private GuiTextField ipField;

    public ScreenArtNetInterface(ContainerArtNetInterface container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 126;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.universeField = new GuiTextField(0, this.fontRenderer, guiLeft + 62, guiTop + 25, 50, 10);
        this.universeField.setEnableBackgroundDrawing(true);
        this.universeField.setTextColor(-1);
        this.universeField.setDisabledTextColour(-1);
        this.universeField.setMaxStringLength(5);
        this.universeField.setValidator(text -> text != null && text.chars().allMatch(Character::isDigit));
        this.universeField.setText(container.blockEntity == null ? "0" : Integer.toString(container.blockEntity.getUniverse()));
        this.universeField.setFocused(true);

        this.ipField = new GuiTextField(1, this.fontRenderer, guiLeft + 40, guiTop + 50, 100, 20);
        this.ipField.setEnableBackgroundDrawing(true);
        this.ipField.setTextColor(-1);
        this.ipField.setDisabledTextColour(-1);
        this.ipField.setMaxStringLength(64);
        this.ipField.setText(container.blockEntity == null ? "127.0.0.1" : container.blockEntity.getIp());

        this.buttonList.add(new GuiButton(BUTTON_SAVE, guiLeft + 40, guiTop + 90, 100, 20, "Save"));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.universeField.updateCursorCounter();
        this.ipField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_SAVE) {
            if (container.blockEntity == null) {
                return;
            }
            String text = this.universeField.getText();
            int universe = text.isEmpty() ? 0 : Integer.parseInt(text);
            if (universe > 32767) {
                universe = 32767;
            }
            TheatricalNetworkHandler.MAIN.sendToServer(
                    new UpdateArtNetInterfacePacket(container.blockEntity.getPos(), universe, ipField.getText()));
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.universeField.textboxKeyTyped(typedChar, keyCode) || this.ipField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.universeField.mouseClicked(mouseX, mouseY, mouseButton);
        this.ipField.mouseClicked(mouseX, mouseY, mouseButton);
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
        drawCentred("ArtNet Interface", 6, 0x404040);
        drawCentred("DMX Universe", 16, 0x404040);
        drawCentred("ArtNet IP", 40, 0x404040);
        // The support question upstream fielded over and over: this address is on the
        // player's own machine, not the server's, and only the owner may feed the interface.
        boolean owned = container.blockEntity != null && container.blockEntity.getPlayer() != null
                && container.blockEntity.getPlayer().equals(mc.player.getUniqueID());
        String note = owned ? "Your machine's address" : TextFormatting.RED + "Owned by another player";
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.7F, 0.7F, 1F);
        drawSmallCentred(note, 74);
        if (owned) {
            drawSmallCentred(statusLine(), 84);
        }
        GlStateManager.popMatrix();
    }

    /** Whether a socket is listening, and when a universe last arrived. */
    private String statusLine() {
        String ip = ipField.getText();
        if (ArtNetManager.isAllInterfaces(ip)) {
            ip = "0.0.0.0";
        }
        if (!TheatricalMod.getArtNetManager().isListening(ip)) {
            return TextFormatting.RED + "Not listening yet";
        }
        long age = TheatricalClient.millisSinceArtNetData(container.blockEntity == null
                ? null : container.blockEntity.getPos());
        if (age < 0) {
            return TextFormatting.GOLD + "Listening, no data yet";
        }
        if (age < 3000) {
            return TextFormatting.DARK_GREEN + "Receiving Art-Net";
        }
        return TextFormatting.GOLD + ("Last data " + (age / 1000L) + "s ago");
    }

    /** Draws inside the 0.7 scale the caller has already applied. */
    private void drawSmallCentred(String text, int y) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(text);
        int width = plain == null ? 0 : fontRenderer.getStringWidth(plain);
        fontRenderer.drawString(text, (int) ((xSize / 2F - width / 2F) / 0.7F), (int) (y / 0.7F), 0x404040);
    }

    private void drawCentred(String text, int y, int colour) {
        fontRenderer.drawString(text, xSize / 2 - fontRenderer.getStringWidth(text) / 2, y, colour);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.universeField.drawTextBox();
        this.ipField.drawTextBox();
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
