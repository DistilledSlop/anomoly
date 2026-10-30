# Inventory and trade assistant

**Value:** Faster survival logistics: auto-tool, auto-refill, totem restock, loot sorting, villager trade selection, potion/food choice, and quick crafting. Meteor/Wurst already expose many of these individually.

**Design:** Model each open menu's slot layout and state ID. Queue one click or recipe/trade action, wait for the server's slot/menu updates, and recover cleanly from a full resync. For villagers, rank offers by emerald cost, stock, reputation and desired enchantments; 26.3 can update newly unlocked offers while the menu is open, so refresh the model on every offer packet. For loot containers, prioritize valuable items and preserve an allowlist of slots that should not be moved. Use a shared priority scheduler with defense automation.

**Constraints:** The server executes menu clicks and validates `stillValid`; client-supplied slot hashes are reconciliation data, not an item creation channel. `handleSelectTrade` is limited to an active merchant menu. The [26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) describe the live villager-trade update.
