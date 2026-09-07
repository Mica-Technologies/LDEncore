/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenDimmerRack.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 GuiContainer, GuiButton, GuiTextField, GlStateManager and Tessellator;
 *   - the button list is rebuilt only when what it shows actually changed, not every tick.
 *     Upstream rebuilt every widget on the screen twenty times a second;
 *   - closing the screen no longer throws when the address field is empty, and the address
 *     is clamped. Upstream parsed the field unconditionally in onClose, so closing with an
 *     empty box threw a NumberFormatException;
 *   - the page arrows do nothing rather than throwing when no distro is connected, and the
 *     dead distance-to-plug calculation upstream left in its foreground layer is gone.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.capabilities.socapex.ISocapexReceiver;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexPatch;
import dev.theatricalmod.theatrical.api.capabilities.socapex.SocapexProvider;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.client.gui.container.ContainerDimmerRack;
import dev.theatricalmod.theatrical.client.gui.widgets.ButtonPlug;
import dev.theatricalmod.theatrical.client.gui.widgets.ButtonSocket;
import dev.theatricalmod.theatrical.network.ChangeDimmerPatchPacket;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.network.UpdateDMXAddressPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScreenDimmerRack extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TheatricalMod.MOD_ID, "textures/gui/dimmer_rack.png");

    private static final int BUTTON_PREV_PAGE = 200;
    private static final int BUTTON_NEXT_PAGE = 201;
    /** Sockets take ids 0..11, plugs 100..107, so the ranges never overlap. */
    private static final int SOCKET_ID_BASE = 0;
    private static final int PLUG_ID_BASE = 100;

    private final ContainerDimmerRack container;
    private final List<ButtonSocket> sockets = new ArrayList<>();
    private final List<ButtonPlug> plugs = new ArrayList<>();

    private GuiTextField dmxStartField;
    private int currentPage = 0;
    private int activePlug = -1;

    /** What the widget list was last built from, so it is only rebuilt when it must be. */
    private String builtFrom = null;

    public ScreenDimmerRack(ContainerDimmerRack container) {
        super(container);
        this.container = container;
        this.xSize = 250;
        this.ySize = 131;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.add(new GuiButton(BUTTON_PREV_PAGE, guiLeft + 172, guiTop + 5, 15, 20, "<"));
        this.buttonList.add(new GuiButton(BUTTON_NEXT_PAGE, guiLeft + 225, guiTop + 5, 15, 20, ">"));

        this.dmxStartField = new GuiTextField(0, this.fontRenderer, guiLeft + 172, guiTop + 100, 50, 10);
        this.dmxStartField.setEnableBackgroundDrawing(true);
        this.dmxStartField.setTextColor(-1);
        this.dmxStartField.setDisabledTextColour(-1);
        this.dmxStartField.setMaxStringLength(3);
        this.dmxStartField.setValidator(text -> text != null && text.chars().allMatch(Character::isDigit));
        this.dmxStartField.setText(container.dimmerRack == null ? "0" : Integer.toString(container.dimmerRack.getDmxStart()));

        this.builtFrom = null;
        generateButtons();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (container.dimmerRack == null) {
            return;
        }
        String text = this.dmxStartField.getText();
        if (!text.isEmpty()) {
            int address = Math.min(Integer.parseInt(text), DMXUniverse.CHANNELS - 1);
            TheatricalNetworkHandler.MAIN.sendToServer(new UpdateDMXAddressPacket(container.dimmerRack.getPos(), address));
        }
    }

    /** A cheap description of everything the widget list depends on. */
    private String currentSignature(List<ISocapexReceiver> receivers) {
        StringBuilder sb = new StringBuilder();
        sb.append(currentPage).append('|').append(activePlug).append('|').append(receivers.size()).append('|');
        for (int channel = 0; channel < SocapexProvider.CHANNELS; channel++) {
            SocapexPatch[] patch = container.getPatch(channel);
            if (patch == null) {
                sb.append('-');
                continue;
            }
            for (SocapexPatch entry : patch) {
                sb.append(entry == null || entry.getReceiver() == null ? "_" : entry.getReceiver().toString() + entry.getReceiverSocket());
            }
        }
        return sb.toString();
    }

    private void generateButtons() {
        List<ISocapexReceiver> receivers = container.getDevices();
        String signature = currentSignature(receivers);
        if (signature.equals(builtFrom)) {
            return;
        }
        builtFrom = signature;

        this.buttonList.removeAll(sockets);
        this.buttonList.removeAll(plugs);
        this.sockets.clear();
        this.plugs.clear();

        int centreX = this.width / 2;
        int top = guiTop;
        for (int channel = 0; channel < SocapexProvider.CHANNELS; channel++) {
            SocapexPatch[] patch = container.getPatch(channel);
            int x = (centreX - 95) + 46 * (channel < 3 ? channel : channel - 3);
            int y = top + (channel < 3 ? 25 : 70);
            for (int socket = 0; socket < SocapexProvider.PATCHES_PER_CHANNEL; socket++) {
                int id = SOCKET_ID_BASE + channel * SocapexProvider.PATCHES_PER_CHANNEL + socket;
                ButtonSocket widget;
                SocapexPatch entry = patch == null || socket >= patch.length ? null : patch[socket];
                if (entry != null && entry.getReceiver() != null) {
                    String identifier = container.getIdentifier(entry.getReceiver());
                    identifier = identifier.isEmpty() ? "" : identifier.toUpperCase().substring(0, 1);
                    widget = new ButtonSocket(id, x, y + (20 * socket), channel, socket == 1, entry, identifier);
                } else {
                    widget = new ButtonSocket(id, x, y + (20 * socket), channel, socket == 1);
                }
                this.buttonList.add(widget);
                this.sockets.add(widget);
            }
        }

        if (!receivers.isEmpty()) {
            if (currentPage >= receivers.size()) {
                currentPage = 0;
            }
            int[] channels = container.getChannelsForReceiver(receivers.get(currentPage));
            for (int i = 0; i < channels.length; i++) {
                if (channels[i] == 1) {
                    continue;
                }
                int x = centreX + 45 + (20 * (i < 3 ? i : i - 3));
                int y = top + (i < 3 ? 45 : 65);
                ButtonPlug plug = new ButtonPlug(PLUG_ID_BASE + i, x, y, i + 1, "", activePlug == i);
                this.buttonList.add(plug);
                this.plugs.add(plug);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.dmxStartField.updateCursorCounter();
        generateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        List<ISocapexReceiver> receivers = container.getDevices();
        if (button.id == BUTTON_PREV_PAGE) {
            if (!receivers.isEmpty()) {
                currentPage = currentPage - 1 < 0 ? receivers.size() - 1 : currentPage - 1;
                activePlug = -1;
            }
            return;
        }
        if (button.id == BUTTON_NEXT_PAGE) {
            if (!receivers.isEmpty()) {
                currentPage = currentPage + 1 > receivers.size() - 1 ? 0 : currentPage + 1;
                activePlug = -1;
            }
            return;
        }
        if (button instanceof ButtonPlug) {
            ButtonPlug plug = (ButtonPlug) button;
            int index = plug.getPlugNumber() - 1;
            activePlug = activePlug == index ? -1 : index;
            return;
        }
        if (button instanceof ButtonSocket) {
            handleSocket((ButtonSocket) button, receivers);
        }
    }

    private void handleSocket(ButtonSocket socket, List<ISocapexReceiver> receivers) {
        if (container.dimmerRack == null) {
            return;
        }
        int channel = socket.getChannelNumber();
        int socketNumber = socket.isSecondSocket() ? 2 : 1;
        if (activePlug == -1) {
            if (socket.isPatched()) {
                TheatricalNetworkHandler.MAIN.sendToServer(
                        new ChangeDimmerPatchPacket(container.dimmerRack.getPos(), channel, socketNumber, new SocapexPatch()));
            }
            return;
        }
        if (!socket.isPatched() && !receivers.isEmpty() && currentPage < receivers.size()) {
            SocapexPatch patch = new SocapexPatch(receivers.get(currentPage).getReceiverPos(), activePlug);
            TheatricalNetworkHandler.MAIN.sendToServer(
                    new ChangeDimmerPatchPacket(container.dimmerRack.getPos(), channel, socketNumber, patch));
            activePlug = -1;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.dmxStartField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.dmxStartField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        drawScaledCustomSizeModalRect(guiLeft, guiTop, 0, 0, xSize, ySize, xSize, ySize, 512, 512);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = "Dimmer Rack";
        fontRenderer.drawString(name, 176 / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0x404040);
        fontRenderer.drawString("Plugs", 180 + fontRenderer.getStringWidth("Plugs") / 2, 6, 0x404040);
        for (int i = 0; i < SocapexProvider.CHANNELS; i++) {
            int x = 33 + 46 * (i < 3 ? i : i - 3);
            int y = i < 3 ? 15 : 62;
            fontRenderer.drawString(Integer.toString(i + 1), x, y, 0x000000);
        }
        List<ISocapexReceiver> receivers = container.getDevices();
        if (!receivers.isEmpty() && currentPage < receivers.size()) {
            String pageName = "Panel " + container.getIdentifier(receivers.get(currentPage).getReceiverPos());
            fontRenderer.drawString(pageName, 150 + fontRenderer.getStringWidth(pageName) / 2, 30, 0x404040);
        } else {
            fontRenderer.drawString("No distro connected", 150, 30, 0x404040);
        }
        if (activePlug != -1) {
            drawPatchLead(mouseX, mouseY);
        }
    }

    /** The lead that follows the cursor while a plug is picked up. */
    private void drawPatchLead(int mouseX, int mouseY) {
        int plugX = (this.width / 2) + 45 + (20 * (activePlug < 3 ? activePlug : activePlug - 3));
        int plugY = guiTop + (activePlug < 3 ? 45 : 65);
        final int colour = 0x13C90A;
        int red = (colour >> 16) & 255;
        int green = (colour >> 8) & 255;
        int blue = colour & 255;
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.glLineWidth(3);
        GlStateManager.color(1F, 1F, 1F, 1F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(plugX - guiLeft + 7, plugY - guiTop + 6, 0).color(red, green, blue, 255).endVertex();
        buffer.pos(mouseX - guiLeft, mouseY - guiTop, 0).color(red, green, blue, 255).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.dmxStartField.drawTextBox();
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
