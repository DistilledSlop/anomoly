package dev.anomoly.gui;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.Setting;
import dev.anomoly.setting.StringSetting;
import dev.anomoly.modules.PluginInspector;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Functional native screen for graphics backends that cannot host the ImGui OpenGL renderer. */
public final class FallbackClickGuiScreen extends Screen {
    private Module.Category category = Module.Category.COMBAT;
    private Module detail;
    private boolean profile;
    private String search = "";
    private int page;

    public FallbackClickGuiScreen() { super(Component.literal("Anomoly")); }

    @Override protected void init() {
        int sidebarWidth = 120;
        int contentX = sidebarWidth + 24;
        int contentWidth = Math.max(240, width - contentX - 16);
        int y = 48;
        for (Module.Category candidate : Module.Category.values()) {
            Module.Category selected = candidate;
            addRenderableWidget(Button.builder(Component.literal(candidate.name()), button -> {
                category = selected;
                detail = null;
                profile = false;
                search = "";
                page = 0;
                rebuildWidgets();
            }).bounds(12, y, sidebarWidth, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.literal("PROFILE"), button -> {
            detail = null;
            profile = true;
            page = 0;
            rebuildWidgets();
        }).bounds(12, y + 8, sidebarWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("PANIC: disable all"), button -> {
            AnomolyClient.MODULES.disableAll();
            rebuildWidgets();
        }).bounds(12, height - 34, 155, 20).build());

        if (profile) initProfile(contentX, contentWidth);
        else if (detail != null) initDetail(contentX, contentWidth);
        else initModules(contentX, contentWidth);
    }

    private void initModules(int x, int contentWidth) {
        EditBox searchBox = new EditBox(font, x, 44, Math.min(290, contentWidth - 84), 20, Component.literal("Search modules"));
        searchBox.setValue(search);
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Search"), button -> {
            search = searchBox.getValue().trim();
            page = 0;
            rebuildWidgets();
        }).bounds(x + Math.min(296, contentWidth - 78), 44, 70, 20).build());

        List<Module> modules = visibleModules();
        int pageSize = Math.max(4, (height - 145) / 34);
        int start = Math.min(page * pageSize, Math.max(0, modules.size() - 1));
        int end = Math.min(start + pageSize, modules.size());
        for (int index = start; index < end; index++) {
            Module module = modules.get(index);
            int rowY = 80 + (index - start) * 34;
            Button toggle = Button.builder(Component.literal((module.enabled() ? "ON  " : "OFF ") + module.name()), button -> {
                module.setEnabled(!module.enabled());
                button.setMessage(Component.literal((module.enabled() ? "ON  " : "OFF ") + module.name()));
            }).bounds(x, rowY, Math.max(130, contentWidth - 96), 24)
                .tooltip(Tooltip.create(Component.literal(module.description()))).build();
            addRenderableWidget(toggle);
            addRenderableWidget(Button.builder(Component.literal("Settings"), button -> {
                detail = module;
                page = 0;
                rebuildWidgets();
            }).bounds(x + contentWidth - 88, rowY, 88, 24).build());
        }
        addPagination(x, contentWidth, modules.size(), pageSize);
    }

    private void initDetail(int x, int contentWidth) {
        Module module = detail;
        addRenderableWidget(Button.builder(Component.literal("Back to modules"), button -> {
            detail = null;
            page = 0;
            rebuildWidgets();
        }).bounds(x, 44, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal(module.enabled() ? "Enabled" : "Disabled"), button -> {
            module.setEnabled(!module.enabled());
            button.setMessage(Component.literal(module.enabled() ? "Enabled" : "Disabled"));
        }).bounds(x + contentWidth - 100, 44, 100, 20).build());

        List<Setting> settings = module.settings();
        int pageSize = Math.max(4, (height - 165) / 32);
        int start = Math.min(page * pageSize, Math.max(0, settings.size() - 1));
        int end = Math.min(start + pageSize, settings.size());
        for (int index = start; index < end; index++) {
            Setting setting = settings.get(index);
            int rowY = 98 + (index - start) * 32;
            if (setting instanceof BooleanSetting toggle) {
                addRenderableWidget(Button.builder(Component.literal(toggle.name() + ": " + (toggle.get() ? "ON" : "OFF")), button -> {
                    toggle.set(!toggle.get());
                    button.setMessage(Component.literal(toggle.name() + ": " + (toggle.get() ? "ON" : "OFF")));
                    AnomolyClient.saveConfig();
                }).bounds(x, rowY, Math.min(contentWidth, 320), 24).build());
            } else if (setting instanceof FloatSetting slider) {
                double initial = (slider.get() - slider.min()) / (slider.max() - slider.min());
                addRenderableWidget(new AbstractSliderButton(x, rowY, Math.min(contentWidth, 320), 24,
                    Component.literal(slider.name()), initial) {
                    { updateMessage(); }
                    @Override protected void updateMessage() {
                        setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", slider.name(), slider.get())));
                    }
                    @Override protected void applyValue() {
                        slider.set(slider.min() + (slider.max() - slider.min()) * (float)value);
                        AnomolyClient.saveConfig();
                    }
                });
            } else if (setting instanceof StringSetting string) {
                EditBox box = new EditBox(font, x, rowY, Math.min(contentWidth, 370), 24, Component.literal(string.name()));
                box.setMaxLength(256);
                box.setValue(string.get());
                box.setResponder(value -> { string.set(value); AnomolyClient.saveConfig(); });
                addRenderableWidget(box);
            }
        }
        addPagination(x, contentWidth, settings.size(), pageSize);
    }

    private void initProfile(int x, int contentWidth) {
        addRenderableWidget(Button.builder(Component.literal(AnomolyClient.PROFILE.enabled() ? "Recording: ON" : "Recording: OFF"), button -> {
            AnomolyClient.PROFILE.setEnabled(!AnomolyClient.PROFILE.enabled());
            button.setMessage(Component.literal(AnomolyClient.PROFILE.enabled() ? "Recording: ON" : "Recording: OFF"));
            AnomolyClient.saveConfig();
        }).bounds(x, 80, Math.min(contentWidth, 220), 24).build());
    }

    private void addPagination(int x, int contentWidth, int total, int pageSize) {
        int pages = Math.max(1, (total + pageSize - 1) / pageSize);
        if (page >= pages) page = pages - 1;
        Button previous = Button.builder(Component.literal("Previous"), button -> {
            page--;
            rebuildWidgets();
        }).bounds(x, height - 36, 90, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal("Next"), button -> {
            page++;
            rebuildWidgets();
        }).bounds(x + contentWidth - 90, height - 36, 90, 20).build();
        next.active = page + 1 < pages;
        addRenderableWidget(next);
    }

    private List<Module> visibleModules() {
        String query = search.toLowerCase(Locale.ROOT);
        return AnomolyClient.MODULES.all().stream()
            .filter(module -> !query.isEmpty()
                ? module.name().toLowerCase(Locale.ROOT).contains(query)
                    || module.description().toLowerCase(Locale.ROOT).contains(query)
                : module.category() == category)
            .toList();
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0101825);
        graphics.fill(0, 0, 139, height, 0xFF152B36);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, "ANOMOLY", 14, 16, 0xFF61EBC6);
        int x = 144;
        if (profile) {
            graphics.text(font, "SERVER PROFILE", x, 16, 0xFF61EBC6);
            graphics.text(font, "Server: " + AnomolyClient.PROFILE.server(), x, 118, 0xFFE2EAF0);
            graphics.text(font, "Brand: " + AnomolyClient.PROFILE.brand(), x, 140, 0xFFE2EAF0);
            graphics.text(font, "Position corrections: " + AnomolyClient.PROFILE.corrections(), x, 162, 0xFFE2EAF0);
            graphics.text(font, "Log: " + AnomolyClient.PROFILE.path(), x, 184, 0xFFAAC0D0);
            PluginInspector plugins = AnomolyClient.MODULES.get(PluginInspector.class);
            graphics.text(font, "Command namespaces: " + (plugins.enabled()
                ? String.join(", ", plugins.namespaces()) : "Enable Plugins module"), x, 206, 0xFFAAC0D0);
        } else if (detail != null) {
            graphics.text(font, detail.name(), x, 16, 0xFF61EBC6);
            graphics.text(font, detail.description(), x, 76, 0xFFAAC0D0);
            var key = AnomolyClient.moduleKey(detail);
            if (key != null) graphics.text(font, "Toggle key: " + key.getTranslatedKeyMessage().getString(), x, 88, 0xFF8199A6);
            List<Setting> settings = detail.settings();
            int pageSize = Math.max(4, (height - 165) / 32);
            int start = Math.min(page * pageSize, Math.max(0, settings.size() - 1));
            for (int index = start; index < Math.min(start + pageSize, settings.size()); index++) {
                if (settings.get(index) instanceof StringSetting string) {
                    graphics.text(font, string.name(), x + Math.min(width - x - 160, 382),
                        106 + (index - start) * 32, 0xFFAAC0D0);
                }
            }
        } else {
            graphics.text(font, search.isEmpty() ? category.name() : "SEARCH RESULTS", x, 16, 0xFF61EBC6);
            graphics.text(font, "Page " + (page + 1), x + 175, height - 32, 0xFFAAC0D0);
        }
        graphics.text(font, "Vulkan/native controls  •  OpenGL uses ImGui", 14, height - 58, 0xFF8199A6);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 229) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!profile && scrollY != 0) {
            int count = detail == null ? visibleModules().size() : detail.settings().size();
            int pageSize = Math.max(4, (height - (detail == null ? 145 : 165)) / (detail == null ? 34 : 32));
            int pages = Math.max(1, (count + pageSize - 1) / pageSize);
            page = Math.max(0, Math.min(pages - 1, page + (scrollY < 0 ? 1 : -1)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
