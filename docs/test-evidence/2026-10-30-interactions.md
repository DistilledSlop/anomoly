# Interaction filters local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world with a physical right-click bound to the normal use key. I used server commands to place targets, equip items, and read server-side item and effect data. The client aimed directly at each target. SmartEat and AntiInteract were off during these tests.

| Case | Observation |
| --- | --- |
| NoBlockInteract off; chest at the crosshair, two golden apples held | Right-click opened the chest screen. The server still reported two golden apples. |
| NoBlockInteract on; same chest and apples | The chest stayed closed. Holding right-click consumed one apple; the server reported one apple and `AbsorptionAmount: 4.0f`. |
| NoEntityInteract off; farmer villager at the crosshair, empty hand | Right-click opened the villager's trade screen. |
| NoEntityInteract on; same villager and empty hand | Right-click left the world view open, with no trade screen. |
| NoEntityInteract on; same villager, iron sword held | Right-click opened the trade screen. Iron sword is absent from the default held-item whitelist, so this controls for the item filter. |

Both modules were toggled through bound Minecraft keys; Anomoly's saved config recorded the enabled state. NoBlockInteract uses the client's normal item-use path after redirecting the clicked block target, and NoEntityInteract redirects only filtered entity right-clicks. Other block types, entity types, item-filter modes, sneak behavior, and third-party anticheats were not tested here.
