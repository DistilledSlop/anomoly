package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** Places known Recipe Book recipes and takes only configured results. */
public final class AutoCrafter extends Module {
    private final StringSetting items = add(new StringSetting("items_to_craft", "Output item IDs in priority order", ""));
    private final StringSetting allowed = add(new StringSetting("allowed_containers", "Allowed containers",
        "crafting_table"));
    private final FloatSetting minDelay = add(new FloatSetting("min_delay_ticks", "Minimum ticks between actions", 2F, 1F, 20F));
    private final FloatSetting maxDelay = add(new FloatSetting("max_delay_ticks", "Maximum ticks between actions", 3F, 1F, 20F));
    private final BooleanSetting stackCrafting = add(new BooleanSetting("stack_crafting", "Craft a full stack when possible", true));
    private final BooleanSetting sequentialCrafting = add(new BooleanSetting("sequential_crafting", "Wait between completed crafts", true));
    private final ChoiceSetting onFull = add(new ChoiceSetting("on_full", "When inventory is full", "Wait",
        "Wait", "Disable", "CloseScreen", "Throw"));
    private RecipeBookMenu observedMenu;
    private RecipeDisplayId pendingRecipe;
    private String pendingTarget;
    private int pendingTicks;
    private int startupDelay;
    private int cooldown;

    public AutoCrafter() {
        super("auto_crafter", "Auto Crafter", "Make listed Recipe Book items in an open crafting or cooking screen.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
            || client.player.isSpectator() || !(client.gui.screen() instanceof AbstractRecipeBookScreen<?>)
            || !(client.player.containerMenu instanceof RecipeBookMenu menu)
            || !containerAllowed(menu) || !client.player.getRecipeBook().isOpen(menu.getRecipeBookType())
            || !menu.getCarried().isEmpty()) return;
        if (menu instanceof AbstractFurnaceMenu && dev.anomoly.AnomolyClient.MODULES.get(AutoSmelter.class).enabled()) return;
        if (observedMenu != menu) {
            observedMenu = menu;
            pendingRecipe = null;
            pendingTarget = null;
            pendingTicks = 0;
            startupDelay = 8;
            cooldown = 0;
            return;
        }
        if (startupDelay > 0) { startupDelay--; return; }
        if (cooldown > 0) { cooldown--; return; }

        List<String> targets = targets();
        if (targets.isEmpty()) return;
        int resultSlot = menu instanceof AbstractFurnaceMenu ? 2 : 0;
        ItemStack result = menu.getSlot(resultSlot).getItem();
        if (!result.isEmpty()) {
            String id = StorageMenu.id(result);
            if (!targets.contains(id) || pendingTarget != null && !pendingTarget.equals(id))
                return; // Leave an unrelated recipe in the grid alone.
            if (!hasSpace(menu, result)) {
                handleFull(client, menu, resultSlot);
                return;
            }
            client.gameMode.handleContainerInput(menu.containerId, resultSlot, 0,
                ContainerInput.QUICK_MOVE, client.player);
            pendingRecipe = null;
            pendingTarget = null;
            pendingTicks = 0;
            cooldown = sequentialCrafting.get() ? delay() : 0;
            return;
        }

        if (pendingRecipe != null) {
            if (menu instanceof AbstractFurnaceMenu furnace && !furnace.getSlot(0).getItem().isEmpty()) return;
            if (++pendingTicks < 20) return;
            pendingRecipe = null;
            pendingTarget = null;
        }
        if (menu instanceof AbstractCraftingMenu crafting)
            for (var slot : crafting.getInputGridSlots()) if (!slot.getItem().isEmpty()) return;

        RecipeDisplayEntry recipe = findRecipe(client, menu, targets);
        if (recipe == null) return;
        pendingRecipe = recipe.id();
        pendingTarget = outputId(recipe, SlotDisplayContext.fromLevel(client.level), targets);
        pendingTicks = 0;
        client.gameMode.handlePlaceRecipe(menu.containerId, pendingRecipe, stackCrafting.get());
        cooldown = delay();
    }

    private RecipeDisplayEntry findRecipe(Minecraft client, RecipeBookMenu menu, List<String> targets) {
        StackedItemContents contents = new StackedItemContents();
        client.player.getInventory().fillStackedContents(contents);
        menu.fillCraftSlotsStackedContents(contents);
        ContextMap context = SlotDisplayContext.fromLevel(client.level);
        for (int priority = 0; priority < targets.size(); priority++) {
            String target = targets.get(priority);
            for (RecipeCollection collection : client.player.getRecipeBook().getCollections())
                for (RecipeDisplayEntry entry : collection.getRecipes()) {
                    if (!fits(menu, entry) || !entry.canCraft(contents)
                        || !matchesOutput(entry, context, target)
                        || consumesLaterTarget(entry, targets.subList(priority + 1, targets.size()))) continue;
                    return entry;
                }
        }
        return null;
    }

    private boolean fits(RecipeBookMenu menu, RecipeDisplayEntry entry) {
        if (menu instanceof BlastFurnaceMenu) return entry.display() instanceof FurnaceRecipeDisplay
            && (entry.category() == RecipeBookCategories.BLAST_FURNACE_BLOCKS
                || entry.category() == RecipeBookCategories.BLAST_FURNACE_MISC);
        if (menu instanceof SmokerMenu) return entry.display() instanceof FurnaceRecipeDisplay
            && entry.category() == RecipeBookCategories.SMOKER_FOOD;
        if (menu instanceof AbstractFurnaceMenu) return entry.display() instanceof FurnaceRecipeDisplay
            && (entry.category() == RecipeBookCategories.FURNACE_BLOCKS
                || entry.category() == RecipeBookCategories.FURNACE_FOOD
                || entry.category() == RecipeBookCategories.FURNACE_MISC);
        if (!(menu instanceof AbstractCraftingMenu crafting)) return false;
        return switch (entry.display()) {
            case ShapedCraftingRecipeDisplay shaped ->
                shaped.width() <= crafting.getGridWidth() && shaped.height() <= crafting.getGridHeight();
            case ShapelessCraftingRecipeDisplay shapeless ->
                shapeless.ingredients().size() <= crafting.getGridWidth() * crafting.getGridHeight();
            default -> false;
        };
    }

    private static boolean matchesOutput(RecipeDisplayEntry entry, ContextMap context, String target) {
        return entry.resultItems(context).stream().anyMatch(stack -> StorageMenu.id(stack).equals(target));
    }

    private static String outputId(RecipeDisplayEntry entry, ContextMap context, List<String> targets) {
        return entry.resultItems(context).stream().map(StorageMenu::id)
            .filter(targets::contains).findFirst().orElse("");
    }

    private static boolean consumesLaterTarget(RecipeDisplayEntry entry, List<String> later) {
        if (later.isEmpty()) return false;
        for (Ingredient ingredient : entry.craftingRequirements().orElse(List.of()))
            if (ingredient.items().anyMatch(item -> later.contains(BuiltInRegistries.ITEM.getKey(item.value()).toString())))
                return true;
        return false;
    }

    private boolean containerAllowed(RecipeBookMenu menu) {
        String type = menu instanceof InventoryMenu ? "inventory"
            : menu instanceof CraftingMenu ? "crafting_table"
            : menu instanceof BlastFurnaceMenu ? "blast_furnace"
            : menu instanceof SmokerMenu ? "smoker"
            : menu instanceof AbstractFurnaceMenu ? "furnace" : "";
        return !type.isEmpty() && StorageMenu.ids(allowed.get().toLowerCase(Locale.ROOT)).contains(type);
    }

    private List<String> targets() {
        List<String> result = new ArrayList<>();
        for (String token : items.get().split(",")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) continue;
            if (!id.contains(":")) id = "minecraft:" + id;
            if (!result.contains(id)) result.add(id);
        }
        return result;
    }

    private static boolean hasSpace(RecipeBookMenu menu, ItemStack result) {
        int start = menu instanceof InventoryMenu ? 9
            : menu instanceof CraftingMenu ? 10 : 3;
        int end = menu instanceof InventoryMenu ? 45 : menu.slots.size();
        for (int slot = start; slot < end; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, result)
                && stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    private void handleFull(Minecraft client, RecipeBookMenu menu, int resultSlot) {
        switch (onFull.get()) {
            case "Disable" -> {
                client.gui.hud.getChat().addClientSystemMessage(Component.literal("[Anomoly] Auto Crafter: Inventory full.")
                    .withStyle(ChatFormatting.YELLOW));
                setEnabled(false);
            }
            case "CloseScreen" -> client.gui.setScreen(null);
            case "Throw" -> {
                client.gameMode.handleContainerInput(menu.containerId, resultSlot, 1,
                    ContainerInput.THROW, client.player);
                cooldown = delay();
            }
            default -> { } // Wait for inventory space.
        }
    }

    private int delay() {
        int a = (int)minDelay.get();
        int b = (int)maxDelay.get();
        return ThreadLocalRandom.current().nextInt(Math.min(a, b), Math.max(a, b) + 1);
    }

    @Override protected void onDisable(Minecraft client) {
        observedMenu = null;
        pendingRecipe = null;
        pendingTarget = null;
        pendingTicks = 0;
        startupDelay = 0;
        cooldown = 0;
    }
}
