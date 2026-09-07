/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * tiles/control/TileEntityBasicLightingControl.java (Theatrical Team, Apache License 2.0);
 * the upstream file targets Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM: 1.12 NBT, capability and lifecycle plumbing; the client sync goes
 * through TheatricalNetworkHandler; the GUI title and container come with the GUI phase.
 * The cue/fade logic is upstream's, unchanged.
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

/**
 * A twelve-fader desk with a grand master, cue storage and timed fades, feeding its own DMX
 * universe.
 */
public class TileEntityBasicLightingControl extends TileEntityTheatricalBase implements ITickable, IAcceptsCable {

    public static final int FADERS = 12;

    @Override
    public CableType[] getAcceptedCables(EnumFacing side) {
        return new CableType[]{CableType.DMX};
    }

    public static class StoredCue {
        private byte[] faders;
        private int fadeInTicks;
        private int fadeOutTicks;

        public StoredCue() {
        }

        public StoredCue(byte[] faders, int fadeInTicks, int fadeOutTicks) {
            this.faders = faders;
            this.fadeInTicks = fadeInTicks;
            this.fadeOutTicks = fadeOutTicks;
        }

        public NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByteArray("faders", faders);
            tag.setInteger("fadeIn", fadeInTicks);
            tag.setInteger("fadeOut", fadeOutTicks);
            return tag;
        }

        public StoredCue fromNBT(NBTTagCompound nbt) {
            this.faders = nbt.getByteArray("faders");
            this.fadeInTicks = nbt.getInteger("fadeIn");
            this.fadeOutTicks = nbt.getInteger("fadeOut");
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
    }

    private int ticks = 0;
    private byte[] faders = new byte[FADERS];
    private int currentStep = 0;
    private HashMap<Integer, StoredCue> storedSteps = new HashMap<>();
    private StoredCue activeCue;

    private boolean isRunMode = false;
    private int fadeInTicks = 0;
    private int fadeOutTicks = 0;

    private int fadeInTicksRemaining = 0;
    private int fadeOutTicksRemaining = 0;
    private byte[] perTickOut, perTickIn;
    private boolean isFadingOut = false;

    private byte grandMaster = -1;

    private final IDMXProvider idmxProvider = new DMXProvider(new DMXUniverse());

    public float convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    public byte[] getFaders() {
        return this.faders;
    }

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
                int stepNumber = Integer.parseInt(key);
                storedSteps.put(stepNumber, new StoredCue().fromNBT(steps.getCompoundTag(key)));
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
        return nbt;
    }

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

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        ticks++;
        if (ticks >= 1) {
            if (isFadingOut) {
                if (fadeOutTicksRemaining > 0) {
                    fadeOutTicksRemaining--;
                    this.doFadeTickOut();
                } else {
                    isFadingOut = false;
                }
            } else {
                if (fadeInTicksRemaining > 0) {
                    fadeInTicksRemaining--;
                    this.doFadeTickIn();
                }
            }
            ticks = 0;
            byte[] dmx = new byte[DMXUniverse.CHANNELS];
            for (int i = 0; i < faders.length; i++) {
                dmx[i] = (byte) (convertByteToInt(faders[i]) * (convertByteToInt(grandMaster) / 255F));
            }
            this.idmxProvider.getUniverse(world).setDmxChannels(dmx);
            TheatricalNetworkHandler.sendProviderUniverse(world, pos, dmx);
            sendDMXSignal();
            markDirty();
        }
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public byte getGrandMaster() {
        return grandMaster;
    }

    public boolean isRunMode() {
        return isRunMode;
    }

    public void toggleMode() {
        this.isRunMode = !this.isRunMode;
    }

    public void clickButton() {
        if (isRunMode()) {
            this.recallNextStep();
        } else {
            this.storeCurrentFaders();
        }
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

    private void doFadeTickIn() {
        for (int i = 0; i < faders.length; i++) {
            this.faders[i] = (byte) (faders[i] - perTickIn[i]);
        }
    }

    private void doFadeTickOut() {
        for (int i = 0; i < faders.length; i++) {
            this.faders[i] = (byte) (faders[i] - perTickOut[i]);
        }
    }

    private void recallNextStep() {
        if (this.storedSteps.size() < this.currentStep) {
            return;
        }
        StoredCue previousCue = activeCue;
        if (!this.storedSteps.containsKey(this.currentStep)) {
            return;
        }
        StoredCue storedCue = storedSteps.get(this.currentStep);
        if (previousCue != null) {
            if (previousCue.fadeOutTicks > 0) {
                this.isFadingOut = true;
                this.fadeOutTicksRemaining = previousCue.getFadeOutTicks();
                this.perTickOut = new byte[FADERS];
                for (int i = 0; i < faders.length; i++) {
                    this.perTickOut[i] = (byte) ((convertByteToInt(faders[i])) / fadeOutTicksRemaining);
                }
            }
        }
        if (storedCue.fadeInTicks > 0) {
            this.fadeInTicksRemaining = storedCue.getFadeInTicks();
            this.perTickIn = new byte[FADERS];
            for (int i = 0; i < faders.length; i++) {
                if (isFadingOut) {
                    this.perTickIn[i] = (byte) (-convertByteToInt(storedCue.getFaders()[i]) / fadeInTicksRemaining);
                } else {
                    this.perTickIn[i] = (byte) ((convertByteToInt(faders[i]) - convertByteToInt(storedCue.getFaders()[i])) / fadeInTicksRemaining);
                }
            }
        } else {
            setFaders(storedCue.getFaders());
        }
        activeCue = storedCue;
        this.currentStep = getNextStep();
        markDirty();
    }

    private Integer getFirst() {
        return this.storedSteps.keySet().stream().min(Integer::compareTo).orElse(currentStep);
    }

    private Integer getNextStep() {
        if (this.storedSteps.size() > 0) {
            Optional<Integer> nextSteps = this.storedSteps.keySet().stream().filter(integer -> integer > this.currentStep).min(Integer::compareTo);
            return nextSteps.orElseGet(this::getFirst);
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
        StoredCue storedCue = new StoredCue(Arrays.copyOf(faders, faders.length), this.fadeInTicks, this.fadeOutTicks);
        storedSteps.put(this.currentStep, storedCue);
        this.currentStep++;
    }

    @Override
    public void invalidate() {
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
}
