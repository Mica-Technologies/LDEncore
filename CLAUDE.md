# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and any other coding agent when
working with code in this repository. It is the agent-facing companion to `README.md`; read both.

## Purpose

**LDEncore** is Mica Technologies' unofficial fork of
[Theatrical](https://github.com/theatricalmod/theatrical-forge) (Forge edition) by Rushmead and the
Theatrical Team. Theatrical adds live-events equipment -- fresnels, moving heads, dimmer racks,
DMX/Art-Net control, power and DMX cabling, truss -- to Minecraft. Mod id: `theatrical`.

**This branch (`1.12`, the GitHub default) is the Forge 1.12.2 port**, which is the reason the
fork exists. It is a translation of upstream's mature 1.16.4 code to 1.12.2 APIs, done bottom-up
and phase by phase; the plan and its current status live in `docs/agent-plans/MASTER_PLAN.md`
(gitignored, see *Planning docs*). Until a subsystem is ported it simply is not here.

**The `1.16.3` branch is the reference.** It holds the resurrected upstream baseline (Theatrical
0.7.2 for Minecraft 1.16.4 plus upstream's unreleased fixes) with a working ForgeGradle 6 build,
and its own `CLAUDE.md` describing that code. When porting, read the 1.16 file there:
`git show 1.16.3:src/main/java/dev/theatricalmod/theatrical/<path>`, or keep a worktree beside
this checkout (`git worktree add ../LDEncore-1.16 1.16.3`). The 1.16 line is frozen: it gets no
feature work, only what is needed to keep it building.

Other inherited upstream branches (`1.15.1`, `1.18.2`, `feature/cccompat`, `new-cables`,
`working-audio-thing`) are untouched. This branch's own pre-2026 history is upstream's 2019
prototype of the mod (Albedo lighting, JSON custom fixtures, ComputerCraft integration); it does
not build and the port is not based on it, but its 1.12-native pieces (TESR, GUI, packet handler,
Albedo hook) are worth reading when the corresponding phase comes up.

Build system is the **GregTechCEu buildscripts** (RetroFuturaGradle) -- the same as the sibling
Mica 1.12.2 mods (RCMC, CSM, LDOG, LDFAWE). `build.gradle` is the unmodified buildscript
(`//version:` header); project settings go in `buildscript.properties`, extra Gradle logic in
`addon.gradle`, dependencies in `dependencies.gradle`, repositories in `repositories.gradle`.

## Rules that are not negotiable

### 1. Mark every changed upstream file (Apache-2.0 §4(b))

Theatrical is Apache-2.0, and section 4(b) of that licence requires that **any modified file
carries a prominent notice stating that it was changed**. This is a licence obligation, not a
style preference. On this branch almost every Java file is a *port* of an upstream 1.16 file:
written by us, after theirs, to a different API. Treat those as modified upstream files and mark
them.

- **Ported Java sources** (a 1.12.2 counterpart of an upstream 1.16 file) -- comment block at
  the very top, above the `package` line:

  ```java
  /*
   * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
   *
   * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
   * <path/File.java> (Theatrical Team, Apache License 2.0); the upstream file targets
   * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
   *
   * CHANGED FROM UPSTREAM: <what differs beyond the mechanical API translation, one line
   * per behavioural change, e.g. "raytraces only while powered">
   */
  ```

- **Fork-authored Java files with no upstream counterpart** -- a shorter header: the first
  line plus "Fork-authored file, Apache License 2.0." No `CHANGED FROM UPSTREAM` line.

- **Gradle, properties, TOML, YAML, shell, `.gitignore`-style files** -- a `# CHANGED FROM
  UPSTREAM (Mica Technologies fork, LDEncore): ...` (or `//` for Gradle) comment at the top when
  the file has an upstream counterpart; "Fork-authored file" when it does not.

- **Files that cannot carry a comment** (JSON assets, PNG textures, the artnet4j jar) -- record
  the file path and what changed in `CHANGELOG-FORK.md` under *Modified files without comment
  support* for the current release section. Assets copied from upstream unchanged need no entry;
  assets converted to the 1.12 format (texture paths, blockstate format, recipes) do.

A useful test before committing: every new or changed file under `src/` should have one of the
headers above, and `git diff --name-only 69fdc47..HEAD` (the commit that started this line)
should list only files that carry a notice or appear in the changelog list.

### 2. Keep the README's "What this fork changes" section current

`README.md` has a *What this fork changes* section. It is the human-readable summary of how these
builds differ from Theatrical, and it exists because we want to be plainly transparent about what
we ship under a name that is not ours. When a change alters behaviour, adds or removes a feature,
changes a dependency, or changes what is in the jar, update that section in the same commit,
alongside the per-release entry in `CHANGELOG-FORK.md`. The two are not redundant: the changelog
is the ledger, the README section is the current-state summary. Ported features are listed in the
changelog as they land, and the README says so.

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
- **Never push** to any remote unless the user has granted it in the current session. The user
  reviews and pushes. Pushing this branch publishes a pre-release, so even with permission,
  push only states that build and load.
- Commit identity for this repo is `mica-alex` (the global default). Verify with
  `git config user.email` before committing.
- Never `git add -f` anything under `docs/agent-plans/`.

## Build commands

Set `JAVA_HOME` to a **JDK 17-22** install before each `./gradlew` invocation (**21 is what CI
uses**; RetroFuturaGradle wants the Gradle process on 21+, and Gradle 8.9 supports up to 22). The
compiler and mod code target **Java 8** via Jabel; only the JVM that runs Gradle changes. Gradle
provisions the Java 8 runtime for `runClient`/`runServer` itself.

On this machine the JDKs are IntelliJ-managed under `~/.jdks/` (Windows:
`C:\Users\<username>\.jdks\`): `azul-17.0.19` works; `java` is **not** on `PATH`.

```bash
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew build       # compile + jar -> build/libs/
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew runClient   # dev client (Java 8)
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew runServer   # dev dedicated server
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.19" ./gradlew clean
```

There are no unit tests yet (`enableJUnit = false`). Verification means building and launching;
a green compile says nothing about whether the mod loads. After any change to registration,
capabilities, rendering or resources, run the client and read `run/logs/latest.log` for missing
models/textures and registry errors -- the `mod-verify` skill is the ritual for this repo.

`.github/scripts/server-smoke-test.sh` (added when the port has enough to load) boots a
dedicated server and asserts `Done (`; until then `./gradlew runServer` with `run/eula.txt`
set to `eula=true` does the same by hand.

## IntelliJ

Import the Gradle project; the buildscript's idea-ext integration generates the run
configurations ("1. Setup Workspace", "2. Run Client", "3. Run Server", "7. Build Jars", and the
Java 17/21 client variants). **Nothing under `.idea/` is versioned on this branch.** The
numbered configs the `1.16.3` branch commits by hand are the same shape; here they are generated.

## Architecture (as ported so far)

Package root is `dev.theatricalmod.theatrical` -- **upstream's namespace, deliberately kept**, as
is the mod id `theatrical`. The id is a compatibility contract with saves and packs; keeping the
package makes every file map 1:1 onto its 1.16 original. Do not rebrand either.

```
TheatricalMod.java      @Mod entry point (1.12.2 lifecycle: preInit / init / postInit).
                        Registers the capabilities, attaches the per-world DMX and socapex
                        networks (server worlds only) and ticks them at the end of every
                        world tick. Network and Art-Net wiring are added as they are ported.
Tags.java               generated by the buildscript at build time (mod id, name, version);
                        never edit, never commit (it lives under build/).
api/                    the mod's API (also published as the -api jar via apiPackage):
                          CableType / CableSide / IAcceptsCable / ICable / ISupport
                          dmx/DMXUniverse            512 bytes + a UUID
                          capabilities/dmx/          DMXProvider walks DMX cables from its
                                                     block and pushes the universe into every
                                                     DMXReceiver it finds; WorldDMXNetwork is
                                                     the per-world provider list, re-walked
                                                     when something sets refresh
                          capabilities/socapex/      the same shape for dimmed power: a
                                                     SocapexProvider (dimmer rack) patches its
                                                     six channels onto receiver sockets;
                                                     WorldSocapexNetwork lists providers
                          capabilities/power/        ITheatricalPowerStorage -- the mod's own
                                                     "mains", deliberately not Forge Energy
                          fixtures/                  Fixture (a Forge registry entry: models,
                                                     pivots, beam, power, DMX footprint),
                                                     FixtureType, HangableType, GelType,
                                                     the IFixture/IFixtureModelProvider
                                                     contracts a fixture tile fulfils
fixtures/               the two built-in Fixture definitions and TheatricalFixtures, which
                        creates the registry in RegistryEvent.NewRegistry and registers them
block/                  TheatricalBlocks (eager instances + Register<Block>, also registers the
                        tiles); BlockHangable (hangs from an ISupport, falls when it goes,
                        lands BROKEN; the flag rides on the item's NBT); light/ (BlockLight ->
                        generic / intelligent / moving light, BlockIlluminator = the invisible
                        light-level block the beam places); cables/ (BlockCable implements
                        api.ICable; six connection props computed in getActualState); power/
                        (dimmer rack, socapex distro); control/ (lighting desk); interfaces/
                        (Art-Net, DMX->redstone); rigging/ (truss, IWB); test/
tiles/                  TileEntityTheatricalBase (readNBT/getNBT drive both save and sync);
                        lights/TileEntityFixture is the heart: every 5 ticks, if shouldTrace(),
                        ray-trace along pan/tilt and keep a BlockIlluminator where the beam
                        lands; power/ (dimmed cable pushes to neighbours; dimmer rack: DMX in,
                        FE in, six socapex channels out; distro: five sockets); control/ (the
                        desk: 12 faders, grand master, cues, fades); interfaces/
items/                  TheatricalItems (ItemBlocks + positioner, wrench, ingredients; cog is
                        oredict gearIron); ItemPositioner links to a generic light by NBT
entity/                 FallingLightEntity (EntityFallingBlock that lands broken)
client/                 TheatricalClient (@SidedProxy; item models, illuminator state mapper,
                        client-side DMX universe updates, and it binds the renderers);
                        TileEntityFixtureRenderer (the fixture's static/pan/tilt parts and its
                        beam), tile/TileEntityRendererBasicLightingDesk (the desk's physical
                        faders, their slots and its two labels), FallingLightRenderer;
                        model/FixtureModels loads, bakes and caches the fixture part models and
                        registers their textures, because 1.12 has no addSpecialModel and the
                        model manager cannot see a model that no blockstate or item model
                        reaches; gui/TheatricalGuiHandler (one id per
                        screen; getClientGuiElement delegates to the proxy so a dedicated
                        server never loads a GuiContainer subclass); gui/container/ (one
                        Container per screen, each with a real canInteractWith); gui/screen/
                        (ScreenDMXAddressBase is shared by the moving light and the redstone
                        interface; the rest are one per block); gui/widgets/ (ButtonFader,
                        ButtonPlug, ButtonSocket)
network/                TheatricalNetworkHandler registers the ten messages on one
                        SimpleNetworkWrapper, keeping upstream's discriminator ids, and sends
                        provider universes only to players tracking the chunk. PacketUtil is
                        the gate every server-bound packet goes through: it checks the chunk is
                        loaded, the tile is the expected type and the sender is within reach,
                        then runs the change on the server thread. Never touch the world
                        directly from a message handler.
util/                   CapabilityStorageProvider (delegates to INBTSerializable), FixtureUtil
TheatricalConfigHandler @Config: fixtures.emitLight / consumePower, rendering.lightBeamOpacity
```

1.12 block-state rules the port follows: every property set must fit 4 metadata bits
(`getStateFromMeta`/`getMetaFromState`); anything that does not (cable connections) is
computed in `getActualState` and left out of metadata. Blockstate JSON uses Forge's
`forge_marker` format so partial property lists work; cables use vanilla multipart.
Blockstate model references have no `block/` prefix on 1.12; model `parent` and texture
references do (`block/...`, `blocks/...`).

Two api-package rules the port introduced, both so `api/` depends on nothing else:

- Cable blocks implement `api.ICable`; the DMX and socapex walkers only ever see that
  interface. Do not reintroduce `instanceof BlockCable` in `api/`.
- `FixtureType` does not know the tile classes. The tiles package calls
  `FixtureType.X.setTileFactory(...)` during registration; `getTileClass()` throws if that was
  never done, which is the intended failure mode.

The target layout is upstream's 1.16 layout, translated: `api/` (capabilities, DMX universe,
fixture registry), `block/`, `tiles/`, `items/`, `network/`, `client/` (renderers, GUIs),
`artnet/`, `fixtures/`, `compat/top/`, `util/`. See the `1.16.3` branch's `CLAUDE.md` for what
each of those does upstream, and the plan for the order they are ported in.

**Dependencies today:** artnet4j only, shaded and relocated (`shadowImplementation` in
`dependencies.gradle`; GPL-3.0, see `NOTICE`). JEI and The One Probe are on the dev classpath via
`includeCommonDevEnvMods` for testing. Patchouli and the TOP compat are added in their phases.

## Conventions & gotchas

### Build

- **`build.gradle` is the buildscript; do not edit it.** Customisation goes in `addon.gradle`,
  `buildscript.properties`, `dependencies.gradle`, `repositories.gradle`. The auto-update check is
  disabled in `gradle.properties` because it fetches GitHub during configuration and has failed
  CI on sibling mods; `./gradlew updateBuildScript` refreshes it deliberately.
- **`apiPackage` must point at a directory that exists.** It is empty until the `api/` package is
  ported (Phase 2); setting it earlier fails configuration with "Could not resolve apiPackage".
- **Jar naming:** `includeMCVersionJar = true`, so the jar is `LDEncore-1.12.2-<version>.jar`
  and `project.version` is `1.12.2-<version>`. That is deliberate: it keeps 1.12.2 jars visibly
  distinct from the 1.16 line's `LDEncore-<version>-forge-mc1.16.jar` on the shared Releases page.
- **Versioning:** `modVersion` is empty, so the buildscript derives it from git via the
  palantir git-version plugin (`git describe --tags --first-parent`). CI tags HEAD immediately
  before building, so release builds get exactly the tag. Local builds off an untagged commit get
  the short sha plus `.dirty` when the tree has changes (e.g. `1.12.2-69fdc47.dirty`); the
  `1.16.3` line's tags are on that branch's first-parent line only, so they are never picked up
  here. There is no `mod_version` fallback (that is a `1.16.3` thing).
- **artnet4j is relocated** to `dev.theatricalmod.theatrical.shadow.ch.bildspur.artnet` in the
  published jar. In dev it is on the classpath un-relocated. Never reference the relocated name
  in source; never turn `minimizeShadowedDependencies` on without checking that the Art-Net client
  still works from a built jar (the minimiser cannot see reflective/threaded use).
- **`Tags` is generated.** `generateGradleTokenClass` writes `dev.theatricalmod.theatrical.Tags`
  with `MODID`, `MODNAME`, `VERSION`; `@Mod` reads it. Do not create a `Tags.java` by hand.
- **Manifest guard:** `addon.gradle` fails `reobfJar` if the published jar declares a
  `TweakClass`, because FML 1.12.2 silently drops such jars from mod discovery. A shaded
  dependency's manifest is the usual way that sneaks in.
- **Modern Java syntax is on** (`enableModernJavaSyntax`, Jabel): `var`, switch expressions,
  records and text blocks compile to Java 8 bytecode. The *library* is still Java 8 -- no
  `List.of`, `Optional.isEmpty`, `String.isBlank`, streams `toList()`.

### Testing in a dev client

- **Never run a Gradle build while a dev client is open.** The build empties
  `build/classes/java/main`, and any class the running client happens to load during that window
  fails to resolve. The JVM caches that failure for the life of the process, so the class throws
  `NoClassDefFoundError` from then on even once the file is back on disk. The symptom is a
  feature that half works -- a screen that draws but whose buttons do nothing -- with no
  exception in the log, and it survives further rebuilds. Always stop the client first, and if a
  client is behaving inexplicably, restart it before believing anything it tells you.
- **Verify each phase in a running client, not just a green build.** Most of the bugs found in
  this port compiled perfectly. Right-click the block, change the value, close the screen, reopen
  it, and check the value came back -- that exercises the packet, the server-side change, the
  save and the sync in one pass.
- **The MCMCP orchestrator drives this instance** as `LDEncore Dev` on ports 25614/25615 (client
  and server), configured in `run/config/mcmcp.cfg`. `run/options.txt` mutes the client so
  background testing is silent. Screen coordinates from `client_gui_widgets` are in scaled GUI
  units; `client_gui_click_at` takes display pixels, which at this scale factor is double.

### Rendering

- **Fixture blocks render nothing themselves** (`ENTITYBLOCK_ANIMATED`); everything visible comes
  from the tile-entity renderer. Their blockstate JSON still matters, because it is what puts the
  fixture textures in the atlas.
- **Fixture part models are not reachable from any blockstate**, so they are baked by hand in
  `FixtureModels` on `ModelBakeEvent` and their textures registered on `TextureStitchEvent.Pre`.
  A new fixture needs its textures listed in its `Fixture` constructor or they will not stitch.
- **Draw parts with `renderModelBrightnessColor`**, which emits a model in its own local
  coordinates, so the pan and tilt rotations compose with it. `renderModel` bakes the block
  position into the vertices and cannot be combined with GL transforms. Because that vertex
  format carries no light value, set the lightmap by hand from `world.getCombinedLight`.
- **A generic light is not a DMX receiver.** It is dimmed by a dimmer rack over socapex; only the
  intelligent/moving fixture takes DMX. Wiring a DMX source to a fresnel and expecting a beam is
  a category error, not a bug.
- **A fixture with `consumePower` on needs a power source to light at all.** The dev world has
  none, so set `fixtures.consumePower=false` in `run/config/theatrical.cfg` when testing beams.
  The config is read at startup, so the client has to be restarted after editing it.

### Porting

- Port one subsystem at a time, in dependency order, and keep the build green at every commit.
  The 1.16 file is the specification; the 2019 file in this branch's history is a hint for the
  1.12 API, not a source of behaviour.
- MCP names: the buildscript uses RetroFuturaGradle's default 1.12.2 mappings (the `stable`
  channel; upstream's 2019 branch used `snapshot_20180814`, so a few names differ from that
  code); expect `world.isRemote`, `getTileEntity`, `NBTTagCompound`, `ITickable`, `EnumFacing`,
  `AxisAlignedBB`.
- Client-only classes must never be reachable from common code on a dedicated server; upstream's
  `DistExecutor` split becomes `@SidedProxy` here. Test with `runServer`, not just `runClient`.
- Fix the known upstream bugs as their code is ported (the plan lists them), and describe each in
  `CHANGELOG-FORK.md` in our own words.

### Versioning and CI

**`printModVersion` / `printArchivesBaseName` / `printModName` / `printMinecraftVersion` /
`printUpstreamVersion` in `addon.gradle` are a contract** with
`.github/workflows/build-mod-release-pre-release-main.yml`. Renaming one breaks the release
build. The workflow reads the last line of each (`| tail -n 1 | xargs`).

Three workflows, matching the sibling Mica mods (see the header comment in each for the
fork-specific deltas):

- `test-mod-build-pr.yml` -- builds every pull request on JDK 21.
- `build-mod-release-pre-release-main.yml` -- on push to `1.12` or `1.16.3`, tags the commit and
  publishes a pre-release with checksums. `workflow_dispatch` with `release=true` cuts a full
  release (pick the branch in the dispatch dialog). The tag is created *before* the build, because
  the version resolution reads it. Both branches share one tag namespace and one Releases page.
- `cleanup-mod-pre-releases.yml` -- prunes pre-releases past 90 days, keeping the newest 3 and
  anything with 5+ downloads. Its `workflow_run` trigger matches the release workflow by name, so
  those two strings must stay in sync. GitHub only fires `workflow_run` and `workflow_dispatch`
  from workflow files on the default branch, which is this one.

The `1.16.3` copy of the release and PR workflows installs JDK 8 + 17 for ForgeGradle 6; this
branch's installs JDK 21 for RetroFuturaGradle. Keep them in step otherwise.

### Fork hygiene

- `.gitignore` and `.gitattributes` keep fork additions in a delimited block below the upstream
  content.
- `CHANGELOG-FORK.md` is ours; add entries under the 1.12.2 line's `## Unreleased`. Upstream had
  no changelog.
- No upstream issue/PR references anywhere that gets committed (rule 3).

## Planning docs

`docs/agent-plans/` is **gitignored** -- it holds implementation plans and agent working notes.
It is local scratch; nothing in it is authoritative. When a plan and the code disagree, **the code
wins**: verify by reading the source before believing a checkbox.

`docs/agent-plans/MASTER_PLAN.md` is the plan for the Forge 1.12.2 port: the phases, the
1.16-vs-1.12 feasibility assessment, the decision to freeze 1.16, and the triage of upstream's
open issues (which may be referenced by number *there*, and only there). Start a work session by
reading its STATUS section.
