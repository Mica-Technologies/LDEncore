/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Fork-authored file, Apache License 2.0.
 */
package dev.theatricalmod.theatrical.command;

import dev.theatricalmod.theatrical.tiles.control.DeskEffect;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl;
import dev.theatricalmod.theatrical.tiles.control.TileEntityBasicLightingControl.StoredCue;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * /encore: runs lighting desks without anyone at them, for admins, command blocks and tooling.
 *
 * Every subcommand names the desk by position. Channel numbers are DMX addresses counted from 0,
 * as everywhere else in the mod; a channel list is any mix of single numbers, ranges ("0-6") and
 * comma lists ("0,7,14").
 */
public class CommandEncore extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList("status", "play", "go", "back", "stop", "loop",
            "autostart", "master", "fader", "record", "cue", "effect");

    @Override
    public String getName() {
        return "encore";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/encore <status|play|go|back|stop|loop|autostart|master|fader|record|cue|effect> <x> <y> <z> ...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException(getUsage(sender));
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        BlockPos pos = parseBlockPos(sender, args, 1, false);
        TileEntityBasicLightingControl desk = findDesk(sender.getEntityWorld(), pos);
        String[] rest = Arrays.copyOfRange(args, 4, args.length);
        switch (sub) {
            case "status":
                for (String line : status(desk)) {
                    tell(sender, line);
                }
                return;
            case "play":
                desk.play(rest.length > 0 ? parseInt(rest[0], 0) : null);
                tell(sender, desk.isPlaying() ? "Playing from cue " + desk.getActiveCueKey() : "Nothing to play: the desk has no cues");
                return;
            case "go":
                desk.go();
                desk.sync();
                tell(sender, "Cue " + desk.getActiveCueKey());
                return;
            case "back":
                desk.back();
                desk.sync();
                tell(sender, "Cue " + desk.getActiveCueKey());
                return;
            case "stop":
                desk.stop();
                tell(sender, "Stopped; the current look stays up");
                return;
            case "loop":
                desk.setLoop(parseOnOff(rest));
                desk.sync();
                tell(sender, "Loop " + (desk.isLoop() ? "on" : "off"));
                return;
            case "autostart":
                desk.setAutoStart(parseOnOff(rest));
                desk.sync();
                tell(sender, "Auto-start " + (desk.isAutoStart() ? "on" : "off"));
                return;
            case "master":
                requireArgs(rest, 1, "/encore master <x> <y> <z> <0-255>");
                desk.setFader(-1, parseInt(rest[0], 0, 255));
                desk.sync();
                tell(sender, "Grand master " + rest[0]);
                return;
            case "fader":
                requireArgs(rest, 2, "/encore fader <x> <y> <z> <1-12> <0-255>");
                desk.setFader(parseInt(rest[0], 1, TileEntityBasicLightingControl.FADERS) - 1, parseInt(rest[1], 0, 255));
                desk.sync();
                tell(sender, "Fader " + rest[0] + " at " + rest[1]);
                return;
            case "record":
                requireArgs(rest, 1, "/encore record <x> <y> <z> <cue> [fadeIn] [fadeOut] [hold]");
                int key = parseInt(rest[0], 0);
                if (rest.length > 1) {
                    desk.setFadeInTicks(parseInt(rest[1], 0, TileEntityBasicLightingControl.MAX_TIMING_TICKS));
                }
                if (rest.length > 2) {
                    desk.setFadeOutTicks(parseInt(rest[2], 0, TileEntityBasicLightingControl.MAX_TIMING_TICKS));
                }
                if (rest.length > 3) {
                    desk.setHoldTicks(parseInt(rest[3], 0, TileEntityBasicLightingControl.MAX_TIMING_TICKS));
                }
                desk.record(key);
                desk.sync();
                tell(sender, "Recorded the current faders as cue " + key);
                return;
            case "cue":
                cue(sender, desk, rest);
                return;
            case "effect":
                effect(sender, desk, rest);
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void cue(ICommandSender sender, TileEntityBasicLightingControl desk, String[] rest) throws CommandException {
        String usage = "/encore cue <x> <y> <z> <cue> <fadein|fadeout|hold|time|delete> [ticks|off]";
        requireArgs(rest, 2, usage);
        int key = parseInt(rest[0], 0);
        String what = rest[1].toLowerCase(Locale.ROOT);
        if ("delete".equals(what)) {
            if (desk.getStoredSteps().remove(key) == null) {
                throw new CommandException("The desk has no cue " + key);
            }
            desk.sync();
            tell(sender, "Deleted cue " + key);
            return;
        }
        StoredCue cue = requireCue(desk, key);
        requireArgs(rest, 3, usage);
        int max = TileEntityBasicLightingControl.MAX_TIMING_TICKS;
        switch (what) {
            case "fadein":
                cue.setFadeInTicks(parseInt(rest[2], 0, max));
                break;
            case "fadeout":
                cue.setFadeOutTicks(parseInt(rest[2], 0, max));
                break;
            case "hold":
                cue.setHoldTicks(parseInt(rest[2], 0, max));
                break;
            case "time":
                cue.setTriggerTime("off".equalsIgnoreCase(rest[2]) ? -1 : parseInt(rest[2], 0, 23999));
                break;
            default:
                throw new WrongUsageException(usage);
        }
        desk.sync();
        tell(sender, "Cue " + key + ": " + describe(cue));
    }

    private void effect(ICommandSender sender, TileEntityBasicLightingControl desk, String[] rest) throws CommandException {
        String usage = "/encore effect <x> <y> <z> <add|clear|list> <desk|cue N> ...";
        requireArgs(rest, 1, usage);
        String action = rest[0].toLowerCase(Locale.ROOT);
        if ("list".equals(action)) {
            for (String line : status(desk)) {
                if (line.contains("effect") || line.startsWith("  ")) {
                    tell(sender, line);
                }
            }
            return;
        }
        requireArgs(rest, 2, usage);
        int consumed;
        List<DeskEffect> target;
        String label;
        if ("desk".equalsIgnoreCase(rest[1])) {
            target = desk.getDeskEffects();
            label = "desk";
            consumed = 2;
        } else if ("cue".equalsIgnoreCase(rest[1])) {
            requireArgs(rest, 3, usage);
            int key = parseInt(rest[2], 0);
            target = requireCue(desk, key).getEffects();
            label = "cue " + key;
            consumed = 3;
        } else {
            throw new WrongUsageException(usage);
        }
        if ("clear".equals(action)) {
            target.clear();
            desk.sync();
            tell(sender, "Cleared the effects on the " + label);
            return;
        }
        if (!"add".equals(action)) {
            throw new WrongUsageException(usage);
        }
        String addUsage = "/encore effect <x> <y> <z> add <desk|cue N> <sine|triangle|saw|square|random|strobe>"
                + " <periodTicks> <min> <max> <spread> <channels...>";
        String[] spec = Arrays.copyOfRange(rest, consumed, rest.length);
        requireArgs(spec, 6, addUsage);
        DeskEffect.Shape shape;
        try {
            shape = DeskEffect.Shape.parse(spec[0]);
        } catch (IllegalArgumentException e) {
            throw new WrongUsageException(addUsage);
        }
        int period = parseInt(spec[1], 2, TileEntityBasicLightingControl.MAX_TIMING_TICKS);
        int min = parseInt(spec[2], 0, 255);
        int max = parseInt(spec[3], 0, 255);
        float spread = (float) parseDouble(spec[4], -1, 1);
        int[] channels = parseChannels(Arrays.copyOfRange(spec, 5, spec.length));
        DeskEffect effect = new DeskEffect(shape, period, min, max, spread, channels);
        target.add(effect);
        desk.sync();
        tell(sender, "Added to the " + label + ": " + effect.describe());
    }

    /** Channels from tokens such as "5", "0-6" or "0,7,14". */
    static int[] parseChannels(String[] tokens) throws CommandException {
        List<Integer> out = new ArrayList<>();
        for (String token : tokens) {
            for (String part : token.split(",")) {
                if (part.isEmpty()) {
                    continue;
                }
                int dash = part.indexOf('-', 1);
                if (dash > 0) {
                    int from = parseInt(part.substring(0, dash), 0, 511);
                    int to = parseInt(part.substring(dash + 1), 0, 511);
                    for (int c = Math.min(from, to); c <= Math.max(from, to); c++) {
                        out.add(c);
                    }
                } else {
                    out.add(parseInt(part, 0, 511));
                }
            }
        }
        if (out.isEmpty()) {
            throw new CommandException("Name at least one channel (0-511)");
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private static List<String> status(TileEntityBasicLightingControl desk) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "%s, cue %s, next %d; loop %s, auto-start %s, master %d",
                desk.isPlaying() ? "Playing" : desk.isRunMode() ? "Run mode" : "Program mode",
                desk.getActiveCueKey() < 0 ? "none" : Integer.toString(desk.getActiveCueKey()), desk.getCurrentStep(),
                desk.isLoop() ? "on" : "off", desk.isAutoStart() ? "on" : "off", Byte.toUnsignedInt(desk.getGrandMaster())));
        TreeMap<Integer, StoredCue> cues = new TreeMap<>(desk.getStoredSteps());
        lines.add(cues.isEmpty() ? "No cues" : cues.size() + " cues:");
        for (Integer key : cues.keySet()) {
            StoredCue cue = cues.get(key);
            lines.add("  cue " + key + ": " + describe(cue));
            for (DeskEffect effect : cue.getEffects()) {
                lines.add("    effect: " + effect.describe());
            }
        }
        lines.add(desk.getDeskEffects().isEmpty() ? "No desk effects" : "Desk effects:");
        for (DeskEffect effect : desk.getDeskEffects()) {
            lines.add("  effect: " + effect.describe());
        }
        return lines;
    }

    private static String describe(StoredCue cue) {
        StringBuilder faders = new StringBuilder();
        for (byte b : cue.getFaders()) {
            if (faders.length() > 0) {
                faders.append(' ');
            }
            faders.append(Byte.toUnsignedInt(b));
        }
        return String.format(Locale.ROOT, "faders [%s], fade in %d, fade out %d, hold %s, time %s",
                faders, cue.getFadeInTicks(), cue.getFadeOutTicks(),
                cue.getHoldTicks() > 0 ? cue.getHoldTicks() + " ticks" : "until Go",
                cue.getTriggerTime() < 0 ? "none" : Integer.toString(cue.getTriggerTime()));
    }

    private static StoredCue requireCue(TileEntityBasicLightingControl desk, int key) throws CommandException {
        StoredCue cue = desk.getStoredSteps().get(key);
        if (cue == null) {
            throw new CommandException("The desk has no cue " + key + "; record it first");
        }
        return cue;
    }

    private static TileEntityBasicLightingControl findDesk(World world, BlockPos pos) throws CommandException {
        if (!world.isBlockLoaded(pos)) {
            throw new CommandException("That position is not loaded");
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityBasicLightingControl)) {
            throw new CommandException("There is no lighting desk at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
        return (TileEntityBasicLightingControl) tile;
    }

    private static boolean parseOnOff(String[] rest) throws CommandException {
        requireArgs(rest, 1, "on or off");
        String value = rest[0].toLowerCase(Locale.ROOT);
        if ("on".equals(value) || "true".equals(value)) {
            return true;
        }
        if ("off".equals(value) || "false".equals(value)) {
            return false;
        }
        throw new CommandException("Expected on or off, got " + rest[0]);
    }

    private static void requireArgs(String[] rest, int count, String usage) throws CommandException {
        if (rest.length < count) {
            throw new WrongUsageException(usage);
        }
    }

    private static void tell(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length >= 2 && args.length <= 4) {
            return getTabCompletionCoordinate(args, 1, targetPos);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 5) {
            switch (sub) {
                case "loop":
                case "autostart":
                    return getListOfStringsMatchingLastWord(args, "on", "off");
                case "effect":
                    return getListOfStringsMatchingLastWord(args, "add", "clear", "list");
                default:
                    return Collections.emptyList();
            }
        }
        if ("effect".equals(sub) && args.length == 6) {
            return getListOfStringsMatchingLastWord(args, "desk", "cue");
        }
        if ("cue".equals(sub) && args.length == 6) {
            return getListOfStringsMatchingLastWord(args, "fadein", "fadeout", "hold", "time", "delete");
        }
        return Collections.emptyList();
    }
}
