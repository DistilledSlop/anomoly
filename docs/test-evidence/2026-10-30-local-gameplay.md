# Local Minecraft 26.3 gameplay test — 2026-10-30

Anomoly ran in a 960×600 OpenGL client on Xvfb and joined a local Minecraft 26.3 vanilla multiplayer server at `127.0.0.1:25577`. The client rendered the world and its ImGui ClickGUI. This test used `ANOMOLY_TEST_NO_SRGB=1` because the software GLX display does not provide the sRGB framebuffer visual requested by Minecraft. The override is inactive unless that environment variable is set.

After fitting the ImGui window within the viewport, I reopened the ClickGUI in the joined world and confirmed the category sidebar, content list, and panic button were visible. Moving the mouse directly to Movement and clicking immediately selected that category. A [screenshot](../screenshots/clickgui-opengl-960x600.png) records the result.

## Auto Walk and panic

I selected Movement in the ClickGUI, enabled Auto Walk, closed the GUI, and did not hold the forward key. The passive profile showed `modules: "auto_walk"` and these positions:

| Client tick | Z position | Active modules |
| ---: | ---: | --- |
| 1840 | -78.5 | `auto_walk` |
| 1860 | -77.245 | `auto_walk` |
| 1880 | -72.931 | `auto_walk` |
| 1900 | -68.614 | `auto_walk` |
| 1920 | -65.3 | `auto_walk` |

The player then reached an obstacle and stopped. After pressing End, the profile changed from `auto_walk` at tick 2320 to an empty module list at tick 2340. This verifies the module's forward movement and the panic key's disable-all behavior in the local world. It does not establish behavior on public servers or with anticheat plugins.

The same profile recorded server brand `vanilla`, one initial join position correction at tick 0, and no later position corrections during the observed period. The test logs are generated locally under `run/config/anomoly/profiles/`; they are ignored by Git because they contain coordinates and server addresses.

## Aimbot and Aim Assist

In a later joined session, I enabled Aimbot with mob targeting and spawned a stationary, invulnerable pig about four blocks forward and two blocks to the side using the local server console. The passive samples showed player yaw/pitch at `0°/0°` before the pig spawned and `−26.57°/10.82°` afterward while `aimbot` was active. No attack key was pressed for this test.

I then disabled Aimbot, enabled Aim Assist with mob targeting, and reset the player view to `0°/0°` through the local server console. The first pig was outside Aim Assist's default 50° horizontal field of view; the logged view stayed at `0°/0°`. I spawned a second stationary pig within that field and held attack for four seconds. Samples changed from `0°/0°` at tick 1740 to `−14.04°/11.72°` at tick 1760 while `aim_assist` was active. The server issued a manual teleport correction when I reset the view, so this session is not suitable for measuring natural anticheat correction rates.

These tests verify client view rotation and the Aim Assist attack-key gate against visible mobs on the local vanilla server. They do not establish performance against moving targets, players, or remote anticheats.

## Passive command inventory

With the Plugins module enabled on the local vanilla server as a non-operator, the client read ten root commands from the server-supplied command tree: `help`, `list`, `me`, `msg`, `random`, `teammsg`, `tell`, `tm`, `trigger`, and `w`. The profiler wrote ten `command_literal` events and the offline analyzer reported the same list. The [server profile screenshot](../screenshots/server-profile-opengl-960x600.png) shows the ImGui page with ten visible commands and no namespaced commands. The client sent no command enumeration request. This verifies passive command collection and display; it does not verify identification of a real server plugin.

## No Rotate and control

On the local server, I enabled No Rotate and issued two separate console commands to the joined player. `execute as Player480 at @s run tp @s ~ ~ ~ 90 20` sent a position correction with requested rotation; the profiler recorded correction ID 2 at tick 143, while yaw/pitch stayed at `−180°/0°` through subsequent samples. `rotate Player480 45 10` exercised the dedicated rotation packet; the view still sampled at `−180°/0°` afterward.

In a second joined session with No Rotate disabled, the same `/rotate` command changed the sampled view from `0°/0°` to `45°/10°` by tick 160. The teleport command then changed it to `90°/20°`, recorded with correction ID 2 at tick 309. This paired control verifies that both server rotation paths normally affect the client and that No Rotate suppresses both in the local test. It does not establish compatibility with a remote anticheat.

## Reproduction

Start a local 26.3 server, then run:

```sh
ANOMOLY_TEST_NO_SRGB=1 ALSOFT_DRIVERS=null xvfb-run -a ./gradlew runClient --offline \
  --args='--graphicsBackend opengl --quickPlayMultiplayer 127.0.0.1:25577 --width 960 --height 600'
```

Use Right Shift to open the ClickGUI and End to disable all modules. The sRGB override exists solely to let software OpenGL testing run on Xvfb; it is not needed on a normal OpenGL display.
