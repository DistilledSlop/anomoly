# Module key binding gameplay test — 2026-10-30

I registered all 79 current modules as optional Minecraft Controls key mappings in a custom **Anomoly Modules** category. They default to `key.keyboard.unknown`, the Minecraft 26.3 representation of an unbound SDL key. The ImGui and native fallback module details display the current key. `tools/module_keys.py --check` verifies that the English control labels match the registered modules.

For an end-to-end test, I bound `key.anomoly.toggle.pot` to K in the local client's `options.txt`, then launched the Minecraft 26.3 client and joined a local vanilla server as Player60. The server gave the player stone in the selected hotbar slot and one healing splash potion in the next slot. Pressing K once with Xdotool caused Pot to select and throw the potion. The server's inventory query then contained only stone, `SelectedItem` was stone, and the saved Pot module state was disabled. This verifies key mapping load, key press handling, one-shot toggle, item use, and slot restoration together.

An initial test exposed an invalid unbound key default of `-1`: Minecraft's `KeyMapping.setAll` indexed outside its keyboard state buffer when entering the world. I corrected the default to SDL scancode `0`, removed the stale local test options created by that failed run, and reran the successful test. No invalid default was committed or pushed.
