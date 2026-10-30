# Crystal and anchor planner

**Value:** Standard high-damage PvP automation, with better prediction and packet scheduling. Meteor and Wurst already have crystal/anchor aura modules.

**Design:** Enumerate valid placement cells using received block states, line of sight and range checks. Estimate explosion exposure for each opponent and self from sampled AABB points; include armor, enchantments, resistance, difficulty, terrain, and knockback escape. Rank placements by expected target damage minus self/friend damage and resource cost. For crystals, sequence place, observed spawn/ID, then attack; for anchors, include charge and dimension rules. Recalculate after block updates and target movement. Expose a minimum self-health margin and a pause when totem state is uncertain.

**Constraints:** Do not predict damage from render geometry alone; the server's collision/block states matter. A raw attack packet's extra range applies to the crystal entity only after it has an ID and passes the handler checks. Plugin cooldowns and item-use checks can differ. Source points: `ServerGamePacketListenerImpl.handleUseItemOn`, `handleAttack`; `Explosion.java` and `RespawnAnchorBlock.java` in the common source jar.
