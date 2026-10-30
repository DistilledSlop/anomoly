# Auto Breed gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player stood in a small stone pen with two adult animals. The module used normal entity interactions. Server commands measured food inventory, parent ages, offspring ages, and the selected slot.

| Probe | Server observation |
| --- | --- |
| Too little food | Two adult sheep stayed out of love and one wheat remained in the hotbar. Auto Breed waited for enough food to feed the pair. |
| Sheep from hotbar | With four wheat in slot 4, the module fed both sheep. The stack fell to two, both parents entered a breeding cooldown, and a new sheep appeared with a negative age. The selected slot returned to 0. |
| Cows from offhand | After removing the sheep, two adult cows received two wheat from the offhand stack. It fell from four to two, both cows entered a breeding cooldown, and a newborn cow appeared with age `-23996`. The selected slot remained 0. |
| Species filter | After setting the animal ID list to `minecraft:sheep`, a fresh cow pair stayed at age 0, the offhand wheat stack stayed at four, and no calf appeared. |

Auto Breed allows a comma-separated animal ID list and checks each animal's vanilla breeding food. It requires two reachable adults of the same class and enough food for both, checks line of sight by default, and waits before trying the same animal again. Minecraft does not synchronize an adult animal's exact breeding cooldown to the client, so the module tracks its own feeding attempts for five minutes; a manual feed or reconnect can make that estimate stale. Other species, moving pairs, line-of-sight rejection, protected regions, and third-party anticheats need separate gameplay tests.
