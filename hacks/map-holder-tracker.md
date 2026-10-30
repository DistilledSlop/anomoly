# Shared-map holder tracker

**Value:** Track another holder of the same map even outside normal entity render distance. This is a social/inventory prerequisite, not a packet that reveals every player.

**Design:** Store each carried `MapId`, map center, scale, dimension and `trackingPosition`. Parse `ClientboundMapItemDataPacket` decorations and maintain separate tracks for player icons versus frames, banners and explorer targets. Convert map bytes to coordinate intervals and heading bins; when off-map markers are clamped, retain only a broad region. Multiple distinct shared maps with different centers can intersect their regions. Annotate last update time, since a missing icon may mean dimension change, dropped map, invisibility equipment, or out-of-range.

**Constraints:** Same saved map ID must be held by both parties; map invisibility equipment suppresses a marker. See [shared-map analysis](../exploits/shared-map-tracking.md).
