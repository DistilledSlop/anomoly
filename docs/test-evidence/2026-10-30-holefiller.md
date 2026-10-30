# Hole Filler gameplay test — 2026-10-30

Tested with a Fabric Anomoly client on a local vanilla Minecraft Java 26.3 server. PlayerA stood at `(6.5,102,5.5)` on a raised stone platform. The candidate hole was air at `(8,100,5)` and `(8,101,5)`, with obsidian below at `(8,99,5)` and on all four cardinal sides at Y 100. A stationary pig stood at `(10.5,100,5.5)`. PlayerA had stone selected in hotbar slot 0 and 16 obsidian blocks in slot 1. Hole Filler had `target_mobs=true`; its default line-of-sight, place range, enemy distance, and normal block-interaction settings were retained.

| Probe | Server observation |
| --- | --- |
| Safe hole near a target | The server reported obsidian at `(8,100,5)` after PlayerA reached the platform. `SelectedItem` was still `minecraft:stone`, confirming hotbar restoration. |
| No nearby target | After killing the pig and clearing the placed block, the server reported air at `(8,100,5)`. |
| Unsafe wall | With the pig resummoned but the east wall replaced by stone, the hole stayed air. |
| Occupied hole | After restoring the obsidian wall and summoning a pig inside the gap, the hole stayed air. |

The positive placement used `MultiPlayerGameMode.useItemOn` against the floor block and the default line-of-sight check. No kick or disconnect occurred. This test covers a stationary mob and hotbar obsidian on a vanilla server. Player targeting, offhand placement, alternate fill blocks, moving targets, protected regions, and third-party anticheat compatibility remain unverified.
