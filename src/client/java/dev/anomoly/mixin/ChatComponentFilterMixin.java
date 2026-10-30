package dev.anomoly.mixin;

import dev.anomoly.modules.ChatFilter;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentFilterMixin {
    @Unique private boolean anomoly$reentry;

    @Shadow protected abstract void addMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"), cancellable = true)
    private void anomoly$filter(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (anomoly$reentry) return;
        Component filtered = ChatFilter.incoming(message);
        if (filtered == message) return;
        ci.cancel();
        if (filtered == null) return;
        anomoly$reentry = true;
        try {
            addMessage(filtered, signature, source, tag);
        } finally {
            anomoly$reentry = false;
        }
    }
}
