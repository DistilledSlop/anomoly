# Auto Mob Heal gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player stood near stationary injured mobs. Server commands measured health, inventory and offhand contents, and the selected hotbar slot.

| Probe | Server observation |
| --- | --- |
| No healing item | An injured iron golem stayed at 50 health with no iron ingots available. |
| Iron golem from hotbar | Three ingots in slot 4 led to two normal repair interactions. The golem rose from 50 to 100 health, one ingot remained, and the selected slot returned to 0. |
| Minimum missing health | At 99 of 100 health, the golem stayed at 99 and the remaining ingot was not used with the default two-health minimum. |
| Tamed horse from offhand | A tamed injured horse rose from 10 to 16 health with three offhand wheat, which healed two health each. The selected slot remained 0. |
| Owned wolf and other-owner control | A tamed wolf owned by the player rose from 10 to 40 health with two offhand cooked beef. Another injured tamed wolf with a different owner stayed at 10 health, and two replacement beef remained untouched. |
| Line of sight | A stone wall between player and injured golem left its health at 50 and two ingots untouched. Removing the wall let the golem reach 100 health and consumed the two ingots. |
| Owned cat from offhand | A tamed cat owned by the player rose from 4 to 8 health using two offhand cod. |

Auto Mob Heal uses normal entity interactions and Minecraft's entity interaction range. It targets injured owned wolves and cats, tamed horses and related mounts, and iron golems by default. The minimum missing health, reach, line of sight, ownership, mount taming, offhand use, hotbar selection, and slot restoration are configurable. Mount variants beyond horses, other food choices, moving mobs, protected regions, and third-party anticheats need separate gameplay tests.
