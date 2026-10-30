# Locator-bearing triangulator

**Basis:** [locator-bar bearing leak](../exploits/waypoint-bearing-triangulation.md). A local 26.3 packet capture showed the same source UUID and float bearings at two receiver positions; intersecting them recovered a stationary source's X/Z to within 0.001 block in that geometry.

Record each waypoint packet with the observer's X/Z and server tick. For each UUID, intersect bearings from separated observation points, fit a best estimate when there are three or more, and display an uncertainty region rather than a single exact marker. Prefer fresh `TRACK` packets or `UPDATE` packets; invalidate a bearing after the target may have moved, after an `UNTRACK`, or on dimension change. Show exact block or chunk positions if the server later supplies those waypoint forms. Combine the estimate with [player radar](player-radar.md) and [chunk intelligence](chunk-intelligence.md) to prioritize a base search.

The source establishes the range and packet format, and the local capture confirms precision for one stationary geometry. Accuracy across moving targets, varied bearing angles, and remote servers remains untested. Crouching, waypoint-hiding equipment, gamerule changes, and range changes can remove or degrade the signal.
