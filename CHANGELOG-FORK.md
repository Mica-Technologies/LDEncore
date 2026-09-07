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

---

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

- None yet.
