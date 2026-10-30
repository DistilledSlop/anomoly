# Auto Pearl gameplay test — 2026-10-30

Tested on a local vanilla Minecraft Java 26.3 server with two Fabric clients. PlayerA had only Auto Pearl enabled. PlayerB had all Anomoly modules disabled and threw an ender pearl manually. Both players were in survival mode on a flat stone platform. PlayerA started at `(5.5, 100, 5.5)` with stone selected in hotbar slot 0 and two ender pearls in slot 1. PlayerB started at `(10.5, 100, 5.5)` facing east and slightly upward, with two pearls in slot 0.

After PlayerB used one pearl, the server reported:

| Server observation | PlayerA | PlayerB |
| --- | ---: | ---: |
| Position | `(28.4618, 100, 5.5158)` | `(29.8788, 100, 5.6388)` |
| Ender pearls remaining | 1 | 1 |
| Selected hotbar slot for PlayerA | 0 | — |

The final horizontal separation was about 1.42 blocks. PlayerA consumed exactly one pearl without a manual input and returned to the stone slot. This confirms enemy pearl detection, the landing search, a server-accepted throw, and slot restoration in this setup. It does not establish performance against moving targets, obstructed terrain, or third-party anticheats.
