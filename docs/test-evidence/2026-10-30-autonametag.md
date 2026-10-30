# Auto Nametag gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. The survival player stood near stationary animals with an iron pickaxe selected. Server commands equipped name tags, summoned animals, and queried their `CustomName`, the player's inventory, and selected hotbar slot.

| Probe | Server observation |
| --- | --- |
| Multiple unnamed targets | A sheep and cow each received `AnomolyTagged` from a stack of eight named tags in hotbar slot 4. The stack fell to six and selected slot 0 was restored. |
| Already named target | A nearby pig kept its original `AlreadyNamed` name. |
| Unnamed tag | Replacing slot 4 with three ordinary, unnamed name tags left a new sheep without `CustomName`. |
| Offhand tag | Putting two named tags in the offhand named the sheep `OffhandTagged`; slot 0 stayed selected. |
| Line of sight | A stone wall kept another sheep unnamed. Removing the wall let the same sheep receive the offhand tag. |

Auto Nametag uses ordinary entity interactions and requires a name tag with a `custom_name` component. It checks vanilla interaction reach and line of sight by default. Moving entities, protected regions, and third-party anticheats need separate tests.
