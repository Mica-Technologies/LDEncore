# Troubleshooting and FAQ

## My fresnel will not light

Work back along the chain:

1. **Is the dimmer rack powered?** It needs Forge Energy from another mod. With The One Probe, look
   at it to see its stored power.
2. **Does the socapex cable join the distro?** A distro accepts socapex on **one face**, the one
   that pointed back at you when you placed it. If the cable stops short of the distro, turn the
   distro with the wrench.
3. **Is the dimmer patched?** Open the rack: the dimmer should show a plug label such as `E1`. If
   the plugs list is empty, the rack cannot reach a distro; check the cable.
4. **Is the dimmer getting DMX?** The rack's start address plus the dimmer number (minus one) is
   the channel it listens on. Fader 1 of a desk is channel 0.
5. **Does one socket feed one light?** Each distro socket and dimmed power cable run feeds a single
   fresnel.

A fresnel never reads DMX itself. Running DMX cable straight into one does nothing.

## My moving head will not light

- **Power.** Each head needs 50 FE per tick from a Forge Energy source, or set
  `fixtures.consumePower` to `false`.
- **Intensity and colour.** Intensity is channel +0; red, green and blue are +1 to +3. Intensity
  with no colour gives a lit head with a black beam.
- **Address.** Check the head's address matches the channels you are sending, and remember
  addresses count from 0.

## My beam points at the sky

Tilt is channel +6. About 128 points a hanging head straight down. Full-scale (255) on every
channel aims the beam upward.

## A light fell off

Its support was removed. Pick it up (it says *Broken!*) and repair it: a fresnel with a Tungsten
Bulb, a moving head with an Electrical Motor and an LED. See [Rigging](../guide/rigging.md).

## Part of my rig went dark

Fixtures go dark when their DMX source can no longer reach them. Look for a broken DMX cable
between the last working fixture and the first dark one. See
[When the signal is lost](dmx.md#when-the-signal-is-lost).

## Art-Net is not arriving

- Open the interface: the status line says whether data is arriving.
- Leave the IP at `0.0.0.0` unless you need a specific interface; only that receives broadcasts.
- Make sure the universe matches your software's output universe.
- Only one program on a computer can listen on UDP port 6454. Close anything else listening on it.
- Firewalls: allow Minecraft (Java) to receive UDP on your local network.

## Art-Net arrives but nothing happens on a server

The server accepts Art-Net only from the player who placed the interface, and only if that player
is an **operator**. Ask an operator to place it, or to op you.

## Does this replace Theatrical?

LDEncore keeps Theatrical's mod id, `theatrical`, so treat them as the same mod: install one or
the other, never both. Configs and pack scripts that name `theatrical` keep working. LDEncore is
for Forge 1.12.2 only; for other versions, use the
[official Theatrical](https://github.com/theatricalmod).

## My show stopped when everyone left

A desk only runs while its chunk is loaded. When nobody is near, the venue unloads and the show
pauses; it carries on as soon as someone walks in, and a show that was playing resumes after a
restart too. To keep it running with nobody there, force-load the chunk with a chunk-loading mod.

## The server or my frame rate struggles with a moving rig

Every time a beam lands somewhere new, the light around that spot is recalculated. See
[Performance](performance.md) for what that costs and the two settings that tame it, above all
Forge's `alwaysSetupTerrainOffThread` on clients.
