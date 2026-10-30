# Auto Crafter gameplay test — 2026-10-30

Tested Anomoly on a local vanilla Minecraft Java 26.3 server. The module used known Recipe Book recipe IDs while the book panel was open. Server commands queried player inventory, cooking-machine slots, dropped items, and saved module state. Cooking machines started with one coal in their fuel slot.

| Probe | Server observation |
| --- | --- |
| Recipe Book gate and crafting table | With the crafting table open but its Recipe Book panel closed, four oak planks remained untouched. Opening the panel let Auto Crafter turn them into eight sticks. |
| Inventory 2×2 grid | With two coal, eight sticks, and `minecraft:torch` listed, the open inventory Recipe Book produced eight torches and left six sticks. |
| Furnace | With raw iron in player inventory and fuel already in the furnace, the module placed the known smelting recipe, collected one iron ingot, and left the furnace's item slots empty. |
| Blast furnace | With the corresponding blasting recipe, it placed raw iron, collected another iron ingot, and left the blast furnace empty. |
| Smoker | With the smoking recipe, it placed raw beef, collected one cooked beef, and left the smoker empty. |
| OnFull: Wait | All 36 player inventory slots held full stone stacks. A cooked beef item placed in the smoker's result slot stayed there while the inventory was full, then moved into the first slot freed. |
| OnFull: Throw | With the inventory full, the result disappeared from the smoker and the server reported one nearby dropped `minecraft:cooked_beef` item. |
| OnFull: CloseScreen | With the inventory full, the module closed the smoker screen, left the result in the machine, and stayed enabled. |
| OnFull: Disable | With the inventory full, the module disabled itself and left the result in the machine. |
| Recipe-order guard | With one iron block and the target order `iron_ingot,iron_block`, the block stayed untouched. Reversing the order to `iron_block,iron_ingot` produced nine iron ingots from the block; the ingots remained after several seconds instead of being crafted back into a block. |

The module currently expects fuel to be loaded before cooking. The default stack-crafting option was exercised by the eight-stick result. Custom recipes, the non-stack and non-sequential options, every inventory-remainder case, and third-party anticheats remain untested.
