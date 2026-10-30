# AntiInteract gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player aimed at a block two blocks away and right-clicked in the game window. Server score objectives tracked the corresponding container-open statistics. The module used its default protected block list.

| Probe | Server observation |
| --- | --- |
| Chest | Right-clicking a chest with an empty hand left `minecraft.open_chest` at 0. |
| Barrel | Replacing the same block with a barrel and right-clicking left `minecraft.open_barrel` at 0. |
| Colored shulker box | Replacing it with a blue shulker box and right-clicking left `minecraft.open_shulker_box` at 0. The default `minecraft:shulker_box` entry covers colored variants. |
| Unlisted block | Replacing it with a crafting table and right-clicking raised `minecraft.interact_with_crafting_table` from 0 to 1. |
| Sneak placement | With a chest restored, holding Sneak and right-clicking placed one cobblestone on its near face; `minecraft.open_chest` stayed 0. |

AntiInteract blocks the client block-use packet for listed blocks and does not intercept left-click breaking. The block-ID list is configurable. This run did not exercise every default container type, custom block IDs, protected regions, or third-party anticheats.
