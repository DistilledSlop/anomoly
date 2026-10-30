# 26.3 teleport acknowledgment probe

The local dedicated server used Minecraft 26.3, Fabric Loader 0.20.1, and the example mod with a temporary property-gated probe. It created an unprivileged survival mock player with an embedded connection, completed login and initial teleport acknowledgment, and cleared a corridor at Y=100. Stone at Y=99 provided support; test chests were placed at X=11 and X=19. Spawn protection was set to zero. The probe invoked the real `ServerGamePacketListenerImpl` handlers from server tick callbacks.

The successful run is `/tmp/mc263-teleport-probe4.log` in this session. Key observations:

```text
TP_PROBE issued teleport expected=(0.5, 100.0, 0.5) id=2
TP_PROBE ack_offset_8 pos=(8.5, 100.0, 0.5) pending=null
TP_PROBE remote_chest_menu=ChestMenu pos=(8.5, 100.0, 0.5)
TP_PROBE next_tick_pos=(8.5, 100.0, 0.5)
teleport-probe moved too quickly! 992.0,0.0,0.0
TP_PROBE forced_correction pos=(8.5, 100.0, 0.5) pending=(8.5, 100.0, 0.5) id=3
TP_PROBE chained_ack_offset_8 pos=(16.5, 100.0, 0.5) pending=null
TP_PROBE chained_chest_menu=ChestMenu pos=(16.5, 100.0, 0.5)
TP_PROBE final_pos=(16.5, 100.0, 0.5)
```

The first probe run had generated terrain in the corridor and was corrected as "moved wrongly." A later run had a chest at X=11 obstructing the second hop; clearing it allowed the hop. These negative controls establish that ordinary collision remains effective. This test does not include a remote socket, an anti-cheat plugin, or sustained speed measurement.

The subsequent run, preserved as [same-tick log excerpt](probes/logs/teleport-same-tick.txt), chained both eight-block offsets **within the same server tick**. After the first accepted offset and chest interaction, the mock sent `ServerboundClientTickEndPacket`, an impossible X=1000.5 movement to trigger a correction, then an acknowledgment for the correction ID with X=16.5. The second chest opened before that server tick ended, and X=16.5 persisted into the next tick:

```text
TP_PROBE ack_offset_8 pos=(8.5, 100.0, 0.5) pending=null
TP_PROBE remote_chest_menu=ChestMenu pos=(8.5, 100.0, 0.5)
TP_PROBE forced_correction pos=(8.5, 100.0, 0.5) pending=(8.5, 100.0, 0.5) id=3
TP_PROBE same_tick_chained_ack pos=(16.5, 100.0, 0.5) pending=null
TP_PROBE chained_chest_menu=ChestMenu pos=(16.5, 100.0, 0.5)
TP_PROBE next_tick_pos=(16.5, 100.0, 0.5)
```
