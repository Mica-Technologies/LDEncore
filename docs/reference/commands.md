# Commands

`/encore` runs [lighting desks](../guide/lighting-desk.md) without anyone standing at them: for
server admins, command blocks, and tools such as MCMCP. It needs operator permission (level 2),
which command blocks have.

Every subcommand names the desk by its position. Positions accept `~` relative coordinates, as
vanilla commands do. **Channels are DMX addresses counted from 0**, as everywhere else in the mod.
**Times are in ticks**: 20 ticks is one second.

## Playing

| Command | Does |
|---|---|
| `/encore status <x> <y> <z>` | Prints the mode, the cue on stage and the next one, loop and auto-start, the grand master, every cue with its times, and every effect |
| `/encore play <x> <y> <z> [cue]` | Starts the stack running from a cue (default: the first) |
| `/encore go <x> <y> <z>` | Plays the next cue |
| `/encore back <x> <y> <z>` | Plays the previous cue |
| `/encore stop <x> <y> <z>` | Stops the stack moving on; the current look stays up |
| `/encore loop <x> <y> <z> <on\|off>` | Whether the stack goes back to the cue the run started from after the last cue |
| `/encore autostart <x> <y> <z> <on\|off>` | Whether the desk starts playing by itself when it loads |

## Building a show without the screen

| Command | Does |
|---|---|
| `/encore fader <x> <y> <z> <1-12> <0-255>` | Sets a fader |
| `/encore master <x> <y> <z> <0-255>` | Sets the grand master |
| `/encore record <x> <y> <z> <cue> [fadeIn] [fadeOut] [hold]` | Stores the current faders as a cue. The times given are also kept as the desk's times for the next recording |

## Cue timing and triggers

`/encore cue <x> <y> <z> <cue> <setting> <value>`

| Setting | Value |
|---|---|
| `hold` | Ticks the cue holds after fading in before the desk moves on. `0` waits for Go |
| `fadein` | Ticks to fade in |
| `fadeout` | Ticks to fade to black when the next cue comes |
| `time` | A time of day, `0` to `23999` (as `/time set` uses: 0 is dawn, 6000 noon, 13000 dusk, 18000 midnight), at which the cue plays by itself. `off` removes it |

`/encore cue <x> <y> <z> <cue> delete` removes a cue.

## Effects

`/encore effect <x> <y> <z> add <desk|cue N> <shape> <period> <min> <max> <spread> <channels...>`

| Argument | Meaning |
|---|---|
| `desk` or `cue N` | Where the effect lives: on the desk (always runs) or on cue N (runs while it is on stage) |
| `shape` | `sine`, `triangle`, `saw`, `square`, `strobe` or `random`. See [Effects](../guide/lighting-desk.md#effects) |
| `period` | Ticks for one full cycle |
| `min`, `max` | The range of DMX values it moves between |
| `spread` | How far along the cycle each channel after the first is shifted, from `-1` to `1`. `0` keeps them together |
| `channels` | One or more channels: single numbers, ranges such as `0-6`, or comma lists such as `0,7,14` |

`/encore effect <x> <y> <z> clear <desk|cue N>` removes those effects.
`/encore effect <x> <y> <z> list` prints them.

## Examples

A head at address 0 sweeping its pan back and forth every five seconds:

```
/encore effect 100 64 200 add desk sine 100 40 215 0 5
```

A rainbow on that head's colour while cue 1 is up:

```
/encore effect 100 64 200 add cue 1 sine 60 0 255 0.333 1-3
```

Four heads at addresses 0, 7, 14 and 21 chasing their intensity:

```
/encore effect 100 64 200 add desk square 40 0 255 0.25 0,7,14,21
```

House lights by day, a two-look show looping all night. Cue 0 is the house look, cues 1 and 2
the show:

```
/encore cue 100 64 200 0 time 0
/encore cue 100 64 200 0 hold 0
/encore cue 100 64 200 1 time 13000
/encore cue 100 64 200 1 hold 200
/encore cue 100 64 200 2 hold 200
/encore loop 100 64 200 on
```

At dusk cue 1 plays and the stack runs 1, 2, 1, 2... because a loop goes back to the cue the run
started from. At dawn cue 0 plays and, with no hold, waits there until dusk.
