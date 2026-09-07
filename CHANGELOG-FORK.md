# LDEncore fork changelog

Changes made by **Mica Technologies** in this unofficial fork of
[Theatrical](https://github.com/theatricalmod/theatrical-forge). Upstream never kept a changelog
file, so this is the only one in the repository; its scope is fork changes only.

Two things to know about the versions below:

- Headings are the **date-based release tags this repo publishes** (`YYYY.MM.DD`), not upstream's
  semver. The two schemes are unrelated.
- Each release records the **upstream release it is based on**. That is the same value as
  `upstream_version` in `gradle.properties`, which the release workflow prints in every GitHub
  release body.

**Licence note.** Theatrical is Apache-2.0. Every inherited file this fork modifies carries a
"CHANGED FROM UPSTREAM" notice at the top of the file. Files whose format cannot carry a comment
(JSON, images, jars) are recorded in the *Modified files without comment support* list under the
release that changed them, so the section 4(b) notice requirement is still met.

The file is named `CHANGELOG-FORK.md` rather than `CHANGELOG.md` to match the sibling Mica forks,
where the bare name is reserved for an upstream mirror.

Two lines of the mod are published from this repository and both are recorded here: the
**1.12.2 port** (the `1.12` branch, jars `LDEncore-1.12.2-<version>.jar`) and the frozen
**1.16.4 baseline** (the `1.16.3` branch, jars `LDEncore-<version>-forge-mc1.16.jar`). Each
branch carries the canonical copy of its own section.

---

# 1.12.2 line

## Unreleased

Translated from upstream **Theatrical 0.7.2** (the final state of the archived `1.16.3` branch).
**Port in progress: these builds are a skeleton that loads and does nothing.** Features are
listed below as they are ported.

### Added

- **The in-game guide, The One Probe overlay and three translations, ported.** The Patchouli
  guide is back, with both categories, all seven entries and their crafting pages; the in-world
  overlay shows a block's power, DMX address and channels, and its socapex channels; and the mod
  now speaks German, French and Hebrew as well as English.
  - The guide is titled *LDEncore Guide*, in keeping with the rest of the fork's naming.
  - The overlay is built differently, in our own words. Upstream had each block implement an
    interface whose method signature named The One Probe's own types, so ordinary blocks carried
    a reference to a mod that need not be installed. The lines are now built from the block's
    capabilities inside the compat package, which is only loaded when The One Probe is present,
    and a block earns the overlay by having the capability rather than by remembering to
    implement an interface. It also shows the DMX start address, which is the number a player
    has to match on their desk and which upstream left out, and a socapex receiver's channels,
    which upstream showed nothing for.
  - Both Patchouli and The One Probe stay optional. Neither is bundled and neither is required;
    a pack without them simply has no guide and no overlay.
  - **Translations.** German by Nicolas Pfeifer (nicode3141), French by SwiTeK and Hebrew by
    CrazyFish159, all contributed to upstream Theatrical under the Apache License 2.0 and never
    released there. This fork adopts them, converted to 1.12's `.lang` format and key names. The
    creative tab and the guide keep the fork's name in every language, being a name rather than
    a word to translate, and any string a contribution does not cover falls back to English.
  - Verified in a dev client: the guide opens and both its crafting pages resolve their recipes,
    the overlay reads "DMX address: 17 (7 channels)" off a moving light, and the game in German
    names the blocks in German.
- **Art-Net, ported.** An Art-Net interface block now receives real Art-Net from lighting
  software on the player's own machine and pushes it down the DMX cables in-game. The player who
  owns the interface reads the feed on their client and forwards it; the server accepts it only
  from that player, and only if they are an operator, since it is the one input that comes from
  outside the game.
  - The Art-Net clients are only created on the side that needs them. Upstream built the manager
    on the common proxy, so a dedicated server opened an Art-Net receive socket that nothing ever
    read from.
  - An address that cannot be bound no longer poisons itself. Upstream stored a client before
    starting it, so once a bind failed every later read on that address got a dead client back
    for the rest of the session -- and an interface is often configured before the network it
    names exists. The failure is now logged once and the address can be retried. The client map
    is synchronised too, where upstream's plain map was touched by both the network thread and
    the tile tick.
  - Sockets are closed when the game stops rather than only when a server stops, so leaving a
    single-player world back to the main menu no longer leaks one per address.
  - An unchanged universe is sent once a second instead of ten times, while a changing one still
    goes at the full rate, so a live cue does not lag.
  - **A real lighting console now works.** Upstream bound the socket to the single address typed
    into the interface, and a socket bound to one address does not receive packets sent to the
    broadcast address on most systems -- which is exactly how a physical desk emits Art-Net. A
    blank address, `0.0.0.0` or `all` now listens on every local interface, and that is the
    default for a newly placed interface; the address can still be set to a single interface,
    and the default is configurable.
  - **The screen says whether anything is arriving.** It now shows whether a socket is actually
    listening on the configured address and how long ago a universe last came in, alongside who
    owns the interface. Upstream showed none of this, and "is it reaching the game at all" is
    the first question anyone wiring up a console has.
  - Upstream's `ArtNetThread` is not carried over: nothing referenced it, its running flag was a
    constant false, and the Art-Net client already runs its own receive thread.
  - A DMX source no longer caches "nothing attached" forever. Upstream recorded the devices on
    a source's cable run the first time it looked and rebuilt that list only when something
    disturbed the network, so a source that looked before its neighbours had finished loading
    could stay dead until a player broke and replaced a block nearby. An empty result is now
    treated as "nothing found yet" and re-checked once a second, while a list with anything in
    it is still cached exactly as before. This is hardening against a race we reasoned about
    rather than a fix for a failure we reproduced.
  - Verified in a dev client against real Art-Net packets: a universe sent to the machine
    reaches the interface, is forwarded to the server, and lights a moving light wired to it,
    with a second interface on a different universe correctly seeing nothing. Broadcast Art-Net
    to 255.255.255.255 is received too, and the screen reports "Receiving Art-Net" while it
    flows.
- **Rendering, ported.** Fixtures are visible again: the fixture renderer draws a light's static,
  pan and tilt parts with its live pan and tilt, hanging correctly from a truss or an internally
  wired bar and flipping when a moving light is hung upside down, and casts the light beam,
  coloured and faded by the fixture's own DMX intensity and the beam-opacity config. The lighting
  desk's physical faders and their slots are drawn on the desk surface along with its step and
  mode, and a light knocked off its bar renders as it falls.
  - One structural difference from upstream, in our own words. On 1.16 a fixture's part models
    are registered for baking with a single call and then fetched from the model manager; 1.12
    has no such call, and the model manager only knows models reachable from a blockstate or an
    item model, which a fixture's parts are not. The fork loads and bakes them itself and hands
    them to the renderers, and registers their textures at stitch time. The parts are then drawn
    in their own local coordinates so that the pan and tilt rotations compose with them, where
    upstream's draw call bakes the block position into the vertices.
  - Two upstream bugs are fixed, in our own words. The lighting desk's physical faders never
    moved: the travel was computed in integer arithmetic, so every fader below full evaluated to
    no movement at all. And a DMX universe arriving at a client was stored but never pushed into
    the fixtures on that cable run, so a fixture's brightness and colour only ever changed for
    players who had the block's screen open. Both now behave as the desk and the lights obviously
    should.
  - The client no longer re-walks a provider's whole DMX cable run on every universe packet.
    Upstream discarded the cached device list each time, which for a provider sending every tick
    meant twenty full network walks a second, on the render thread. The list is rebuilt on a
    timer instead, so rewiring still shows up promptly.
- **Networking and every screen, ported.** All ten client/server packets and the six screens
  behind them: the DMX address screen shared by the moving light and the DMX-redstone
  interface, the Art-Net interface screen, the generic fixture's pan/tilt sliders, the dimmer
  rack's patch panel, and the basic lighting desk with its twelve faders, grand master, cue
  list and fade times. Right-clicking a block now opens its interface again.
  - The packet layer is hardened, in our own words. Every server-bound packet now resolves its
    block through one shared helper that checks the chunk is loaded, the tile is of the type
    the packet expects, and the sender is within reach, then runs the change on the server
    thread. Upstream took the position on the wire at face value and touched the world from
    the network thread, so a crafted or stale packet could edit a block the sender was nowhere
    near, or trip a concurrent-modification crash. Payloads are bounds-checked and clamped as
    well: a fader index outside the desk's range is dropped rather than indexing the array, and
    a zero or negative fade time no longer reaches the per-tick division that threw.
  - DMX universe updates are sent only to the players tracking that chunk. Upstream broadcast
    every provider's universe to every player in the dimension.
  - Two upstream bugs are fixed, in our own words. Patching a dimmer rack silently did nothing
    until the rack was energised, because the rack only looked for the distros on its socapex
    run after its power check, so an unpowered rack believed nothing was connected and refused
    every patch; the scan now happens first, which is when players actually patch. And the
    dimmer rack screen walked the whole socapex cable run once per rendered frame; the result
    is now reused for a second.
  - Two screens differ structurally from upstream: the moving light's and the redstone
    interface's DMX address screens were identical, and are now one shared base; and the
    generic fixture's sliders send at most one packet per tick instead of one per pixel of
    drag.
  - Verified in a dev client: each of the six screens opens, accepts a change and shows it
    again after being closed and reopened. On the lighting desk, moving faders and pressing
    Go records a cue, and the step and mode buttons move the desk between cues and between
    program and run mode.
- **Every block, item and tile entity, ported.** Truss, IWB, generic light, moving light,
  the four cables, dimmer rack, socapex distro, Art-Net interface, DMX-redstone interface,
  basic lighting desk, the illuminator light block, the dev-only test DMX block, the
  positioner and wrench, and the four crafting ingredients, with their tiles, the falling
  light entity, the config options (one `config/theatrical.cfg` with upstream's two
  categories), and all assets converted to the 1.12 layout (blockstates in Forge's format,
  textures under `blocks/`/`items/`, recipes using ore-dictionary ingredients, `en_us.lang`).
  The dimmed-power, socapex and DMX networks all run on the server.
  - Bug fixes on the way, in our own words: the dimmer rack read its DMX channels at the
    wrong offset and so put out nothing at any DMX address other than 0; the remote
    positioner aimed generic lights the wrong way on two of the four facings; dimmed power
    cables and cable tiles now save their state (a reloaded cable came back empty and did
    not flag its network); the network walkers only follow cable arms that actually connect
    (upstream's check always answered yes).
  - The dev-only test DMX block now accepts cable connections, so a cable run visibly
    attaches to it. Upstream's did not, which left the debugging block looking unwired even
    while it was driving the universe.
  - Fork-authored asset: a placeholder wrench texture. Upstream's wrench referenced a texture
    that was never shipped.
  - Verified in a dev client: every block places and renders, and a test DMX block feeding a
    cable run into a DMX-redstone interface lights redstone lamps, so the DMX network walker,
    the receiver and the redstone output all work end to end.
- **The API layer, ported.** Everything under `api/`: the DMX universe, the DMX provider and
  receiver capabilities and the per-world DMX network, the socapex provider/receiver
  capabilities and the per-world socapex network, the mod's own power capability, cable
  types and sides, and the fixture registry with the two built-in fixtures (fresnel and
  moving head). The capabilities register and the world networks attach and tick on a
  dedicated server. No blocks use any of it yet.
  - Two upstream bugs are fixed on the way, in our own words: a DMX receiver addressed within
    the last few channels of a universe no longer throws when values arrive (the copy is
    clamped to the end of the universe), and a receiver's last channel can now actually be
    written by `updateChannel` (an off-by-one refused it).
  - Two ports differ structurally from upstream so the API package stands alone: the network
    walkers follow cables through a new `ICable` interface instead of the cable block class,
    and fixture types have their tile-entity factory registered by the tiles package instead
    of naming the tile classes themselves. Baked-model handles are no longer stored on the
    common `Fixture` class (a client-only type on a dedicated server).
  - The world network capability objects are now the objects that get ticked; upstream
    attached one instance and ticked a different, default-constructed one.
  - The DMX receiver saves its start address and channel count itself.
- **The 1.12.2 line itself.** The branch now builds with the GregTechCEu buildscripts
  (RetroFuturaGradle, Gradle on JDK 21, mod on Java 8 via Jabel) like the sibling Mica 1.12.2
  mods, with the same date-tag versioning and GitHub Actions workflows as the 1.16 line. The
  mod registers on Forge 1.12.2 and logs its version; nothing else is ported yet.
- `mcmod.info` identifies the mod as LDEncore, credits Theatrical/Rushmead, and links to this
  repository.

### Changed

- **artnet4j is shaded and relocated** (`dev.theatricalmod.theatrical.shadow.ch.bildspur.artnet`)
  rather than merged into the jar as-is, so it cannot clash with another mod bundling the same
  library.

### Modified files without comment support

- `libs/artnet4j-0.6.1.jar` -- unchanged upstream file, now consumed through `shadowImplementation`
  and relocated at build time.
- `assets/theatrical/blockstates/*.json` -- rewritten in Forge's 1.12 blockstate format (cables keep
  upstream's multipart form); model references lose the `block/` prefix 1.12 does not use.
- `assets/theatrical/models/block/**/*.json` -- texture references moved to 1.12's `blocks/`
  folder; Blockbench's `credit`/`groups` metadata dropped; `lighting_console.json` had its UVs
  rescaled because 1.12 does not read `texture_size`.
- `assets/theatrical/models/item/*.json` -- texture references moved to `items/`; the Patchouli
  guide and the unused bundled-cable models are not carried over.
- `assets/theatrical/textures/**` -- moved from `block/` and `item/` to `blocks/` and `items/`;
  the `.psd` source file upstream shipped inside the jar is not carried over; `items/wrench.png`
  is new (fork-authored placeholder).
- `assets/theatrical/recipes/*.json` -- converted from 1.16 data-pack recipes: tag ingredients
  became ore-dictionary entries, coloured wool became `minecraft:wool` with metadata, the empty
  pattern row in the lighting desk recipe became a blank row.
- `assets/theatrical/lang/en_us.lang` -- generated from upstream's `en_us.json`, plus a name
  for the illuminator block, which upstream never gave one and which therefore showed its raw
  translation key in any overlay that named it.

---

# 1.16.4 line

## Unreleased

Based on upstream **Theatrical 0.7.2** (the final state of the archived `1.16.3` branch,
Minecraft 1.16.4 / Forge 35.1.37). No gameplay code has been changed yet.

### Changed

- **Build system rewritten** so the mod can be built again. Upstream used ForgeGradle 3 on Gradle
  4.9 and depended on jcenter and a The One Probe maven that no longer exist. The fork moves to
  ForgeGradle 6 on Gradle 8 (Java 17 to run Gradle, Java 8 toolchain for the mod), pulls The One
  Probe from McJty's maven and Patchouli/PatchouliProvider from BlameJared's, and drops the
  Powah/Lollipop dev-only runtime mods whose CurseForge file pins no longer resolve. Dev runs
  get ModLauncher 8.1.3 on their classpath so they launch on current Java 8 builds (the jar itself
  is unaffected), and the dev server runs headless.
  (`build.gradle`, `gradle.properties`, `settings.gradle`, `gradle/wrapper/*`, `gradlew`,
  `gradlew.bat`)
- **Versioning** now comes from the git tag CI creates (`YYYY.MM.DD`), falling back to the
  upstream base version for local builds. Jars are named `LDEncore-<version>.jar`.
- **Mod metadata** (`src/main/resources/META-INF/mods.toml`) now identifies the mod as LDEncore in
  the in-game mod list, credits Theatrical/Rushmead, and points the issue tracker and display URL
  at this repository instead of upstream's, so players of these builds are not sent to the
  archived upstream tracker.

### Added

- Mica Technologies repository scaffolding: `README.md` rewritten for the fork, `CLAUDE.md`,
  `NOTICE`, this changelog, `.editorconfig`, `.gitattributes`, fork additions in `.gitignore`,
  shared IntelliJ run configurations under `.idea/runConfigurations/`, and the standard GitHub
  Actions workflows (PR build check, tagged release/pre-release publishing, pre-release pruning).

### Modified files without comment support

- `gradle/wrapper/gradle-wrapper.jar` -- replaced with the Gradle 8.7 wrapper jar (was 4.9).
- `gradlew` -- content unchanged; file mode changed from 644 to 755 so CI can execute it.
