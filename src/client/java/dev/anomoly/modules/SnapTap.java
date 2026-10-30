package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public final class SnapTap extends Module {
    private boolean lastLeftWins, lastForwardWins;
    private boolean prevLeft, prevRight, prevForward, prevBack;

    public SnapTap() {
        super("snap_tap", "SnapTap", "When opposite movement keys are held together, move in the direction of the key pressed last.", Category.MOVEMENT);
    }

    public void apply(KeyboardInput input) {
        if (!enabled()) return;
        Input keys = input.keyPresses;
        if (keys.left() && !prevLeft) lastLeftWins = true;
        if (keys.right() && !prevRight) lastLeftWins = false;
        if (keys.forward() && !prevForward) lastForwardWins = true;
        if (keys.backward() && !prevBack) lastForwardWins = false;
        prevLeft = keys.left();
        prevRight = keys.right();
        prevForward = keys.forward();
        prevBack = keys.backward();
        boolean left = keys.left(), right = keys.right(), forward = keys.forward(), back = keys.backward();
        if (left && right) { left = lastLeftWins; right = !lastLeftWins; }
        if (forward && back) { forward = lastForwardWins; back = !lastForwardWins; }
        if (left == keys.left() && right == keys.right() && forward == keys.forward() && back == keys.backward()) return;
        input.keyPresses = new Input(forward, back, left, right, keys.jump(), keys.shift(), keys.sprint());
        ((dev.anomoly.mixin.ClientInputAccessor)input).anomoly$setMoveVector(new Vec2((left ? 1 : 0) - (right ? 1 : 0), (forward ? 1 : 0) - (back ? 1 : 0)));
    }

    @Override protected void onDisable(Minecraft client) {
        prevLeft = prevRight = prevForward = prevBack = false;
    }
}
