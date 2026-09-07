/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * client/gui/screen/ScreenIntelligentFixture.java (Theatrical Team, Apache License 2.0); the
 * upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: the screen body lives in ScreenDMXAddressBase, which upstream
 * duplicated between this screen and the DMX-redstone interface's.
 */
package dev.theatricalmod.theatrical.client.gui.screen;

import dev.theatricalmod.theatrical.client.gui.container.ContainerIntelligentFixture;

public class ScreenIntelligentFixture extends ScreenDMXAddressBase {

    public ScreenIntelligentFixture(ContainerIntelligentFixture container) {
        super(container, container.blockEntity, container.blockEntity == null ? null : container.blockEntity.getPos());
    }

    @Override
    protected String getScreenTitle() {
        return "Intelligent Fixture";
    }
}
