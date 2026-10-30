# Elytra flight planner

**Value:** Fast survival travel with landing and fuel planning. Meteor ElytraFly and Wurst flight features show the baseline; a 26.3 planner should emphasize server-valid trajectories.

**Design:** Simulate elytra lift/drag and firework boosts over a rolling horizon, with altitude, fuel, durability and terrain constraints. Select a cruise altitude and waypoint path that maximizes distance per rocket, and reserve enough height for emergency landing. Account for ping and the server's elytra movement check gamerule. Use received chunks for terrain clearance; request no arbitrary remote chunks. Detect server corrections and return to normal flight controls rather than repeatedly spamming invalid moves.

**Constraints:** Vanilla elytra physics and server speed checks constrain outcomes; packet spoofing does not grant creative flight. Source: `ServerGamePacketListenerImpl.shouldCheckPlayerMovement` and movement handler, plus `LivingEntity` fall-flying code.
