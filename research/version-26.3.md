# 26.3 changes relevant to a client

This is a focused porting and tactics note based on the [official Java 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) and the local 26.3 jars.

| Change | Client implication |
| --- | --- |
| Abandoned camps in 18 biome variants; new explorer maps | Index new structure data and maps for route planning. The structure set uses spacing 37, separation 8, salt 91231127. |
| Explorer maps are distinct item types, rather than renamed filled maps | Inventory filters, auto-loot and map handling must recognize the new items and preserve their map IDs; explorer maps cannot be zoomed out. |
| Wool slabs and stairs dampen vibrations | New stealth route surfaces; the vanilla occlusion tag still lists only full wool. |
| Straw bed sleeps once without setting spawn; destroyed on use in Nether/End | Route planning can skip a night without changing respawn point; inventory planner must treat it as consumable. |
| Ender-pearl teleport resets momentum | Old pearl boost/escape predictions need revision. |
| Maps always show player facing direction | Shared-map tracking can estimate heading when the holder is inside the map. |
| Villager offers can update while trading screen stays open | Trade assistant should process live offer updates. |
| SDL3 replaces GLFW, physical scancodes replace prior key mapping | Keybind, mouse-input and overlay mixins need a 26.3 port instead of direct 1.21-era assumptions. |
| `white-list=true` by default | A local test server requires an explicit test player allowlist or a controlled offline setting. This is not a privilege bypass. |
| Data pack 121.0, resource pack 97.1 | Load 26.3 registries, components and tags when simulating blocks/items. |

The packet trust findings in [exploits](../exploits/README.md) are **observed in 26.3 source**. This table does not assert they were introduced in 26.3; historical novelty requires a version-by-version diff.
