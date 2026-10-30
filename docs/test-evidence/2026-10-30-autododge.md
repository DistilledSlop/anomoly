# Auto Dodge gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. PlayerA stood in survival at `(10.5,100,10.5)` on a stone floor with 20 health, facing west. Natural health regeneration was disabled for the paired damage comparison. The server summoned an arrow at `(2.5,101,10.5)` with `Motion:[0.35d,0.0d,0.0d]` and `NoGravity:1b` for every shot. This made the arrow travel east through the stationary player's body unless the client moved.

| Probe | Server observation |
| --- | --- |
| Auto Dodge enabled | PlayerA moved to `(10.5,100,8.3452)` along the lateral Z axis and stayed at 20 health. A later check found the same position, confirming the synthetic movement key was released. |
| Auto Dodge disabled | From the original position and health, PlayerA stayed at Z 10.5, took 1 damage, and was knocked to X 11.4878. |
| One unsafe side | With the negative-Z landing block removed, the revised module moved PlayerA to Z 12.6548, the solid side, and health stayed 20. |
| Both sides unsafe | With both lateral landing blocks removed, Auto Dodge did not sidestep. The arrow dealt 1 damage and knocked PlayerA to X 11.4878, while Z stayed 10.5. |

The module predicts arrow segments using Minecraft 26.3's air drag and gravity, checks terrain before accepting a predicted hit, and uses ordinary movement key state for the dodge. It rechecks collision and floor support along the candidate path each tick. The test covers a no-gravity arrow and a stationary player on a vanilla server. Gravity-affected arrows, spectral arrows, thrown tridents, moving-player prediction, crowded terrain, and third-party anticheat compatibility remain unverified.
