# Auto Mount gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player stood near stationary rideable entities while holding an iron pickaxe in hotbar slot 0. Server commands queried the player's `RootVehicle`, equipment, and selected slot. Dismount and entity removal separated each probe.

| Probe | Server observation |
| --- | --- |
| Chest boat exclusion | A nearby oak chest boat stayed empty and the player remained on foot. Auto Mount did not open its inventory. |
| Boat and raft | An ordinary oak boat and a bamboo raft each became the player's `RootVehicle` in separate runs. |
| Passenger minecart | A plain minecart on rail became the player's `RootVehicle`. |
| Horse saddle guard | A tamed horse without a saddle was ignored. Equipping a saddle made it the player's `RootVehicle`; the selected hotbar slot stayed 0. |
| Pig saddle guard | An unsaddled pig was ignored. After a saddle was equipped, the player mounted it. |
| Strider, camel, and llama | A saddled strider, saddled camel, and tamed unsaddled llama were mounted in separate runs. The llama case verified its exception from the saddle requirement. |
| Line of sight | An oak boat behind a stone wall was ignored. Removing the wall let the player mount it. |
| Occupancy | A boat with a pig passenger was ignored. After the passenger was removed, the player mounted the boat. |
| Empty hand | With an item in both hands and every hotbar slot, an ordinary boat was ignored. Clearing the offhand let the player mount it, with slot 0 still selected. |

Auto Mount uses normal entity interactions and checks the player's current interaction reach. Its entity categories, tame and saddle requirements, empty-hand rule, line of sight, range, attempt interval, and hotbar restoration are configurable. Donkeys, mules, skeleton and zombie horses, moving vehicles, protected regions, and third-party anticheats need separate gameplay tests.
