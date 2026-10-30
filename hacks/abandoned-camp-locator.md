# 26.3 abandoned camp locator

**Value:** The 26.3 Wilderness Bound release adds abandoned camps and related explorer maps. The vanilla data jar contains `data/minecraft/worldgen/structure_set/abandoned_camp.json` with `random_spread`, **spacing 37, separation 8, salt 91231127** and several biome-specific camp structures. This gives a version-specific search target.

**Design:** If the world seed is known, implement the 26.3 random-spread placement for candidate regions, then filter by biome tags and terrain before planning a route. If the seed is unknown, use explorer-map decorations, observed camps and biome patterns as weaker evidence; do not claim deterministic coordinates. Render candidate regions with confidence, loot/route cost, and visited status. The locator should load structure-set JSON from the 26.3 game data, avoiding hardcoded assumptions that break on datapacks.

**Constraints:** Structure-set candidates can fail biome/terrain requirements and servers can replace data packs. This does not derive a server seed from one camp. See the [official release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) and the local common jar's structure-set and `has_structure/abandoned_camp_*` biome tags.
