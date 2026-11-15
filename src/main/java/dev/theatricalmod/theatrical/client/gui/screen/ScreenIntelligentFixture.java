/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenIntelligentFixture.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the screen body lives in ScreenDMXAddressBase, which upstream
 * duplicated between this screen and the DMX-redstone interface's. Fork addition: a Mode button
 * (Show / Room / Auto) for the head's Room Light.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.client.gui.container.ContainerIntelligentFixture;
import dev.theatricalmod.theatrical.network.SetRoomModePacket;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.tiles.lights.TileEntityIntelligentFixture;
import net.minecraft.client.gui.GuiButton;

import java.io.IOException;

public class ScreenIntelligentFixture extends ScreenDMXAddressBase {

    private static final int BUTTON_MODE = 10;

    private final ContainerIntelligentFixture container;
    private GuiButton modeButton;

    public ScreenIntelligentFixture(ContainerIntelligentFixture container) {
        super(container, container.blockEntity, container.blockEntity == null ? null : container.blockEntity.getPos());
        this.container = container;
    }

    @Override
    protected String getScreenTitle() {
        return "Intelligent Fixture";
    }

    @Override
    public void initGui() {
        super.initGui();
        modeButton = new GuiButton(BUTTON_MODE, guiLeft + 40, guiTop + 72, 100, 16, "");
        buttonList.add(modeButton);
        updateModeLabel();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Follows the tile, so the label changes once the server has applied a click.
        updateModeLabel();
    }

    private void updateModeLabel() {
        TileEntityIntelligentFixture tile = container.blockEntity;
        if (modeButton == null || tile == null) {
            return;
        }
        String name;
        switch (tile.getRoomMode()) {
            case ROOM:
                name = "Room light";
                break;
            case AUTO:
                name = "Auto";
                break;
            case SHOW:
            default:
                name = "Show (DMX)";
                break;
        }
        modeButton.displayString = "Mode: " + name;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        TileEntityIntelligentFixture tile = container.blockEntity;
        if (button.id == BUTTON_MODE && tile != null) {
            TheatricalNetworkHandler.MAIN.sendToServer(new SetRoomModePacket(tile.getPos(), tile.getRoomMode().next()));
            return;
        }
        super.actionPerformed(button);
    }
}
