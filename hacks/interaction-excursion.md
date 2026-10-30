# Same-tick interaction excursion

**Basis:** [tick-end movement reset](../exploits/tick-end-movement-reset.md). A local 26.3 survival-player handler test moved eight blocks, opened a chest three blocks farther, and returned to its original position within one server tick. The return persisted into the following tick, with no teleport correction.

A client module can plan a short unobstructed route to an interaction point, send one position packet, perform a block interaction or eligible attack from that position, send `ServerboundClientTickEndPacket`, and send a return position packet. It should use the server's movement budget and loaded collision shapes, record both intermediate and final server responses, and stop if a correction or disconnect appears. This extends the effective range of an interaction in open terrain without requiring a pearl, teleport acknowledgment, or lasting position change.

Only chest interaction and eight-block horizontal travel were locally tested. Inventory clicks after returning, combat damage, remote sockets, and anti-cheat servers need separate verification. A wall or obstructing block can prevent the excursion.
