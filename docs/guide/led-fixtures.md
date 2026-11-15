# LED fixtures and room light

LDEncore's LED fixtures are the lights a venue lives with every day: strips, battens, panels.
Each one is two lights in one:

- a **show light**: full RGB colour under DMX, from a desk, Art-Net or effects;
- a **room light**: plain warm white that lights the room like any ordinary lamp, floor included.

A fixture can switch between the two by itself, so a small venue whose show runs on a schedule
gets its normal lighting back the moment the show stops.

## The fixtures

| Fixture | Mounts | Joins into runs |
|---|---|---|
| **RGB LED Strip** | Any face: floor, ceiling or wall, level or upright | Yes, end to end |

**Placing a strip.** Click the face you want it on. On a floor or ceiling it runs the way you are
facing; on a wall it runs level, or upright if you are looking well up or down the wall. A strip
placed against the end of another strip on the same surface lines up with it and joins its run.

## Modes

Right-click a fixture to open its screen. **Mode** cycles through three settings:

| Mode | What the fixture does |
|---|---|
| **Show** | Colour and brightness come from DMX. The fixture glows, but lights nothing around it. |
| **Room** | Plain warm white at full brightness. It lights the room, and keeps the floor beneath it bright too (see [Room light](#room-light)). |
| **Auto** (the default) | Room light while its **House** channel is at 128 or more, while it (or any segment of its run) has a redstone signal, or when no DMX has reached it for 3 seconds. Show otherwise. |

Auto is what makes a venue run itself:

- **From a desk cue.** Put the House channel up in the cue that ends the show (or a cue triggered
  at a time of day), and down in the cues of the show itself. The house lights come up and go
  down with the show.
- **From redstone.** A lever, a daylight sensor or a door switch on any segment brings the room
  light on while it is powered.
- **With no console.** A fixture in Auto that hears no DMX at all (nothing is wired to it, or the
  desk was removed or its cable cut) comes on as a room light after 3 seconds, as house dimmers do
  when their console goes away. A freshly placed fixture therefore simply lights the room.
  A desk that is blacked out still sends its zeros, so a blacked-out desk keeps the fixtures dark.

The no-signal delay, and whether there is one at all, is set by `roomLight.noSignalSeconds` in the
[configuration](../reference/configuration.md#roomlight).

## Channels

From the fixture's start address:

| Channel | Does |
|---|---|
| +0 | Red |
| +1 | Green |
| +2 | Blue |
| +3 | Dimmer (scales the colour) |
| +4 | House: 128 or more puts a fixture in Auto into room light |

So five channels per fixture, or per run.

## Runs

Strips that touch end to end, on the same surface and running the same way, form a **run**:

- **One address, one mode.** Change the settings of any segment and Save: the whole run takes
  them. A segment placed against a run takes the run's settings, so extending one needs no
  setting up.
- **One cable.** DMX passes from segment to segment, so a cable into any segment reaches the whole
  run.
- **Breaking a segment** splits the run in two. Each half keeps the settings it had.

### Pixel mode

Turn on **Pixel per segment** and each segment gets its own colour. The run then uses more
channels: the first segment's Red, Green and Blue are +0 to +2, the shared Dimmer and House stay at
+3 and +4, and each further segment adds its own Red, Green and Blue from +5 on:

| Channels | Segment |
|---|---|
| +0, +1, +2 | 1st (red, green, blue) |
| +3, +4 | Dimmer and House, for the whole run |
| +5, +6, +7 | 2nd |
| +8, +9, +10 | 3rd |
| ... | ... |

Segments count from the run's **west** end (east-west runs), **north** end (north-south runs) or
**bottom** (upright strips). A run of *n* segments uses 5 + 3 × (*n* − 1) channels; the screen
shows the range. Any segment whose channels would fall past 511 stays dark.

Pixel mode is made for the desk's [effects](lighting-desk.md#effects): a sine wave with a spread
across each segment's red channel runs a colour chase down the strip.

## Room light

A fixture acting as a room light gives off full block light itself, and also keeps an invisible
light block just above the floor beneath it, as [City Super Mod](https://github.com/Mica-Technologies/minecraft-city-super-mod)'s
bright lights do. Without that, a light on a high ceiling leaves the floor dim, because
Minecraft's light fades with distance.

- It looks straight down from the fixture, up to 16 blocks, and sits in the air just above the
  first solid block.
- A run places one under every other segment, which is enough to light the floor evenly.
- It is removed when the fixture goes back to show mode or is broken, and moves if the floor
  beneath it changes.
- Anything can be placed into it, as into air.
- **In show mode there is none.** The room is lit only by what the show does, so a chase costs
  nothing in light updates.

LDEncore does not need City Super Mod, and the two work side by side: neither ever replaces the
other's light block, and an LDEncore fixture whose floor is already lit by a CSM light places
nothing more.

The floor light, and how far down it looks, can be turned off or changed with
`roomLight.floorLight` and `roomLight.floorLightDepth`.
