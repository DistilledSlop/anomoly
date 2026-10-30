# BlockBounce local gameplay test — 2026-10-30

I ran the Anomoly 26.3 Fabric client against a local vanilla 26.3 multiplayer server. The test block was at (80, 100, 80), and the player stood at the center of its top surface. I assigned `B` to BlockBounce, used it to toggle the module, pressed Space once for each jump, and queried the player's server-side `Pos[1]` during the jump. AntiBounce and the other movement modules were disabled.

| Surface and module state | Starting feet Y | Server Y samples after jump |
| --- | ---: | --- |
| Slime, off | 101.0000 | 101.89, 101.85, 101.61, 101.25 |
| Slime, on | 101.0000 | 104.27, 103.29, 103.13, 103.16 |
| Stone, on | 101.0000 | 102.25, 101.00, 101.00 |
| Honey, on | 100.9375 | 103.53, 100.94, 100.94 |
| Red bed, on | 100.5625 | 104.51, 103.10, 102.25 |
| Slime, off again | 101.0000 | 101.87, 101.59, 101.31 |

The on/off slime runs showed a large increase in jump height. The stone control kept ordinary jump height with BlockBounce enabled. Honey and bed runs showed the surface detection and extra upward motion on their lower collision tops. These are sampled positions rather than exact jump apices. Third-party anticheats and nondefault motion values were not tested.

During the first client launch, a separate TimeChanger clock mixin caused a login disconnect. `ClientClockInstanceMixin` existed but was absent from `anomoly.mixins.json`; adding it let the same client join and complete these tests.
