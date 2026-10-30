# Client feature designs for 26.3

These are separate module writeups, not implemented features. Priorities favor features supported by the 26.3 source review. Standard cheats are included because they form a usable client baseline. Each module should have explicit enabled state, per-server profile, rate limit, packet logging, and an emergency stop key.

## Combat

- [Reach-aware aura](reach-aware-aura.md): target by actual server distance envelope and line of sight mode.
- [Spear assist](spear-assist.md): use the separate `STAB` raycast and weapon reach in 26.3.
- [Spear momentum burst](spear-momentum-burst.md): amplify spear reach and kinetic damage with accepted position steps.
- [Crystal and anchor planner](crystal-anchor-planner.md): predicted blast outcomes, placement and self-damage.
- [Defense automation](defense-automation.md): totems, armor, shield, projectile avoidance, escape.
- [Projectile aim and trajectories](projectile-aim.md): server-side ballistics and moving target prediction.

## Reconnaissance

- [Block probe](block-probe.md): confirm nearby hidden block states with correction packets.
- [Pick-block oracle](pick-block-oracle.md): use server-confirmed inventory selection to identify a hidden block's clone item.
- [Chunk intelligence and base finder](chunk-intelligence.md): index received blocks and block entities.
- [Map holder tracker](map-holder-tracker.md): use shared map decoration updates with uncertainty.
- [Player radar](player-radar.md): track loaded entities, spawn/despawn, equipment, and last positions.
- [Locator triangulator](locator-triangulator.md): intersect distant player waypoint bearings to estimate X/Z.
- [New-chunk and route survey](new-chunk-survey.md): prioritize exploration paths and confidence.
- [26.3 abandoned camp locator](abandoned-camp-locator.md): exploit known structure placement and new maps when the seed is known.
- [Vibration-aware stealth route](stealth-route.md): use new wool shapes with the correct dampening/occlusion distinction.

## Movement and world

- [Movement packet lab](movement-packet-lab.md): test the tick-end framing weakness and safe bounds.
- [Interaction excursion](interaction-excursion.md): move, interact, and return within one server tick in clear terrain.
- [Correction hop](correction-hop.md): use 26.3 teleport acknowledgments for validated movement offsets.
- [Vertical phase route](vertical-phase.md): cross a thin solid roof or floor using the vertical residual error.
- [Navigator](navigator.md): pathfinding with server collision and route hazards.
- [Elytra flight planner](elytra-planner.md): route, fuel, momentum and landing prediction.
- [Mining and placement scheduler](mining-placement.md): efficient legal packet cadence plus optional blind interaction.
- [Inventory and trade assistant](inventory-trade.md): server-verified menus and survival automation.
- [Crafter slot race](crafter-slot-race.md): toggle an empty slot after leaving the machine's menu range in the same tick.

See [mainstream survey](mainstream-survey.md) for the public-client baseline and gaps. Exploit details and proof are in [../exploits](../exploits/README.md).
