# Auto Smelter gameplay test — 2026-10-30

Tested Anomoly on a local vanilla Minecraft Java 26.3 server. The player opened each machine's normal menu, and Auto Smelter moved items with ordinary inventory clicks. Server commands queried the block entity's input, fuel, and result slots, cooking timer, player inventory, and saved module state.

| Probe | Server observation |
| --- | --- |
| Furnace | Three raw iron and one coal in the player's hotbar became three iron ingots in player inventory. The server reported a 200-tick cooking recipe and a progressing timer while the module ran. The furnace's item slots were empty and the module disabled itself after the final result was collected. |
| Blast furnace | Two raw gold and one coal became two gold ingots. The server reported a 100-tick recipe and an empty machine at completion; the module disabled itself. |
| Smoker | Two raw beef and one coal became two cooked beef. The machine was empty and the module disabled itself after collecting the output. |
| Missing fuel | With one raw iron and no selected fuel, the module moved the iron into the furnace, left it uncooked there, and disabled itself. No item was lost. |
| Fuel refill and completion options | After restarting with the paused raw iron still in the furnace, `fuel_per_refill=2`, `disable_when_done=false`, and `close_when_done=true`, the module moved exactly two coal from a stack of four. One coal was burning and one remained in the fuel slot; the player kept two. It collected the fourth iron ingot, closed the furnace screen, and remained enabled. |

The module checks the active machine's vanilla recipe input set and only selects configured input and fuel IDs. This test covers the default inputs for all three machine types and the listed settings on vanilla 26.3. Custom recipes, a full player inventory, large fuel refills, and third-party anticheats remain untested.
