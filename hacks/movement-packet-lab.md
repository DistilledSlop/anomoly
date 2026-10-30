# Movement packet lab

**Value:** Discover whether the 26.3 client-tick-end framing weakness yields practical speed, reach, or escape tactics on a stock server, with observable evidence rather than folklore.

**Design:** Instrument outgoing position and tick-end packets and incoming correction packets. Run isolated scenarios on a local server: duplicate position, position/tick-end/position, move/interact/return, and vehicle variants. Vary displacement, collision, grounded state, and server tick phase. Record server position before every action, final position, disconnect reason and accepted interaction. If a repeatable sequence works, expose only validated profiles with strict displacement limits and a correction-rate monitor.

**Confirmed behavior:** A local 26.3 handler test moved eight blocks, opened a chest from that position, and returned to the original position within one server tick by placing a client-tick-end packet between the position packets. The return persisted on the next tick. This supports an interaction excursion module in clear terrain. Speed, horizontal collision, floating and anti-cheat checks still apply. See [tick-end analysis](../exploits/tick-end-movement-reset.md).
