# Performance

What a lighting rig costs, where the cost comes from, and the two settings worth knowing about.
The figures are from a 64-head benchmark measured on 2026-11-04 (method at the bottom).

## The short version

- **An idle rig is close to free.** A steady look on 64 moving heads costs the server about
  0.02 ms a tick and the client about 6 µs a head per frame.
- **A moving rig costs light updates.** Wherever a beam lands, the mod lights that spot for real,
  and every time the spot moves, Minecraft recalculates the light around it on the server and
  rebuilds the chunk section on every nearby client. That is nearly all of what a busy rig costs.
- **The server side is capped.** Light updates are applied within a time budget per tick
  (`fixtures.lightUpdateBudgetMillis`, 2 ms by default), so even a rig that re-aims every head at
  once cannot stall the server. Under heavy load the world light trails the beams slightly; the
  beams themselves always move at once.
- **For clients, turn on Forge's `alwaysSetupTerrainOffThread`** in packs that use moving rigs. It
  moves the chunk rebuilds off the render thread, and in the worst-case test it took a client from
  hitching to a steady frame rate. See [Recommended settings](#recommended-settings).

## Measured costs

64 moving heads under truss, all on one DMX run, measured in the same flat world before and after
the fixes in this fork's next release. "Tick" is server time per 50 ms tick; "render work" is the
client's CPU time per frame.

| Scene | What | Before | After |
|---|---|---|---|
| A: static look from a desk | Desk, server | 124 µs a tick | **7.8 µs** a tick |
| | All 64 heads, server | 10.7 µs a tick | 16 µs a tick |
| | Fixture rendering, client | 31 µs a head | **6.2 µs** a head |
| | Client render work | 2.47 ms a frame | **0.96 ms** a frame |
| B: every head re-aimed at random once a second (worst case) | Server tick, mean | 3.3 ms | **~1 ms** |
| | Server tick, worst in 5 s | **199 ms** | **10 ms** |
| | Client, default Forge settings | p95 5 ms, worst 42 ms, 1% low 15 fps | p95 23 ms, worst 33 ms, 1% low 27 fps |
| | Client, `alwaysSetupTerrainOffThread=true` | not measured | **p99 2.5 ms, worst 5 ms, steady 120 fps** |
| C: a show running, all heads sweeping their pan continuously | Server tick, mean / worst | not measured before | 1.2 / 5.1 ms |
| | Light updates | up to 8 a head a second | **0.8** a head a second |

Scene B is deliberately pathological: 128 light changes a second, all real. A programmed show
(scene C) changes the light far less, because a beam's light only follows it once the beam has
settled for half a second, or once a second while it keeps moving.

## What was changed

| Cause | Fix |
|---|---|
| Each fixture part was drawn one quad at a time, a draw call per quad, every frame | Parts are compiled into display lists once and replayed |
| The desk re-sent its whole 512-channel universe to every nearby player, and re-pushed it to every fixture, every tick | Only when the output changes, with a keep-alive once a second |
| Every beam move recalculated the light immediately, so 64 heads moving together took one tick to 199 ms | Light updates go through a per-tick time budget, and repeated writes to one block collapse into one |
| A sweeping beam moved its light on every trace, four times a second | The light follows once the beam has settled, or once a second while it moves |

## Recommended settings

**Forge, `config/forge.cfg`, client side:**

```
B:alwaysSetupTerrainOffThread=true
```

Forge describes it as possibly causing "weird rendering lag". In practice that means a chunk
section can show its new lighting a frame or two late. For a pack with moving rigs that is a good
trade: without it, every beam that moves near the player can cost a frame.

**LDEncore, `config/theatrical.cfg`:**

| Setting | When to change it |
|---|---|
| `fixtures.lightUpdateBudgetMillis` | Lower it on a busy server if lighting rigs show in the tick time; raise it if the light on the floor trails the beams too visibly and the server has headroom |
| `fixtures.emitLight` | `false` turns off the real light where beams land: the visible beams stay, and the light updates (the main cost) disappear entirely |
| `rendering.lightBeamOpacity` | `0` hides the visible beams, per player |

## How it was measured

A flat world with 64 moving heads in an 8×8 grid, 4 blocks apart, hanging under truss 6 blocks
above the floor, chained on one DMX cable run from a single source block. The player stood 10
blocks outside the grid, facing it, at night. Server costs come from per-tile-entity timing and
tick statistics over 10-second windows, client costs from per-renderer timing and frame statistics
over 8 to 10 seconds, all through MCMCP. "Before" was measured on the 2026.11.04 release, "after"
on the build with the fixes. Each pair is from separate sessions, so allow a few percent of
session-to-session noise.
