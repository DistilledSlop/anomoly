# AntiCheat Detect profiling test — 2026-10-30

The candidate rules use a shared JSON signature file with links to the vendors' command documentation. AntiCheat Detect reads the command tree and payload names the server already sent. Its module tick only checks those observations and writes local chat notices; it sends no commands or plugin enumeration packets.

| Probe | Observation |
| --- | --- |
| Live vanilla server | On a local Minecraft Java 26.3 server, the OpenGL ImGui [profile page](../screenshots/anticheat-profile-vanilla.png) showed brand `vanilla`, no recognizable anticheat signature, and one correction after five seconds. A deliberate server teleport caused that correction; the candidate list stayed empty. The module was enabled throughout. |
| Existing vanilla JSONL profile | The offline analyzer found two post-join position corrections and no candidate in a separate local vanilla session. |
| Java classifier: bare command | A visible `/grim` root produced `GrimAC` with `weak` confidence and the exact command as evidence. |
| Java classifier: namespace | A visible `/grim:grim` root produced `GrimAC` with `plausible` confidence. |
| Java classifier: payload | An observed `nocheatplus:check` payload ID produced `NoCheatPlus` with `plausible` confidence. |
| Java classifier: generic signals | Vanilla `help`, `minecraft:help`, and `minecraft:brand` produced no candidate. The Python analyzer returned the same candidate outcomes for the example signals. |

These direct classifier probes check matching and confidence labels. They do not prove that a server with such a command runs that anticheat: other plugins can expose or spoof the same name. Position corrections can also result from lag, teleports, or other server behavior. Real GrimAC, NoCheatPlus, Spartan, Vulcan, and Intave servers still need observational testing before this catalog entry is marked gameplay verified.

For a first session on AnticheatTest, enable passive profile recording and AntiCheat Detect with other hacks off. Join and play normally for a short baseline. The client writes `config/anomoly/profiles/<session>.jsonl`; run `python3 research/analyze_anomoly_profile.py <path> --json` afterward. Review the log before sharing it because it contains the server address and player coordinates. The profiler adds no active probes, though a server can still disconnect a player for reasons outside this profiler.
