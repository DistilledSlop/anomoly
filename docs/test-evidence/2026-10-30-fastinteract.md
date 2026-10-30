# Fast Interact local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world. Fast Interact's default Fast Place, Fast Break, and Fast Use switches and zero-tick intervals were used. The standalone Fast Place, Fast Break, and FastExp modules were disabled. I toggled Fast Interact through a bound key, held the normal use or attack mouse button for 1.2 seconds in each case, and read server state after each hold.

| Branch | Off | On | Server measurement |
| --- | ---: | ---: | --- |
| Fast Use | 6 bottles | 24 bottles | Bottles of enchanting consumed from a fresh stack of 64 while aiming into open sky. |
| Fast Place | 6 placements | 12 placements | Dirt placements counted by `minecraft.used:minecraft.dirt`. A temporary server tick function cleared the placed dirt at (50, 124, 51), exposing the same stone target at (50, 124, 52) for each click. |
| Fast Break | 4 breaks | 11 breaks | In creative mode, the server tick function restored a stone target after each removal and incremented a scoreboard counter whenever it found the target cell empty. |

Fast Interact limits the client's existing right-click and block-break delays; it does not change item consumption duration. The temporary datapack was disabled and removed after the test. Nonzero interval settings, survival mining progress, and third-party anticheats were not tested here.
