# Enderman Look gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A stationary enderman stood about three blocks east of the player. Server commands set the player's rotation and queried it after each probe.

| Probe | Server observation |
| --- | --- |
| Avoid gaze | Facing east at yaw −90 and pitch 0 did not trigger a turn because the player's crosshair missed the enderman's eyes. Facing its eyes at yaw −90 and pitch −17 caused Enderman Look to turn to yaw 0, away from the enderman. |
| Look at enderman | With `look_at=true`, the player started at yaw 90 and pitch 0, then turned to yaw −90 and pitch about −17, toward the enderman's eyes. |
| Visibility | A stone wall between the player and enderman kept yaw 90 unchanged. Removing the wall made the player turn toward the enderman again. |

The module yields to active combat and projectile aiming modules. This run covered one stationary enderman; multiple endermen, moving targets, and third-party anticheats need separate tests.
