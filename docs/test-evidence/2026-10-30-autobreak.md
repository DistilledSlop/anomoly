# Auto Break / Auto Mine gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. PlayerC had Auto Break enabled, was in survival mode with an empty inventory, and no attack key was pressed. The server teleported PlayerC to `(100.5,100,50.5)` at yaw 0 and pitch 0, facing a dirt block at `(100,101,53)`.

| Probe | Server observation |
| --- | --- |
| Targeted block | Within four seconds, the server found air at `(100,101,53)` after Auto Break mined the dirt. PlayerC's rotation remained yaw 0, pitch 0. |
| Looked away | The server turned PlayerC to yaw 90, pitch 0, then restored the dirt block. Four seconds later the server still found dirt at `(100,101,53)`. |

The Auto Mine catalog entry describes the same behavior: mine the block under the crosshair without holding attack. Anomoly exposes it as one module, **Auto Break**. This test does not cover mining speed changes, protected blocks, tools, or third-party anticheats.
