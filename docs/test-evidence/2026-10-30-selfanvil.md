# Self Anvil gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. PlayerC stood in survival at `(70.5,100,50.5)` over a stone floor with stone selected in hotbar slot 0, obsidian in slot 1, and three anvils in slot 2. Self Anvil used its default one-block-hole requirement, two-block drop height, line-of-sight check, and ten-health threshold. Server console block, inventory, position, and health queries supplied the observations.

| Probe | Server observation |
| --- | --- |
| Open floor | With no walls, the feet and scaffold cells remained air; obsidian and anvil counts stayed at 16 and 3. |
| One-block hole | After four obsidian walls were added at feet height, Self Anvil built two north support blocks at `(70,101,49)` and `(70,102,49)`, then dropped an anvil into the feet cell `(70,100,50)`. The hotbar counts changed to 14 obsidian and 2 anvils; stone remained selected. The player stood at Y=100.5 on the landed anvil. |
| Low health | With natural health regeneration disabled, the anvil removed, and PlayerC at 9.33 health, the feet cell remained air and the remaining anvil was not consumed. |
| Offhand | With the hotbar anvil removed, one anvil placed in the offhand, and health restored, Self Anvil dropped the offhand anvil into the same cell. The offhand anvil was consumed and health was 18 after the drop. |
| Occupied cell | With the final code loaded, a cobweb occupied the feet cell while PlayerC held an anvil. The cobweb remained and the anvil count stayed at one. After the cobweb was removed, an anvil landed in the cell, the item was consumed, and health fell from 18 to 16. |

The short drop occupied the hole and dealt two damage in the occupied-cell repair probe. A player attempting to enter that occupied hole, other wall materials, moving players, protected regions, and third-party anticheats need separate gameplay tests.
