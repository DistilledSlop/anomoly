# Defense automation

**Value:** Survival and combat reliability: auto-totem, armor replacement, shield timing, health-triggered escape, and projectile dodge. These are common in Meteor/Wurst, but they work best as one coordinated decision engine.

**Design:** Track health plus absorption, incoming projectile trajectories, explosion predictions, inventory slot state, damage cooldown, and ping. Choose offhand totem before expected lethal damage; swap to shield/food only when the survival margin permits. Trigger armor replacement when armor durability crosses a threshold. For arrows and wind charges, simulate a short horizon and choose a collision-free dodge. A packet logger should show whether swaps were acknowledged by the server menu. Use a priorities table so emergency totem wins over auto-eat or trade automation.

**Constraints:** The server owns inventories and damage. A visual client-side swap is not protection until the server accepts the menu action. 26.3's `handleContainerClick` executes the click and reconciles state IDs; do not trust client `changedSlots` as arbitrary items. Source: `ServerGamePacketListenerImpl.java:1979–2012`; `AbstractContainerMenu` click logic.
