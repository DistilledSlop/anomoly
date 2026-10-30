# Crystal Aura gameplay test — 2026-10-30

Tested on a local vanilla Minecraft Java 26.3 server. PlayerA stood at `(5.5,100,5.5)` with stone selected and 12 end crystals in hotbar slot 1. An obsidian base was at `(8,99,5)` and a stationary 100-health pig at `(9.5,100,5.5)`. Crystal Aura was the only enabled module, with mob targets enabled. The server's block, entity, health, and inventory state was read from its console.

The 26.3 `EndCrystalItem` source requires an obsidian or bedrock base, an empty block above, and no entities in the two-block crystal volume. `ServerExplosion` uses radius 6 and computes damage from distance and raycast exposure. Crystal Aura applies that raw calculation on the client, rejects positions exceeding its self-damage and remaining-health limits, then sends normal block-use and entity-attack packets. It also avoids targets in the early damage cooldown window.

| Run | Server observation |
| --- | --- |
| Creative, automatic place and break | A crystal appeared at `(8.5,100,5.5)` and detonated. The pig fell from 100 to 25.208336 health, a 74.791664 hit matching the client's predicted raw damage. |
| Creative, second detonation | The first blast knocked PlayerA out of break range. After PlayerA returned to `(5.5,100,5.5)`, the module detonated the next crystal and the pig died. |
| Creative, offhand crystals | With stone selected and crystals only in the offhand, the current code placed and detonated a crystal. A fresh pig fell from 100 to 25.208336 health; stone stayed selected. |
| Survival, default safety settings | With the same base, target, and crystal inventory, the pig stayed at 100 health, PlayerA stayed at 20 health, and no crystal appeared. The predicted unarmored self-damage exceeded the configured limit. |

An early probe was blocked by a dropped porkchop in the crystal volume, as vanilla placement requires. The placement ray was also corrected to enter the obsidian's top face rather than passing through the stone floor. After clearing the item and fixing the ray, the automated sequence worked. No kick or disconnect occurred during these local runs.

Creative mode bypasses Crystal Aura's self-damage gate because the player is invulnerable. The survival control verifies refusal in one dangerous setup; it does not verify every safe placement, armor combination, moving target, or third-party anticheat. The damage score does not account for armor, resistance, or enchantment reductions on the target.
