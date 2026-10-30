# Auto Anvil gameplay test — 2026-10-30

Tested with two Anomoly clients on a local vanilla Minecraft Java 26.3 server. PlayerC stood at `(47.5,100,50.5)` in survival with stone selected in hotbar slot 0. PlayerB stood at `(50.5,100,50.5)` in survival on a flat stone floor. Auto Anvil used its default four-block drop height, 4.5-block placement reach, line-of-sight check, and helmet requirement. Server console block, inventory, equipment, and health queries supplied the observations.

| Probe | Server observation |
| --- | --- |
| No helmet | With PlayerB's head slot empty, the scaffold and drop cells remained air. |
| Nearly broken helmet | After equipping PlayerB with an iron helmet carrying `minecraft:damage=164`, the module built an obsidian side scaffold through Y=104 and dropped an anvil at `(50,100,50)`. PlayerB's health went from 20 to 16 and the helmet was no longer worn. PlayerC still had stone selected; its anvil stack fell from four to three. |
| Clear final placement face | An initial scaffold on the near side blocked its own final placement ray. The module was revised to choose a side whose future anvil placement face remains visible. In the successful run, the north scaffold occupied `(50,100..104,49)` and the anvil landed at `(50,100,50)`. |
| Offhand anvil and fresh helmet | With a full iron helmet on PlayerB, an anvil only in PlayerC's offhand, and the existing scaffold, the offhand anvil was consumed and another anvil landed. PlayerB's health went from 20 to 14.096; entity data showed the helmet still worn with `minecraft:damage=3`. Stone remained selected. |
| No anvil | After the offhand anvil was consumed and the landed block was removed, Auto Anvil left the drop cell air even with PlayerB back in position and wearing the helmet. |

The server's falling-anvil physics caused the health and helmet changes. The module uses ordinary block interactions and a local line-of-sight check. Moving targets, protected regions, other anvil variants, other drop heights, and third-party anticheats still need separate gameplay tests.
