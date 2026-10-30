# Navigator

**Value:** Reliable travel to resources, structures, teammates or waypoints with fewer deaths. Public Navigator, AutoWalk, Scaffold, SafeWalk and Baritone-like pathing are the baseline.

**Design:** Build a costed 3D graph from received block states and collision shapes. Consider walk, jump, swim, climb, boat, mine, place, pearl and elytra transitions only when the needed item/state is available. Estimate time, food, durability, exposure, lava/fall risk and packet rejection likelihood. Replan on block updates and mark unloaded edges as uncertain. Use the chunk intelligence index as a destination source and camp locator as a structure source.

**Constraints:** The server collision engine is authoritative; client predictions should be checked against corrections. A route into an unloaded chunk is a plan, not known terrain. Keep an emergency stop and no automatic destructive mining by default.
