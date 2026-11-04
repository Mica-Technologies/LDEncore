# DMX to redstone

The **DMX-Redstone Interface** turns **one DMX channel** into a redstone signal, so a lighting
cue can drive redstone lamps, pistons, doors or anything else redstone can switch.

## Using it

1. Run DMX cable to the interface.
2. Right-click it and set its **DMX address**: the channel it listens to.
3. Take redstone from any side.

The signal strength follows the channel's value:

| DMX value | Redstone strength |
|---|---|
| 0 | 0 (off) |
| about 17 | 1 |
| 128 | 7 |
| 255 | 15 (full) |

Each step of strength is about 17 DMX values (`strength = value × 15 ÷ 255`, rounded down).

## Ideas

- Put house lights on a desk fader: redstone lamps all round the auditorium that switch on when
  it comes up.
- Fly scenery: a cue that fires a piston.
- A cue that opens the stage doors.

As with any receiver, if the interface's DMX is cut off it drops to 0.
