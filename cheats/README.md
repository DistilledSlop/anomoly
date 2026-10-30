# Cross-client cheat catalog

Snapshot: **2026-10-29**. The [machine-readable catalog](catalog.json) contains **682 conservatively deduplicated features** from 1,229 published module entries across eight clients. Every feature has a [behavior page](features/killaura.md) with its summary, aliases, client-specific descriptions, and source links. Category pages are the readable inventory:

| Category | Inventory |
| --- | --- |
| Combat | [Combat](combat.md) |
| Movement | [Movement](movement.md) |
| Player and inventory | [Player](player.md) |
| World and building | [World](world.md) |
| Rendering and intelligence | [Render](render.md) |
| Protocol and exploit | [Exploit](exploit.md) |
| Chat, client and utility | [Misc](misc.md) |
| Minigames and fun | [Fun](fun.md) |

The deduplication rule is conservative: punctuation, spaces and capitalization are ignored; a [reviewable alias table](../research/build_cheat_catalog.py) joins behaviorally equivalent names. For example, `Aura`/`KillAura`, `Hitbox`/`Hitboxes`, `HoleFill`/`HoleFiller`, and the three miss-cooldown names now share entries. `Search` and `XRay` remain separate because highlighting selected blocks and hiding surrounding terrain are different render behaviors. `AutoBow` and `BowAimbot` remain separate because AutoBow can also manage charging and firing. `HitParticles` and `Particles` remain separate because ThunderHack registers both as distinct modules. No entry merges two registrations from the same client.

All 1,229 client variants now have a behavior description. Most come from the publisher's feature page, documentation, or translation file; a labeled subset comes from reviewing the public module source. The [source review supplements](../research/sources/thunderhack-supplements.json) explicitly identify two ThunderHack registrations, `AntiBallPlace` and `NoPacketException`, as empty stubs in that source snapshot. These descriptions track what the clients advertise or implement; they are not a claim that every behavior works on Minecraft 26.3 or an arbitrary server.

## Sources and limits

| Client | Inventory basis | Scope |
| --- | --- | --- |
| [Meteor](https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java) | Local source at `e3a2e77` | 169 registered modules, including nested packages |
| [Wurst](https://github.com/Wurst-Imperium/Wurst7/blob/master/src/main/java/net/wurstclient/hack/HackList.java) | Local source at `4fa258b` | 151 registered hacks; unregistered classes excluded |
| [Impact](https://impactclient.net/features) | Publisher feature page | 117 listed modules; older client feature set |
| [RusherHack](https://rusherhack.org/features/) | Publisher feature list and module pages | 166 listed modules |
| [LiquidBounce](https://liquidbounce.net/docs/modules/overview) | Publisher's eight module category pages | 240 listed modules |
| [ThunderHack Recode](https://github.com/Pan4ur/ThunderHack-Recode/blob/main/src/main/java/thunder/hack/core/manager/client/ModuleManager.java) | Public source at `49c76bc` | 230 registered modules and HUD elements; repository archived in 2024 |
| [Vape](https://www.vape.gg/index.php/features) | Publisher feature page | 15 explicitly matched names; **partial** |
| [Future](https://www.futureclient.net/#features) | User-saved [HTML](<../Future - Minecraft Client.html>) for the Combat tab and [tab transcript](../tabs.txt) for the six dynamic tabs | 141 published modules across seven tabs |

The page inventories are claims about each client, not a statement that their features work on Minecraft 26.3 or on an arbitrary server. Modules like `ForceOp`, `ItemGenerator`, `CreativeFlight`, and old `CrashChest` remain cataloged because a publisher lists them. They do **not** establish a present-day vanilla privilege or item-creation exploit. See the [26.3 permission audit](../exploits/permission-boundaries.md). GUI widgets, cosmetics and automation are retained because the publishers list them as modules; the category pages make them easy to skip.

This is a broad Java Edition cross-section rather than a literal inventory of every client or every private addon. The Aristois wiki feature list found during the survey is dated 2021, and BleachHack's official download page is centered on 1.20.4 and older, so neither was used as a current module inventory. Vape coverage is partial. Future's 141 names cover the seven published feature tabs in the saved snapshot, not undisclosed features or addons. New 26.3 player-interaction research is kept [separate from this catalog](../trolling/README.md).

Rebuild the snapshot with `research/build_cheat_catalog.py`; it needs Python `requests` and `beautifulsoup4`, the two saved Future files, and local source checkouts at `/tmp/meteor-client`, `/tmp/wurst7`, and `/tmp/thunderhack-recode`. The generated JSON and category pages are checked in so readers do not need to run the scraper.
