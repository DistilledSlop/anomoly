# Nearby block probe

**Value:** Confirm a hidden block state in a small radius and reveal nearby obfuscated or occluded targets. This follows the [blind block probe](../exploits/blind-block-probe.md), rather than merely making client-rendered blocks transparent.

**Design:** Offer a manual crosshairless probe and a low-rate scan mode. Build valid `ServerboundUseItemOnPacket` hit results for positions within the server's block range. Correlate response block updates by position and timestamp, compare against the chunk palette, and mark contradictions. A two-block face response makes each probe cover a pair of cells. Use a blacklist for blocks with side effects, and stop scanning if item use, menu opening or server correction appears. Display confidence and last observed server state.

**Constraints:** Server plugins may intercept/block update packets, and block interaction may have side effects. This can reveal **states**, not chest inventory. It cannot scan arbitrary unloaded coordinates.
