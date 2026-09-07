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
on our release infrastructure. It starts from the final state of upstream's default `1.16.3`
branch (Theatrical 0.7.2 for Minecraft 1.16.4, plus the unreleased fixes that landed after it),
which is what this repository currently builds. The 1.12.2 backport is the goal, not yet the
state of the code; see the roadmap below.

**Scope of support:**

| | |
|---|---|
| **Minecraft version (today)** | 1.16.4 (Forge 35.1.37) -- the inherited upstream baseline |
| **Minecraft version (goal)** | 1.12.2, Forge -- the backport this fork exists for |
| **Mod loader** | Forge only |
| **Other versions** | Not supported here -- use the [official mod](https://www.curseforge.com/minecraft/mc-mods/theatrical) |

Upstream kept one branch per Minecraft version (`1.12`, `1.15.1`, `1.16.3`, `1.18.2`, plus a few
feature branches), and the fork inherited them all. Two matter here:

- **`1.12`** -- the default branch, and where the Forge 1.12.2 backport is being built. Until the
  port lands it still contains upstream's 2019 prototype: an early, much smaller version of the
  mod from before Theatrical was rewritten for 1.15/1.16, which does not build with current
  tooling. That old code is reference material for the port, not its starting point.
- **`1.16.3`** -- the resurrected 1.16.4 baseline described above, which the port is translated
  from and which currently produces the published builds.

The other inherited branches are left untouched.

### Roadmap

1. **Now:** make the inherited 1.16.4 code build again on a current toolchain, publish it from
   this repository, and document the fork. *(This is done -- see below.)*
2. **Next:** backport the mod to Forge 1.12.2 on the `1.12` branch, feature by feature, keeping
   the 1.16 behaviour as the reference.
3. **Then:** decide, based on how the backport goes, whether the 1.16.4 line keeps receiving
   fixes alongside 1.12.2 or is frozen at its current state.

### Downloads

Builds are published to this repository's [Releases](../../releases) page.

- **Releases** are versioned `YYYY.MM.DD` and are the ones to use.
- **Pre-releases** are cut automatically on every push. They are not guaranteed to be stable, and
  they are pruned after 90 days.

Every release names the upstream Theatrical version it is based on.

---

## What this fork changes

We want to be transparent about exactly how these builds differ from Theatrical. This section is
kept current; [`CHANGELOG-FORK.md`](CHANGELOG-FORK.md) has the per-release detail, and every
modified file carries a notice at its top (see [Licensing](#licensing)).

**Changed so far (no gameplay changes yet):**

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

**Not changed:** all Java sources, assets, data-generator output and the in-game Patchouli guide
are exactly as upstream left them.

---

## What the mod does

Theatrical adds equipment from the live-events industry. In the 1.16.4 version this fork
currently builds, that is:

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

Requires two JDKs. ForgeGradle 6 needs **Java 17 or newer to run Gradle**; the mod compiles
against a **Java 8** toolchain, which Gradle will locate or provision automatically (the foojay
resolver is configured in `settings.gradle`).

```bash
# Build the mod (jar lands in build/libs/)
JAVA_HOME="/path/to/jdk-17" ./gradlew build

# Run the dev client / server
JAVA_HOME="/path/to/jdk-17" ./gradlew runClient
JAVA_HOME="/path/to/jdk-17" ./gradlew runServer

# Regenerate data-generator output (recipes, loot tables, item models, the Patchouli book)
JAVA_HOME="/path/to/jdk-17" ./gradlew runData

# Generate the "Forge Client/Server/Data" IntelliJ run configurations
JAVA_HOME="/path/to/jdk-17" ./gradlew genIntellijRuns
```

Run `clean` and `build` as separate invocations; ForgeGradle resolves Minecraft during
configuration and `clean build` in one go deletes what it just resolved.

### Versioning

Release builds take their version from the git tag that CI creates immediately before building
(`YYYY.MM.DD`, or `YYYY.MM.DD-pre.HHMM.<tz>+<sha>` for a pre-release). Local builds with no such
tag fall back to `mod_version` in `gradle.properties`, which records the upstream release this fork
sits on. To force a version, pass `-PmodVersionOverride=...`.

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
