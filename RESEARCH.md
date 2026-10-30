# Minecraft Java 26.3 client research

This workspace began as Fabric's `fabric-example-mod` **26.3** branch at commit `91f19010a4e764f193fe50acfe2c8ee68f1f15cd`. It now contains the Anomoly client. The source audit below remains a research notebook; see [README.md](README.md) for the currently implemented modules and build instructions.

## What is installed

| Material | Location |
| --- | --- |
| Built example mod | `build/libs/modid-1.0.0.jar` |
| Decompiled common/client source | `minecraft-26.3/sources/` (also in Loom cache) |
| Minecraft 26.3 client/server jars | `minecraft-26.3/jars/` |
| Minecraft assets | `minecraft-26.3/assets/` (index `26.3-34.json` and all hashed objects) |
| Vanilla data pack resources | `minecraft-26.3/jars/minecraft-common.jar` |

The asset index lists **5,147 objects / 485,039,791 bytes**, and every indexed object is present in `minecraft-26.3/assets/objects`. Those files are hardlinked to Loom's cache, so the workspace copy uses no extra data blocks on this filesystem; copying the directory elsewhere creates an independent copy. The large downloaded files and generated build output are excluded from Git by `.gitignore`. To rebuild, run `build`, `genSources`, and `downloadAssets` as separate Gradle invocations with Java 25; combining `build genSources` triggers a Gradle implicit dependency validation failure in this Loom/Gradle combination.

Run `python3 research/verify.py` to check every asset's SHA-1 and size, key source control-flow evidence, and the 26.3 abandoned-camp placement parameters.

**Checks performed:** `./gradlew build` succeeded; `./gradlew genSources` succeeded; `./gradlew downloadAssets` succeeded; `python3 research/verify.py` verified all 5,147 asset SHA-1 hashes and the cited source patterns; `./gradlew runServer` launched a local 26.3/Fabric 0.20.1 server and reached `Done`. Local mock-player probes exercised the actual server handlers and [confirmed through-wall chest opening and pig damage](research/local-probe-results.md), [two correction hops in one server tick](research/teleport-ack-probe-results.md), [vertical crossing through stone and bedrock roofs](research/vertical-phase-probe-results.md), a [move/interact/return sequence](research/probes/logs/tick-end-roundtrip.txt), [spear reach/damage amplification](research/spear-motion-probe-results.md), [hidden chest identification via survival pick-block](research/probes/logs/pick-block.txt), and [a persistent crafter slot toggle at invalid menu distance](research/probes/logs/crafter-window.txt). Separate probes [captured distant-player waypoint packets](research/probes/logs/waypoint-bearings.txt) at two positions and [captured two locator packets during an eight-block excursion and return](research/probes/logs/locator-pulse.txt). The probe source is [preserved](research/probes/README.md) outside the compiled mod. The clean example mod rebuilt successfully after the latest probe. These behaviors have **not** been reproduced over a remote client connection or on anti-cheat servers.

## Findings and confidence

| Finding | Status | Impact |
| --- | --- | --- |
| 26.3 teleport acknowledgment accepts a bounded client position offset | Source-confirmed; two eight-block hops and chest interactions reproduced in one server tick | Resource-free correction-based movement through open space |
| Spear uses client-authored movement for ray length and kinetic damage | Nine-block jab and 151 damage to a Warden locally reproduced | Extended spear reach and high-damage held hit after an accepted step |
| Vertical movement residual is always zeroed after collision | Source-confirmed in 26.2 and 26.3; stone and bedrock roof crossings reproduced locally | Bounded vertical phase through solid layers with an empty endpoint |
| Attack packet's extra 3 block range and absent sight check | Source-confirmed; through-wall damage reproduced locally | Reach/through wall combat on vanilla servers |
| Block-use packet returns two authoritative block states without sight check | Source-confirmed; through-wall chest opening reproduced locally | Hidden block probe; through wall block interaction |
| Survival pick-block packet has no sight check and selects an owned clone item | Hidden chest behind stone selected from hotbar locally reproduced | Nearby item-type oracle independent of block-update responses |
| Crafter slot-state packet omits menu distance validity | Empty slot toggled at invalid menu distance and persisted after closure | One-tick nearby machine configuration change |
| Client tick end packet clears position-per-tick guard | Move eight blocks, open a chest, return in one server tick and persist locally reproduced | Remote interaction excursion without a correction |
| Shared map decorations follow other holders | Confirmed in 26.3 server source | Remote player tracking with a shared map |
| Locator bar sends a distant player's UUID and float bearing | Two packets captured; one stationary source triangulated within 0.001 block locally | Estimate a transmitting player's X/Z from two observation points |
| Chunk packet includes complete section data and block entity summaries | Confirmed in 26.3 server source | Xray and base finding within sent chunks |
| Creative slot, gamemode, flight and operator probes | Server checks confirmed | No vanilla privilege escalation established |

See [exploits](exploits/README.md) for packet analyses, [hacks](hacks/README.md) for feature designs, the [eight-client cheat catalog](cheats/README.md) for a deduplicated mainstream inventory, and [player-interaction research](trolling/README.md) for separate 26.3 ideas. "Exploit" here means a client/server trust or visibility mismatch; some are longstanding vanilla behavior, not new bugs introduced by 26.3. The teleport acknowledgment path is new in 26.3 relative to 26.2; the vertical residual error also exists in 26.2. No claim of a novel operator, creative, or item injection bypass is made.

See [26.3-specific changes](research/version-26.3.md) for porting implications of new maps, structures, wool blocks, straw beds, input backend, and the pearl momentum change.

## Sources

- [Official 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) for game/data changes.
- [Fabric 26.3 porting notes](https://www.fabricmc.net/2026/09/15/263.html).
- [Fabric example mod 26.3 branch](https://github.com/FabricMC/fabric-example-mod/tree/26.3).
- [Wurst's public module registry](https://github.com/Wurst-Imperium/Wurst7/blob/master/src/main/java/net/wurstclient/hack/HackList.java) and [Meteor categories](https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/Categories.java) for the public-client baseline.

All Minecraft implementation references below refer to the locally generated **26.3** source jars, primarily `net.minecraft.server.network.ServerGamePacketListenerImpl`, `net.minecraft.server.level.ServerPlayerGameMode`, and `net.minecraft.world.level.saveddata.maps.MapItemSavedData`. Decompiled line numbers are local navigation aids, not stable APIs.
