# Locator pulse: transient false bearings from a same-tick excursion

**Status:** Local two-player 26.3 handler test confirmed that one accepted 8-block movement and a same-tick return emitted two waypoint packets to a distant receiver. The first packet was `TRACK` because the connection had just been established; the return was `UPDATE`. A warmed connection should emit updates on both legs, but that specific variant was not separately captured. This is a new *application* of the [movement excursion](../exploits/tick-end-movement-reset.md) and [waypoint-bearing](../exploits/waypoint-bearing-triangulation.md) findings, not a claim that waypoint packets were newly introduced.

## Why it works

`Entity.setPosRaw` calls `ServerWaypointManager.updateWaypoint` whenever a transmitting entity moves. The distant `EntityAzimuthConnection` sends a bearing update when the angle changes by more than 0.008726646 radians (0.5°). Thus accepted position changes can become packets to other players **during the movement handler**, before the mover has settled at its final position for the tick. The ordinary player attributes set transmit and receive range to 60,000,000 blocks, subject to spectator and crouch rules.

At a receiver at `(0.5, 100, 0.5)` and source at `(400.5, 100, 400.5)`, the probe sent a move to `(408.5, 100, 400.5)` then a client-tick-end packet and a return move. It captured `TRACK` at angle `-0.795299`, followed by `UPDATE` at `-0.7853982`. The source ended where it began. See [the saved log](../research/probes/logs/locator-pulse.txt) and [reproducible probe](../research/probes/LocatorPulseProbe.java).

## Use as a player-interaction feature

An indicator could pulse a false bearing and then restore the real one, causing locator-bar motion for players who track the source. Repeated pulses could also serve as a rudimentary signal to cooperating clients that decode raw waypoint packets. Whether a vanilla client *visibly renders* a pulse sent and reversed within one frame has not been measured; the packet-level effect is confirmed. Crouching, spectator state, disabled locator-bar gamerule, anti-cheat movement rejection, or a receiver outside the permitted range suppresses the effect.

## Source anchors

- `net.minecraft.world.entity.Entity#setPosRaw` near line 3931: movement calls `updateWaypoint`.
- `net.minecraft.world.waypoints.WaypointTransmitter.EntityAzimuthConnection#update`: 0.5° threshold and `UPDATE` packet.
- `net.minecraft.server.waypoints.ServerWaypointManager#updateWaypoint`: fanout to connected receivers.
- `net.minecraft.world.entity.player.Player#createAttributes`: default 60,000,000-block ranges.
