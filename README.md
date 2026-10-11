# AntiLag — Meteor Client addon

A client-side anti-lag / performance addon for [Meteor Client](https://meteorclient.com)
on **Minecraft Java Edition 1.21.11** (Fabric).

It reduces the rendering cost of lag machines, entity spam, particle spam and
excessive block entities on multiplayer servers. Everything it does is **purely
client-side and fully reversible**: it never removes entities or block entities
from the world, never edits authoritative server state and never cancels inbound
packets.

- Mod ID / namespace: `antilagaddon`
- Package: `com.antilagaddon`
- Target: Minecraft `1.21.11`, Fabric Loader `0.18.2`, Meteor `1.21.11-SNAPSHOT`, Java 21

> **Scope / limitations.** The addon does **not** prevent server crashes, server-side
> lag or network saturation, and it does not hide entities on the server or for other
> players. It only changes what *your* client draws. It is not a cheat: it does not
> attack servers, other players, or bypass anticheat.

## Modules

All four modules live under the **AntiLag** category in Meteor's ClickGUI and use
Meteor's normal settings, keybinds and config persistence.

### AntiLag (main)
The configurable master set of protections:

- **Entity suppression** — independent toggles and maximum render distances for item
  entities, experience orbs, projectiles, falling blocks, armor stands, vehicles,
  display entities and other non-living entities, plus an optional **entity-render
  budget** that keeps the nearest *N* suppressible entities visible. Players and the
  local player are always kept visible.
- **Particle suppression** — a global toggle plus per-category toggles (explosion,
  smoke/cloud, flame/soul, potion, block-destruction) and a per-tick **particle
  budget**.
- **Block-entity rendering** — distance limits for signs, chests/barrels/shulkers,
  banners, spawners and a misc category. Client world updates and block-entity state
  are never touched; only drawing past the configured distance is skipped.
- **Adaptive performance** — automatic suppression levels (**Normal → Balanced →
  Performance → Emergency**) driven by FPS/frame time, with configurable thresholds,
  upgrade/downgrade cooldowns and hysteresis so the level does not flap.

### EntityLimiter
Standalone entity limiter. Reuses the AntiLag entity distance/budget settings (single
source of truth) but has its own enable flag and scan cadence, so it can apply entity
suppression even when the main module is off.

### ParticleLimiter
Standalone particle limiter. Reuses the AntiLag particle category/budget settings with
its own enable flag.

### LagMonitor
Read-only diagnostics: FPS, frame time, client tick time, entity count and observable
inbound/outbound packet rates, plus chunk/block update counts and recent tick spikes.
Optional chat warnings for large tick spikes. It never cancels packets, drops chunk or
entity updates, or blocks networking threads.

### LagMonitor HUD
A HUD element (Meteor HUD group **AntiLag**) showing FPS, frame time, tick time, entity
count and inbound packet rate. Individual lines can be toggled.

## Design notes

- **No server contact.** Suppression is decided entirely from the client world and the
  camera position.
- **Cheap.** Distance checks are simple squared-distance comparisons; the entity budget
  is rebuilt by a bounded, throttled scan (at most every `scan-interval` ticks) rather
  than counted during rendering. Particle counting is a single per-tick integer.
- **Reversible.** Disabling a module short-circuits every check, restoring normal
  rendering immediately.
- **Minimal mixins.** Only one mixin is used (`EntityRenderManagerMixin`) because Meteor
  has no cancellable per-entity render event. Particles and block entities use Meteor's
  own public events (`ParticleEvent`, `RenderBlockEntityEvent`). No networking mixins.

## Building

Requires JDK 21 and a working network connection to fetch Minecraft, Yarn and Meteor.

```bash
./gradlew build
```

The distributable JAR is written to:

```
build/libs/antilagaddon-1.0.0.jar
```

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put **Meteor Client** for 1.21.11 in your `mods/` folder.
3. Put `antilagaddon-1.0.0.jar` in the same `mods/` folder.
4. Launch the game. The addon appears under the **AntiLag** category in Meteor's GUI
   and its HUD element under the **AntiLag** HUD group.

No server-side installation is required.

## Compatibility

- Minecraft 1.21.11, Fabric Loader 0.18.2, Java 21.
- Meteor Client `1.21.11-SNAPSHOT`.
- Mixin target verified against the 1.21.11 intermediary jar.

## License

MIT — see [LICENSE](LICENSE).
