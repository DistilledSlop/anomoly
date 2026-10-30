package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;

/** Loads an open cooking furnace and collects its output through ordinary menu clicks. */
public final class AutoSmelter extends Module {
    private final StringSetting inputs = add(new StringSetting("input_ids", "Smeltable item IDs",
        "minecraft:raw_iron,minecraft:raw_copper,minecraft:raw_gold,minecraft:iron_ore,minecraft:gold_ore,"
            + "minecraft:copper_ore,minecraft:deepslate_iron_ore,minecraft:deepslate_gold_ore,"
            + "minecraft:deepslate_copper_ore,minecraft:ancient_debris,minecraft:beef,minecraft:porkchop,"
            + "minecraft:chicken,minecraft:mutton,minecraft:rabbit,minecraft:cod,minecraft:salmon,minecraft:potato"));
    private final StringSetting fuels = add(new StringSetting("fuel_ids", "Fuel item IDs", "minecraft:coal,minecraft:charcoal"));
    private final FloatSetting fuelPerRefill = add(new FloatSetting("fuel_per_refill", "Fuel items per refill", 1F, 1F, 16F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between inventory actions", 5F, 1F, 20F));
    private final BooleanSetting disableWhenDone = add(new BooleanSetting("disable_when_done", "Turn off when finished", true));
    private final BooleanSetting closeWhenDone = add(new BooleanSetting("close_when_done", "Close furnace when finished", false));
    private AbstractFurnaceMenu observedMenu;
    private int startupDelay;
    private int cooldown;
    private int settledTicks;

    public AutoSmelter() {
        super("auto_smelter", "Auto Smelter", "Smelt selected inventory items in an open furnace, blast furnace, or smoker.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
            || !(client.gui.screen() instanceof AbstractFurnaceScreen<?>)
            || !(client.player.containerMenu instanceof AbstractFurnaceMenu menu)
            || !menu.getCarried().isEmpty()) return;
        if (observedMenu != menu) {
            observedMenu = menu;
            startupDelay = 8;
            cooldown = 0;
            settledTicks = 0;
            return;
        }
        if (startupDelay > 0) { startupDelay--; return; }
        if (cooldown > 0) { cooldown--; return; }

        ItemStack result = menu.getSlot(2).getItem();
        if (!result.isEmpty()) {
            if (!StorageMenu.hasSpace(menu, result)) {
                stop(client, "Inventory full; output left in the furnace", false);
                return;
            }
            click(client, menu, 2, 0, ContainerInput.QUICK_MOVE);
            return;
        }

        if (menu.getSlot(0).getItem().isEmpty()) {
            int source = findInput(client, menu);
            if (source >= 0) {
                moveStack(client, menu, source, 0);
                settledTicks = 0;
                return;
            }
            if (++settledTicks >= 10) {
                settledTicks = 0;
                if (disableWhenDone.get()) stop(client, "All selected items processed", true);
                if (closeWhenDone.get()) client.gui.setScreen(null);
            }
            return;
        }
        settledTicks = 0;
        if (!menu.isLit() && menu.getSlot(1).getItem().isEmpty()) {
            int source = findFuel(menu);
            if (source < 0) {
                stop(client, "No selected fuel available", false);
                return;
            }
            moveFuel(client, menu, source);
        }
    }

    private int findInput(Minecraft client, AbstractFurnaceMenu menu) {
        var key = menu instanceof BlastFurnaceMenu ? RecipePropertySet.BLAST_FURNACE_INPUT
            : menu instanceof SmokerMenu ? RecipePropertySet.SMOKER_INPUT : RecipePropertySet.FURNACE_INPUT;
        var allowed = client.level.recipeAccess().propertySet(key);
        Set<String> ids = StorageMenu.ids(inputs.get());
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty() && ids.contains(StorageMenu.id(stack)) && allowed.test(stack)) return slot;
        }
        return -1;
    }

    private int findFuel(AbstractFurnaceMenu menu) {
        Set<String> ids = StorageMenu.ids(fuels.get());
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty() && ids.contains(StorageMenu.id(stack)) && stack.has(DataComponents.COOKING_FUEL)) return slot;
        }
        return -1;
    }

    private void moveStack(Minecraft client, AbstractFurnaceMenu menu, int source, int target) {
        click(client, menu, source, 0, ContainerInput.PICKUP);
        click(client, menu, target, 0, ContainerInput.PICKUP);
        if (!menu.getCarried().isEmpty()) click(client, menu, source, 0, ContainerInput.PICKUP);
    }

    private void moveFuel(Minecraft client, AbstractFurnaceMenu menu, int source) {
        int count = Math.min((int)fuelPerRefill.get(), menu.getSlot(source).getItem().getCount());
        if (count <= 0) return;
        click(client, menu, source, 0, ContainerInput.PICKUP);
        for (int i = 0; i < count && !menu.getCarried().isEmpty(); i++)
            click(client, menu, 1, 1, ContainerInput.PICKUP);
        if (!menu.getCarried().isEmpty()) click(client, menu, source, 0, ContainerInput.PICKUP);
    }

    private void click(Minecraft client, AbstractFurnaceMenu menu, int slot, int button, ContainerInput input) {
        client.gameMode.handleContainerInput(menu.containerId, slot, button, input, client.player);
        cooldown = (int)interval.get();
    }

    private void stop(Minecraft client, String message, boolean success) {
        client.gui.hud.getChat().addClientSystemMessage(Component.literal("[Anomoly] Auto Smelter: " + message + ".")
            .withStyle(success ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        setEnabled(false);
    }

    @Override protected void onDisable(Minecraft client) {
        observedMenu = null;
        startupDelay = 0;
        cooldown = 0;
        settledTicks = 0;
    }
}
