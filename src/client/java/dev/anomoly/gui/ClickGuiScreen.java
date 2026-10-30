package dev.anomoly.gui;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    public ClickGuiScreen() { super(Component.literal("Anomoly")); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0101420);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (ImGuiRenderer.failed()) {
            graphics.centeredText(font, "Anomoly ImGui could not initialize. Check the game log.", width / 2, height / 2, 0xFFFFFFFF);
        } else if (!ImGuiRenderer.supported()) {
            graphics.centeredText(font, "Anomoly ClickGUI requires the OpenGL graphics backend", width / 2, height / 2 - 9, 0xFFFFFFFF);
            graphics.centeredText(font, "Select OpenGL in Video Settings, or use --graphicsBackend opengl; Esc to close", width / 2, height / 2 + 7, 0xFFAAC0D0);
        }
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() || event.key() == 229) { onClose(); return true; }
        feedKey(event.key(), true);
        return true;
    }

    @Override public boolean keyReleased(KeyEvent event) {
        feedKey(event.key(), false);
        return true;
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (ImGuiRenderer.initialized()) ImGui.getIO().addInputCharacter(event.codepoint());
        return true;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        feedButton(event.button(), true);
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        feedButton(event.button(), false);
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (ImGuiRenderer.initialized()) ImGui.getIO().addMouseWheelEvent((float) scrollX, (float) scrollY);
        return true;
    }

    private static void feedButton(int sdlButton, boolean down) {
        if (!ImGuiRenderer.initialized()) return;
        int button = switch (sdlButton) { case 1 -> 0; case 3 -> 1; case 2 -> 2; default -> -1; };
        if (button >= 0) {
            ImGui.getIO().addMousePosEvent((float) net.minecraft.client.Minecraft.getInstance().mouseHandler.xpos(),
                (float) net.minecraft.client.Minecraft.getInstance().mouseHandler.ypos());
            ImGui.getIO().addMouseButtonEvent(button, down);
        }
    }

    private static void feedKey(int scanCode, boolean down) {
        if (!ImGuiRenderer.initialized()) return;
        int key = switch (scanCode) {
            case 40 -> ImGuiKey.Enter; case 41 -> ImGuiKey.Escape; case 42 -> ImGuiKey.Backspace;
            case 43 -> ImGuiKey.Tab; case 44 -> ImGuiKey.Space; case 79 -> ImGuiKey.RightArrow;
            case 80 -> ImGuiKey.LeftArrow; case 81 -> ImGuiKey.DownArrow; case 82 -> ImGuiKey.UpArrow;
            case 74 -> ImGuiKey.Home; case 77 -> ImGuiKey.End; case 76 -> ImGuiKey.Delete;
            case 224 -> ImGuiKey.LeftCtrl; case 225 -> ImGuiKey.LeftShift;
            case 226 -> ImGuiKey.LeftAlt; case 228 -> ImGuiKey.RightCtrl;
            case 229 -> ImGuiKey.RightShift; case 230 -> ImGuiKey.RightAlt;
            default -> scanCode >= 4 && scanCode <= 29 ? ImGuiKey.A + scanCode - 4 : ImGuiKey.None;
        };
        if (key != ImGuiKey.None) ImGui.getIO().addKeyEvent(key, down);
    }
}
