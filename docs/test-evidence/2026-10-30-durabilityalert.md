# Durability Alert gameplay test — 2026-10-30

Tested with Anomoly on a local vanilla Minecraft Java 26.3 server. PlayerC had Durability Alert enabled with its default 10% remaining durability threshold, 60-second repeat interval, and armor-only scope. The server changed equipped items with `/item replace`; the client log recorded each warning sent to the chat HUD.

| Probe | Client observation |
| --- | --- |
| Healthy helmet | Equipping a new iron helmet produced no warning. |
| Low helmet | Equipping an iron helmet with `minecraft:damage=150` produced `Durability Alert: Iron Helmet in helmet slot has 15 durability left (9%).` The iron helmet has 165 maximum durability. |
| No immediate repeat | The log still contained one warning after five seconds with the damaged helmet equipped. |
| Repair and re-arm | Equipping a healthy helmet for several ticks and then a damaged one produced a second warning. Sending both replacements in the same server tick did not re-arm the warning, as the client did not observe the healthy intermediate state. |
| Armor-only default | With a healthy helmet and a nearly broken iron pickaxe in the selected hotbar slot, the warning count remained two. |

The module sends a local client chat message and logs the same warning once per threshold crossing, with a configurable repeat interval for equipment that stays low. The optional held-item setting was compiled but not separately gameplay tested.
