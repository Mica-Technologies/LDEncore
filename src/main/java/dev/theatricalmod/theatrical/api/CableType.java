/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * api/CableType.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - texture paths use 1.12's "blocks/" folder rather than 1.16's "block/";
 *   - IStringSerializable exposes getName() on 1.12 (getString() on 1.16);
 *   - the commented-out getItemForCable() stub was dropped; the cable items come back with
 *     the items package and can add a real implementation then.
 */
package dev.theatricalmod.theatrical.api;

import dev.theatricalmod.theatrical.TheatricalMod;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;

public enum CableType implements IStringSerializable {

    NONE(-1, null, "none"),
    DMX(0, new ResourceLocation(TheatricalMod.MOD_ID, "blocks/cables/cable"), "dmx"),
    DIMMED_POWER(1, new ResourceLocation(TheatricalMod.MOD_ID, "blocks/cables/power"), "power"),
    POWER(3, new ResourceLocation(TheatricalMod.MOD_ID, "blocks/cables/dimmed_power"), "dimmed_power"),
    SOCAPEX(2, new ResourceLocation(TheatricalMod.MOD_ID, "blocks/cables/socapex"), "socapex"),
    BUNDLED(99, new ResourceLocation(TheatricalMod.MOD_ID, "blocks/cables/bundled"), "bundled");

    private final int index;
    private final ResourceLocation texture;
    private final String name;

    CableType(int index, ResourceLocation location, String name) {
        this.index = index;
        this.texture = location;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public static CableType byIndex(int index) {
        for (CableType type : CableType.values()) {
            if (type.getIndex() == index) {
                return type;
            }
        }
        return NONE;
    }

    @Override
    public String getName() {
        return name;
    }
}
