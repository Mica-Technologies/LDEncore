# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and any other coding agent when
working with code in this repository. It is the agent-facing companion to `README.md`; read both.

## Purpose

**LDEncore** is Mica Technologies' unofficial fork of
[Theatrical](https://github.com/theatricalmod/theatrical-forge) (Forge edition) by Rushmead and the
Theatrical Team. Theatrical adds live-events equipment -- fresnels, moving heads, dimmer racks,
DMX/Art-Net control, power and DMX cabling, truss -- to Minecraft. Mod id: `theatrical`.

The fork exists to **backport the mod to Forge 1.12.2**. What the repository currently builds is
the inherited upstream baseline: **Minecraft 1.16.4 / Forge 35.1.37, Theatrical 0.7.2** plus the
unreleased fixes that landed on upstream's `1.16.3` branch before it was archived (February 2023).
The backport is planned, not started; the plan lives in `docs/agent-plans/MASTER_PLAN.md`
(gitignored, see *Planning docs*).

Branches: **`1.12` is the repository's default branch and the home of the Forge 1.12.2
backport.** Until the port lands there it still holds upstream's 2019 prototype -- an early
version of the mod from before the 1.15/1.16 rewrite (140 Java files, Albedo-based lighting, a
JSON custom-fixture system) that does not build and is reference material only, not the port's
starting point. **`1.16.3`** is the resurrected upstream baseline (this scaffolding, the working
1.16.4 build) and the source the port is translated from. `1.15.1`, `1.18.2`,
`feature/cccompat`, `new-cables` and `working-audio-thing` were inherited from upstream and are
untouched. GitHub only honours `workflow_run` and `workflow_dispatch` for workflow files on the
default branch, so the `.github/workflows/` set has to exist on `1.12` too (Phase 1 of the plan
copies the scaffolding there).

Build system is **ForgeGradle 6** with a hand-written `build.gradle` -- *not* the GregTechCEu
Buildscripts used by the sibling Mica 1.12.2 mods (RCMC, CSM, LDOG, LDFAWE). That is a deliberate
choice for the 1.16 baseline; the 1.12.2 backport branch is expected to adopt the GregTechCEu
buildscripts like its siblings, and the plan covers that.

## Rules that are not negotiable

### 1. Mark every changed upstream file (Apache-2.0 §4(b))

Theatrical is Apache-2.0, and section 4(b) of that licence requires that **any modified file
carries a prominent notice stating that it was changed**. This is a licence obligation, not a
style preference. Every time you edit a file inherited from upstream:

- **Java sources** -- add (or extend) a comment block at the very top of the file, above the
  `package` line:

  ```java
  /*
   * CHANGED FROM UPSTREAM (Mica Technologies fork, LDEncore):
   *   - <what changed and why, one line per change>
   * Original work Copyright the Theatrical Team (Rushmead and contributors),
   * licensed under the Apache License, Version 2.0.
   */
  ```

  Upstream's own 1.16 sources mostly have no licence header; three files do. Keep theirs and put
  the fork notice above it.

- **Gradle, properties, TOML, YAML, shell, `.gitignore`-style files** -- a `# CHANGED FROM
  UPSTREAM (Mica Technologies fork, LDEncore): ...` (or `//` for Gradle) comment at the top.

- **Files that cannot carry a comment** (JSON assets/data, PNG textures, the artnet4j jar, the
  `.psd`) -- record the file path and what changed in `CHANGELOG-FORK.md` under *Modified files
  without comment support* for the current release section. That list is the notice.

- **New files you create** are fork additions, not modifications, and need no `CHANGED FROM
  UPSTREAM` notice. Give new Java files a short header identifying them as part of the LDEncore
  fork under Apache-2.0 so a reader can tell fork code from upstream code at a glance.

A useful test before committing: `git diff --name-only <upstream-base>..HEAD` should list only
files that either carry the notice or appear in the changelog list. Today the upstream base is
commit `764cff3` (upstream's final `1.16.3` commit).

### 2. Keep the README's "What this fork changes" section current

`README.md` has a *What this fork changes* section. It is the human-readable summary of how these
builds differ from Theatrical, and it exists because we want to be plainly transparent about what
we ship under a name that is not ours. When a change alters behaviour, adds or removes a feature,
changes a dependency, or changes what is in the jar, update that section in the same commit,
alongside the per-release entry in `CHANGELOG-FORK.md`. The two are not redundant: the changelog
is the ledger, the README section is the current-state summary.

### 3. Stay silent towards the upstream repository

The upstream repository is archived and we do not want to draw attention to this fork before it is
fully fleshed out. Concretely:

- **Never reference an upstream issue or pull request by number, slug or URL** in a commit
  message, tag, PR title/description, release note, code comment or changelog entry. No
  `#32`, no `theatricalmod/theatrical-forge#32`, no `Fixes ...`, no
  `github.com/theatricalmod/theatrical-forge/issues/...`. GitHub turns any of those into a
  cross-reference that shows up on the upstream issue.
- Describe the problem in your own words instead: *"fix the stack overflow when socapex cables
  form a loop"*, not *"fix upstream #32"*.
- The same applies to `theatricalmod/Theatrical` and the other upstream org repositories.
- Do not open issues, PRs or discussions on any upstream repository, and do not comment on
  existing ones.
- Linking to the upstream *repository* itself for attribution (README, NOTICE, release body) is
  fine and required; it is issue/PR references that create backlinks.

The plan doc in `docs/agent-plans/` is gitignored and may list upstream issue numbers for
tracking, since it never leaves this machine. That is the only place they belong.

### 4. Git

- **Create commits** when work reaches a logical checkpoint -- keep them descriptive and
  well-organized. Use conventional, descriptive commit messages that explain *why*, not just
  *what*, and group related changes; don't lump unrelated work together.
- **Never push** to any remote. The user will review and push manually.
- Commit identity for this repo is `mica-alex` (the global default). Verify with
  `git config user.email` before committing.
- Never `git add -f` anything under `docs/agent-plans/`.

## Build commands

ForgeGradle 6 requires **Java 17+ to run Gradle**, while the mod compiles against a **Java 8**
toolchain (`java_target=8` in `gradle.properties`). Both are needed. Set `JAVA_HOME` to a 17+ JDK;
Gradle locates or auto-provisions the Java 8 toolchain via the foojay resolver in
`settings.gradle`. On this machine the JDKs are IntelliJ-managed under `~/.jdks/` (Windows:
`C:\Users\<username>\.jdks\`): `azul-17.0.19` and `azul-1.8.0_482`. `java` is **not** on `PATH`,
so `JAVA_HOME` must be set explicitly. If toolchain detection ever fails to find the Java 8 JDK,
export `JAVA_HOME_8_X64=<path to the JDK 8>` -- `gradle.properties` reads it (that is how CI
finds its JDKs too).

```bash
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew build           # compile + jar -> build/libs/
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew runClient       # dev client
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew runServer       # dev dedicated server
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew runData         # regenerate src/generated/resources
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew genIntellijRuns # Forge Client/Server/Data IDE configs
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew clean           # NOT `clean build` -- see gotchas
```

There are no unit tests (`:test` is `NO-SOURCE`). Verification means building and launching; a
green compile says nothing about whether the mod loads, so after any change to registration,
capabilities, rendering or resources, run the client and read `run/logs/latest.log` for missing
models/textures and registry errors.

## IntelliJ run configurations

`.idea/runConfigurations/` holds six **versioned** run configurations, numbered the way the
GregTechCEu buildscript numbers its generated ones in the sibling mods:

| | Task |
|---|---|
| 1. Run Client | `runClient` |
| 2. Run Server | `runServer` |
| 3. Run Data Generators | `runData` |
| 4. Build Jars | `build` |
| 5. Clean | `clean` |
| 6. Generate IntelliJ Runs | `genIntellijRuns` |

They are plain `GradleRunConfiguration` files referencing nothing but `$PROJECT_DIR$` and a task
name, which is what makes them portable enough to version. **If a task is renamed, update the
matching XML** -- nothing verifies these automatically.

**Do not commit anything else from `.idea/`.** ForgeGradle's `genIntellijRuns` writes its own
`Application` configs into the same directory (`runClient.xml`, `runServer.xml`, `runData.xml`).
Those are the better configs for day-to-day debugging, but they embed absolute paths into
`~/.gradle` and the checkout. `.gitignore` versions only files matching `[0-9]__*.xml`.

## Architecture

Package root is `dev.theatricalmod.theatrical` -- **upstream's namespace, deliberately kept**, as
is the mod id `theatrical`. The id is a compatibility contract with saves and packs; renaming the
package would make every comparison against upstream a manual diff. Do not rebrand either.

```
TheatricalMod.java              @Mod entry point. Registers the Fixture registry, the two config
                                specs, deferred registers, capabilities, world capabilities
                                (DMX network, socapex network) and the world-tick that drives them;
                                owns the ArtNetManager
TheatricalCommon / Client       sided proxies; client registers renderers, screens, key handling
TheatricalConfigHandler         ForgeConfigSpec: common (emitLight, consumePower), client
                                (lightBeamOpacity)
api/                            the mod's own API: capabilities (DMX provider/receiver, socapex
                                provider/receiver, TheatricalPower energy storage, the world-level
                                DMX and socapex networks), the DMX universe model, fixture
                                definitions (Fixture is a Forge registry entry; IFixture, IRGB,
                                HangableType, GelType), CableType/CableSide
artnet/                         ArtNetManager + ArtNetThread wrapping the shaded artnet4j client.
                                One client per IP string. Runs client-side; data is forwarded to the
                                server by SendArtNetToServerPacket
block/                          blocks by family: light/ (generic fixture, moving light, the
                                Illuminator block that is the beam's light source, BlockLight base,
                                BlockHangable), cables/ (BlockCable + power/dimmed-power variants),
                                power/ (dimmer rack, socapex distribution), control/ (basic lighting
                                console), interfaces/ (Art-Net, DMX-redstone), rigging/ (truss, IWB),
                                test/. TheatricalBlocks holds the DeferredRegister
tiles/                          block entities, same families. TileEntityFixture is the heart of the
                                lighting model: it ray-traces along pan/tilt every tick, places or
                                updates an Illuminator block where the beam lands, and removes the
                                old one. TileEntityCable and the power/ tiles push power to
                                neighbours; the DMX side is world-network based instead
                                (WorldDMXNetwork walks cables and caches receivers)
client/                         TileEntityFixtureRenderer (fixture models + beam), FallingLightRenderer,
                                gui/ (containers + screens for every block with a UI, widgets for
                                faders/plugs/sockets), tile/ (render types, lighting desk renderer)
network/                        SimpleChannel packets, one class per action. TheatricalNetworkHandler
                                registers them
entity/                         FallingLightEntity -- a fixture whose support was removed falls as
                                an entity and breaks
fixtures/                       the two registered Fixture definitions (fresnel, moving light)
data/                           data generators; output committed under src/generated/resources
                                (recipes, loot tables, item models, tags, en_us lang, and the
                                Patchouli book via PatchouliProvider)
compat/top/                     The One Probe integration, registered through IMC
util/                           CapabilityStorageProvider, FixtureUtil
```

**Dependencies:** The One Probe (compile, optional at runtime, integration behind IMC), Patchouli
(compile; the book is data, so the mod runs without Patchouli installed), PatchouliProvider (datagen
only), artnet4j (shaded into the jar; GPL-3.0, see `NOTICE`). All come from mavens named in
`build.gradle`.

**Resources:** hand-written assets live in `src/main/resources`; everything under
`src/generated/resources` is datagen output and should be regenerated with `runData` rather than
edited by hand. `src/main/resources/META-INF/accesstransformer.cfg` opens two vanilla members
(`WorldRenderer.drawShape`, `FallingBlockEntity.fallTile`).

## Conventions & gotchas

### Build

- **Dependency POMs with `_mapped_` coordinates crash configuration.** The One Probe's published
  POM declares Forge and JEI with `_mapped_official_1.16.5` versions. ForgeGradle 6's
  deobfuscating repository tries to remap those again and recurses until the daemon overflows its
  stack, and it does so while resolving the origin artifact, before `transitive = false` on our
  declaration applies. `build.gradle` strips the declared dependencies with a component metadata
  rule. If a new dependency produces `StackOverflowError` / `Could not initialize class
  net.minecraftforge.artifactural.api.artifact.Internal` at configuration time, that is the cause;
  add it to the same rule.
- **Dev runs need ModLauncher 8.1.3.** Forge 1.16.4 ships ModLauncher 8.0.9, which calls a
  `ManifestEntryVerifier` constructor Java 8u321+ removed; `runClient`/`runServer` then die at
  launch with `NoSuchMethodError: sun.security.util.ManifestEntryVerifier.<init>`. `build.gradle`
  puts `cpw.mods:modlauncher:8.1.3` on the runtime classpath (`runtimeOnly`) so Gradle's conflict
  resolution replaces 8.0.9 for dev runs only. The built jar is unaffected. Do not remove it when
  tidying dependencies.
- **`clean build` in one invocation fails.** ForgeGradle resolves the Minecraft dependency during
  configuration and `clean` then deletes what it resolved. Run them separately; CI only ever runs
  `build`.
- **`gradlew` must stay mode `100755`** (upstream committed it as `100644`, which fails every
  `run: ./gradlew` step on the Ubuntu runners). `.gitattributes` pins it to LF.
- **Snapshot mappings.** The code is written against MCP `snapshot_20201028-1.16.3` names
  (`func_`/`field_` in the AT, `getTileEntity`, `isRemote` in code). Do not switch the channel to
  `official` casually: every source file would need renaming.
- **Dev-run libraries.** Non-mod jars are only visible to ModLauncher if they are on the
  `minecraft_classpath` token, which is why `build.gradle` has the `library` configuration and the
  `lazyToken`. Put any new plain-Java library on `library` (or `shade`, which extends it), not
  bare `implementation`.
- **`org.gradle.daemon=false`** is inherited from upstream and kept; ForgeGradle's memory use
  makes a lingering daemon more trouble than the startup cost.

### Versioning and CI

`build.gradle` resolves the mod version in this order:

1. `-PmodVersionOverride=...` or the `MOD_VERSION` environment variable
2. the release-shaped git tag on HEAD (`YYYY.MM.DD`, or `YYYY.MM.DD-pre.HHMM.<tz>+<sha>`), which
   CI creates immediately before building
3. `mod_version` in `gradle.properties` -- the upstream release this fork sits on

The resolved value becomes the manifest `Implementation-Version`, which `mods.toml` reads through
`${file.jarVersion}`. `upstream_version` in `gradle.properties` is printed into every release body.

**`printModVersion` / `printArchivesBaseName` / `printModName` / `printMinecraftVersion` /
`printUpstreamVersion` are a contract** with `.github/workflows/build-mod-release-pre-release-main.yml`.
Renaming one breaks the release build. `build.gradle` prints a JVM banner during configuration
that `-q` does not suppress, which is why the workflow reads those tasks with
`| tail -n 1 | xargs`. Don't "simplify" it.

Three workflows, matching the sibling Mica mods (see the header comment in each for the
fork-specific deltas):

- `test-mod-build-pr.yml` -- builds every pull request.
- `build-mod-release-pre-release-main.yml` -- on push to `1.12` or `1.16.3`, tags the commit and
  publishes a pre-release with checksums. `workflow_dispatch` with `release=true` cuts a full
  release (pick the branch in the dispatch dialog). The tag is created *before* the build, because
  the version resolution above reads it. Both branches share one tag namespace and one Releases
  page; the release name carries the Minecraft version, and `printMinecraftVersion` is what feeds
  it.
- `cleanup-mod-pre-releases.yml` -- prunes pre-releases past 90 days, keeping the newest 3 and
  anything with 5+ downloads. Its `workflow_run` trigger matches the release workflow by name, so
  those two strings must stay in sync.

### Fork hygiene

- Keep the diff against upstream small and legible. Where a fork-specific change is needed in an
  inherited file, the `CHANGED FROM UPSTREAM` notice (rule 1) is where the *why* goes; keep it
  specific enough that a future reader can tell whether the change is still needed.
- `.gitignore` and `.gitattributes` keep fork additions in a delimited block below the upstream
  content, for the same reason.
- `CHANGELOG-FORK.md` is ours; add entries under `## Unreleased`. Upstream had no changelog.
- No upstream issue/PR references anywhere that gets committed (rule 3).

## Planning docs

`docs/agent-plans/` is **gitignored** -- it holds implementation plans and agent working notes.
It is local scratch; nothing in it is authoritative. When a plan and the code disagree, **the code
wins**: verify by reading the source before believing a checkbox.

`docs/agent-plans/MASTER_PLAN.md` is the plan for the Forge 1.12.2 backport: the phases, the
1.16-vs-1.12 feasibility assessment, whether the 1.16 line stays maintained, and the triage of
upstream's open issues (which may be referenced by number *there*, and only there). Start a work
session by reading its STATUS section.
