# Lost Cities — Fabric port

A Fabric port of [**Lost Cities**](https://github.com/McJtyMods/LostCities) by **McJty**, the
mod that turns the Overworld into an abandoned, overgrown city.

This is a **1:1 port**, not a reimplementation. The generator, the asset system, the profiles,
the commands, the config keys and the public API are the original's, unchanged. Where Forge or
NeoForge supplied something Fabric does not have, the port supplies the closest equivalent and
documents it — see [How the port works](#how-the-port-works) and
[Known differences](#known-differences-from-the-original). Worlds generate the same content as
they do on NeoForge, and datapacks written for the original work here without edits.

**Supported: Minecraft 1.20 through 1.21.11**, from a single source tree.

---

## Supported versions

One jar per version band. Pick the band that contains your Minecraft version.

| Minecraft | Jar | Java | Fabric API tested | Forge Config API Port |
|---|---|---|---|---|
| 1.20 – 1.20.1 | `lostcities-<v>+1.20-1.20.1.jar` | 17 | `0.92.7+1.20.1` | `8.0.3` |
| 1.20.2 – 1.20.4 | `lostcities-<v>+1.20.2-1.20.4.jar` | 17 | `0.97.3+1.20.4` | `20.4.3` |
| 1.20.5 – 1.20.6 | `lostcities-<v>+1.20.5-1.20.6.jar` | 21 | `0.100.8+1.20.6` | `20.6.1` |
| 1.21 – 1.21.1 | `lostcities-<v>+1.21-1.21.1.jar` | 21 | `0.116.15+1.21.1` | `21.1.6` |
| 1.21.2 – 1.21.4 | `lostcities-<v>+1.21.2-1.21.4.jar` | 21 | `0.119.4+1.21.4` | `21.4.3` |
| 1.21.5 – 1.21.8 | `lostcities-<v>+1.21.5-1.21.8.jar` | 21 | `0.136.1+1.21.8` | `21.8.2` |
| 1.21.9 – 1.21.11 | `lostcities-<v>+1.21.9-1.21.11.jar` | 21 | `0.141.6+1.21.11` | `21.11.1` |

Each jar is compiled against the **top** version of its band and declares the whole band in
`fabric.mod.json`. See [Known differences](#known-differences-from-the-original) for what that
implies.

## Installation

Three things are required:

1. **Fabric Loader** `>= 0.15.0`
2. **[Fabric API](https://modrinth.com/mod/fabric-api)**
3. **[Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)** — a hard
   dependency, not optional. The original's config code is used verbatim and is written against
   NeoForge's `ModConfigSpec`; this mod provides that API on Fabric. (On 1.20.1 the same mod
   supplies its Forge-era ancestor, `ForgeConfigSpec`, which the build renames onto.)

Drop all three plus this mod's jar into `mods/`. The mod runs on both client and dedicated
server, and must be present on both.

## Getting started

1. **Create New World** → **More** tab → the **Cities** button.
2. Pick a profile, or **Disabled** for an ordinary world.
3. Create the world as usual.

The button only appears on the **More** tab. Everything it sets can also be set by hand in the
config, which is what dedicated servers do — see [Configuration](#configuration).

For a first look, `onlycities` is the most immediate: the whole map is city. `default` is the
intended experience.

## Generation profiles

17 profiles ship with the mod. They are written out to `config/lostcities/profiles/` on first
run, and you can edit them there or copy one as the basis of your own.

| Profile | What you get |
|---|---|
| `default` | The classic. Cities at 1% chunk chance, radius up to 128, 0–8 floors and up to 3 cellar levels, explosions and 5% ruined buildings, rubble layer on. |
| `nodamage` | `default` with nothing ruined and no rubble layer. Buildings are intact. |
| `onlycities` | Same buildings, but 20% chunk chance and radius up to 256 — continuous megacity. |
| `rarecities` | 0.1% chunk chance and nothing ruined. Mostly wilderness with the occasional find. |
| `tallbuildings` | 4–19 floors, larger and more frequent explosions. Heavy to generate. |
| `largecities` | Perlin noise decides where cities go instead of a flat chance, giving large organic sprawl. Up to 9 floors, 40% building chance. |
| `ancient` | Jungle-reclaimed city: 90% ruined, vines at 10%, dense rubble. |
| `wasteland` | Water replaced with air, bare ground, no park vegetation, 50% ruined. |
| `safe` | No spawners, no loot, lighting on. For building and screenshots. |
| `cavern` | Cities in underground caverns: ground level 40, sea level 32, lit inside, dark outside. |
| `floating` | Cities on floating islands: 3% chance, ground level 50, at most one cellar. |
| `space` | Cities in glass bubbles in the void: 90% sphere chance, city radius 90. |
| `biosphere` | Jungles in glass bubbles on barren land: 80% city chance, 70% ruined, radius 65. |
| `biosphere_caves` | The same bubbles inside large caverns, 90% city chance. |
| `atlantis` | Drowned cities with sea level raised to 89, 10% ruined. |
| `bio_wasteland` | Internal — the barren landscape used outside biospheres. Not meant to be selected directly. |
| `void_outside` | Internal — the void used outside space bubbles. Not meant to be selected directly. |

Several profiles are designed to sit on a matching world type from
[**Lost Worlds**](https://github.com/McJtyMods/LostWorlds): `cavern` with `caves`, `floating`
with `islands`, `space` with `spheres`, `biosphere_caves` with `cavespheres`, `atlantis` with
`atlantis`. They still work without it, they just look better with it. Lost Worlds has no Fabric
port, so on Fabric these profiles generate onto ordinary vanilla terrain.

Upstream also carries `customized`, `waterbubbles`, `chisel`, `realistic` and `water_empty`
commented out in `ProfileSetup`. They are commented out here too, byte for byte — the port did
not enable or drop anything.


## Commands

All twelve of the original's commands are present, under `/lostcities` with `/lost` as an alias.
They require operator level, and most are tools for authoring your own city assets rather than
things you need in normal play.

| Command | Purpose |
|---|---|
| `/lostcities debug` | Toggle debug output for the generator. |
| `/lostcities stats` | Generation statistics for the current dimension. |
| `/lostcities map` | Print an ASCII map of the city layout around you. |
| `/lostcities locate <building>` | Find the nearest instance of a building. |
| `/lostcities locatepart <part>` | Find the nearest instance of a part. |
| `/lostcities listparts` | List every registered part. |
| `/lostcities createbuilding` | Start a new building from the current selection. |
| `/lostcities createpart` | Start a new part from the current selection. |
| `/lostcities editpart <part>` | Open a part for in-world editing. |
| `/lostcities resumeedit` | Resume an interrupted edit session. |
| `/lostcities exportpart <part>` | Write a part back out as JSON. |
| `/lostcities saveprofile` | Write the active profile to disk. |

Upstream's `resetchunks` is commented out in `ModCommands` with a `@todo 1.21` note. It is
commented out here too — the port did not remove it.

## Configuration

Four places, matching the original exactly:

| Path | Contents |
|---|---|
| `config/lostcities-server.toml` | Selected profile, structure/village avoidance, todo queue size, cache cleanup, sapling growth, special bed block. |
| `config/lostcities/common.toml` | Per-dimension profile assignments, `optimizedHeightmap`, `heightSampleSize`. |
| `config/lostcities/client.toml` | Client-only settings. |
| `config/lostcities/profiles/*.json` | The 17 built-in profiles, written on first run. |

On a dedicated server there is no world-creation screen, so `selectedProfile` in
`lostcities-server.toml` is how you choose the profile:

```toml
[profiles]
	selectedProfile = "onlycities"
	selectedCustomJson = ""
```

> **Careful:** an unknown profile name here makes world initialisation fail. This is inherited
> upstream behaviour, not something the port introduced.

## Datapacks

Datapacks work unchanged, and datapacks written for the Forge or NeoForge original need no
edits. You can add buildings, parts, palettes, city styles, conditions, variants, world styles
and multibuildings (2×2, 3×3 chunks) as JSON under:

```
saves/<world>/datapacks/<your pack>/data/<namespace>/lostcities/<category>/
```

where `<category>` is one of `buildings`, `parts`, `palettes`, `citystyles`, `worldstyles`,
`multibuildings`, `conditions`, `variants`, `styles`, `scattered` or `stuff`. The mod's own
assets sit at exactly those paths inside the jar, so they double as reference.

## How the port works

The guiding rule: **keep the original's source, replace only the loader-specific plumbing.**
That is what makes the 1:1 claim testable, and it is also what makes the port maintainable —
upstream commits are applied here with `git cherry-pick`, not reimplemented by hand.

Two decisions do most of the work.

**Official Mojang mappings, not Yarn.** The original is written against Mojmap names, so with
`loom.officialMojangMappings()` its 217 source files compile essentially verbatim. Choosing Yarn
would have meant translating every vanilla identifier, and every future upstream commit with it.

**One source tree for seven Minecraft versions,** via [Stonecutter](https://stonecutter.kikugie.dev/).
Version-specific code lives in comment predicates in the shared sources:

```java
//? if >=1.21.9 {
return level.getRespawnData().pos();
//?} elif >=1.20.5 {
/*return level.getLevelData().getSpawnPos();
*///?} else {
/*return level.getSharedSpawnPos();
*///?}
```

Sources are written in the dialect of the **top** version (1.21.11), because that is the dialect
of the upstream branch commits are taken from.

### The rename pipeline

Vanilla's mass renames between versions are handled separately, by an ordered regex table in
`build.gradle.kts` applied to the generated sources under `build/generated/stonecutter/`. A
Stonecutter predicate per call site would be unmanageable — `ResourceLocation → Identifier` alone
touches about 460 lines. The working tree is never modified, so nothing version-specific reaches
git and cherry-picks stay clean.

Rules are grouped by the version that introduced the change, and **group order is load-bearing**:
the `<1.20.5` and `<1.20.2` groups match text that only exists after the `<1.21` and `<1.21.2`
groups have run, so they come last. Most rules are pure renames, but not all — `getMaxY()` is
inclusive while `getMaxBuildHeight()` is exclusive, so that rule wraps the call and subtracts one
rather than swapping a name.

Rules are deliberately anchored to receiver names where a bare method name would be ambiguous
(the mod has its own `getMinY()`/`getMaxY()` in `varia/GeometryTools`, and its own
`getOrThrow` on `AssetRegistries`). If upstream renames a variable, the rule stops matching and
the build fails loudly instead of silently rewriting the wrong thing.

Stonecutter's own `replacements` feature is not used: it registers rules but never applies them
to generated sources in 0.9.7.

### What replaced what

Every Forge/NeoForge facility the original relies on, and the Fabric equivalent used here:

| Original (Forge / NeoForge) | Fabric replacement | Notes |
|---|---|---|
| `ModConfigSpec`, `ModConfig` | **Forge Config API Port** | Hard dependency. Lets `setup/Config.java` and `config/LostCityProfile.java` be used with no edits at all. |
| AccessTransformer | Access widener (`lostcities.accesswidener`) | 6 entries. Loom's `validateAccessWidener` proves each one resolves, per version, at build time. |
| `LevelEvent.CreateSpawnPosition` | One mixin on `MinecraftServer.setInitialSpawn` | Lets a profile place the world spawn in a city, outside one, in a sphere, and so on. |
| Biome modifier JSONs | `BiomeModifications.addFeature` in code | Injects the `lostcities` and `spheres` placed features at `RAW_GENERATION` and `TOP_LAYER_MODIFICATION` for the `is_overworld` tag. |
| `ServerLifecycleHooks` | `ServerLifecycleEvents` + `varia/ServerAccess` | Tracks the running server. |
| `ITeleporter` | `TeleportTransition` / `DimensionTransition` / `FabricDimensions` | Three eras. Before 1.21 vanilla could not set an arrival point, so `FabricDimensions.teleport` with a `PortalInfo` carries dimension, position, zero velocity and preserved rotation. |
| `NeoForgeMod.Tags.Biomes.IS_VOID` | Conventional tag in the `c:` namespace | `worldgen/LostTags`. |
| Datagen for block tags | Six checked-in JSONs under `src/generated/resources` | Same output, no datagen run needed. |
| `RegisterCommandsEvent` | `CommandRegistrationCallback` | Same twelve commands. |

### Verifying parity

Compile-time checks catch most of it: the access widener is validated per version, and 217
files failing to compile is a loud signal. Two things the compiler cannot check are checked by
hand instead.

**Mixins are not compiler-validated.** A wrong target descriptor fails at runtime, not at build,
so every version gets a `runServer` on a fresh world before it is accepted.

**Generation output is compared block by block.** Worlds are generated from the same seed and the
same `onlycities` profile on each version, then `.mca` files are read directly and blocks tallied.
The comparison intersects only chunks that reached `Status: minecraft:full` in *every* world —
without that filter the counts are meaningless, because the server warms a different number of
spawn chunks per version and half-generated proto-chunks get counted as empty.

On the 49 chunks that are fully generated in all six tested versions, city content is identical:

| Block | 1.20.1 | 1.20.4 | 1.20.6 | 1.21.1 | 1.21.4 | 1.21.8 |
|---|---|---|---|---|---|---|
| `iron_bars` | 241 | 241 | 241 | 241 | 241 | 241 |
| `chest` | 8 | 8 | 8 | 8 | 8 | 8 |
| `spawner` | 17 | 17 | 17 | 17 | 17 | 17 |
| `glass` | 160 | 160 | 160 | 160 | 160 | 160 |
| `bookshelf` | 38 | 38 | 38 | 38 | 38 | 38 |

Stone bricks and cave vegetation drift by under 3% across versions, in both directions. That is
vanilla terrain, not the mod: `onlycities` uses `landscapeType: default`, so cities sit on
ordinary ground and the damage pass replaces a slightly different number of blocks. Ore counts
differ for the same reason.

## Known differences from the original

Stated plainly, because a 1:1 claim is only worth anything if the exceptions are listed.

**The NeoForge IMC API is not available.** Upstream exposes `ILostCities` and `ILostCitiesPre`
through inter-mod communication, which Fabric has no equivalent for. Every interface in
`mcjty.lostcities.api` is present and unchanged, so mods can reach the implementation directly
via `LostCities.lostCitiesImp` — but a mod written against the NeoForge IMC handshake will not
find it. This is the one API-surface difference in the port.

**Range jars compile against the top of their band.** The `1.20.2-1.20.4` jar, for example, is
built against 1.20.4. If vanilla added a method partway through a band, calling it would throw
`NoSuchMethodError` on the lower members. This was checked for Forge Config API Port across all
seven bands, but not exhaustively for vanilla. One concrete open case: the single-argument
`setLootTable` overload exists in 1.20.4 and not in 1.20.1, and exactly where inside 1.20.2–1.20.4
it appeared has not been verified. If it arrived in 1.20.3, loot chests would fail on 1.20.2.

**1.21.9–1.21.11 has not had a generation comparison run.** It compiles and boots; the block-count
table above covers the other six.

**`optimizedHeightmap` is off by default and unverified.** `NoiseChunkOpt` and `HeightGenOpt` are
about 1050 lines re-implementing vanilla's `NoiseChunk` internals — the most version-fragile code
in the mod. It compiles on every version; its runtime behaviour has not been compared against the
unoptimised path. Leave it `false` unless you are testing it.

### Bugs inherited from upstream

These are present in the original and were deliberately not "fixed", since fixing them would be a
deviation rather than a port:

- An invalid `selectedProfile` crashes world initialisation in `Config.getProfileForDimension`.
- `lostcities/stuff/example.json` references `#forge:stone`, a tag that does not exist.
- `PacketRequestProfile` and `PacketReturnProfileToClient` are registered but never sent; their
  `handle()` methods are upstream stubs marked `@todo 1.14`. Both are kept and registered for
  parity.

## Building from source

Requires JDK 21 (Gradle toolchains handle the Java 17 targets).

```bash
./gradlew build
```

That builds every version. To collect all seven jars plus sources jars into
`build/libs/<mod version>/`:

```bash
./gradlew buildAndCollect
```

Single version, which is much faster while developing:

```bash
./gradlew :1.21.11:build
```

Run a client or dedicated server on one version:

```bash
./gradlew :1.20.1:runClient
```

Version-specific properties (Fabric API version, Forge Config API Port version, the Minecraft
range that goes into `fabric.mod.json`) live in `stonecutter.properties.toml`. Generated,
preprocessed sources — what the compiler actually sees, after Stonecutter and the rename
pipeline — land in `versions/<version>/build/generated/stonecutter/`. Read those when a
version-specific compile error makes no sense against the shared source.

## Credits

- **[McJty](https://github.com/McJtyMods)** — author of Lost Cities. This is his mod; the port
  only moves it to a different loader.
- **[Fuzs](https://github.com/Fuzss)** — Forge Config API Port, without which the original's
  config code would have had to be rewritten.
- **[KikuGie](https://github.com/kikugie)** — Stonecutter, which is what makes seven Minecraft
  versions from one source tree practical.
- **Fabric team** — Loom and the Fabric API.

## Licence

MIT, the same licence as the original. See [LICENSE](LICENSE) — it carries McJty's copyright for
the original mod alongside the port's.

**Original mod:** <https://github.com/McJtyMods/LostCities>





