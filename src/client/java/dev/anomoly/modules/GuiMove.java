package dev.anomoly.modules;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anomoly.Module;
import dev.anomoly.mixin.ClientInputAccessor;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Forwards physical movement input through non-text GUI screens. */
public final class GuiMove extends Module {
    private final ChoiceSetting screens = add(new ChoiceSetting("screens", "Allowed screens", "Inventory",
        "Inventory", "All"));
    private final BooleanSetting jump = add(new BooleanSetting("jump", "Allow jumping", true));
    private final BooleanSetting sneak = add(new BooleanSetting("sneak", "Allow sneaking", false));
    private final BooleanSetting sprint = add(new BooleanSetting("sprint", "Allow sprinting", true));
    private final BooleanSetting arrowRotate = add(new BooleanSetting("arrow_rotate", "Rotate view with arrow keys", true));
    private final FloatSetting rotateSpeed = add(new FloatSetting("rotate_speed", "Degrees per arrow key tick", 4, 0, 20));

    public GuiMove() {
        super("gui_move", "GUI Move", "Walk, jump, and optionally sneak while inventory screens are open.",
            Category.MOVEMENT);
    }

    public void apply(KeyboardInput input) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || client.player == null || !allowed(client.gui.screen())) return;
        boolean forward = pressed(client.options.keyUp);
        boolean backward = pressed(client.options.keyDown);
        boolean left = pressed(client.options.keyLeft);
        boolean right = pressed(client.options.keyRight);
        input.keyPresses = new Input(forward, backward, left, right,
            jump.get() && pressed(client.options.keyJump),
            sneak.get() && pressed(client.options.keyShift),
            sprint.get() && pressed(client.options.keySprint));
        float x = left == right ? 0 : left ? 1 : -1;
        float y = forward == backward ? 0 : forward ? 1 : -1;
        ((ClientInputAccessor)input).anomoly$setMoveVector(new Vec2(x, y).normalized());
    }

    @Override public void tick(Minecraft client) {
        if (!arrowRotate.get() || !allowed(client.gui.screen())) return;
        float speed = rotateSpeed.get();
        float yaw = client.player.getYRot();
        float pitch = client.player.getXRot();
        if (InputConstants.isKeyDown(80)) yaw -= speed; // SDL left arrow
        if (InputConstants.isKeyDown(79)) yaw += speed; // SDL right arrow
        if (InputConstants.isKeyDown(82)) pitch -= speed; // SDL up arrow
        if (InputConstants.isKeyDown(81)) pitch += speed; // SDL down arrow
        client.player.setYRot(yaw);
        client.player.setXRot(Mth.clamp(pitch, -90, 90));
    }

    private boolean allowed(Screen screen) {
        if (screen == null || screen instanceof ChatScreen || screen instanceof AbstractSignEditScreen
            || screen instanceof BookEditScreen || screen instanceof AbstractCommandBlockEditScreen
            || screen.getFocused() instanceof EditBox
            || screen instanceof dev.anomoly.gui.ClickGuiScreen
            || screen instanceof dev.anomoly.gui.FallbackClickGuiScreen) return false;
        return screens.get().equals("All") || screen instanceof AbstractContainerScreen<?>;
    }

    private static boolean pressed(KeyMapping mapping) {
        if (mapping.isUnbound()) return false;
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        return key.getType() == InputConstants.Type.KEYBOARD && InputConstants.isKeyDown(key.getValue());
    }
}
