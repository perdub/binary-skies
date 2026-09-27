# Binary Skies — Minecraft 1.21.1 Fabric

A small **server-side** atmospheric mod for a planet with an irregular day/night cycle and a second visible star.

## Design

- Minecraft server ticks are untouched.
- The mod only controls the world's **time of day**, so block random ticks, redstone, furnaces and normal entity ticking keep running at normal server speed.
- The first star is the normal Minecraft sun. Its apparent day/night path follows the artificial time-of-day clock.
- Every cycle gets a fresh duration in the configured range (default 15–20 minutes), with independently randomized day, sunset, night and dawn lengths.
- A rare anomaly can interrupt sunset and jump daylight back toward noon, creating the "sunset → suddenly noon again" effect.
- A second star has its own period and orbital phase. It is rendered to each player with vanilla particle packets, so the client does not need Binary Skies or another client mod.
- The second star is independent of the normal Minecraft day clock, so sometimes it can be above the horizon during the normal night and sometimes both stars can be visible.
- Natural hostile-mob spawning and sleeping follow the artificial time-of-day because the actual `ServerWorld` time-of-day is changed.
- The total server tick counter is not changed.

## Requirements

- Minecraft Java Edition 1.21.1
- Fabric Loader 0.16.x+
- Fabric API 0.116.x for 1.21.1
- Java 21 for development

## Install

1. Put `binary-skies-0.1.0.jar` into the server's `mods` directory.
2. Start the server once.
3. Edit `config/binary-skies.json` if needed.
4. Restart after config changes.

**Clients do not install Binary Skies.**

### Polymer

Polymer is **not required** for the current implementation and there is no resource pack to link into Polymer. The second star deliberately uses vanilla particles so the feature remains server-side with a completely vanilla client.

If the server already uses Polymer AutoHost for other content, Binary Skies can coexist with it without special configuration.

## Commands

Operator level 2 is required.

- `/skies info`
- `/skies pause`
- `/skies resume`
- `/skies reset`
- `/skies setphase day|sunset|night|dawn`
- `/skies speed 1..8` (debug/testing only)
- `/skies anomalies true|false`

## Default atmosphere

A cycle is 15–20 real minutes, but its internal proportions are randomized. A typical sequence may feel like:

`day → long golden sunset → short night → dawn`

or:

`day → sunset → ... wait, why is it noon again?`

The anomaly is deliberately uncommon (12% per cycle by default).

## Advancements

Hidden advancements are included:

- **Second Dawn** — daylight returns after the sky interrupts sunset.
- **Long Night** — awarded when a generated night is at least eight minutes long.
- **The Sky Refuses** — awarded when the rare sunset anomaly occurs.

## Building locally

The project uses Gradle. Java 21 is required.

```bash
gradle clean build --no-daemon
```

The built mod is written to `build/libs/`.

## GitHub Actions / releases

The repository contains two workflows:

- `.github/workflows/ci.yml` builds the project on pushes to `main`/`master`, pull requests, and manual runs, then uploads the jars as a workflow artifact.
- `.github/workflows/release.yml` builds and publishes a GitHub Release whenever a tag matching `v*.*.*` is pushed. The tag version is passed to Gradle, so the generated `fabric.mod.json` and jar use the release version.

Create a release with:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The release workflow uses Java 21, Gradle 8.10.2, and attaches the versioned mod jar, sources jar, and SHA-256 checksum to the GitHub Release.

### Stable download URL

Every successful push to `main` or `master` updates a rolling `latest` GitHub Release. The mod jar is always uploaded with the stable asset name `binary-skies.jar`, so consumers that need a fixed URL can use:

```text
https://github.com/<owner>/<repo>/releases/latest/download/binary-skies.jar
```

This URL does not contain the mod version and therefore remains unchanged when a new build is published. The same rolling release also contains `binary-skies-sources.jar` and `binary-skies.jar.sha256`.

Versioned releases are still created by pushing tags such as `v0.1.0`; those releases are kept separate from the rolling `latest` release.

### Localization
Advancement titles and descriptions use Minecraft translation keys. The project includes English (`en_us`) and Russian (`ru_ru`) localization.
