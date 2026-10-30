# No Push water-current test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world. A stone channel at x=90 had a water source at z=90 and flowing water at the test cell, z=92. The server teleported the player to (90.5, 100, 92.5) for each run. No movement keys were held. I toggled No Push with its `Water and lava currents` setting enabled and queried the player's server-side Z position about 1.5 seconds after each teleport.

| No Push state | Starting Z | Server Z after about 1.5 seconds |
| --- | ---: | ---: |
| Off | 92.50 | 94.88 |
| On | 92.50 | 92.50 |
| Off again | 92.50 | 94.74 |

The module's fluid setting makes the local player ignore fluid current forces, which also implements the cataloged AntiWaterPush behavior. The forced test chunks were released afterward. Entity collision behavior, lava, and third-party anticheats were not tested.
