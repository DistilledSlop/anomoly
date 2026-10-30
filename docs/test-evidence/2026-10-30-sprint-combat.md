# Sprint combat gameplay test — 2026-10-30

Tested on a local vanilla Minecraft Java 26.3 server with two Fabric clients. PlayerA attacked PlayerB with an iron sword from the same starting positions for each paired comparison. Only the named module was enabled for each enhanced run; all were off for the controls. PlayerB had no combat modules enabled. The server's inline entity predicate `{type:"minecraft:entity_properties",entity:"this",predicate:{flags:{is_sprinting:true}}}` reported its sprint state.

## Source audit

The 26.3 `Player.attack` path gives a full-strength sprint attack an extra 0.5 knockback, then `Player.causeExtraKnockback` multiplies the attacker's horizontal motion by 0.6 and clears sprint. The server applies `START_SPRINTING` and `STOP_SPRINTING` commands in packet order. The modules hook the client's normal `MultiPlayerGameMode.attack` path, so they cover manual attacks and modules that call that method.

`LivingEntity.hurtServer` returns before knockback when the damage cooldown is active and the new hit is no stronger than the previous hit. Sprint state alone cannot bypass that server rule. This leaves Super Knockback's catalog claim about hurt-timer targets unresolved.

## Super Knockback

PlayerB started at `(7.8, 100, 5.5)` with 20 health. PlayerA stood at `(5.5, 100, 5.5)`, facing PlayerB, and clicked once without sprinting. About 1.2 seconds after the hit:

| Run | PlayerB X | Displacement | Health |
| --- | ---: | ---: | ---: |
| Module off | 9.7886 | 1.9886 blocks | 17 |
| Super Knockback on | 11.4160 | 3.6160 blocks | 17 |

The same damage and greater displacement verify the sprint knockback bonus on a damageable target. The test does not demonstrate knockback through the target's damage cooldown.

## Keep Sprint and Sprint Reset

PlayerA began each run sprinting toward PlayerB. The server sprint predicate matched before each hit. PlayerB was reset to 20 health and the original position between runs. Each hit left PlayerB at 14 health.

| Run | Server sprint before | Server sprint after |
| --- | --- | --- |
| Both modules off | true | false |
| Keep Sprint on | true | true |
| Sprint Reset on | true | true |

These checks verify server sprint state immediately after a player hit. Keep Sprint's local horizontal motion restoration is implemented from the pre-attack velocity but was not separately measured in this test. The test does not establish compatibility with third-party anticheats.
