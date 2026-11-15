/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.block.led;

import net.minecraft.util.math.AxisAlignedBB;

/**
 * Rotates boxes the way a blockstate variant's {@code "x"} and {@code "y"} rotate its model:
 * about the block's centre, x first, then y, where x = 90 turns an up-facing model to face north
 * (as vanilla's piston blockstate does) and y = 90 turns a north-facing one to face east.
 *
 * LED fixtures draw their body as a baked model rotated by their blockstate and their lens from
 * the tile renderer; both, and the selection box, take their rotation from the same block method,
 * so they cannot drift apart.
 */
public final class LedGeometry {

    private LedGeometry() {
    }

    /** A box in sixteenths, as model JSON writes it. */
    public static AxisAlignedBB px(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AxisAlignedBB(x0 / 16D, y0 / 16D, z0 / 16D, x1 / 16D, y1 / 16D, z1 / 16D);
    }

    public static AxisAlignedBB rotate(AxisAlignedBB box, int xRotation, int yRotation) {
        double[] a = rotate(box.minX, box.minY, box.minZ, xRotation, yRotation);
        double[] b = rotate(box.maxX, box.maxY, box.maxZ, xRotation, yRotation);
        // The constructor sorts each axis, so a corner swapped by the rotation is fine.
        return new AxisAlignedBB(a[0], a[1], a[2], b[0], b[1], b[2]);
    }

    private static double[] rotate(double px, double py, double pz, int xRotation, int yRotation) {
        double x = px - 0.5D;
        double y = py - 0.5D;
        double z = pz - 0.5D;
        for (int i = 0; i < Math.floorMod(xRotation / 90, 4); i++) {
            // About X: up -> north.
            double ny = z;
            double nz = -y;
            y = ny;
            z = nz;
        }
        for (int i = 0; i < Math.floorMod(yRotation / 90, 4); i++) {
            // About Y: north -> east.
            double nx = -z;
            double nz = x;
            x = nx;
            z = nz;
        }
        return new double[]{x + 0.5D, y + 0.5D, z + 0.5D};
    }

    /** The union of several boxes. */
    public static AxisAlignedBB union(AxisAlignedBB... boxes) {
        AxisAlignedBB out = boxes[0];
        for (int i = 1; i < boxes.length; i++) {
            out = out.union(boxes[i]);
        }
        return out;
    }
}
