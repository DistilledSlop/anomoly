# Auto Shearer gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player stood on a stone floor near one adult sheep and one baby sheep, both stationary. Server commands queried the sheep's `Sheared` state, inventory, selected hotbar slot, and offhand equipment.

| Probe | Server observation |
| --- | --- |
| No shears | The adult and baby stayed unsheared while the module was enabled and no shears were available. |
| Hotbar shears | Supplying one shears item in slot 4 caused the adult to become sheared once. Shears damage became 1, the baby stayed unsheared, and the selected slot returned to 0. |
| Wall and offhand | With a three-block-high stone wall between player and adult, resetting the adult to unsheared and moving shears to the offhand produced no interaction. Removing the wall sheared the adult, raised offhand shears damage to 1, and left the selected slot at 0. |

Auto Shearer uses normal entity interactions and checks the player's current entity interaction range. The configured range, interval, line of sight, offhand use, hotbar selection, and slot restoration can be changed. Moving sheep, server protection plugins, and third-party anticheats need separate gameplay tests.
