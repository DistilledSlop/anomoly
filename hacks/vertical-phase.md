# Vertical phase route

**Basis:** [vertical residual phase](../exploits/vertical-residual-phase.md), confirmed by ordinary position packets through a stone roof in both directions and through a three-layer bedrock roof on a local 26.3 server.

A client could identify a thin roof or floor from loaded chunk data, find a clear destination on the other side, and send one vertical position packet within the server movement budget. It should check the destination's full player bounding box, track corrections, and stop if the server rejects the move. A route planner could pair this with a block probe or received chunk data to find the safe landing space, and with the [correction hop](correction-hop.md) to reach the starting point quickly.

The local test used direct handler calls on an embedded connection. Remote protocol and anti-cheat behavior remain untested. The movement budget and endpoint collision check still apply; this is a bounded vertical crossing, not arbitrary noclip.
