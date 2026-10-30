package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Filters physical right-click targets by entity type and held main-hand item. */
public final class NoEntityInteract extends Module {
    private final ChoiceSetting entityMode = add(new ChoiceSetting("entity_type_filter",
        "Entity filter", "Blacklist", "Blacklist", "Whitelist"));
    private final StringSetting entityTypes = add(new StringSetting("entity_types",
        "Entity IDs (comma separated)", "minecraft:villager,minecraft:armor_stand"));
    private final ChoiceSetting itemMode = add(new ChoiceSetting("holding_item_filter",
        "Main-hand item filter", "Whitelist", "Whitelist", "Blacklist"));
    private final StringSetting holdingItems = add(new StringSetting("holding_items",
        "Item IDs; empty and mining_tools are supported",
        "empty,minecraft:shears,minecraft:tnt,minecraft:water_bucket,"
            + "minecraft:lava_bucket,minecraft:cobweb,mining_tools"));

    public NoEntityInteract() {
        super("no_entity_interact", "NoEntityInteract",
            "Filter right-click interactions with selected entities and held items.", Category.PLAYER);
    }

    public boolean bypass(Minecraft client) {
        if (!enabled() || client.player == null || client.gui.screen() != null
            || client.player.isSpectator() || !(client.hitResult instanceof EntityHitResult hit)) return false;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(hit.getEntity().getType()).toString();
        boolean listedEntity = ids(entityTypes.get()).contains(type);
        boolean blockedEntity = entityMode.get().equals("Blacklist") ? listedEntity : !listedEntity;
        if (!blockedEntity) return false;
        ItemStack held = client.player.getMainHandItem();
        Set<String> items = ids(holdingItems.get());
        boolean listedItem = (held.isEmpty() && items.contains("empty"))
            || (!held.isEmpty() && items.contains(BuiltInRegistries.ITEM.getKey(held.getItem()).toString()))
            || (items.contains("mining_tools") && (held.is(ItemTags.PICKAXES)
                || held.is(ItemTags.AXES) || held.is(ItemTags.SHOVELS) || held.is(ItemTags.HOES)));
        return itemMode.get().equals("Whitelist") ? listedItem : !listedItem;
    }

    private static Set<String> ids(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty())
            .map(s -> s.contains(":") || s.equals("empty") || s.equals("mining_tools")
                ? s : "minecraft:" + s).collect(Collectors.toSet());
    }
}
