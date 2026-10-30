# Spear momentum burst

**Basis:** [client-authored spear motion](../exploits/spear-client-motion-amplification.md). The local 26.3 handler test extended an iron spear stab to a target about nine blocks from the advanced player position. A held spear then dealt 151 damage to a stationary Warden after an eight-block packet step.

The client should track the held spear's item-use delay, the current server-accepted position, and the last movement vector. For a target in a clear lane, choose a step that passes the server movement budget and collision checks, aim the head ray, and use the resulting `knownMovement` for a stab or timed held-spear contact. The jab mode can return to the starting point using a client-tick-end packet once the attack is processed. The held mode should verify the damage event and correction state before retreating; a server tick must occur while the forward vector is still recorded.

The module needs a conservative lane solver and an immediate stop on correction, wall collision, target loss, or failed weapon charge. Datapacks can change spear components. The 151-damage result was against a Warden on a local server with no anti-cheat; damage against armored players and remote-client timing remain untested.
