package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Runs vanilla brewing recipes from water bottles to a configured potion. */
public final class AutoBrewer extends Module {
    private record RecipeStep(String inputPotion, Item reagent) { }
    private record Stage(String potion, Item bottle, Item reagent) { }
    private static final Map<String, RecipeStep> RECIPES = recipes();

    private final StringSetting targetPotion = add(new StringSetting("potion_id", "Potion ID", "minecraft:strength"));
    private final StringSetting outputBottle = add(new StringSetting("output_item", "Output item (potion, splash_potion, lingering_potion)", "minecraft:potion"));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Ticks between inventory actions", 5F, 1F, 20F));
    private int cooldown;

    public AutoBrewer() {
        super("auto_brewer", "Auto Brewer", "Brew a chosen potion from water bottles while a brewing stand is open.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || !(client.gui.screen() instanceof BrewingStandScreen)
            || !(client.player.containerMenu instanceof BrewingStandMenu menu)
            || !menu.getCarried().isEmpty()) return;
        if (cooldown > 0) { cooldown--; return; }
        List<Stage> plan = plan();
        if (plan.isEmpty() || menu.getBrewingTicks() > 0) return;
        ItemStack remainder = menu.getSlot(3).getItem();
        if (remainder.is(Items.GLASS_BOTTLE)) {
            if (StorageMenu.hasSpace(menu, remainder)) quickMove(client, menu, 3);
            return;
        }
        int last = plan.size() - 1;
        int common = -1;
        int empty = 0;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = menu.getSlot(slot).getItem();
            if (bottle.isEmpty()) { empty++; continue; }
            int stage = stageOf(bottle, plan);
            if (stage < 0) return; // Leave unrelated or custom-effect potions untouched.
            if (stage == last) {
                if (StorageMenu.hasSpace(menu, bottle)) quickMove(client, menu, slot);
                return;
            }
            if (common >= 0 && stage != common) return; // Never brew a mixed batch.
            common = stage;
        }
        if (common < 0 || common == 0 && empty > 0) {
            int waterSlot = findWaterBottle(menu);
            if (waterSlot >= 0) {
                quickMove(client, menu, waterSlot);
                return;
            }
            if (common < 0) return;
        }
        if (menu.getFuel() <= 0) {
            if (menu.getSlot(4).hasItem()) return; // Wait for the stand to consume fuel.
            int powder = findItem(menu, Items.BLAZE_POWDER);
            if (powder >= 0) moveOne(client, menu, powder, 4);
            return;
        }
        Item nextReagent = plan.get(common + 1).reagent();
        ItemStack ingredient = menu.getSlot(3).getItem();
        if (!ingredient.isEmpty()) {
            if (ingredient.is(nextReagent)) return; // The server will start the brew.
            if (StorageMenu.hasSpace(menu, ingredient)) quickMove(client, menu, 3);
            return;
        }
        int source = findItem(menu, nextReagent);
        if (source >= 0) moveOne(client, menu, source, 3);
    }

    private void quickMove(Minecraft client, BrewingStandMenu menu, int slot) {
        client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
        cooldown = (int) delay.get();
    }

    /** Take a stack, deposit exactly one item, and return the remainder to its source slot. */
    private void moveOne(Minecraft client, BrewingStandMenu menu, int source, int target) {
        client.gameMode.handleContainerInput(menu.containerId, source, 0, ContainerInput.PICKUP, client.player);
        client.gameMode.handleContainerInput(menu.containerId, target, 1, ContainerInput.PICKUP, client.player);
        if (!menu.getCarried().isEmpty())
            client.gameMode.handleContainerInput(menu.containerId, source, 0, ContainerInput.PICKUP, client.player);
        cooldown = (int) delay.get();
    }

    private static int findItem(BrewingStandMenu menu, Item item) {
        for (int slot = 5; slot < menu.slots.size(); slot++)
            if (menu.getSlot(slot).getItem().is(item)) return slot;
        return -1;
    }

    private static int findWaterBottle(BrewingStandMenu menu) {
        for (int slot = 5; slot < menu.slots.size(); slot++)
            if (stageOf(menu.getSlot(slot).getItem(), List.of(new Stage("minecraft:water", Items.POTION, null))) == 0) return slot;
        return -1;
    }

    private static int stageOf(ItemStack stack, List<Stage> plan) {
        if (stack.isEmpty()) return -1;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.isPotionWithoutCustomEffects()) return -1;
        String potion = BuiltInRegistries.POTION.getKey(contents.potion().get().value()).toString();
        for (int i = 0; i < plan.size(); i++)
            if (stack.is(plan.get(i).bottle()) && potion.equals(plan.get(i).potion())) return i;
        return -1;
    }

    private List<Stage> plan() {
        String id = targetPotion.get().trim();
        if (!id.contains(":")) id = "minecraft:" + id;
        Item output = switch (outputBottle.get().trim()) {
            case "minecraft:potion", "potion" -> Items.POTION;
            case "minecraft:splash_potion", "splash_potion" -> Items.SPLASH_POTION;
            case "minecraft:lingering_potion", "lingering_potion" -> Items.LINGERING_POTION;
            default -> null;
        };
        if (output == null) return List.of();
        List<Stage> reversed = new ArrayList<>();
        String cursor = id;
        while (!cursor.equals("minecraft:water")) {
            RecipeStep step = RECIPES.get(cursor);
            if (step == null || reversed.size() > 12) return List.of();
            reversed.add(new Stage(cursor, Items.POTION, step.reagent()));
            cursor = step.inputPotion();
        }
        reversed.add(new Stage("minecraft:water", Items.POTION, null));
        Collections.reverse(reversed);
        if (output == Items.SPLASH_POTION || output == Items.LINGERING_POTION)
            reversed.add(new Stage(id, Items.SPLASH_POTION, Items.GUNPOWDER));
        if (output == Items.LINGERING_POTION)
            reversed.add(new Stage(id, Items.LINGERING_POTION, Items.DRAGON_BREATH));
        return reversed;
    }

    private static Map<String, RecipeStep> recipes() {
        Map<String, RecipeStep> map = new HashMap<>();
        add(map, "awkward", "water", Items.NETHER_WART);
        add(map, "thick", "water", Items.GLOWSTONE_DUST);
        add(map, "mundane", "water", Items.REDSTONE);
        add(map, "weakness", "water", Items.FERMENTED_SPIDER_EYE);
        add(map, "long_weakness", "weakness", Items.REDSTONE);
        add(map, "wind_charged", "awkward", Items.BREEZE_ROD);
        add(map, "oozing", "awkward", Items.SLIME_BLOCK);
        add(map, "infested", "awkward", Items.STONE);
        add(map, "weaving", "awkward", Items.COBWEB);
        add(map, "night_vision", "awkward", Items.GOLDEN_CARROT);
        add(map, "long_night_vision", "night_vision", Items.REDSTONE);
        add(map, "invisibility", "night_vision", Items.FERMENTED_SPIDER_EYE);
        add(map, "long_invisibility", "invisibility", Items.REDSTONE);
        add(map, "fire_resistance", "awkward", Items.MAGMA_CREAM);
        add(map, "long_fire_resistance", "fire_resistance", Items.REDSTONE);
        add(map, "leaping", "awkward", Items.RABBIT_FOOT);
        add(map, "long_leaping", "leaping", Items.REDSTONE);
        add(map, "strong_leaping", "leaping", Items.GLOWSTONE_DUST);
        add(map, "swiftness", "awkward", Items.SUGAR);
        add(map, "long_swiftness", "swiftness", Items.REDSTONE);
        add(map, "strong_swiftness", "swiftness", Items.GLOWSTONE_DUST);
        add(map, "slowness", "swiftness", Items.FERMENTED_SPIDER_EYE);
        add(map, "long_slowness", "long_swiftness", Items.FERMENTED_SPIDER_EYE);
        add(map, "strong_slowness", "slowness", Items.GLOWSTONE_DUST);
        add(map, "turtle_master", "awkward", Items.TURTLE_HELMET);
        add(map, "long_turtle_master", "turtle_master", Items.REDSTONE);
        add(map, "strong_turtle_master", "turtle_master", Items.GLOWSTONE_DUST);
        add(map, "water_breathing", "awkward", Items.PUFFERFISH);
        add(map, "long_water_breathing", "water_breathing", Items.REDSTONE);
        add(map, "healing", "awkward", Items.GLISTERING_MELON_SLICE);
        add(map, "strong_healing", "healing", Items.GLOWSTONE_DUST);
        add(map, "harming", "healing", Items.FERMENTED_SPIDER_EYE);
        add(map, "strong_harming", "harming", Items.GLOWSTONE_DUST);
        add(map, "poison", "awkward", Items.SPIDER_EYE);
        add(map, "long_poison", "poison", Items.REDSTONE);
        add(map, "strong_poison", "poison", Items.GLOWSTONE_DUST);
        add(map, "regeneration", "awkward", Items.GHAST_TEAR);
        add(map, "long_regeneration", "regeneration", Items.REDSTONE);
        add(map, "strong_regeneration", "regeneration", Items.GLOWSTONE_DUST);
        add(map, "strength", "awkward", Items.BLAZE_POWDER);
        add(map, "long_strength", "strength", Items.REDSTONE);
        add(map, "strong_strength", "strength", Items.GLOWSTONE_DUST);
        add(map, "slow_falling", "awkward", Items.PHANTOM_MEMBRANE);
        add(map, "long_slow_falling", "slow_falling", Items.REDSTONE);
        return Map.copyOf(map);
    }

    private static void add(Map<String, RecipeStep> map, String output, String input, Item reagent) {
        map.put("minecraft:" + output, new RecipeStep("minecraft:" + input, reagent));
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
