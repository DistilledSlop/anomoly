# 26.3 spear motion probe

The local dedicated server used Minecraft 26.3 with a temporary property-gated Fabric probe. It created an unprivileged survival mock player over an embedded connection, gave it an iron spear, cleared the test corridor, and invoked the real packet handlers. For the isolated spear run, the probe also called `ServerGamePacketListenerImpl.tick()` each server tick so the mock player's held-item timer advanced. Earlier mixed runs showed why this matters: the mock entity itself ticked, but its item-use timer remained at 72,000 until its packet listener was ticked.

The successful output is preserved in [the log excerpt](probes/logs/spear-motion.txt). Relevant observations:

```text
TP_PROBE spear_advance pos=(8.5, 100.0, 0.5) known=(8.0, 0.0, 0.0) target_health=10.0
TP_PROBE spear_stab pos=(8.5, 100.0, 0.5) target_health=7.0
TP_PROBE spear_zero_known=(0.0, 0.0, 0.0) zero_ray_entities=Optional.empty
TP_PROBE spear_return pos=(0.5, 100.0, 0.5) pending=null
TP_PROBE kinetic_timer server_tick=40 player_tick=40 remaining=71990 using=true
TP_PROBE kinetic_advance pos=(8.5, 100.0, 0.5) known=(8.0, 0.0, 0.0) target_health=500.0 player_tick=44 remaining=71986
TP_PROBE kinetic_next_tick target_health=349.0 alive=true player_known=(8.0, 0.0, 0.0) using=true player_tick=45 remaining=71985
```

The source ray trace reported the pig among hit entities with the forward movement vector, and none after two client-tick-end packets set the vector to zero. The Warden was stationary and had no AI; the 151-health difference was measured after one server player tick. This is a local handler/item test, not a remote client or plugin compatibility test.
