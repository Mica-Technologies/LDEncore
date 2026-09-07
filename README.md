# LDEncore

An **unofficial fork** of [Theatrical](https://github.com/theatricalmod/theatrical-forge) (the
Forge edition) by [Rushmead](https://github.com/Rushmead) and the Theatrical Team, maintained by
[Mica Technologies](https://github.com/Mica-Technologies). "LD" is for Limitless Development, the
prefix all of our mod forks carry; "Encore" is a nod to the theatre.

> ### ⚠️ This is not the official Theatrical mod
>
> - This repository is **not affiliated with, endorsed by, or supported by Rushmead or the
>   Theatrical Team.**
> - Builds published here are **not** the official mod. They are our own builds, with our own
>   changes, and they carry our own version numbers.
> - **Do not report problems with this fork to the upstream project.** The Theatrical Team did not
>   write these changes and cannot support them. File issues on
>   [this repository's issue tracker](../../issues) instead.
> - If you want the official mod, get it from
>   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/theatrical). The upstream Forge
>   repository this fork was taken from was archived in February 2023; the Theatrical Team's
>   current work lives in their [Theatrical](https://github.com/theatricalmod/Theatrical)
>   repository for modern Minecraft versions, and that is where anyone wanting Theatrical on
>   1.20+ should look.

## Why this fork exists

Theatrical brings live-events equipment into Minecraft: stage lighting, dimmers, DMX and Art-Net
control, power distribution and rigging. Upstream development of the Forge 1.16 line stopped when
the repository was archived, and no version of the mod exists for **Forge 1.12.2**, which is what
our servers and modpacks run.

This fork exists to **bring Theatrical to Forge 1.12.2**, and to keep it buildable and maintained
on our release infrastructure. Two lines of work live in this repository:

- **The 1.12.2 port**, on this branch (`1.12`, the default). It is a translation of the mature
  1.16 code to Forge 1.12.2 APIs, built with the same GregTechCEu buildscripts as our other
  1.12.2 mods. **It is in progress.** Right now it is a mod skeleton that loads and does nothing;
  fixtures, dimming, cabling and control arrive phase by phase (see the roadmap).
- **The 1.16.4 baseline**, on the `1.16.3` branch: the final state of upstream's default branch
  (Theatrical 0.7.2 for Minecraft 1.16.4 plus the unreleased fixes after it), resurrected so it
  builds again on a current toolchain. It is the reference the port is translated from, and it
  is what the published builds currently are.

**Scope of support:**

| | |
|---|---|
| **Minecraft version (goal)** | 1.12.2, Forge -- the backport this fork exists for; in progress on `1.12` |
| **Minecraft version (published today)** | 1.16.4 (Forge 35.1.37) -- the inherited upstream baseline, from `1.16.3` |
| **Mod loader** | Forge only |
| **Other versions** | Not supported here -- use the [official mod](https://www.curseforge.com/minecraft/mc-mods/theatrical) |

Upstream kept one branch per Minecraft version (`1.12`, `1.15.1`, `1.16.3`, `1.18.2`, plus a few
feature branches), and the fork inherited them all. `1.12` originally held upstream's 2019
prototype, an early and much smaller version of the mod from before Theatrical was rewritten for
1.15/1.16; that code remains in this branch's history as reference material, but the port is not
built on it. The other inherited branches are left untouched.

### Roadmap

1. **Done:** make the inherited 1.16.4 code build again on a current toolchain, publish it from
   this repository, and document the fork.
2. **Done:** start the 1.12.2 line on this branch with the fleet build system and an empty mod
   that loads on Forge 1.12.2.
3. **In progress:** port the mod, bottom-up -- API and capabilities, then blocks/items/tiles and
   assets, networking and GUIs, rendering, Art-Net and control, the in-game guide and compat --
   keeping the 1.16 behaviour as the reference and fixing the known upstream bugs on the way.
   Everything up to and including rendering is done; Art-Net and control are next.
4. **Then:** release the 1.12.2 line. The 1.16.4 line is frozen at its resurrected state: it
   stays buildable, but 1.12.2 is the only version that receives work.

### Downloads

Builds are published to this repository's [Releases](../../releases) page.

- **Releases** are versioned `YYYY.MM.DD` and are the ones to use.
- **Pre-releases** are cut automatically on every push. They are not guaranteed to be stable, and
  they are pruned after 90 days.

Both lines publish to the same page; tell them apart by the jar name and the Minecraft version in
the release title. 1.12.2 jars are `LDEncore-1.12.2-<version>.jar`; 1.16.4 jars are
`LDEncore-<version>-forge-mc1.16.jar`. **Until the port reaches a playable state, 1.12.2
pre-releases are skeleton builds and not worth installing.** Every release names the upstream
Theatrical version it is based on.

---

## What this fork changes

We want to be transparent about exactly how these builds differ from Theatrical. This section is
kept current; [`CHANGELOG-FORK.md`](CHANGELOG-FORK.md) has the per-release detail, and every
modified file carries a notice at its top (see [Licensing](#licensing)).

**The 1.12.2 line (this branch):** a port in progress. The Java sources here are written by
Mica Technologies after the corresponding 1.16 upstream files, file by file, and each carries a
header saying so. Nothing of upstream's 1.16 source is compiled on this branch; the reference
copy lives on `1.16.3`. The build is the GregTechCEu buildscripts (RetroFuturaGradle) rather than
upstream's ForgeGradle, and artnet4j is shaded under a relocated package instead of being merged
in as-is. Ported so far: the API and capabilities, every block, item and tile entity, the falling
light entity, the config and all assets, the networking and every screen, and the rendering (see
the changelog for what each step fixed on the way). Still to come: Art-Net polling, The One Probe
overlays and the Patchouli guide. Deliberate fork differences beyond the
port itself: the creative tab is labelled LDEncore; the placeholder wrench texture is ours
(upstream shipped none); every server-bound packet is now validated for reach, block type and
payload range before it is allowed to change the world, and DMX updates go only to the players
near the block rather than to everyone in the dimension; and several upstream bugs are fixed (a
dimmer rack that ignored every DMX address other than 0, a dimmer rack that refused to be patched
until it was powered, a positioner that aimed lights the wrong way on two facings, cables that
forgot their state on reload).

**The 1.16.4 baseline (`1.16.3` branch) -- no gameplay changes:**

- **Build system.** Rewritten from ForgeGradle 3 / Gradle 4.9 to ForgeGradle 6 / Gradle 8, because
  the original could no longer be built: it depended on jcenter and on a The One Probe maven that
  have both shut down. Dependencies now come from McJty's maven (The One Probe) and BlameJared's
  maven (Patchouli, PatchouliProvider). The Powah and Lollipop dev-environment mods upstream used
  for hand-testing were dropped; the mod never referenced them.
- **Versioning and releases.** Versions come from the git tag CI creates (`YYYY.MM.DD`); jars are
  named `LDEncore-<version>.jar`. Releases go to GitHub Releases from this repository.
- **Mod metadata.** The in-game mod list shows the mod as *LDEncore*, credits Theatrical and
  Rushmead, and links to this repository's issue tracker rather than the archived upstream one.
  The mod id stays `theatrical`, so worlds and packs made with the official 1.16.4 mod keep working
  with these builds (and the two cannot be installed together).
- **Repository scaffolding.** README, `CLAUDE.md`, `NOTICE`, changelog, editor/git config, shared
  IntelliJ run configurations and GitHub Actions workflows, all following the conventions of our
  other mod forks.

**Not changed on `1.16.3`:** all Java sources, assets, data-generator output and the in-game
Patchouli guide are exactly as upstream left them.

---

## What the mod does

Theatrical adds equipment from the live-events industry. This describes the 1.16.4 version the
port is translated from; it is the feature set the 1.12.2 line is working towards, and until a
feature is listed in [`CHANGELOG-FORK.md`](CHANGELOG-FORK.md) as ported it is not in the 1.12.2
builds yet.

### Fixtures

- **Generic light** -- a tungsten fresnel. Hangs from truss, pans and tilts, and throws a beam
  whose end point actually lights up the world. A **positioner** item lets a player aim it by
  walking to where the light should land; a **wrench** rotates fixtures.
- **Moving light** -- an intelligent moving-head fixture with pan, tilt and colour, driven
  entirely over DMX.

Both consume power from the dimming/distribution blocks (configurable) and emit real block light
(configurable). The beam's opacity is a client-side setting.

### Power and dimming

- **Dimmer rack** -- takes DMX in, takes power in, and puts out six dimmed channels. The dimmer
  rack is where you patch dimmer channels to the sockets that feed your lights.
- **Socapex distribution** -- breaks a six-channel socapex multicore out to individual fixtures.
- **Cables:** power, dimmed power, socapex and DMX. Each only connects to what it should.

### Control

- **Art-Net interface** -- receives Art-Net from real lighting software (grandMA dot2 onPC, Hog 4
  PC, QLC+, and so on) and feeds it to the in-game DMX network. See the note below on how this
  works on a server.
- **DMX redstone interface** -- drives a DMX channel from a redstone signal, for shows without
  external software.
- **Basic lighting desk** -- an in-game console with faders, so a show can be run without any
  external control at all.

### Rigging

- **Truss** and **IWB** (internally wired bar) blocks to hang fixtures from.

### In-game guide

The mod ships a [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) book covering
fixtures, dimming, control, rigging and a glossary. Patchouli is optional at runtime; the book only
appears when it is installed.

### Art-Net on a server

This is the thing most people trip over, so it is worth spelling out. An Art-Net interface only
accepts data from **the player who placed it**, and that player **must be a server operator**. The
data path is: your lighting software sends Art-Net to *your client*, and your client forwards it to
the server through Minecraft's own connection. So the IP address you enter in the interface is a
**local address on the machine running your client** (typically `127.0.0.1`), not the server's
address, and your lighting software should target that same machine.

### Configuration

`config/theatrical-common.toml`:

| Option | Default | What it does |
|---|---|---|
| `fixtures.emitLight` | `true` | Whether fixtures emit real block light where their beam lands |
| `fixtures.consumePower` | `true` | Whether moving lights consume power |

`config/theatrical-client.toml`:

| Option | Default | What it does |
|---|---|---|
| `rendering.lightBeamOpacity` | `0.4` | Opacity of rendered light beams, `0` to `1`. `0` disables beam rendering |

---

## Building

### This branch (`1.12`, Forge 1.12.2)

The build is the [GregTechCEu buildscripts](https://github.com/GregTechCEu/Buildscripts)
(RetroFuturaGradle), the same setup as our other 1.12.2 mods. Gradle itself runs on **JDK 17 to
22 (21 recommended, and what CI uses)**; the compiler and the mod stay on Java 8 via Jabel, and
Gradle provisions the Java 8 runtime it needs.

```bash
# Build the mod (jar lands in build/libs/ as LDEncore-1.12.2-<version>.jar)
JAVA_HOME="/path/to/jdk-21" ./gradlew build

# Run the dev client / server (Java 8 runtime, provisioned automatically)
JAVA_HOME="/path/to/jdk-21" ./gradlew runClient
JAVA_HOME="/path/to/jdk-21" ./gradlew runServer

# Apple Silicon: see addon.gradle for the -Prosetta path
```

IntelliJ run configurations are generated on project import by the buildscript's idea-ext
integration; nothing under `.idea/` is versioned on this branch.

### The `1.16.3` branch (Forge 1.16.4)

ForgeGradle 6 needs **Java 17 or newer to run Gradle**; the mod compiles against a **Java 8**
toolchain, which Gradle provisions automatically. Same commands as above, plus `runData` to
regenerate data-generator output and `genIntellijRuns` for the IDE run configurations. Run `clean`
and `build` as separate invocations there.

### Versioning

Release builds take their version from the git tag that CI creates immediately before building
(`YYYY.MM.DD`, or `YYYY.MM.DD-pre.HHMM.<tz>+<sha>` for a pre-release). On this branch the
buildscript derives it with `git describe`, so a local build off an untagged commit reports the
nearest tag plus a commit count and `.dirty` marker; on `1.16.3` an untagged build falls back to
the upstream base version. Every release body names the upstream Theatrical release the build is
translated from (`upstream_version` in `gradle.properties`).

See [CLAUDE.md](CLAUDE.md) for the fuller developer notes, including the rules every contributor
(human or otherwise) has to follow about marking changed files.

---

## Credits and upstream

All original design, code, models and artwork are by **Rushmead (Stuart Pomeroy)**, **James
Conway (615283)** and the Theatrical contributors and community, with the in-game guide by
FreneticScribbler and models by Manmaed and others. This fork claims no credit for the mod itself.

- Upstream repository (archived): <https://github.com/theatricalmod/theatrical-forge>
- Official downloads: <https://www.curseforge.com/minecraft/mc-mods/theatrical>
- The Theatrical Team's current project: <https://github.com/theatricalmod/Theatrical>

> **Note on community links:** upstream's README links to the Theatrical Discord server. We
> deliberately do not reproduce that link here, because putting it in *our* README implies it is a
> support channel for *this* fork -- it is not, and its members should not be fielding questions
> about our builds. Follow the upstream repository link above if you are looking for the Theatrical
> community.

## Licensing

Theatrical is licensed under the **Apache License, Version 2.0**, and so is this fork. The original
[`LICENSE`](LICENSE) is retained unchanged. What that means in practice, and what we do about it:

- The licence lets anyone fork, modify and redistribute the mod, including commercially, provided
  the terms are followed. This fork does not relicense anything and adds no restrictions.
- Section 4(b) requires that **modified files carry prominent notices stating that they were
  changed**. Every inherited file this fork modifies has a `CHANGED FROM UPSTREAM` notice at the top
  saying what changed and why. Files whose format cannot carry a comment (JSON, images, jars) are
  listed in [`CHANGELOG-FORK.md`](CHANGELOG-FORK.md) instead.
- [`NOTICE`](NOTICE) identifies the upstream work, its authors and the bundled third-party
  library ([artnet4j](https://github.com/theatricalmod/artnet4j), GPL-3.0, shaded into the jar
  exactly as upstream shipped it).
- The [What this fork changes](#what-this-fork-changes) section above is the human-readable
  summary and is kept current alongside the changelog.

Contributions to this fork are accepted under the same Apache-2.0 terms.
