# AntiBounce local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world. A single slime block sat at (80, 100, 80) above a stone floor. With no movement keys held, the server teleported the player to (80.5, 106, 80.5) for each drop. I toggled AntiBounce through a bound key and queried the player's server-side `Pos[1]` several times after each teleport.

| Module state | Server Y samples after drop |
| --- | --- |
| Off | 102.43, 103.68, 102.42 |
| On | 101.00, 101.00, 101.00, 101.00 |
| Off again | 103.88, 101.78, 103.59, 103.12 |

The top of the slime block is y=101. In the two disabled runs, samples above y=103 show rebounds. With AntiBounce on, the player landed at y=101 and stayed there across the sampled interval. The 26.3 client computes bounce restitution through `Entity.isSuppressingBounce()`; the module makes that predicate true for the local player. Other bouncy blocks and third-party anticheats were not tested.
