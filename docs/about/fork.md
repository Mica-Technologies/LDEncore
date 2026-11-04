# This is a fork

**LDEncore** is an unofficial fork of [Theatrical](https://github.com/theatricalmod/theatrical-forge)
(the Forge edition) by the Theatrical Team, maintained by
[Mica Technologies](https://github.com/Mica-Technologies) for **Minecraft Forge 1.12.2**.

!!! danger "This is not the official Theatrical mod"

    - This repository is **not affiliated with, endorsed by, or supported by the Theatrical team.**
    - Builds published here are **not** the official mod. They are our own builds, with our own
      changes, and they carry our own version numbers.
    - **Do not report problems with this fork to the upstream project.** The Theatrical team did
      not write the 1.12.2 port or these changes and cannot support them. Bring problems to
      Mica Technologies, through [this repository](https://github.com/Mica-Technologies/LDEncore)
      or the pack you got it in.
    - For any Minecraft version other than 1.12.2, the official Theatrical is what you want.

## Why this fork exists

Theatrical's Forge edition was archived, and its last release targets Minecraft 1.16.4. Our
servers and modpacks run Forge 1.12.2, so this fork ports the mature 1.16 mod to 1.12.2: every
block, fixture, screen and network, translated file by file, with the bugs found along the way
fixed.

## Scope of support

| | |
|---|---|
| **Minecraft version** | 1.12.2 |
| **Mod loader** | Forge |
| **Other versions** | Not supported here. The repository also keeps the resurrected 1.16.4 baseline, frozen: it builds, but gets no further work. |

## It keeps Theatrical's identity in some places

The mod id is still `theatrical` and the code keeps upstream's package names, so that packs and
configs written for Theatrical keep working and every file maps onto its upstream original. The
names players see (the mod list, the creative tab, the guide) say **LDEncore**, so nobody mistakes
it for the official mod.

## What changed

Every file this fork changed from upstream says so at the top, as the Apache License requires.
The repository's
[README](https://github.com/Mica-Technologies/LDEncore#what-this-fork-changes) summarises what the
fork changes, and
[`CHANGELOG-FORK.md`](https://github.com/Mica-Technologies/LDEncore/blob/1.12/CHANGELOG-FORK.md)
lists every change by release.
