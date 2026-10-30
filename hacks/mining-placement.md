# Mining and placement scheduler

**Value:** Mine and build efficiently at the server's accepted rate. Meteor PacketMine/SpeedMine/Nuker and Wurst FastBreak/FastPlace/Scaffold are the baseline.

**Design:** Compute block break progress from actual tool, enchantments, status and block state. Send start/stop in the correct sequence, reconcile block updates and action acknowledgments, and avoid premature repeated starts. For placement, schedule face/hit vectors within `blockInteractionRange + 1`, reserve inventory slots, and verify each block update before chaining. A separate experimental mode can try occluded `UseItemOn` interactions where the block-probe finding applies; keep it off during ordinary building because it may operate the wrong block.

**Constraints:** `ServerPlayerGameMode.handleBlockBreakAction` owns break progress and reach; `handleUseItemOn` owns interaction and spawn protection. A client cannot mine an unloaded remote block by repeating packets. The 0.7 stop-progress acceptance threshold in the server code is worth measuring for latency optimization, but is not instant mining.
