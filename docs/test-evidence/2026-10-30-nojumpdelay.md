# NoJumpDelay local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world. A 3×3 stone floor at y=119 and a roof at y=122 made a two-block-high jump space; the player stood at x=50.5, y=120, z=50.5. The server recorded jumps with the `minecraft.custom:minecraft.jump` scoreboard criterion. I held Space for four seconds in each run and toggled NoJumpDelay with a bound J key between runs.

| Module state | Server-observed jumps in four seconds | Server position after run |
| --- | ---: | --- |
| Off | 8 | (50.5, 120, 50.5) |
| On | 28 | (50.5, 120, 50.5) |
| Off again | 8 | (50.5, 120, 50.5) |

The 26.3 `LivingEntity.aiStep` path decrements a private ten-tick `noJumpDelay` field and checks it before a grounded jump. The client mixin clears that field for the local player when the module is enabled. The paired run confirms a faster held-jump cadence in a low-ceiling situation and restoration of the vanilla cadence when disabled. Open-air jumps may finish after the normal cooldown already expires; this module does not permit midair jumps. Third-party anticheats were not tested.
