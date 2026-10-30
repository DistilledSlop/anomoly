# Auto Brewer gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. The player opened a brewing stand and left its screen open while Auto Brewer moved ingredients and bottles through the normal menu. Server commands queried the stand's item slots, brew timer, fuel, and the player's inventory.

| Probe | Server observation |
| --- | --- |
| Three strength potions | From three water bottles, nether wart, and blaze powder in the player inventory, Auto Brewer loaded all three bottles, used one powder as fuel, and inserted one wart. The stand reported an active 400-tick brew. It then inserted one blaze powder for the strength stage. The stand finished empty and the player held three `minecraft:potion` items with `minecraft:strength` contents. |
| Resume a partial batch | A single awkward potion inserted into the open stand resumed at the blaze-powder step and returned as another strength potion. |
| Unrelated potion | A healing potion inserted while strength remained the target stayed in the stand, unchanged, with brew time 0. |
| Splash weakness | With the target set to `minecraft:weakness` and output set to `minecraft:splash_potion`, the module brewed three water bottles with one fermented spider eye, then inserted one gunpowder. The stand emptied and the player held three splash weakness potions. |
| Lingering weakness | With output set to `minecraft:lingering_potion`, a preloaded splash weakness potion caused Auto Brewer to insert one dragon's breath. The player received one lingering weakness potion. Reopening the menu moved the glass-bottle remainder from the ingredient slot into player inventory. |

The configured recipe map was compared with Minecraft 26.3's `VanillaBrewingProvider`: all 44 chosen ingredient edges exist in the provider, and every brewable potion output in that provider has a path. The module pauses when the stand contains an unrelated or mixed batch and when completed bottles lack inventory space. Custom server recipes, every individual potion path, and third-party anticheats still need separate gameplay tests.
