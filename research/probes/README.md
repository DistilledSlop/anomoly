# Local handler probe reproduction

`TeleportProbe.java` is the exact temporary Fabric source used for the teleport acknowledgment and vertical phase tests. It is stored here as evidence, outside the mod's compiled `src` tree. To rerun on a disposable local world:

1. Copy it to `src/main/java/com/example/TeleportProbe.java`.
2. Add `ServerLifecycleEvents.SERVER_STARTED.register(TeleportProbe::setup)` and `ServerTickEvents.END_SERVER_TICK.register(TeleportProbe::tick)` to `ExampleMod.onInitialize()` behind `Boolean.getBoolean("mc.research.teleportProbe")`. Import both Fabric event classes.
3. Set `spawn-protection=0` in the local `run/server.properties` and accept the EULA in `run/eula.txt`.
4. Run `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 JAVA_TOOL_OPTIONS=-Dmc.research.teleportProbe=true ./gradlew runServer --no-daemon`. For the isolated spear test, use `JAVA_TOOL_OPTIONS='-Dmc.research.teleportProbe=true -Dmc.research.spearOnly=true'`. That mode skips the earlier deliberately invalid movement sequence and ticks the mock packet listener so item-use time advances.
5. Search the server log for `TP_PROBE`, `moved wrongly`, and `moved too quickly`. Remove the temporary compiled source and restore `ExampleMod.java` afterward.

The probe creates an unprivileged survival mock player over an embedded Netty connection, completes login, clears a small test corridor, then invokes real server packet handlers. It is a handler test, not a remote client or anti-cheat test. It changes blocks in `run/world`, so use a disposable world. Relevant output is preserved in [tick-end round trip](logs/tick-end-roundtrip.txt), [same-tick teleport](logs/teleport-same-tick.txt), [bedrock crossing](logs/vertical-bedrock.txt), and [spear motion](logs/spear-motion.txt) excerpts.

`PickProbe.java` is a separate nearby block oracle test. Copy it to `src/main/java/com/example/PickProbe.java`, register its `setup` and `tick` methods with the same Fabric lifecycle events behind `Boolean.getBoolean("mc.research.pickProbe")`, and run with `JAVA_TOOL_OPTIONS=-Dmc.research.pickProbe=true`. Its [saved output](logs/pick-block.txt) records a survival pick-block request through a stone wall.

`WaypointProbe.java` creates two local players and captures azimuth waypoint packets from the receiver's embedded outbound channel. Copy it to `src/main/java/com/example/WaypointProbe.java`, register `setup` and `tick` behind `Boolean.getBoolean("mc.research.waypointProbe")`, and run with `JAVA_TOOL_OPTIONS=-Dmc.research.waypointProbe=true`. Its [saved output](logs/waypoint-bearings.txt) includes two receiver positions, the source UUID, and float bearings.

`CrafterProbe.java` tests menu validity after movement. Copy it to `src/main/java/com/example/CrafterProbe.java`, register `setup` and `tick` behind `Boolean.getBoolean("mc.research.crafterProbe")`, and run with `JAVA_TOOL_OPTIONS=-Dmc.research.crafterProbe=true`. Its [saved output](logs/crafter-window.txt) shows a persistent slot toggle while the menu reports invalid distance.

`LocatorPulseProbe.java` creates two mock players, then sends an accepted eight-block move, a client-tick-end packet, and a return move from the transmitter. Copy it to `src/main/java/com/example/LocatorPulseProbe.java`, register its `setup` and `tick` with the same lifecycle events, and run the local server. Its [saved output](logs/locator-pulse.txt) shows two outbound waypoint packets with different angles while the source returns to its original position. This is the packet-level test for the [locator pulse](../../trolling/locator-pulse.md) idea.
