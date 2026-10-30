# ReverseStep local gameplay test — 2026-10-30

I tested Anomoly against a local vanilla Minecraft Java 26.3 server. An upper stone block at (80, 101, 80) put the player's feet at y=102. A lower stone path began at z=81 with its surface at y=101. The player faced south, walked forward, and fell one block at the edge. Other movement modules were disabled. I used `-Danomoly.profile.sampleIntervalTicks=1` to capture client position and velocity once per game tick, then queried the server after landing. The selected raw samples are in [the trace CSV](2026-10-30-reversestep-traces.csv).

| Run | Descent from the edge | Server landing |
| --- | --- | --- |
| Off | y=101.9216 → 101.7664 → 101.5358 → 101.2315 → 101.0000 over five movement ticks | y=101.0 |
| Instant, 20 simulation ticks, gradual off | y=102.0000 → 101.0001 in one movement tick | y=101.0 |
| Strict, downward motion 1.0 | y=101.9216 with velocity −1.0, then y=101.0000 | y=101.0 |
| Accelerator, factor 3.0 | y=101.9216 with velocity −0.4657, then y=101.4559 with velocity −1.6043, then y=101.0000 | y=101.0 |
| Instant, gradual on | y=101.9216 → 101.6144 → 101.3073 → 101.0001 → 101.0000 | y=101.0; player remained connected |

The three modes performed distinct descents and all landed on the lower path. Instant mode with gradual descent uses the normal client movement packet cadence. The first implementation sent multiple position packets in one server tick; the 26.3 server disconnected the client with “Invalid move player packet received.” I removed that burst, spread the descent across ticks, and repeated the test successfully. The gradual option no longer sends extra packets.

I also tested three guards in Strict mode. With slime at the landing block, velocity followed the ordinary fall curve (−0.1552, −0.2305, −0.3043) instead of the configured −1.0. With the next landing more than one block below, the first drop likewise followed ordinary gravity; a later separate one-block drop still activated Strict. A deliberate Space jump rose and fell with ordinary jump velocities, without a −1.0 override. Other listed hazardous blocks, higher maximum distances, vehicles, and third-party anticheats remain untested.

The Dear ImGui ClickGUI displayed ReverseStep's mode, maximum distance, whole-number simulation ticks, gradual option, Strict motion, and Accelerator factor. Moving the simulation-ticks slider saved `30` as an integer in Anomoly's config. The [publisher's ReverseStep documentation](https://liquidbounce.net/docs/modules/movement/reversestep) describes the three modes and guards that informed this implementation.
