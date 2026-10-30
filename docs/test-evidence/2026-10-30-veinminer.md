# Vein Miner gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. A survival player stood at `(400.5,100,400.5)` facing a coal ore block at `(402,100,400)` and held an iron pickaxe. An actual mouse hold through the headless client started the break; the module then continued through connected blocks after the mouse was released. Server console queries measured block states, inventory, and selected slot.

| Probe | Server observation |
| --- | --- |
| Face-connected ore vein | The clicked coal ore and its three face-connected neighbors at `(402,100,401)`, `(403,100,401)`, and `(403,101,401)` became air. The iron pickaxe took four durability points in total. |
| Different and disconnected blocks | An adjacent stone block at `(402,100,399)` and a nearby but disconnected coal ore at `(401,100,398)` remained intact. |
| No-click control | A new pair of coal ore blocks at `(399,100,400)` and `(399,100,401)` remained intact for several seconds while Vein Miner was enabled and no attack was made. The pickaxe's damage stayed at four. |
| Vein-size limit | With `max_blocks=2`, clicking a three-block connected chain mined the clicked block and one neighbor. The third ore at `(403,100,401)` remained, and the pickaxe's damage rose from four to six. |

Vein Miner defaults to ore and log block IDs, with a setting for additional block IDs or all block types. It searches face-connected blocks by default and can include diagonals. The queue is capped by block count and search radius; mining is limited by the player's actual block interaction reach. The client uses ordinary mining start, continue, and stop actions. Tool auto-selection, diagonal connections, logs, moving players, protected regions, and third-party anticheats need separate gameplay tests. The optional line-of-sight check is off by default.
