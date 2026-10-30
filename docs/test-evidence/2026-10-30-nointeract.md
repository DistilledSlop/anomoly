# No Interact local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world and sent physical mouse clicks to the client. Server commands placed targets, changed equipment, and queried block states, entity data, and interaction scoreboards. No Interact was toggled with a bound key. Other interaction and automatic combat modules were disabled. The module's four guards follow the categories in the [publisher's NoInteract feature](https://github.com/MeteorDevelopment/meteor-client/blob/e3a2e77967629d216e127b5ceecf71fb15f9d2e5/src/main/java/meteordevelopment/meteorclient/systems/modules/player/NoInteract.java).

## Four action guards

The first configuration blacklisted stone for mining, chests for block use, pigs for entity hits, and villagers for entity use. The player stood at (50.5, 123, 50.5) and aimed at each target to the south.

| Action | Disabled control | Enabled result |
| --- | --- | --- |
| Mine stone in creative mode | The target became air. | The stone remained. |
| Open a chest | The server's chest-open score rose to 1. | The score stayed at 0. |
| Hit a stationary pig with an iron sword | Pig health fell from 10 to 4. | Pig health stayed at 10. |
| Interact with a farmer villager | The server's villager-talk score rose to 1. | The score stayed at 0. |

## Filter settings

For the second configuration, the module stayed enabled. A mining whitelist containing only stone let stone break, while an unlisted cobblestone block remained intact. With an empty block list and `Both` block-use hands, the chest-open score stayed at 0; disabling the module raised it to 1 on the same chest.

With an empty entity-hit list and `Babies = Hit`, an adult pig took six sword damage, while a baby pig at the same target kept 10 health. Disabling the module let the baby take six damage. With an empty entity-use list and `Named = Interact`, an unnamed farmer opened trading and raised the villager-talk score to 1; an otherwise identical named farmer left the score at 0.

For the third configuration, only `Entity use hands = Mainhand` was active. A white sheep stayed white and two main-hand red dyes remained when clicked with the module on. Disabling it changed the sheep's color to red (`Color: 14b`) and consumed one dye. After restoring white wool, moving red dye to the offhand, and enabling the module again, the same click dyed the sheep red. This confirms the selected-hand filter still permits the other hand.

Friend-name filtering, opposite offhand suppression, nonempty entity whitelists, switching a guard on during an in-progress survival break, and third-party anticheats were not separately tested.
