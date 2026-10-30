# Criticals gameplay test — 2026-10-30

Tested against a local vanilla Minecraft Java 26.3 multiplayer server. PlayerA stood at `(5.5, 100, 5.5)` with an iron sword, facing a stationary pig at `(7.8, 100, 5.5)`. All other modules were disabled for the manual tests. The target's health was read from the server console after each attempt.

The 26.3 source requires a descending, airborne, nonsprinting attacker with sufficient attack cooldown for a critical hit. The server also disconnects a client that sends a second movement packet in the same server tick. Criticals therefore uses an ordinary jump and waits for descent before sending the usual attack packet. It sends a sprint-stop command before the jump and attack, checks the target and held item while waiting, and skips the jump when the player has insufficient headroom.

| Run | Target health before | Target health after | Damage | Observation |
| --- | ---: | ---: | ---: | --- |
| Criticals off, manual attack | 10 | 4 | 6 | Normal iron sword hit |
| Criticals on, manual attack | 10 | 1 | 9 | Player rose to Y 101.2522; no damage before descent |
| Criticals on, one-block low ceiling | 10 | 4 | 6 | Jump was skipped; normal hit still landed |
| Criticals and Kill Aura on, isolated automatic attack | 100 | 91 | 9 | One jump-timed critical hit |

The first combined Kill Aura probe exposed an early `ServerboundPunchPacket`: it reset the server's attack cooldown before the delayed attack and left the target at 96.02752 health after an isolated attempt. The integration now holds the swing and punch packet until the attack actually lands. A fresh 100-health target then fell to 91 health in the same isolated setup. No kick or disconnect occurred in these local runs.

The target was stationary, and these tests used a vanilla server. Moving targets, other weapons, and third-party anticheat compatibility remain unverified.
