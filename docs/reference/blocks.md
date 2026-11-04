# Blocks and items

Everything is in the **LDEncore** creative tab. Use JEI to look up recipes.

## Fixtures

| Block | What it is |
|---|---|
| **Generic Light** (fresnel) | A conventional tungsten light. Hangs under truss or a bar. Its brightness is the dimmed power it is fed through a dimmed power cable; it does not read DMX. Aim it with its screen or the positioner. Beam up to 25 blocks. |
| **Moving Light** (moving head) | An intelligent fixture with pan, tilt, colour and focus, controlled over seven DMX channels. Hangs or stands. Needs Forge Energy. Beam up to 50 blocks. See [Moving heads](../guide/moving-heads.md). |

## Control

| Block | What it is |
|---|---|
| **Basic Lighting Desk** | Twelve faders, a grand master, a cue stack with fades, holds and loops, and an effects engine that can drive any of its 512 channels. Can run a show unattended, driven by redstone, the time of day or `/encore`. See [The lighting desk](../guide/lighting-desk.md). |
| **ArtNet Interface** | Brings Art-Net from software on your computer into the game as DMX. See [Art-Net](../guide/art-net.md). |
| **DMX-Redstone Interface** | One DMX channel out as a redstone signal. See [DMX to redstone](../guide/redstone.md). |
| **Test DMX Block** | Creative only, no recipe. Sends random values on every channel once a second, for checking a rig is wired up. |

## Power

| Block | What it is |
|---|---|
| **Dimmer Rack** | Six dimmers. Takes DMX and Forge Energy in, and sends six dimmed circuits out over socapex. Patched in its screen. |
| **Socapex Distro** | A breakout box: one socapex input (on the face pointing back at whoever placed it) split into five sockets for dimmed power cable. |

## Cables

| Block | Carries |
|---|---|
| **DMX Cable** | DMX, from a desk or interface to fixtures, racks and redstone interfaces. Passes through fixtures, so heads can be chained. |
| **Socapex Cable** | Six dimmed circuits at once, from a dimmer rack to a distro. |
| **Dimmed Power Cable** | One dimmed circuit, from a distro socket to one fresnel. |
| **Power Cable** | Carries Forge Energy from a generator or storage block to truss, bars and moving heads, with no loss. See [Moving heads](../guide/moving-heads.md#power). |

Cables join up automatically with neighbouring cables of the same kind and with any block that
accepts them.

## Rigging

| Block | What it is |
|---|---|
| **MT100 Truss** | Box truss to hang lights from. Placed along an axis, like a log. Carries Forge Energy to the moving heads hung from it. |
| **Internally Wired Bar (IWB)** | A lighting bar. Right-click it holding a light to hang the light underneath. Carries Forge Energy to the moving heads hung from it. |

## Items

| Item | Use |
|---|---|
| **Remote Light Positioner** | Aims fresnels: link one and it follows you until you let go. See [Rigging](../guide/rigging.md#aiming-a-fresnel). |
| **Wrench** | Rotates blocks in place. |
| **Tungsten Bulb**, **LED**, **Electrical Motor**, **Mechanical Cog** | Crafting ingredients. The bulb, and the motor with an LED, also repair broken lights. The cog is also registered as `gearIron` in the ore dictionary. |

## The light itself

Wherever a beam lands, the mod places an invisible **Illuminator** block that gives off light in
proportion to the fixture's intensity. You never place or see it, and it disappears when the beam
moves on or goes dark. Turn this off with `fixtures.emitLight` if you only want the visible beams.
