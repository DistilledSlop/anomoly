# Reach-aware aura

**Value:** Automatically select and attack an exposed target, or use the broader vanilla server acceptance envelope when a server permits it. Meteor's KillAura and Wurst's Killaura are the baseline; the 26.3 improvement is to compute reach from the equipped item's `ATTACK_RANGE` component and the target AABB instead of a fixed three-block radius.

**Design:** Maintain tracked entity IDs, bounding boxes, health, team/friend rules, and attack cooldown. Compute eye-to-AABB distance and compare with `AttackRange.effectiveMaxRange + hitboxMargin + 3` for a raw packet mode; a conservative mode uses the normal client aim ray. Rank by threat, distance, health, and line of sight. Send `ServerboundAttackPacket` for one selected non-piercing target and wait for the next legal cooldown. Include a visible indicator of why each target is eligible.

**Constraints:** The server disconnects on attacks against self, items, experience orbs, and some arrows. The target must exist on the server and be attackable. `PIERCING_WEAPON` uses `STAB` instead. Plugin anti-cheat may enforce sight or shorter reach. See [attack analysis](../exploits/attack-range-and-sight.md).
