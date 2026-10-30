# Anomoly

Anomoly is a full Minecraft Java 26.3 hacked client built as a Fabric mod, with an ImGui ClickGUI, modules, configuration, and passive anticheat profiling. Module coverage is expanding. The [cheat catalog](cheats/README.md) and source audit support implementation; the treemap below shows what is currently coded and tested.

## Hack progress

[![Anomoly hack progress treemap](docs/progress.svg)](https://distilledslop.github.io/anomoly/progress.html)

The [searchable progress report](https://distilledslop.github.io/anomoly/progress.html) is generated from the 682 catalog entries plus Anomoly's Spear Assist. It currently tracks <!-- anomoly-progress:start -->**70 coded**, **0 partial**, **17 gameplay verified**, and **596 backlog**<!-- anomoly-progress:end --> features. Blue tiles are coded modules, amber tiles are partial implementations, green tiles have gameplay test evidence, and gray tiles are backlog. Run `python3 tools/progress.py` after changing a module's status; CI runs `python3 tools/progress.py --check` to verify the treemap and [raw data](docs/progress.json).

## Build and run

Use Java 25 and run `./gradlew build`. The distributable is `build/libs/anomoly-1.0.0.jar`; it includes ImGui Java and native libraries for Windows, Linux, and macOS. Install it with Fabric Loader 0.20.1 and Fabric API 0.165.0+26.3.

The Dear ImGui ClickGUI uses the OpenGL backend. Choose **OpenGL** in Minecraft's video settings before starting the game, or launch with `--graphicsBackend opengl`. On Vulkan, Anomoly opens a functional native settings screen with the same modules and profile controls. Right Shift opens or closes the GUI, End disables every module, C holds Zoom, and H holds Free Look when those modules are enabled. These keys appear in Minecraft's key controls.

Every module also has an optional toggle key under **Minecraft Controls → Anomoly Modules**. They start unbound; assign a key in Controls, then use it in the world to toggle the module or trigger a one-shot module such as Pot. An expanded module in the ImGui ClickGUI shows its current key. The key mappings are saved by Minecraft in `options.txt`, while module states and settings remain in Anomoly's config. A [local input test](docs/test-evidence/2026-10-30-module-keys.md) confirms a bound Pot key performed the one-shot action.

The ClickGUI has a category sidebar, module search, expandable settings, and a server profile page. Player Radar also shows nearby loaded players on the HUD with relative direction and distance.

![Anomoly ImGui ClickGUI showing the Movement category in a live Minecraft world](docs/screenshots/clickgui-opengl-960x600.png)

The [local gameplay test](docs/test-evidence/2026-10-30-local-gameplay.md) verifies Auto Walk movement, Aimbot and Aim Assist rotation, No Rotate against both server rotation paths, the panic key, ClickGUI rendering and input, and passive profile collection in a Minecraft 26.3 multiplayer world.

## Implemented modules

| Category | Modules |
| --- | --- |
| Combat | Auto Pearl, Auto Weapon, Auto Sword, Auto Clicker, Trigger Bot, Anti Bot, Kill Aura, Aimbot, Aim Assist, Bow Aimbot, Projectile Aimbot, Auto Shoot, Auto Bow, Bow Spam, Auto Rod, Spear Assist, Reach, Auto Leave, Auto Armor, Auto Bow Release, Auto Potion, Throw Pot, Pot, Auto Exp, Auto GApple, Auto Totem |
| Movement | Auto Sprint, Auto Walk, Auto Jump, Air Jump, Auto Sneak, Fast Ladder, Fast Fall, No Fall, Safe Walk, Step, Anti Web, Velocity, No Push, No Slow |
| Player | Auto Respawn, Auto Eat, Auto Fish, Anti AFK, Auto Break, Auto Switch, Auto Tool, Auto Replenish, Auto Deposit, Auto Drop (also Auto Eject), Chest Stealer, Chest Cleaner, No Rotate (also No Server Rotate) |
| World | Fast Place, Fast Break (also Break Delay) |
| Render | Fullbright, Max Brightness, No Hurt Cam, No Bob, Auto F5, FOV Modifier, No FOV, Zoom, Camera Clip, Camera Tweaks, Free Look, No Fog, No Overlay, No Fire Overlay, Anti Overlay, Anti Wobble, No Pumpkin, Anti Blind, Player Radar, Weather Changer, Time Changer |
| Exploit | Anti Reduced Debug Info, Portal Menu, Plugins |
| Fun | Twerk |
| Misc | Auto Reconnect |

Modules default to off. Their states and settings are saved in `config/anomoly.json`. The current Kill Aura defaults to visible targets within three blocks and full attack cooldown; its range and other settings are adjustable in the GUI. Server anticheats can reject or punish module behavior, so the passive profiler makes no guarantee of compatibility.

Aimbot turns toward the visible entity closest to your crosshair within its configured range and field of view. Aim Assist makes smaller corrections while you hold attack. Both default to player targets, can optionally include mobs or invisible entities, and yield to Kill Aura when it is enabled. Neither module attacks on its own.

Bow Aimbot and Projectile Aimbot calculate aim using Minecraft 26.3 projectile flight physics, including drag, gravity, bow charge, target movement, and terrain collision. They never fire for you. The [projectile aim gameplay test](docs/test-evidence/2026-10-30-projectile-aim.md) records bow and crossbow hits at about 16 blocks and a snowball hit on a blaze at about 12 blocks; other weapon modes need separate gameplay tests.

Auto Shoot uses that same solver to select, aim, and throw eggs or snowballs at a controlled rate. It can select a projectile from the hotbar and restore your previous slot. A [local gameplay test](docs/test-evidence/2026-10-30-autoshoot.md) confirms it killed a stationary blaze without manual input after setup.

Auto Bow selects, aims, charges, and fires bows, crossbows, and tridents using normal item interactions. It can shorten bow charge for faster, weaker shots, select a weapon from the hotbar, and restore the previous slot. A [local gameplay test](docs/test-evidence/2026-10-30-autobow.md) confirms automatic hits with all three weapon types. Riptide tridents are excluded because they launch the player instead of a projectile.

Bow Spam repeatedly releases a bow or crossbow while use is held. Its bow charge setting controls the tradeoff between shot rate and power, while crossbows wait for the 26.3 charge duration. A [local gameplay test](docs/test-evidence/2026-10-30-bowspam.md) confirms repeated bow and crossbow shots.

Auto Exp throws experience bottles at your feet when equipped Mending armor or a held Mending tool needs repair. It stops at the configured durability threshold and can select bottles from the hotbar or use them from the offhand. The [local gameplay test](docs/test-evidence/2026-10-30-autoexp.md) confirms armor and tool repair and a no-Mending control.

Auto Potion, which also covers AutoPot, checks potion effects and throws only instant-health splash potions when health is low. Throw Pot is a separate one-shot toggle: it throws one potion, or two below its lower health threshold. Both restore the previous hotbar slot. [Local gameplay tests](docs/test-evidence/2026-10-30-potions.md) confirm healing, the one- and two-potion modes, and rejection of harming potions.

Pot is a separate one-shot hotbar swap for any splash or lingering potion. It throws at the current view direction and restores the previous slot. A [local gameplay test](docs/test-evidence/2026-10-30-pot.md) confirms both potion types and a poison splash potion.

Auto Rod solves the fishing hook's 26.3 flight path, selects a rod, casts at a visible target, reels when it hooks or times out, and restores the previous slot. The [local gameplay test](docs/test-evidence/2026-10-30-autorod.md) confirms repeated hook-and-reel cycles against a stationary pig. It keeps the rod selected while a hook is active because the server removes the hook if neither hand holds a rod.

Auto Pearl watches pearls thrown by other players, simulates their landing, and searches for a throw angle that lands within the configured error margin. It uses a normal item interaction, limits turn speed, and can select a pearl from the hotbar before restoring the previous slot. A [local two-player gameplay test](docs/test-evidence/2026-10-30-autopearl.md) records a landing about 1.4 blocks from the other player.

Anti Bot is a shared filter for automatic attacks and aiming. When enabled, it excludes player entities without a player-list entry; optional settings can also exclude newly spawned or invisible players. These are conservative signals and cannot identify every decoy. The module is coded but still needs a decoy-specific gameplay test.

No Rotate preserves the local view direction across server position corrections and dedicated rotation packets. Its two packet paths can be controlled separately in the GUI.

Weather Changer defaults to a clear local sky and can also set rain, thunder, time, and moon phase. Time Changer controls time and moon phase independently; Weather Changer takes priority when its time override is on.

Auto Reconnect retries only after an unexpected disconnect from a multiplayer server observed while the module was enabled. Its delay and maximum attempt count are configurable; manual disconnects and Auto Leave do not trigger retries.

Chest Stealer, Chest Cleaner, Auto Deposit, and Auto Drop accept comma-separated item IDs such as `minecraft:rotten_flesh`. Chest Stealer skips items selected for cleaning or depositing when those modules are enabled.

## Passive anticheat profile

The profiler is on by default. Each joined session creates a JSONL log in `config/anomoly/profiles/`. It records the server brand, observed custom payload channel names, root commands and namespaces exposed in the server command tree, one movement sample per second including view rotation, latency, position corrections, active module names, and the disconnect reason. It sends no test packets or plugin enumeration requests. This means it can show behavioral evidence but may not identify the exact anticheat plugin.

Enable the Plugins module to inspect visible root commands and command namespaces on the ClickGUI server profile page. This reads the command tree the server already sent. Commands hidden by server permissions or custom filtering cannot be discovered this way; a namespace alone does not prove which plugin implements a command.

The [local command inventory test](docs/test-evidence/2026-10-30-local-gameplay.md#passive-command-inventory) and [server profile screenshot](docs/screenshots/server-profile-opengl-960x600.png) show the profiler and ClickGUI reading ten commands from a vanilla server without an enumeration request.

After a session, run:

```sh
python3 research/analyze_anomoly_profile.py config/anomoly/profiles/<session>.jsonl
```

The analyzer separates initial join corrections from later ones and reports correction rates by module exposure, observed channels, and disconnect details. Module rates overlap when several modules are enabled, so they are signals for controlled testing rather than proof of cause. Review the raw log before sharing it: it contains server addresses and player coordinates.

## Research

See [RESEARCH.md](RESEARCH.md), [hacks](hacks/README.md), and [exploits](exploits/README.md) for 26.3 source findings and feature designs. Their claims describe vanilla behavior in local probes; compatibility with a remote anticheat requires evidence from a real session.
