package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Four independent interaction guards with entity and block lists. */
public final class NoInteract extends Module {
    private final StringSetting mineBlocks = add(new StringSetting("mine_blocks", "Blocks to protect from mining", ""));
    private final ChoiceSetting mineMode = add(mode("mine_mode", "Mine list mode"));
    private final StringSetting interactBlocks = add(new StringSetting("interact_blocks", "Blocks to protect from use", ""));
    private final ChoiceSetting blockMode = add(mode("block_mode", "Block use list mode"));
    private final ChoiceSetting blockHands = add(hands("block_hands", "Block use hands"));
    private final StringSetting hitEntities = add(new StringSetting("hit_entities", "Entities to protect from hits", ""));
    private final ChoiceSetting hitMode = add(mode("hit_mode", "Entity hit list mode"));
    private final StringSetting interactEntities = add(new StringSetting("interact_entities", "Entities to protect from use", ""));
    private final ChoiceSetting entityMode = add(mode("entity_mode", "Entity use list mode"));
    private final ChoiceSetting entityHands = add(hands("entity_hands", "Entity use hands"));
    private final StringSetting friendNames = add(new StringSetting("friend_names", "Friend names (comma separated)", ""));
    private final ChoiceSetting friends = add(action("friends", "Friends"));
    private final ChoiceSetting babies = add(action("babies", "Baby animals"));
    private final ChoiceSetting named = add(action("named", "Named entities"));

    public NoInteract() {
        super("no_interact", "No Interact",
            "Block selected mining, block use, entity hits, and entity use.", Category.PLAYER);
    }

    public boolean blockMine(Minecraft client, BlockPos pos) {
        return enabled() && client.level != null && matches(mineMode, mineBlocks.get(),
            BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(pos).getBlock()).toString());
    }

    public boolean blockUse(Minecraft client, BlockHitResult hit, InteractionHand hand) {
        if (!enabled() || client.level == null) return false;
        return selected(blockHands, hand) || matches(blockMode, interactBlocks.get(),
            BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(hit.getBlockPos()).getBlock()).toString());
    }

    public boolean entityHit(Entity entity) {
        return enabled() && (special(entity, "Hit") || matches(hitMode, hitEntities.get(),
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
    }

    public boolean entityUse(Entity entity, InteractionHand hand) {
        return enabled() && (selected(entityHands, hand) || special(entity, "Interact")
            || matches(entityMode, interactEntities.get(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
    }

    private boolean special(Entity entity, String action) {
        return (applies(friends, action) && entity instanceof Player player
                && names(friendNames.get()).contains(player.getGameProfile().name().toLowerCase(Locale.ROOT)))
            || (applies(babies, action) && entity instanceof Animal animal && animal.isBaby())
            || (applies(named, action) && entity.hasCustomName());
    }

    private static boolean applies(ChoiceSetting setting, String action) {
        String value = setting.get();
        return value.equals("Both") || value.equals(action);
    }

    private static boolean selected(ChoiceSetting setting, InteractionHand hand) {
        String value = setting.get();
        return value.equals("Both") || value.equals(hand == InteractionHand.MAIN_HAND ? "Mainhand" : "Offhand");
    }

    private static boolean matches(ChoiceSetting mode, String raw, String id) {
        boolean listed = ids(raw).contains(id);
        return mode.get().equals("Blacklist") ? listed : !listed;
    }

    private static Set<String> ids(String raw) {
        return names(raw).stream().map(s -> s.contains(":") ? s : "minecraft:" + s)
            .collect(Collectors.toSet());
    }

    private static Set<String> names(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    private static ChoiceSetting mode(String id, String name) {
        return new ChoiceSetting(id, name, "Blacklist", "Blacklist", "Whitelist");
    }

    private static ChoiceSetting hands(String id, String name) {
        return new ChoiceSetting(id, name, "None", "None", "Mainhand", "Offhand", "Both");
    }

    private static ChoiceSetting action(String id, String name) {
        return new ChoiceSetting(id, name, "None", "None", "Hit", "Interact", "Both");
    }
}
