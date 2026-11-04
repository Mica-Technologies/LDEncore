# DMX addresses and channels

## The basics

- A **universe** is 512 channels, each a value from 0 to 255.
- Every desk, ArtNet Interface and Test DMX Block drives **its own universe**, carried along the
  DMX cable connected to it. Two desks on separate cable runs do not interfere.
- Addresses in LDEncore are **numbered from 0**: the first channel is address 0, the last is
  511. (Real consoles usually number from 1, so address 0 here is channel 1 on your console.)
- A receiver uses a block of consecutive channels starting at its address.

## Channel counts

| Receiver | Channels | Layout |
|---|---|---|
| **Moving head** | 7 | +0 intensity, +1 red, +2 green, +3 blue, +4 focus, +5 pan, +6 tilt |
| **Dimmer rack** | 6 | +0 to +5: dimmers 1 to 6 |
| **DMX-Redstone Interface** | 1 | +0: redstone strength |

A fresnel is not a DMX receiver: it is dimmed through a rack. See
[Your first lit fresnel](../guide/first-rig.md).

## Patch sheet example

One desk can address two racks, or one head plus a rack:

| Address | Fixture |
|---|---|
| 0–6 | Moving head A |
| 7–12 | Dimmer rack 1 (six fresnels) |
| 13–19 | Moving head B (beyond the desk's twelve faders: drive it with desk effects, or Art-Net) |
| 20 | DMX-Redstone Interface for the house lights |

## When the signal is lost

A receiver that its source no longer reaches, because a cable was broken or the desk, interface or
test block was removed, is **blacked out**: every channel drops to 0 within about a second. That
turns a head off and swings it to its home position, turns a rack's dimmers off, and drops a
redstone interface to 0. Reconnect the run and it picks straight back up.

If the *source* keeps running but its input stops (your Art-Net software closes, say), the
fixtures keep their last look, as real Art-Net nodes do.

## Checking a rig

- With [The One Probe](../guide/installation.md#optional-mods-worth-adding) installed, look at a
  fixture or rack to see its address and channel count; sneak to see each channel's value.
- A **Test DMX Block** on the run sends random values every second: if everything moves, the
  wiring is right.
