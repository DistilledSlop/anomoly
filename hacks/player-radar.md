# Player radar and last-seen intelligence

**Value:** Improve combat awareness and base defense within the server's entity tracking envelope. Standard ESP/tracers show current entities; a record of spawn, equipment, motion and disappearance gives more useful cues.

**Design:** Subscribe to clientbound entity add, move, teleport, metadata, equipment and remove events. Keep per-UUID tracks with position, velocity, health (if available), held weapon and last seen time. Predict a short range of possible positions through unobserved terrain, with uncertainty growing over time. Use incoming sound/game events as weak bearing evidence only when the packet supplies a location. Combine with shared-map tracks where an actual shared map exists. Distinguish a despawn from a logout or chunk unload; do not label an unseen player as located.

**Constraints:** The vanilla server does not send hidden remote players' exact positions. Entity IDs are local/session-specific. Spectator and plugin visibility rules can remove data.
