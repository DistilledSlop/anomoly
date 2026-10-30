# Local 26.3 server handler probe

On 2026-10-29, a temporary property-gated Fabric probe ran against a local dedicated **Minecraft 26.3 / Fabric Loader 0.20.1** server. It created an unprivileged survival `ServerPlayer` with an embedded connection, completed the player-loaded and teleport-acknowledgment handshakes, placed the player at `(0.5, 100, 0.5)`, placed stone at `(2, 100, 0)`, and targeted `(4, 100, 0)` behind that stone. Spawn protection was set to zero for this controlled world. The probe called the actual server packet handlers; it did not send bytes through a remote network socket or use an anti-cheat plugin.

| Handler call | Observed result |
| --- | --- |
| `handleUseItemOn` with an empty hand and `BlockHitResult` at the hidden chest | `player.containerMenu` became `ChestMenu`, confirming a through-wall interaction. |
| `handleAttack` with a pig entity ID behind the wall after the entity became visible in the server entity manager | Pig health changed from `10.0` to `9.792`, confirming damage through the wall. The small damage reflects the test's attack timing; it is not a damage claim for a normal cooled attack. |

Relevant log lines from `/tmp/mc263-probe4.log`:

```text
RESEARCH_PROBE pre_use pos=(0.5, 100.0, 0.5) loaded=true may_interact=true spawn_protected=false client_loaded=true
RESEARCH_PROBE chest_menu=ChestMenu at distance=4.0 behind_stone=true
RESEARCH_PROBE before_attack found=true pos=(0.5, 100.0, 0.5) in_range=true
RESEARCH_PROBE pig_health_before=10.0 after=9.792 behind_stone=true
```

The temporary probe source was removed after the run and the original example mod was rebuilt. The log remains in `/tmp/mc263-probe4.log` for this session; this file records the setup and outcome in the workspace. The test confirms vanilla handler behavior under these conditions. It does not establish success on plugin-protected servers, a maximum practical reach, or the tick-end movement sequence.
