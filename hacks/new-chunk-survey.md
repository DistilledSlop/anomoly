# New-chunk and route survey

**Value:** Find likely active regions and exploration routes. Wurst's NewChunks and Meteor's StashFinder cover related terrain scouting.

**Design:** Record chunk arrivals and any version-specific clues in received chunk sections, light, block entities and structures. Classify known generated chunks, recently modified chunks and uncertain cases separately. Combine with travel traces (roads, bridges, portals, tunnels) to rank routes and base likelihood. Let users set a target biome/structure or a maximum search radius; choose routes that sample more unseen chunks per minute while controlling elytra fuel. Preserve raw evidence for later rescoring.

**Constraints:** Chunk freshness heuristics are fallible and depend on server generation history, upgrades, pregeneration, and plugins. A freshly sent chunk is not necessarily newly generated. Do not expose a binary 'new' label without confidence.
