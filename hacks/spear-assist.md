# Spear assist

**Value:** Automate the distinct piercing-weapon attack introduced before 26.3 and still present in this release. A normal `ServerboundAttackPacket` does nothing while holding a `PIERCING_WEAPON`; `ServerboundPlayerActionPacket.Action.STAB` calls `PiercingWeapon.attack` instead.

**Design:** Use the item's `ATTACK_RANGE` component, derive the server ray from eye and head look direction, and choose a rotation that intersects the target AABB while minimizing friendly fire. Send a rotation update before `STAB`, only when minimum attack charge is met. The source item's default spear range is 2.0–4.5 survival blocks (2.0–6.5 creative), with a 0.125 hitbox margin; inspect actual item components because datapacks can alter them. The ray also extends by forward `knownMovement`; see the tested [spear momentum burst](spear-momentum-burst.md). A spear attack can hit multiple entities along the collider-clipped ray, so score the entire line, not one target.

**Constraints:** `PiercingWeapon.attack` uses `ProjectileUtil.getHitEntitiesAlong(..., ClipContext.Block.COLLIDER)`, so a solid wall blocks it. It honors invulnerability, attackable state, player harm rules, and cooldown. Test aim/packet order against a local server. See `PiercingWeapon.java:55–88`, `ServerGamePacketListenerImpl.java:1317–1340`, and `Item.java:549` in the common source jar.
