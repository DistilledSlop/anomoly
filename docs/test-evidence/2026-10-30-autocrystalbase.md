# Auto Crystal Base gameplay test — 2026-10-30

Tested on a local vanilla Minecraft Java 26.3 server. PlayerA stood at `(5.5,100,5.5)` with stone selected in hotbar slot 0, obsidian in slot 1, and end crystals in the offhand. Auto Crystal Base used normal `MultiPlayerGameMode.useItemOn` block interactions. The server console supplied block states, health, inventory, and position observations.

The base planner checks that an obsidian block can be placed into replaceable space with a supporting face and a clear crystal volume above it. It scores the expected blast with the *future obsidian block present*, because that block can shield a target. When Crystal Aura is enabled, it uses Crystal Aura's target and damage thresholds, and Crystal Aura waits one tick after base placement. Without Crystal Aura, Auto Crystal Base uses its own thresholds and stops when a usable base already exists.

| Probe | Server observation |
| --- | --- |
| Ground-level target on flat stone | A pig at `(9.5,100,5.5)` remained at 100 health. No base appeared at the candidate positions; a base one block above the stone floor would shield the pig from its crystal blast. |
| Elevated target, Auto Crystal Base only | With the pig at `(9.5,101,5.5)` on a stone pedestal and PlayerA creative, one obsidian base appeared at `(8,100,5)`. Nearby `(9,100,4)` and `(9,100,6)` remained empty. Stone remained selected and the pig stayed at 100 health. |
| Default survival safety | In the same elevated setup with PlayerA at 20 health, Auto Crystal Base placed no block and the pig stayed at 100 health. The predicted raw self-damage exceeded the default limit. |
| Crystal Aura handoff | Switching PlayerA to creative let Auto Crystal Base place the single base. Enabling Crystal Aura then placed and detonated a crystal from it, killing the 100-health pig and knocking PlayerA back to X 4.2783. |

An earlier planner that scored the location before considering the placed obsidian produced three bases on the flat floor; Crystal Aura correctly ignored them because the new blocks shielded the pig. The projected blast check corrected that behavior. No kick or disconnect occurred in these local runs.

This test covers a static elevated mob, hotbar obsidian, and offhand crystals. Offhand obsidian, moving targets, protected regions, and third-party anticheat compatibility remain unverified. The module does not mine solid floor blocks to replace them with obsidian.
