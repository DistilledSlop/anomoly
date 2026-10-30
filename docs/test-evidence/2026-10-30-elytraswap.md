# Elytra Swap local gameplay test — 2026-10-30

I launched the Fabric 26.3 client on a local vanilla 26.3 multiplayer server and bound the Elytra Swap module key to K. Server commands populated the chest, inventory, and offhand slots; `data get entity` read the resulting equipment and inventory after each key press. The module's saved `config/anomoly.json` state was `false` after each one-shot action.

| Case | Server observation |
| --- | --- |
| Inventory source and return | A diamond chestplate in the chest slot and an inventory elytra swapped places after one K press. A second K press restored the diamond chestplate and returned the elytra to the original inventory slot. |
| Offhand source and return | With an elytra in the offhand, K equipped it and moved the diamond chestplate into the offhand. K again restored both items. |
| Durability choice | An elytra with damage 430 (two durability remaining) in inventory slot 0 was skipped in favor of an undamaged elytra in slot 1. The server showed the worn wing unchanged in slot 0. |
| Worn-only control | With only the damage-430 wing available, K left the diamond chestplate equipped and the wing in inventory. |
| Binding Curse control | A diamond chestplate carrying `minecraft:binding_curse` stayed equipped despite a fresh elytra in inventory. |
| Empty chest slot | After the chestplate was removed by a server command, K equipped the fresh elytra from inventory into the empty chest slot. |
| Chestplate selection | With an elytra equipped and leather and netherite chestplates available, K equipped the netherite chestplate and moved the elytra into its inventory slot. |

The test did not exercise an open inventory screen, a full inventory with offhand automation enabled, or a third-party anticheat server.
