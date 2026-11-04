# The lighting desk

The **Basic Lighting Desk** is a small console: twelve channel faders, a grand master, a cue stack
with fades and hold times, and an effects engine. It can be played by hand, or left to run a show
on its own with nobody at it. Run DMX cable from it to your fixtures or dimmer racks.

![The lighting desk screen](../assets/lighting-desk-screen.png)

## Faders

The twelve faders on the left send **DMX channels 0 to 11** of the desk's universe. The tall
fader on the right is the **grand master**, which scales all twelve. With the grand master at the
bottom, the faders send nothing. [Effects](#effects) are not scaled by it.

Drag a fader up to raise its channel. The physical faders on the desk block in the world move with
them, so everyone can see the state of the desk at a glance.

## Program mode: recording cues

The label at the top of the controls says which mode the desk is in. In **Program Mode**:

1. Set the faders to the look you want.
2. Set the times, in ticks (20 ticks is one second):
    - **Hold**: how long this cue stays up before the desk moves on by itself. `0` means it waits
      for Go.
    - **Fade In**: how long this cue takes to arrive.
    - **Fade Out**: how long this cue takes to fade to black when the next one comes.
3. Press **Go** to record the faders as the cue at the current **step**. The desk moves on to the
   next step.

The `<-` and `->` buttons move between steps. Stepping onto a step that already holds a cue loads
its faders, so you can adjust and re-record it. The **Cues** list on the right shows what is
stored.

## Run mode: playing a show

Press **Mode** to switch to **Run Mode**. Now **Go** plays the next stored cue. If the previous cue
has a fade-out, it fades to black first, then the new cue fades in over its fade-in.

While the stack is running the label says **Playing**. A cue with a hold time moves on to the next
by itself once it has faded in and held. A cue with no hold waits for Go.

| Button | Does |
|---|---|
| **Go** | Plays the next cue, and starts the stack running |
| `<-` `->` | Choose which cue plays next, without playing it |
| **Stop** | Stops the stack moving on. The current look stays up |
| **Loop** | When on (green), the last cue is followed by the cue the run started from, for ever. When off, the stack stops after the last cue |
| **Auto** | When on (green), the desk starts playing from the first cue by itself whenever it loads |
| **Mode** | Back to Program Mode, which also stops the stack |

## Leaving a show running

Turn **Loop** on and give every cue a hold time, and the desk plays its show by itself. A show that
is playing when the world saves **picks up where it was** when the area loads again, after a server
restart included. With **Auto** on, the desk also starts by itself even if it was stopped.

A desk only runs while its chunk is loaded, which is while a player is nearby (or the chunk is
force-loaded). In an empty venue the show pauses, and carries on as someone walks in.

## Redstone

A redstone pulse into the desk is **Go**. If the desk is in Program Mode, it switches to Run Mode
first, so a pulse never records over a cue. Some ways to use it:

- A button on the wall of the booth, for a house-lights cue.
- A daylight sensor, so the show starts at dusk.
- A command-block clock, to step a stack on a beat.

## Time of day

A cue can be set to play by itself at a time of day, for example house lights at dawn and the show
at dusk. When the world clock passes that time, the desk plays that cue and starts running from
it, so a looping show started at dusk cycles through its own cues and never back into the house
look. Set it with [`/encore cue ... time`](../reference/commands.md#cue-timing-and-triggers); there
is a full day-and-night example on that page.

## Effects

Effects make a desk look alive without programming every cue: pan and tilt sweeps, colour
rainbows, chases, pulses, strobes. An effect writes a waveform onto **any of the desk's 512
channels**, not just its twelve faders, so one desk with effects can drive far more fixtures than
it has faders.

- **Desk effects** run whenever the desk does, whatever cue is up.
- **Cue effects** run only while their cue is on stage, so each cue can bring its own movement.

An effect overrides the fader value on its channels. Effects are set up with
[`/encore effect`](../reference/commands.md#effects).

| Shape | Looks like |
|---|---|
| `sine` | A smooth swing between min and max, for sweeps and gentle pulses |
| `triangle` | A steady back-and-forth |
| `saw` | A ramp up, then a snap back, for one-way sweeps |
| `square` | On for half the period, off for the other half, for chases |
| `strobe` | A short flash once per period |
| `random` | A new level each period, held until the next |

**Spread** shifts each listed channel along the cycle after the first. With red, green and blue on
a `sine` and a spread of `0.333`, the colour runs round the rainbow; with several heads' intensity
channels on a `square` and a spread of `0.25`, the light chases from head to head.

Effects run on the world clock, so every desk running the same effect stays in step, and a show
that resumes after a restart is where it would have been.

## Which channels drive what

| Desk fader | DMX channel | Typical use |
|---|---|---|
| 1 | 0 | Dimmer 1 of a rack at address 0, or intensity of a head at address 0 |
| 2–4 | 1–3 | Dimmers 2–4, or a head's red, green, blue |
| 5 | 4 | Dimmer 5, or a head's focus |
| 6 | 5 | Dimmer 6, or a head's pan |
| 7 | 6 | A head's tilt |
| 8–12 | 7–11 | A second head at address 7 (its intensity and colour) |

Twelve faders cover one moving head plus a rack. Beyond that, drive the other channels with
effects, or drive the rig from real software over [Art-Net](art-net.md).

## With several players

The desk is shared. Any player can open it, and a change one player makes shows up live in
everyone else's open screen.
