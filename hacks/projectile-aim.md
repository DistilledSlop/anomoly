# Projectile aim and trajectories

**Value:** Predict bows, crossbows, tridents, pearls, snowballs, and wind charges for combat and travel. This is a mainstream feature, but a source-based solver can account for each item's distinct launch speed and drag.

**Design:** Build per-projectile integrators from the 26.3 entity classes. For each candidate aim vector, simulate gravity, drag, fluid, collision and moving-target interpolation for a few seconds. Pick a trajectory that clears intervening blocks and leads the target; show uncertainty from ping and target acceleration. Make a separate pearl landing predictor that considers ender-pearl teleport momentum reset in 26.3. Avoid using one projectile model for every item.

**Constraints:** Prediction is bounded by received chunks and opponent motion assumptions. Server-side projectiles may diverge due to lag or plugins. The [official 26.3 notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) explicitly note that ender-pearl teleport now resets momentum.
