# 26.3 vertical phase probe

The local dedicated server used Minecraft 26.3 and the property-gated temporary Fabric probe described in [teleport test record](teleport-ack-probe-results.md). The mock player was unprivileged and in survival mode. The probe called the actual `ServerGamePacketListenerImpl.handleMovePlayer` handler with `ServerboundMovePlayerPacket.Pos` packets.

The successful stone run is `/tmp/mc263-vertical-probe2.log` in this session. A 3×3 stone roof covered Y=102 above the player at Y=100. The destination at Y=104 was empty.

```text
TP_PROBE next_tick_pos=(16.5, 100.0, 0.5)
TP_PROBE vertical_roof_cross_pos=(16.5, 104.0, 0.5) roof=Block{minecraft:stone} pending=null
TP_PROBE vertical_next_tick_pos=(16.5, 104.0, 0.5)
TP_PROBE vertical_return_pos=(16.5, 100.0, 0.5) roof=Block{minecraft:stone} pending=null
TP_PROBE vertical_return_next_tick_pos=(16.5, 100.0, 0.5)
```

The server did not log `moved wrongly` or issue a teleport correction during these vertical moves. The probe ran in process rather than through a remote socket; this verifies vanilla handler behavior and state persistence, not compatibility with anti-cheat plugins.

The additional bedrock run is preserved as a [log excerpt](probes/logs/vertical-bedrock.txt). The probe replaced the roof with three layers of bedrock at Y=102–104, kept Y=106–107 clear, and requested Y=106 from Y=100:

```text
TP_PROBE bedrock_roof_cross_pos=(16.5, 106.0, 0.5) center=Block{minecraft:bedrock} pending=null
TP_PROBE bedrock_next_tick_pos=(16.5, 106.0, 0.5)
```
