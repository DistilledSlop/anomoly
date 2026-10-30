package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.FallingBlock;

/** Selects a useful offhand item with an emergency totem override. */
public final class Offhand extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Offhand mode", "Auto",
        "Auto", "Totem", "Crystal", "Gapple", "Strength", "Block"));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Ticks between swaps", 4, 1, 40));
    private final FloatSetting health = add(new FloatSetting("totem_health", "Totem health threshold", 14, 0, 20));
    private final BooleanSetting predictFall = add(new BooleanSetting("predict_fall", "Equip totem before lethal fall", true));
    private final BooleanSetting nearbyExplosives = add(new BooleanSetting("nearby_explosives", "Equip totem near explosives", true));
    private final BooleanSetting missingArmor = add(new BooleanSetting("missing_armor", "Equip totem with missing armor", false));
    private final BooleanSetting restoreAfterDanger = add(new BooleanSetting("restore_after_danger", "Restore item after danger", true));
    private final BooleanSetting crystalAura = add(new BooleanSetting("crystal_aura", "Crystals while Crystal Aura runs", true));
    private final BooleanSetting crystalFallback = add(new BooleanSetting("crystal_fallback", "Crystals when totems run out", true));
    private final BooleanSetting gappleSword = add(new BooleanSetting("gapple_sword", "Apples while holding sword", true));
    private final BooleanSetting gappleKillAura = add(new BooleanSetting("gapple_kill_aura", "Require Kill Aura for apples", true));
    private final BooleanSetting strengthSword = add(new BooleanSetting("strength_sword", "Potion while holding sword", true));
    private final BooleanSetting strengthKillAura = add(new BooleanSetting("strength_kill_aura", "Require Kill Aura for potion", true));
    private final BooleanSetting scaffoldBlock = add(new BooleanSetting("scaffold_block", "Blocks while Scaffold runs", true));
    private final BooleanSetting eagleBlock = add(new BooleanSetting("eagle_block", "Blocks while Eagle runs", true));
    private LocalPlayer observedPlayer;
    private int startup;
    private int cooldown;
    private int displacedSlot = -1;
    private ItemStack displaced = ItemStack.EMPTY;

    public Offhand() {
        super("offhand", "Offhand", "Choose totems, crystals, apples, strength potions, or blocks for the offhand.", Category.PLAYER);
    }

    public void selectMode(String selection) {
        if (!enabled()) return;
        if (!mode.get().equals(selection)) {
            mode.set(selection);
            AnomolyClient.saveConfig();
        }
    }

    public void cycleMode() {
        if (!enabled()) return;
        mode.next();
        AnomolyClient.saveConfig();
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (observedPlayer != player) {
            observedPlayer = player;
            startup = 20;
            cooldown = 0;
            displacedSlot = -1;
            displaced = ItemStack.EMPTY;
            return;
        }
        if (startup > 0) { startup--; return; }
        if (cooldown > 0) { cooldown--; return; }
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem()
            || client.gui.screen() != null || player.containerMenu != player.inventoryMenu
            || !player.inventoryMenu.getCarried().isEmpty()
            || AnomolyClient.MODULES.get(AutoTotem.class).enabled()
            || AnomolyClient.MODULES.get(SmartEat.class).busy()
            || AnomolyClient.MODULES.get(AutoMend.class).enabled()) return;

        boolean emergency = danger(client, player);
        String wanted = emergency ? "Totem" : mode.get();
        if (wanted.equals("Auto")) wanted = automatic(client, player);
        if ("Totem".equals(wanted) && !available(player, "Totem") && crystalFallback.get()) wanted = "Crystal";
        if (wanted == null || wanted.equals("Auto")) {
            if (!emergency && restoreAfterDanger.get()) restore(client, player);
            return;
        }
        ItemStack current = player.getOffhandItem();
        if (matches(current, wanted)) {
            if (!emergency) forgetDisplaced();
            return;
        }
        int source = find(player, wanted);
        if (source < 0) return;
        if (emergency && !current.isEmpty() && displacedSlot < 0) {
            displacedSlot = source;
            displaced = current.copy();
        }
        swap(client, player, source);
        cooldown = (int) delay.get();
    }

    private boolean danger(Minecraft client, LocalPlayer player) {
        float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();
        if (effectiveHealth <= health.get()) return true;
        if (missingArmor.get()) for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) if (player.getItemBySlot(slot).isEmpty()) return true;
        if (predictFall.get() && !player.onGround() && !player.isFallFlying()
            && player.fallDistance > 3 && effectiveHealth - (player.fallDistance - 3) <= health.get()) return true;
        if (nearbyExplosives.get()) for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal && entity.distanceToSqr(player) <= 36
                || entity instanceof Creeper creeper && creeper.getSwellDir() > 0
                    && creeper.distanceToSqr(player) <= 25) return true;
        }
        return false;
    }

    private String automatic(Minecraft client, LocalPlayer player) {
        if (scaffoldBlock.get() && AnomolyClient.MODULES.get(Scaffold.class).enabled()
            && available(player, "Block")) return "Block";
        if (eagleBlock.get() && AnomolyClient.MODULES.get(Eagle.class).enabled()
            && available(player, "Block")) return "Block";
        if (crystalAura.get() && AnomolyClient.MODULES.get(CrystalAura.class).enabled()
            && available(player, "Crystal")) return "Crystal";
        boolean sword = player.getMainHandItem().is(ItemTags.SWORDS);
        if (strengthSword.get() && sword && (!strengthKillAura.get()
            || AnomolyClient.MODULES.get(KillAura.class).enabled())
            && !player.hasEffect(MobEffects.STRENGTH) && available(player, "Strength")) return "Strength";
        if (gappleSword.get() && sword && (!gappleKillAura.get()
            || AnomolyClient.MODULES.get(KillAura.class).enabled())
            && available(player, "Gapple")) return "Gapple";
        return null;
    }

    private boolean available(LocalPlayer player, String wanted) {
        return matches(player.getOffhandItem(), wanted) || find(player, wanted) >= 0;
    }

    private int find(LocalPlayer player, String wanted) {
        var menu = player.inventoryMenu;
        int fallback = -1;
        for (int slot = 9; slot < 45; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!matches(stack, wanted)) continue;
            if (wanted.equals("Gapple") && stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return slot;
            if (fallback < 0) fallback = slot;
        }
        return fallback;
    }

    private static boolean matches(ItemStack stack, String wanted) {
        if (stack.isEmpty()) return false;
        return switch (wanted) {
            case "Totem" -> stack.is(Items.TOTEM_OF_UNDYING);
            case "Crystal" -> stack.is(Items.END_CRYSTAL);
            case "Gapple" -> stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GOLDEN_APPLE);
            case "Strength" -> strength(stack);
            case "Block" -> stack.getItem() instanceof BlockItem block
                && !(block.getBlock() instanceof FallingBlock)
                && block.getBlock().defaultBlockState().isSolid();
            default -> false;
        };
    }

    private static boolean strength(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (var effect : contents.getAllEffects())
            if (effect.getEffect().equals(MobEffects.STRENGTH)) return true;
        return false;
    }

    private void restore(Minecraft client, LocalPlayer player) {
        if (displacedSlot < 0) return;
        ItemStack saved = player.inventoryMenu.getSlot(displacedSlot).getItem();
        if (ItemStack.isSameItemSameComponents(saved, displaced)) {
            swap(client, player, displacedSlot);
            cooldown = (int) delay.get();
        }
        forgetDisplaced();
    }

    private void forgetDisplaced() {
        displacedSlot = -1;
        displaced = ItemStack.EMPTY;
    }

    private static void swap(Minecraft client, LocalPlayer player, int source) {
        int id = player.inventoryMenu.containerId;
        boolean occupied = !player.inventoryMenu.getSlot(45).getItem().isEmpty();
        client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(id, 45, 0, ContainerInput.PICKUP, player);
        if (occupied) client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
    }

    @Override protected void onDisable(Minecraft client) {
        observedPlayer = null;
        startup = 0;
        cooldown = 0;
        forgetDisplaced();
    }
}
