# Projectile aim gameplay test — 2026-10-30

The client ran on a local Minecraft 26.3 vanilla multiplayer server using the OpenGL/Xvfb test setup described in the [earlier gameplay report](2026-10-30-local-gameplay.md). I built a level stone platform at Y=99, kept both players stationary at `(5.5, 100, 5.5)`, and used server commands to place stationary mobs ahead of them. Both aim modules were configured to include mobs, with a 180° field of view and 90° maximum turn per tick. The modules changed view rotation but did not fire; I used the mouse to release the bow and throw the snowball.

## Bow Aimbot

I gave the first player a bow and arrows and placed a pig at `(7.5, 100, 21.5)`, about 16 blocks ahead. With `bow_aimbot` active, profile samples settled at yaw `−7.13°`, pitch `+1.63°`. I held use for approximately 1.8 seconds, then released. The server recorded that the pig **was shot by the player** and awarded the Take Aim advancement. The [captured game frame](../screenshots/bow-aimbot-hit.png) shows the pig dying and the advancement message. This verifies a completed bow hit; it does not measure performance against moving targets or anticheats.

In a separate session, I gave another player a crossbow and arrows and placed a fresh pig at the same position. Bow Aimbot settled at yaw `−7.13°`, pitch `+1.86°`. I charged the crossbow, released use to load it, then clicked use once to fire. The server recorded that the pig **was shot by the player** and awarded both Ol' Betsy and Take Aim. The [crossbow game frame](../screenshots/crossbow-aimbot-hit.png) shows the pig dying and both advancements. This verifies the arrow-loaded crossbow path. Firework-loaded crossbows still need a gameplay test.

## Projectile Aimbot

I gave the second player snowballs and placed a stationary, no-AI blaze at `(7.5, 100, 17.5)`, about 12 blocks ahead. The server reported the blaze at `20.0f` health before the throw. With `projectile_aimbot` active, profile samples settled at yaw `−9.46°`, pitch `−2.60°`. One manually thrown snowball hit the blaze; the server then reported `17.0f` health. The [captured game frame](../screenshots/projectile-aimbot-snowball-hit.png) shows the snowball near the blaze. Minecraft 26.3's Snowball source applies three damage to a blaze on hit, matching the observed health change.

## Flight model and limits

The solver uses the launch speed, drag, gravity, spawn offset, and update order from Minecraft 26.3's `BowItem`, `CrossbowItem`, `TridentItem`, `SnowballItem`, `EggItem`, `EnderpearlItem`, `ThrowablePotionItem`, `ExperienceBottleItem`, `WindChargeItem`, `AbstractArrow`, `ThrowableProjectile`, and `FireworkRocketEntity` sources. It leads targets linearly from their current velocity and rejects trajectories that hit a solid block or fluid before the target. Random weapon inaccuracy and changes in target movement can still cause misses.

Bow, arrow-loaded crossbow, and snowball variants were tested against live targets in this report. Firework-loaded crossbows, tridents, eggs, pearls, potions, experience bottles, wind charges, and moving targets need separate gameplay tests. The local vanilla server has no anticheat plugin, so this does not establish anticheat compatibility.
