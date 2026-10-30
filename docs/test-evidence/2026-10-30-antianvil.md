# Anti Anvil gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. PlayerA stood in survival at `(5.5,100,5.5)` on a stone floor with 20 health, stone selected in hotbar slot 0, and obsidian in slot 1. Natural health regeneration was disabled during the paired damage comparison. The overhead space was cleared before each drop. Anti Anvil had its default obsidian, line-of-sight, place-range, and two-tick placement settings.

The server summoned falling anvils from Y 111 with `BlockState:{id:"minecraft:anvil"}`, `HurtEntities:1b`, and `FallHurtAmount:1.0f`. Minecraft 26.3 uses `id` in the falling-block state field; an initial command using the older `Name` field produced sand, so that invalid fixture was cleared and excluded from the anvil comparison.

| Probe | Server observation |
| --- | --- |
| No falling anvil | The roof cell `(5,102,5)` remained air while Anti Anvil was enabled. |
| Direct hit, Anti Anvil enabled | Obsidian appeared at `(5,100,4)`, `(5,101,4)`, `(5,102,4)`, and the roof `(5,102,5)`. The anvil landed on the roof at `(5,103,5)`. PlayerA remained at 20 health with stone selected. |
| Direct hit, Anti Anvil disabled | After clearing the shield and repeating the same drop, the roof stayed air. PlayerA fell from 20 to 10 health and was knocked to X 6.4223. |
| Off-axis anvil | With the module enabled again, an anvil falling at X 7.5 landed at `(7,100,5)`. The player's roof stayed air and health stayed 20. |

Anti Anvil reads falling-block state and motion on the client, predicts the crossing of the space above the player's head, and invokes the same normal block-placement planner used by Self Trap only for a matching threat. No server kick occurred during the probes. After the final observation, the headless client was stopped and the server later logged a drowning event before disconnect; that later event was not part of the anvil comparison. This test covers a centered vertical anvil and a stationary player. Moving anvils, moving players, alternate shield blocks, protected regions, and third-party anticheat compatibility remain unverified.
