# Inventory Cleaner gameplay test — 2026-10-30

Tested Anomoly on a local vanilla Minecraft Java 26.3 server. The module used normal inventory menu actions. Server commands populated player slots and then read the resulting inventory and offhand equipment; an Xvfb screenshot confirmed when the inventory screen was open.

| Probe | Server observation |
| --- | --- |
| Hotbar layout | With `sword,food,block` as the first three layout entries, a diamond sword, five cooked beef, and 32 stone moved from main inventory to hotbar slots 0, 1, and 2. |
| Stack merge and blacklist | A second 16-stone stack merged into the protected hotbar stone stack, leaving 48 stone there. Three blacklisted rotten flesh disappeared from inventory. The first run exposed a merge gap; this result was recorded after fixing it. |
| Greedy replacement | Replacing the hotbar sword with a wooden sword and adding a netherite sword to main inventory put the netherite sword into hotbar slot 0 and moved the wooden sword to main inventory. |
| Whitelist and offhand | With only stone and shield whitelisted, one dirt disappeared and a shield moved from main inventory to the offhand; `equipment.offhand` reported one shield. |
| Block quantity cap | With `maximum_blocks=32`, a 64-stone stack sorted into the first hotbar slot and was reduced to exactly 32 stone. This exercised individual item throws from a protected layout slot. |
| Inventory screen open | An Xvfb screenshot showed the inventory screen open. Adding eight stone and one dirt to main inventory left the capped 32-stone hotbar stack and no dirt or extra stone in inventory after the module ran. |
| Ignored hotbar slot | With slot 0 set to `ignore` and rotten flesh blacklisted, two rotten flesh stayed in hotbar slot 0 while three rotten flesh in main inventory were discarded. |

The default closed-inventory mode and open-inventory mode were both exercised. Container-screen operation, all item category choices, every quantity cap, non-greedy sorting, and third-party anticheats remain untested.
