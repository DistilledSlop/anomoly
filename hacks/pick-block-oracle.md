# Pick-block inventory oracle

**Basis:** [survival pick-block oracle](../exploits/survival-pick-block-oracle.md). A local 26.3 server handler selected an existing chest item from hotbar slot 3 when the client claimed an unseen chest behind stone.

The client can reserve known inventory slots for candidate block items, send a low-rate pick-block packet for a loaded nearby position, and compare the server-confirmed held slot and inventory changes against its candidate map. It should restore the previous selected slot afterward and mark the result as an item-type match, not an exact block state. Combine this with [block probes](block-probe.md) and [chunk intelligence](chunk-intelligence.md) when investigating a nearby concealed base.

The local test used an empty chest and stock server handlers. Whether this works on a specific anti-xray plugin depends on how that plugin handles pick-block requests and held-slot responses. The oracle cannot reveal items the player lacks a matching candidate for, and it cannot create items in survival.
