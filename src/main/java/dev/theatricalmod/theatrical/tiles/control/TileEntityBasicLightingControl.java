/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/control/TileEntityBasicLightingControl.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - 1.12 NBT, capability and lifecycle plumbing; the client sync goes through
 *     TheatricalNetworkHandler;
 *   - when the desk is removed, the fixtures it fed are blacked out rather than left on its
 *     last look;
 *   - its universe is pushed and sent only when it changes, with a once-a-second keep-alive,
 *     instead of every tick;
 *   - autonomous playback, all fork additions: a cue can hold for a set time and then advance
 *     by itself, the stack can loop, the desk can start playing on its own when it loads, a
 *     playing show survives a restart, a cue can be triggered by the time of day, and effects
 *     (waveforms written onto any of the 512 channels) run on the desk and per cue;
 *   - a redstone pulse is always Go, entering run mode first if needed; upstream's pulse in
 *     program mode recorded a cue;
 *   - fades are interpolated, not stepped by a byte-sized per-tick delta: upstream's delta
 *     overflowed on a short fade across a large change, and a fade could end a step or two short
 *     of its target, which a looping show would compound.
 */
package dev.theatricalmod.theatrical.tiles.control;

import dev.theatricalmod.theatrical.TheatricalMod;
import dev.theatricalmod.theatrical.api.CableType;
import dev.theatricalmod.theatrical.api.IAcceptsCable;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.DMXProvider;
import dev.theatricalmod.theatrical.api.capabilities.dmx.provider.IDMXProvider;
import dev.theatricalmod.theatrical.api.dmx.DMXUniverse;
import dev.theatricalmod.theatrical.network.TheatricalNetworkHandler;
import dev.theatricalmod.theatrical.tiles.TileEntityTheatricalBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A twelve-fader desk with a grand master, a cue stack with fades, holds, a loop, time-of-day
 * triggers and effects, feeding its own DMX universe.
 */
public class TileEntityBasicLightingControl extends TileEntityTheatricalBase implements ITickable, IAcceptsCable {

    public static final int FADERS = 12;
    /** Longest fade or hold accepted from a player or a command, in ticks (an hour). */
    public static final int MAX_TIMING_TICKS = 72000;
    /** Ticks between unchanged re-sends of the universe. */
    private static final int KEEPALIVE_TICKS = 20;

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DMX};
    }

    /** One stored cue: a fader look plus how it arrives, how long it stays and what runs with it. */
    public static class StoredCue {
        private byte[] faders;
        private int fadeInTicks;
        private int fadeOutTicks;
        /** Ticks to hold after the fade in before advancing by itself; 0 waits for Go. */
        private int holdTicks;
        /** World time of day (0-23999) at which this cue plays by itself, or -1. */
        private int triggerTime = -1;
        private final List<DeskEffect> effects = new ArrayList<>();

        public StoredCue() {
        }

        public StoredCue(byte[] faders, int fadeInTicks, int fadeOutTicks, int holdTicks) {
            this.faders = faders;
            this.fadeInTicks = fadeInTicks;
            this.fadeOutTicks = fadeOutTicks;
            this.holdTicks = holdTicks;
        }

        public NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByteArray("faders", faders);
            tag.setInteger("fadeIn", fadeInTicks);
            tag.setInteger("fadeOut", fadeOutTicks);
            tag.setInteger("hold", holdTicks);
            tag.setInteger("time", triggerTime);
            tag.setTag("effects", writeEffects(effects));
            return tag;
        }

        public StoredCue fromNBT(NBTTagCompound nbt) {
            byte[] saved = nbt.getByteArray("faders");
            this.faders = saved.length == FADERS ? saved : Arrays.copyOf(saved, FADERS);
            this.fadeInTicks = nbt.getInteger("fadeIn");
            this.fadeOutTicks = nbt.getInteger("fadeOut");
            this.holdTicks = nbt.getInteger("hold");
            this.triggerTime = nbt.hasKey("time") ? nbt.getInteger("time") : -1;
            readEffects(nbt.getTagList("effects", Constants.NBT.TAG_COMPOUND), effects);
            return this;
        }

        public byte[] getFaders() {
            return faders;
        }

        public int getFadeInTicks() {
            return fadeInTicks;
        }

        public int getFadeOutTicks() {
            return fadeOutTicks;
        }

        public int getHoldTicks() {
            return holdTicks;
        }

        public int getTriggerTime() {
            return triggerTime;
        }

        public List<DeskEffect> getEffects() {
            return effects;
        }

        public void setFadeInTicks(int ticks) {
            this.fadeInTicks = MathHelper.clamp(ticks, 0, MAX_TIMING_TICKS);
        }

        public void setFadeOutTicks(int ticks) {
            this.fadeOutTicks = MathHelper.clamp(ticks, 0, MAX_TIMING_TICKS);
        }

        public void setHoldTicks(int ticks) {
            this.holdTicks = MathHelper.clamp(ticks, 0, MAX_TIMING_TICKS);
        }

        public void setTriggerTime(int time) {
            this.triggerTime = time < 0 ? -1 : time % 24000;
        }
    }

    private int ticks = 0;
    private byte[] faders = new byte[FADERS];
    private int currentStep = 0;
    private HashMap<Integer, StoredCue> storedSteps = new HashMap<>();
    private StoredCue activeCue;
    /** The key of the cue now on stage, or -1. */
    private int activeKey = -1;

    private boolean isRunMode = false;
    /** The values the screen records into the next cue. */
    private int fadeInTicks = 0;
    private int fadeOutTicks = 0;
    private int holdTicks = 0;

    // A cue change fades the previous cue out to black (if it has a fade out), then fades the
    // new one in from wherever the faders are; either part may be instant.
    private float[] fadeFrom;
    private byte[] fadeTarget;
    private int fadeOutTotal;
    private int fadeOutLeft;
    private int fadeInTotal;
    private int fadeInLeft;

    private byte grandMaster = -1;

    // Autonomous playback.
    private boolean loop = false;
    private boolean autoStart = false;
    /** Whether the stack is advancing by itself. Saved, so a running show resumes on load. */
    private boolean playing = false;
    /** Ticks until the active cue advances by itself; -1 when it waits for Go. */
    private int holdLeft = -1;
    /** The cue this run started from; a loop goes back to it rather than to the first cue. */
    private int loopStartKey = -1;
    /** Effects that run whenever the desk does, regardless of the cue. */
    private final List<DeskEffect> deskEffects = new ArrayList<>();
    /** Set once auto-start has been considered since this tile loaded. */
    private boolean autoStartChecked = false;
    private long lastDayTime = -1;

    private int ticksSinceSend = KEEPALIVE_TICKS;
    private byte[] lastSent = null;

    private final IDMXProvider idmxProvider = new DMXProvider(new DMXUniverse());

    public float convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    public byte[] getFaders() {
        return this.faders;
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public void readNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("faders")) {
            byte[] saved = nbt.getByteArray("faders");
            faders = saved.length == FADERS ? saved : Arrays.copyOf(saved, FADERS);
        }
        if (nbt.hasKey("storedSteps")) {
            storedSteps = new HashMap<>();
            NBTTagCompound steps = nbt.getCompoundTag("storedSteps");
            for (String key : steps.getKeySet()) {
                try {
                    storedSteps.put(Integer.parseInt(key), new StoredCue().fromNBT(steps.getCompoundTag(key)));
                } catch (NumberFormatException ignored) {
                    // A hand-edited tag with a non-numeric key: skip that entry, keep the rest.
                }
            }
        }
        if (nbt.hasKey("currentStep")) {
            currentStep = nbt.getInteger("currentStep");
        }
        if (nbt.hasKey("grandMaster")) {
            grandMaster = nbt.getByte("grandMaster");
        }
        if (nbt.hasKey("isRunMode")) {
            isRunMode = nbt.getBoolean("isRunMode");
        }
        if (nbt.hasKey("fadeInTicks")) {
            fadeInTicks = nbt.getInteger("fadeInTicks");
        }
        if (nbt.hasKey("fadeOutTicks")) {
            fadeOutTicks = nbt.getInteger("fadeOutTicks");
        }
        holdTicks = nbt.getInteger("holdTicks");
        loop = nbt.getBoolean("loop");
        autoStart = nbt.getBoolean("autoStart");
        playing = nbt.getBoolean("playing");
        holdLeft = nbt.hasKey("holdLeft") ? nbt.getInteger("holdLeft") : -1;
        loopStartKey = nbt.hasKey("loopStart") ? nbt.getInteger("loopStart") : -1;
        activeKey = nbt.hasKey("activeCue") ? nbt.getInteger("activeCue") : -1;
        activeCue = storedSteps.get(activeKey);
        readEffects(nbt.getTagList("effects", Constants.NBT.TAG_COMPOUND), deskEffects);
    }

    @Override
    public NBTTagCompound getNBT(@Nullable NBTTagCompound nbt) {
        nbt = super.getNBT(nbt);
        nbt.setByteArray("faders", faders);
        NBTTagCompound steps = new NBTTagCompound();
        for (Integer key : storedSteps.keySet()) {
            steps.setTag(Integer.toString(key), storedSteps.get(key).toNBT());
        }
        nbt.setTag("storedSteps", steps);
        nbt.setInteger("currentStep", currentStep);
        nbt.setByte("grandMaster", grandMaster);
        nbt.setBoolean("isRunMode", isRunMode);
        nbt.setInteger("fadeInTicks", fadeInTicks);
        nbt.setInteger("fadeOutTicks", fadeOutTicks);
        nbt.setInteger("holdTicks", holdTicks);
        nbt.setBoolean("loop", loop);
        nbt.setBoolean("autoStart", autoStart);
        nbt.setBoolean("playing", playing);
        nbt.setInteger("holdLeft", holdLeft);
        nbt.setInteger("loopStart", loopStartKey);
        nbt.setInteger("activeCue", activeKey);
        nbt.setTag("effects", writeEffects(deskEffects));
        return nbt;
    }

    private static NBTTagList writeEffects(List<DeskEffect> effects) {
        NBTTagList list = new NBTTagList();
        for (DeskEffect effect : effects) {
            list.appendTag(effect.toNBT());
        }
        return list;
    }

    private static void readEffects(NBTTagList list, List<DeskEffect> into) {
        into.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            DeskEffect effect = DeskEffect.fromNBT(list.getCompoundTagAt(i));
            if (effect != null) {
                into.add(effect);
            }
        }
    }

    // ---------------------------------------------------------------- faders

    public void setFaders(byte[] faders) {
        this.faders = Arrays.copyOf(faders, FADERS);
    }

    public void setFader(int fader, int value) {
        if (fader != -1) {
            if (fader >= 0 && fader < FADERS) {
                this.faders[fader] = (byte) value;
            }
        } else {
            this.grandMaster = (byte) value;
        }
    }

    public byte getGrandMaster() {
        return grandMaster;
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        if (!autoStartChecked) {
            autoStartChecked = true;
            if (autoStart && !playing && !storedSteps.isEmpty()) {
                play(null);
            }
        }
        ticks++;
        if (ticks < 1) {
            return;
        }
        ticks = 0;
        tickTriggers();
        tickFades();
        tickHold();

        byte[] dmx = new byte[DMXUniverse.CHANNELS];
        for (int i = 0; i < faders.length; i++) {
            dmx[i] = (byte) (convertByteToInt(faders[i]) * (convertByteToInt(grandMaster) / 255F));
        }
        long now = world.getTotalWorldTime();
        for (DeskEffect effect : deskEffects) {
            effect.apply(dmx, now);
        }
        if (isRunMode && activeCue != null) {
            for (DeskEffect effect : activeCue.getEffects()) {
                effect.apply(dmx, now);
            }
        }

        // Only push and send when the output changed, plus a keep-alive once a second. The
        // keep-alive is what lets a receiver that was just wired in, or a client that just came
        // into range, pick the universe up, and it drives the client's own re-walk of the cable
        // run. Upstream re-pushed and re-sent the whole universe every tick.
        ticksSinceSend++;
        boolean changed = !Arrays.equals(dmx, lastSent);
        if (changed || ticksSinceSend >= KEEPALIVE_TICKS) {
            ticksSinceSend = 0;
            lastSent = dmx;
            this.idmxProvider.getUniverse(world).setDmxChannels(dmx);
            TheatricalNetworkHandler.sendProviderUniverse(world, pos, dmx);
            sendDMXSignal();
            if (changed) {
                // Fades and effects move the output without a player touching anything.
                markDirty();
            }
        }
    }

    /** Plays any cue whose time of day has just come round. */
    private void tickTriggers() {
        long dayTime = Math.floorMod(world.getWorldTime(), 24000L);
        long previous = lastDayTime;
        lastDayTime = dayTime;
        if (previous < 0 || previous == dayTime) {
            return;
        }
        for (int key : new TreeMap<>(storedSteps).keySet()) {
            int time = storedSteps.get(key).getTriggerTime();
            if (time < 0) {
                continue;
            }
            boolean crossed = previous < dayTime ? (time > previous && time <= dayTime)
                    : (time > previous || time <= dayTime);
            if (crossed) {
                play(key);
                return;
            }
        }
    }

    private void tickFades() {
        if (fadeOutLeft > 0) {
            fadeOutLeft--;
            float t = 1F - (float) fadeOutLeft / fadeOutTotal;
            for (int i = 0; i < FADERS; i++) {
                faders[i] = (byte) Math.round(fadeFrom[i] * (1F - t));
            }
            if (fadeOutLeft == 0) {
                // The fade in starts from black.
                fadeFrom = new float[FADERS];
                if (fadeInTotal == 0 && fadeTarget != null) {
                    setFaders(fadeTarget);
                }
            }
            return;
        }
        if (fadeInLeft > 0) {
            fadeInLeft--;
            float t = 1F - (float) fadeInLeft / fadeInTotal;
            for (int i = 0; i < FADERS; i++) {
                int target = Byte.toUnsignedInt(fadeTarget[i]);
                faders[i] = (byte) Math.round(fadeFrom[i] + (target - fadeFrom[i]) * t);
            }
            if (fadeInLeft == 0) {
                setFaders(fadeTarget);
            }
        }
    }

    /** Advances a playing stack once the active cue has been up for its fade in plus its hold. */
    private void tickHold() {
        if (!playing || holdLeft < 0) {
            return;
        }
        if (holdLeft > 0) {
            holdLeft--;
            return;
        }
        holdLeft = -1;
        Integer next = nextKeyAfter(activeKey);
        if (next == null) {
            if (!loop) {
                playing = false;
                sync();
                return;
            }
            // Back to where this run started, so a stack started at its third cue by a dusk
            // trigger loops cues three onward and never wanders back into the day look.
            next = storedSteps.containsKey(loopStartKey) ? Integer.valueOf(loopStartKey) : firstKey();
        }
        if (next != null) {
            recall(next);
            sync();
        }
    }

    // ---------------------------------------------------------------- cue stack

    public int getCurrentStep() {
        return currentStep;
    }

    public boolean isRunMode() {
        return isRunMode;
    }

    public void toggleMode() {
        this.isRunMode = !this.isRunMode;
        if (!isRunMode) {
            playing = false;
            holdLeft = -1;
        }
    }

    /** The screen's Go button: plays the next cue in run mode, records one in program mode. */
    public void clickButton() {
        if (isRunMode()) {
            go();
        } else {
            this.storeCurrentFaders();
        }
    }

    /** A redstone pulse: always Go, entering run mode first so a pulse never records a cue. */
    public void redstonePulse() {
        go();
        sync();
    }

    /** Plays the next cue and lets the stack advance by itself from there. */
    public void go() {
        if (storedSteps.isEmpty()) {
            return;
        }
        isRunMode = true;
        Integer key = storedSteps.containsKey(currentStep) ? currentStep : firstKeyFrom(currentStep);
        if (key == null) {
            key = firstKey();
        }
        if (key != null) {
            if (!playing) {
                loopStartKey = key;
            }
            playing = true;
            recall(key);
        }
    }

    /** Goes back one cue and plays it. */
    public void back() {
        if (storedSteps.isEmpty()) {
            return;
        }
        isRunMode = true;
        Integer previous = activeKey < 0 ? firstKey() : previousKeyBefore(activeKey);
        if (previous == null) {
            previous = lastKey();
        }
        if (previous != null) {
            recall(previous);
        }
    }

    /** Starts playing from a cue (or the first) and advances by itself, looping if set. */
    public void play(@Nullable Integer cue) {
        if (storedSteps.isEmpty()) {
            return;
        }
        Integer key = cue != null && storedSteps.containsKey(cue) ? cue : firstKey();
        if (key == null) {
            return;
        }
        isRunMode = true;
        playing = true;
        loopStartKey = key;
        recall(key);
        sync();
    }

    /** Stops the stack advancing; the current look stays up. */
    public void stop() {
        playing = false;
        holdLeft = -1;
        sync();
    }

    public void moveForward() {
        if (isRunMode) {
            this.currentStep = this.getNextStep();
        } else {
            this.currentStep++;
            if (this.storedSteps.containsKey(this.currentStep)) {
                setFaders(storedSteps.get(this.currentStep).getFaders());
            }
        }
    }

    public void moveBack() {
        if (this.isRunMode) {
            this.currentStep = this.getPreviousStep();
        } else {
            if (this.currentStep - 1 < 0) {
                return;
            }
            this.currentStep--;
            if (this.storedSteps.containsKey(this.currentStep)) {
                setFaders(storedSteps.get(this.currentStep).getFaders());
            }
        }
    }

    public HashMap<Integer, StoredCue> getStoredSteps() {
        return storedSteps;
    }

    /** Puts a cue on stage with its fades, and starts its hold if the stack is playing. */
    private void recall(int key) {
        StoredCue cue = storedSteps.get(key);
        if (cue == null) {
            return;
        }
        StoredCue previous = activeCue;
        fadeTarget = Arrays.copyOf(cue.getFaders(), FADERS);
        fadeFrom = new float[FADERS];
        for (int i = 0; i < FADERS; i++) {
            fadeFrom[i] = Byte.toUnsignedInt(faders[i]);
        }
        fadeOutTotal = previous != null ? previous.getFadeOutTicks() : 0;
        fadeOutLeft = fadeOutTotal;
        fadeInTotal = cue.getFadeInTicks();
        fadeInLeft = fadeInTotal;
        if (fadeOutTotal == 0 && fadeInTotal == 0) {
            setFaders(fadeTarget);
        }
        activeCue = cue;
        activeKey = key;
        Integer next = nextKeyAfter(key);
        if (next == null) {
            // After the last cue, the next one is where a loop would go back to.
            next = storedSteps.containsKey(loopStartKey) ? Integer.valueOf(loopStartKey) : firstKey();
        }
        currentStep = next != null ? next : key;
        holdLeft = playing && cue.getHoldTicks() > 0 ? fadeOutTotal + fadeInTotal + cue.getHoldTicks() : -1;
        markDirty();
    }

    @Nullable
    private Integer firstKey() {
        return storedSteps.keySet().stream().min(Integer::compareTo).orElse(null);
    }

    @Nullable
    private Integer lastKey() {
        return storedSteps.keySet().stream().max(Integer::compareTo).orElse(null);
    }

    @Nullable
    private Integer firstKeyFrom(int step) {
        return storedSteps.keySet().stream().filter(k -> k >= step).min(Integer::compareTo).orElse(null);
    }

    @Nullable
    private Integer nextKeyAfter(int key) {
        return storedSteps.keySet().stream().filter(k -> k > key).min(Integer::compareTo).orElse(null);
    }

    @Nullable
    private Integer previousKeyBefore(int key) {
        return storedSteps.keySet().stream().filter(k -> k < key).max(Integer::compareTo).orElse(null);
    }

    private Integer getNextStep() {
        if (this.storedSteps.size() > 0) {
            Optional<Integer> nextSteps = this.storedSteps.keySet().stream().filter(integer -> integer > this.currentStep).min(Integer::compareTo);
            return nextSteps.orElseGet(() -> firstKey() != null ? firstKey() : currentStep);
        }
        return currentStep;
    }

    private Integer getPreviousStep() {
        if (this.storedSteps.size() > 0) {
            Optional<Integer> previous = this.storedSteps.keySet().stream().filter(integer -> integer < this.currentStep).max(Comparator.naturalOrder());
            return previous.orElseGet(() -> this.storedSteps.keySet().stream().max(Comparator.naturalOrder()).orElse(currentStep));
        }
        return currentStep;
    }

    private void storeCurrentFaders() {
        record(this.currentStep);
        this.currentStep++;
    }

    /** Stores the current faders, with the screen's fade and hold times, as a cue. */
    public void record(int key) {
        StoredCue existing = storedSteps.get(key);
        StoredCue cue = new StoredCue(Arrays.copyOf(faders, faders.length), this.fadeInTicks, this.fadeOutTicks, this.holdTicks);
        if (existing != null) {
            // Re-recording a cue keeps what the screen cannot set: its effects and trigger time.
            cue.getEffects().addAll(existing.getEffects());
            cue.setTriggerTime(existing.getTriggerTime());
        }
        storedSteps.put(key, cue);
        markDirty();
    }

    // ---------------------------------------------------------------- options

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getActiveCueKey() {
        return activeKey;
    }

    public List<DeskEffect> getDeskEffects() {
        return deskEffects;
    }

    public int getFadeInTicks() {
        return fadeInTicks;
    }

    public void setFadeInTicks(int fadeInTicks) {
        this.fadeInTicks = fadeInTicks;
    }

    public int getFadeOutTicks() {
        return fadeOutTicks;
    }

    public void setFadeOutTicks(int fadeOutTicks) {
        this.fadeOutTicks = fadeOutTicks;
    }

    public int getHoldTicks() {
        return holdTicks;
    }

    public void setHoldTicks(int holdTicks) {
        this.holdTicks = MathHelper.clamp(holdTicks, 0, MAX_TIMING_TICKS);
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void invalidate() {
        // The receivers it fed would otherwise hold their last look for ever.
        if (world != null) {
            idmxProvider.blackoutDevices(world);
        }
        TheatricalMod.refreshDmxNetwork(world);
        super.invalidate();
    }

    @Override
    public void onLoad() {
        TheatricalMod.refreshDmxNetwork(world);
    }

    public void sendDMXSignal() {
        idmxProvider.updateDevices(world, pos);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DMXProvider.CAP || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DMXProvider.CAP) {
            return DMXProvider.CAP.cast(idmxProvider);
        }
        return super.getCapability(capability, facing);
    }
}
