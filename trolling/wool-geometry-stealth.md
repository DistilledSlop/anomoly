# Sculk-sensor misdirection with 26.3 wool shapes

**Status:** Source and bundled data confirm the rule; no live sensor probe has been run. This is a 26.3-specific tactic using new wool slabs and stairs.

The 26.3 `minecraft:dampens_vibrations` block tag contains wool, wool carpets, **wool slabs**, and **wool stairs**. `VibrationSystem.User.isValidVibration` rejects an event when its affected block state has this tag. These extra shapes let a player construct a walkable or decorative route around a sensor that damps relevant block events while looking less like an obvious full-wool or carpet path. That can be used for a surprise visit or a bait corridor where another player's sculk detector stays quiet for events on the wool geometry.

This does **not** mean the shape blocks every sound or every sculk event. Sneaking's event filter and entity vibration damping have separate rules, and the tag applies to an event's `affectedState`; events involving a different block state may still propagate. The `occludes_vibration_signals` line-of-sight tag is a separate mechanic and should not be conflated with dampening. A sensor test with each intended action should precede any claim of reliable stealth.

**Evidence:** `data/minecraft/tags/block/dampens_vibrations.json` in `minecraft-26.3/jars/minecraft-common.jar`, and `net.minecraft.world.level.gameevent.vibrations.VibrationSystem.User#isValidVibration` near line 424. The new wool shapes are listed in the [official 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3).
