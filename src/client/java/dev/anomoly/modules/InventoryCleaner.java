package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;

/** Sorts player slots and applies explicit keep rules with ordinary menu clicks. */
public final class InventoryCleaner extends Module {
    private final StringSetting layout = add(new StringSetting("hotbar_layout",
        "Nine slot categories or item IDs, comma separated",
        "weapon,bow,pickaxe,axe,ignore,potion,food,block,block"));
    private final ChoiceSetting offhand = add(new ChoiceSetting("offhand_item", "Offhand category", "Ignore",
        "Ignore", "Shield", "Pearl", "Gapple", "Food", "Block", "None"));
    private final BooleanSetting greedy = add(new BooleanSetting("greedy", "Replace worn items with better matches", true));
    private final BooleanSetting merge = add(new BooleanSetting("merge_stacks", "Merge partial stacks", true));
    private final BooleanSetting containers = add(new BooleanSetting("containers", "Run in open containers", false));
    private final ChoiceSetting screens = add(new ChoiceSetting("screens", "Run when", "Both", "Both", "Closed", "Inventory"));
    private final StringSetting blacklist = add(new StringSetting("blacklist", "Item IDs to always drop", ""));
    private final ChoiceSetting filter = add(new ChoiceSetting("filter_mode", "Item drop filter", "Blacklist",
        "Blacklist", "Whitelist"));
    private final StringSetting whitelist = add(new StringSetting("whitelist", "Item IDs to keep in whitelist mode", ""));
    private final FloatSetting blocks = add(new FloatSetting("maximum_blocks", "Maximum block items", 512, 0, 2500));
    private final FloatSetting arrows = add(new FloatSetting("maximum_arrows", "Maximum arrows", 128, 0, 2500));
    private final FloatSetting throwables = add(new FloatSetting("maximum_throwables", "Maximum throwables", 64, 0, 600));
    private final FloatSetting food = add(new FloatSetting("maximum_food_points", "Maximum food points", 200, 0, 2000));
    private final FloatSetting water = add(new FloatSetting("maximum_water_buckets", "Maximum water buckets", 2, 0, 16));
    private final FloatSetting lava = add(new FloatSetting("maximum_lava_buckets", "Maximum lava buckets", 2, 0, 16));
    private final FloatSetting milk = add(new FloatSetting("maximum_milk_buckets", "Maximum milk buckets", 2, 0, 16));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Ticks between actions", 3, 1, 40));
    private int cooldown;

    public InventoryCleaner() {
        super("inventory_cleaner", "Inventory Cleaner",
            "Sort hotbar items, merge stacks, and discard configured surplus.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        var player = client.player;
        if (client.gameMode == null || player.isSpectator() || player.isUsingItem()
            || !player.containerMenu.getCarried().isEmpty()) return;
        boolean inventory = player.containerMenu instanceof InventoryMenu;
        if (!inventory && !containers.get()) return;
        if (inventory) {
            boolean open = client.gui.screen() instanceof InventoryScreen;
            if (client.gui.screen() != null && !open) return;
            if (screens.get().equals("Closed") && open || screens.get().equals("Inventory") && !open) return;
        } else {
            if (client.gui.screen() == null || AnomolyClient.MODULES.get(AutoCrafter.class).enabled()
                || AnomolyClient.MODULES.get(AutoSmelter.class).enabled()
                || AnomolyClient.MODULES.get(AutoBrewer.class).enabled()) return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        int first = inventory ? 9 : menu.slots.size() - 36;
        int hotbar = inventory ? 36 : menu.slots.size() - 9;
        if (sortHotbar(client, menu, first, hotbar)) return;
        if (inventory && sortOffhand(client, menu, first)) return;
        if (merge.get() && mergeStacks(client, menu, first, hotbar)) return;
        cleanItems(client, menu, first, hotbar);
    }

    private boolean sortHotbar(Minecraft client, AbstractContainerMenu menu, int first, int hotbar) {
        List<String> wants = layout();
        for (int index = 0; index < 9; index++) {
            String want = wants.get(index);
            if (want.equals("ignore") || want.equals("none")) continue;
            int destination = hotbar + index;
            ItemStack current = menu.getSlot(destination).getItem();
            int source = -1;
            double best = matches(current, want) ? (greedy.get() ? quality(current) : Double.POSITIVE_INFINITY) : -1;
            for (int slot = first; slot < first + 36; slot++) {
                if (slot == destination || slot >= hotbar && wants.get(slot - hotbar).equals("ignore")
                    || slot >= hotbar && slot < destination && !wants.get(slot - hotbar).equals("none"))
                    continue;
                ItemStack candidate = menu.getSlot(slot).getItem();
                if (!matches(candidate, want)) continue;
                double score = quality(candidate);
                if (source < 0 && best < 0 || greedy.get() && score > best + 0.001) {
                    source = slot;
                    best = score;
                }
            }
            if (source < 0) continue;
            click(client, menu, source, index, ContainerInput.SWAP);
            return true;
        }
        return false;
    }

    private boolean sortOffhand(Minecraft client, AbstractContainerMenu menu, int first) {
        String want = offhand.get().toLowerCase(Locale.ROOT);
        if (want.equals("ignore") || want.equals("none")
            || AnomolyClient.MODULES.get(AutoTotem.class).enabled()
            || AnomolyClient.MODULES.get(AutoMend.class).enabled()
            || AnomolyClient.MODULES.get(Offhand.class).enabled()) return false;
        ItemStack current = menu.getSlot(45).getItem();
        if (matches(current, want)) return false;
        List<String> wants = layout();
        for (int slot = first; slot < first + 36; slot++) {
            if (slot >= 36 && !wants.get(slot - 36).equals("none")) continue;
            if (!matches(menu.getSlot(slot).getItem(), want)) continue;
            click(client, menu, slot, 0, ContainerInput.PICKUP);
            click(client, menu, 45, 0, ContainerInput.PICKUP);
            if (!menu.getCarried().isEmpty()) click(client, menu, slot, 0, ContainerInput.PICKUP);
            cooldown = (int)delay.get();
            return true;
        }
        return false;
    }

    private boolean mergeStacks(Minecraft client, AbstractContainerMenu menu, int first, int hotbar) {
        List<String> wants = layout();
        // Fill selected hotbar stacks first, then consolidate the rest of the inventory.
        for (int pass = 0; pass < 2; pass++)
            for (int destination = first; destination < first + 36; destination++) {
                if (destination >= hotbar && wants.get(destination - hotbar).equals("ignore")) continue;
                boolean protectedHotbar = destination >= hotbar && !wants.get(destination - hotbar).equals("none");
                if (pass == 0 && !protectedHotbar || pass == 1 && protectedHotbar) continue;
                ItemStack target = menu.getSlot(destination).getItem();
                if (target.isEmpty() || target.getCount() >= target.getMaxStackSize()) continue;
                for (int source = first + 35; source >= first; source--) {
                    if (source == destination || source >= hotbar && !wants.get(source - hotbar).equals("none")
                        || pass == 1 && source <= destination) continue;
                    ItemStack stack = menu.getSlot(source).getItem();
                    if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, target)) continue;
                    click(client, menu, source, 0, ContainerInput.PICKUP);
                    click(client, menu, destination, 0, ContainerInput.PICKUP);
                    if (!menu.getCarried().isEmpty()) click(client, menu, source, 0, ContainerInput.PICKUP);
                    cooldown = (int)delay.get();
                    return true;
                }
            }
        return false;
    }

    private void cleanItems(Minecraft client, AbstractContainerMenu menu, int first, int hotbar) {
        Set<String> blocked = StorageMenu.ids(blacklist.get());
        Set<String> kept = StorageMenu.ids(whitelist.get());
        List<String> wants = layout();
        for (int slot = first + 35; slot >= first; slot--) {
            if (slot >= hotbar && wants.get(slot - hotbar).equals("ignore")) continue;
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            String id = StorageMenu.id(stack);
            boolean blacklisted = blocked.contains(id) || filter.get().equals("Whitelist") && !kept.isEmpty() && !kept.contains(id);
            if (blacklisted) {
                click(client, menu, slot, 1, ContainerInput.THROW);
                return;
            }
        }
        String[] categories = {"block", "arrow", "throwable", "food", "water", "lava", "milk"};
        int[] limits = {(int)blocks.get(), (int)arrows.get(), (int)throwables.get(), (int)food.get(),
            (int)water.get(), (int)lava.get(), (int)milk.get()};
        for (int i = 0; i < categories.length; i++) {
            int total = 0;
            for (int slot = first; slot < first + 36; slot++)
                if (slot < hotbar || !wants.get(slot - hotbar).equals("ignore"))
                    total += quantity(menu.getSlot(slot).getItem(), categories[i]);
            if (total <= limits[i]) continue;
            for (int pass = 0; pass < 2; pass++)
                for (int slot = first + 35; slot >= first; slot--) {
                    if (slot >= hotbar && wants.get(slot - hotbar).equals("ignore")) continue;
                    boolean protectedHotbar = slot >= hotbar && !wants.get(slot - hotbar).equals("none");
                    if (pass == 0 && protectedHotbar || pass == 1 && !protectedHotbar) continue;
                    ItemStack stack = menu.getSlot(slot).getItem();
                    int amount = quantity(stack, categories[i]);
                    if (amount == 0) continue;
                    int excess = total - limits[i];
                    int button = amount <= excess ? 1 : 0;
                    click(client, menu, slot, button, ContainerInput.THROW);
                    return;
                }
        }
    }

    private static int quantity(ItemStack stack, String category) {
        if (stack.isEmpty()) return 0;
        if (category.equals("food")) {
            var properties = stack.get(DataComponents.FOOD);
            return properties == null ? 0 : properties.nutrition() * stack.getCount();
        }
        if (category.equals("arrow")) return (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW)
            || stack.is(Items.TIPPED_ARROW)) ? stack.getCount() : 0;
        if (category.equals("throwable")) return (stack.is(Items.EGG) || stack.is(Items.SNOWBALL)
            || stack.is(Items.WIND_CHARGE)) ? stack.getCount() : 0;
        return matches(stack, category) ? stack.getCount() : 0;
    }

    private static boolean matches(ItemStack stack, String category) {
        if (stack.isEmpty()) return false;
        return switch (category) {
            case "sword" -> stack.is(ItemTags.SWORDS);
            case "weapon" -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE)
                || stack.is(ItemTags.SPEARS);
            case "spear" -> stack.is(ItemTags.SPEARS);
            case "mace" -> stack.is(Items.MACE);
            case "bow" -> stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
            case "crossbow" -> stack.getItem() instanceof CrossbowItem;
            case "axe" -> stack.is(ItemTags.AXES);
            case "pickaxe" -> stack.is(ItemTags.PICKAXES);
            case "shovel" -> stack.is(ItemTags.SHOVELS);
            case "hoe" -> stack.is(ItemTags.HOES);
            case "rod" -> stack.getItem() instanceof FishingRodItem;
            case "shield" -> stack.getItem() instanceof ShieldItem;
            case "water" -> stack.is(Items.WATER_BUCKET);
            case "lava" -> stack.is(Items.LAVA_BUCKET);
            case "milk" -> stack.is(Items.MILK_BUCKET);
            case "pearl" -> stack.is(Items.ENDER_PEARL);
            case "gapple" -> stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
            case "food" -> stack.get(DataComponents.FOOD) != null;
            case "potion" -> stack.getItem() instanceof PotionItem;
            case "block" -> stack.getItem() instanceof BlockItem;
            case "throwable", "throwables" -> stack.is(Items.EGG) || stack.is(Items.SNOWBALL)
                || stack.is(Items.WIND_CHARGE);
            default -> StorageMenu.id(stack).equals(category.contains(":") ? category : "minecraft:" + category);
        };
    }

    private static double quality(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        double remaining = stack.isDamageableItem() ? 1.0 - (double)stack.getDamageValue() / stack.getMaxDamage() : 1.0;
        return stack.getMaxDamage() * 2.0 + remaining + stack.getCount() / 1000.0;
    }

    private List<String> layout() {
        String[] tokens = layout.get().toLowerCase(Locale.ROOT).split(",", -1);
        List<String> result = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) result.add(i < tokens.length && !tokens[i].trim().isEmpty()
            ? tokens[i].trim() : "ignore");
        return result;
    }

    private void click(Minecraft client, AbstractContainerMenu menu, int slot, int button, ContainerInput input) {
        client.gameMode.handleContainerInput(menu.containerId, slot, button, input, client.player);
        cooldown = (int)delay.get();
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
