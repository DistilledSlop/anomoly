# AutoBuild gameplay test — 2026-10-30

Tested Anomoly on a local vanilla Minecraft Java 26.3 server. F6 toggled AutoBuild in each run. Server commands checked the resulting blocks and player inventory. The Dear ImGui ClickGUI also displayed AutoBuild's structure and filter choice controls, sliders, and toggles during the test.

| Probe | Server observation |
| --- | --- |
| Platform | With Platform mode, radius 2, stone in hotbar slot 4, and a pickaxe selected in slot 0, the module filled every one of the 24 empty positions around a single existing center block. A server `execute if blocks` comparison confirmed the entire 5×5 stone floor. Stone fell from 40 to 16, the selected slot stayed 0, and the module disabled itself after completion. |
| Portal | With Portal mode, 20 obsidian in slot 4 and flint and steel in slot 5, the module built a complete 4×5 obsidian frame two blocks ahead of the player, lit it, and disabled itself. Obsidian fell to 6, flint and steel gained one damage, the selected slot stayed 0, and the server reported `minecraft:nether_portal` in both bottom interior cells. |
| Missing material | At another clear portal site, removing obsidian before enabling the module left the candidate frame empty and caused the module to disable itself. |

These results cover the default reach and timing settings on a vanilla server. Larger platforms may require player movement to reach the outer cells. The optional visible-face check and behavior under third-party anticheats remain untested.
