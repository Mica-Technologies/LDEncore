/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.tiles.control;

import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Locale;

/**
 * One running effect on a lighting desk: a waveform written onto a set of DMX channels.
 *
 * Each listed channel follows the same {@link Shape} between {@link #min} and {@link #max} over
 * {@link #periodTicks}, with each channel after the first shifted along the cycle by
 * {@link #spread}. A pan sweep is one channel on a sine; a rainbow is a head's red, green and blue
 * on a sine with a third of a cycle between them; a chase is several heads' intensity channels on
 * a square wave with an even spread.
 *
 * The phase comes from the world's total time, so every desk running the same effect stays in
 * step, and a show that resumes after a restart is in the same place it would have been.
 */
public final class DeskEffect {

    /** Waveforms, each giving a level from 0 to 1 for a phase from 0 to 1. */
    public enum Shape {
        SINE, TRIANGLE, SAW, SQUARE, RANDOM, STROBE;

        public static Shape parse(String name) {
            return Shape.valueOf(name.trim().toUpperCase(Locale.ROOT));
        }
    }

    private final Shape shape;
    private final int periodTicks;
    private final int min;
    private final int max;
    private final float spread;
    private final int[] channels;

    public DeskEffect(Shape shape, int periodTicks, int min, int max, float spread, int[] channels) {
        this.shape = shape;
        this.periodTicks = Math.max(2, periodTicks);
        this.min = MathHelper.clamp(min, 0, 255);
        this.max = MathHelper.clamp(max, 0, 255);
        this.spread = spread;
        int[] valid = Arrays.stream(channels).filter(c -> c >= 0 && c < DMXUniverse.CHANNELS).toArray();
        this.channels = valid;
    }

    /** Writes this effect's current value onto each of its channels. */
    public void apply(byte[] dmx, long worldTime) {
        for (int i = 0; i < channels.length; i++) {
            dmx[channels[i]] = (byte) valueAt(worldTime, i);
        }
    }

    /** The DMX value (0-255) for the i-th listed channel at a given world time. */
    public int valueAt(long worldTime, int index) {
        double cycle = (double) Math.floorMod(worldTime, (long) periodTicks) / periodTicks;
        double phase = cycle + index * (double) spread;
        phase -= Math.floor(phase);
        double level;
        switch (shape) {
            case SINE:
                level = 0.5 - 0.5 * Math.cos(phase * Math.PI * 2);
                break;
            case TRIANGLE:
                level = phase < 0.5 ? phase * 2 : 2 - phase * 2;
                break;
            case SAW:
                level = phase;
                break;
            case SQUARE:
                level = phase < 0.5 ? 1 : 0;
                break;
            case STROBE:
                // On for one tick of each cycle (two for longer cycles), off for the rest.
                level = phase * periodTicks < Math.max(1, periodTicks / 10) ? 1 : 0;
                break;
            case RANDOM:
            default:
                // A new level each cycle, held for the cycle; different for each channel.
                long cycleNumber = Math.floorDiv(worldTime + (long) (index * spread * periodTicks), (long) periodTicks);
                level = hash(cycleNumber * 31 + index) / (double) 0xFFFF;
                break;
        }
        return (int) Math.round(min + (max - min) * level);
    }

    private static int hash(long value) {
        long h = value * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return (int) (h & 0xFFFF);
    }

    public Shape getShape() {
        return shape;
    }

    public int getPeriodTicks() {
        return periodTicks;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public float getSpread() {
        return spread;
    }

    public int[] getChannels() {
        return channels.clone();
    }

    /** A one-line description, as shown by /encore status. */
    public String describe() {
        return String.format(Locale.ROOT, "%s %d-%d every %d ticks, spread %.2f, channels %s",
                shape.name().toLowerCase(Locale.ROOT), min, max, periodTicks, spread, Arrays.toString(channels));
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("shape", shape.name().toLowerCase(Locale.ROOT));
        tag.setInteger("period", periodTicks);
        tag.setInteger("min", min);
        tag.setInteger("max", max);
        tag.setFloat("spread", spread);
        tag.setIntArray("channels", channels);
        return tag;
    }

    /** Reads an effect, or returns null for a tag that does not describe one. */
    public static DeskEffect fromNBT(NBTTagCompound tag) {
        try {
            return new DeskEffect(Shape.parse(tag.getString("shape")), tag.getInteger("period"),
                    tag.hasKey("min") ? tag.getInteger("min") : 0, tag.hasKey("max") ? tag.getInteger("max") : 255,
                    tag.getFloat("spread"), tag.getIntArray("channels"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
