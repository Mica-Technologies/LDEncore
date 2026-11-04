# Configuration

Settings live in `config/theatrical.cfg`, and can also be changed in game from the mod list's
**Config** button.

## `fixtures`

| Option | Default | What it does |
|---|---|---|
| `emitLight` | `true` | Whether a beam actually lights up where it lands. Turn off to keep the visible beams but leave the world's lighting alone. |
| `consumePower` | `true` | Whether moving heads need Forge Energy. Turn off for building and testing, or for a creative world with no power mods. It does **not** affect fresnels, which are always powered through a dimmer rack. |
| `lightUpdateBudgetMillis` | `2.0` | The most milliseconds per world tick spent moving the real light where beams land (0.1 to 50). Every move makes Minecraft recalculate the light around it, which is the main server cost of a moving rig; the beams themselves always move at once, only the light they cast catches up within this budget. See [Performance](performance.md). |

## `rendering`

These are client-side: each player can set their own.

| Option | Default | Range | What it does |
|---|---|---|---|
| `lightBeamOpacity` | `0.4` | 0–1 | How solid the visible beams look. `0` hides the beams entirely (the light they cast stays). |

## `artnet`

| Option | Default | What it does |
|---|---|---|
| `defaultListenAddress` | `0.0.0.0` | The network address a newly placed ArtNet Interface listens on, on the player's own computer. `0.0.0.0` means every interface, which is what hears a real console's broadcast. Set one address to listen on only that. Existing interfaces keep their own setting. |

!!! note

    Changes made in the in-game config screen apply at once. Edits to `theatrical.cfg` itself
    are read when the game starts, so restart after editing the file by hand.
