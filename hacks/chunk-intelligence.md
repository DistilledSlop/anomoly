# Chunk intelligence and base finder

**Value:** Find bases, stashes, portals, farms and tunnels in already received chunks. Public StashFinder, StorageESP and Xray modules provide the baseline; the proposed improvement is a persistent scored evidence index.

**Design:** On every chunk packet, index block-state palette runs and block-entity type/position. Score clusters of chests, barrels, shulkers, hoppers, furnaces, ender chests, portals, redstone, rails, artificial lighting, flat excavation, and non-biome palette changes. Give each feature a coordinate, timestamp and confidence. Combine evidence across chunk borders and store a deduplicated local database by server, dimension and chunk. Render heatmap/waypoints, then let the nearby block probe confirm specific suspicious positions.

**Constraints:** A chunk packet does not expose all container contents, and no data exists for unsent chunks. Worldgen structures, villages and abandoned camps can mimic a base; suppress known structure signatures. Anti-xray can falsify ore states. See [chunk visibility](../exploits/chunk-data-visibility.md).
