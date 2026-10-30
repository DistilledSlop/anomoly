# LiquidInteract local gameplay test — 2026-10-30

I ran Anomoly in a local vanilla Minecraft Java 26.3 multiplayer world. The player stood at (50.5, 123, 50.5), facing south with dirt in the main hand. A water source was at (50, 124, 52), a stone backing at (50, 124, 54), and a light block at (50, 124, 51) kept water from spreading toward the player without blocking the view. The interaction cells were reset between clicks. I toggled LiquidInteract with a bound key and queried server block states after each physical right-click.

| Module state | Server-observed result |
| --- | --- |
| On | Dirt replaced the water source at (50, 124, 52); the stone-facing cell at z=53 remained clear. One dirt was consumed. |
| Off | Dirt appeared at (50, 124, 53), against the stone behind the water. The water source at z=52 remained. One dirt was consumed. |

The 26.3 client normally raycasts blocks with fluids excluded. LiquidInteract changes only the fluid-inclusion argument of the local player's ordinary target raycast. The paired server result confirms that this changes a real placement target, not just the displayed crosshair. Lava and third-party anticheats were not tested.
