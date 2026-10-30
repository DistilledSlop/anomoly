# Auto Tunnel gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 multiplayer server. A survival player held an iron pickaxe, carried cobblestone and torches, and faced east on a stone floor. Server console queries measured block states, player position, inventory, and selected slot; the client log recorded pause and completion messages.

| Probe | Server and client observation |
| --- | --- |
| 2×2 corridor, length 3 | The module mined all 12 stone cells across three segments, walked from X 400.5 to X 403.27, then disabled itself with a `completed 3 blocks` message. The pickaxe gained 12 damage points. |
| Floor repair | With both floor cells missing under a new 2×2 wall, it mined the four wall cells, placed cobblestone in both floor gaps, crossed them, restored the pickaxe slot, and stopped at the one-block length limit. |
| Fluid guard | A lava source beside the next segment left the stone wall intact and the player stationary. The client reported `Fluid beside the next tunnel segment`. |
| Protected block entity | Replacing a wall cell with a chest left the chest intact and the player stationary. The client reported the protected-block pause once while the chest remained, and kept the pickaxe selected. |
| Falling-block guard and recovery | Sand above the next wall left the wall intact and the player stationary. The client reported `Falling block above the next tunnel segment`. Removing the sand let it mine the segment and complete the configured length. |
| Torch placement | With a three-block interval and four torches in the hotbar, it walked a three-block clear corridor, placed one torch at `(408,100,401)`, left three torches, and disabled at the length cap. |

The module uses normal Minecraft mining and placement interactions, chooses a suitable hotbar tool, restores the previous slot, and stops at the configured length. Width, height, floor repair, edge guard, hazard guards, and torch interval are adjustable. These runs establish the tested path on a vanilla server; protected claims, moving hazards, longer tunnels, and third-party anticheats need separate tests. Its safety checks reduce exposure but cannot guarantee that a player will survive every tunnel.
