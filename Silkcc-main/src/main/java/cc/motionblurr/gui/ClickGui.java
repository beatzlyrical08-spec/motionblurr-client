package cc.motionblurr.gui;

import cc.motionblurr.MotionBlurrClient;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.module.setting.BooleanSetting;
import cc.motionblurr.module.setting.ColorSetting;
import cc.motionblurr.module.setting.KeybindSetting;
import cc.motionblurr.module.setting.ModeSetting;
import cc.motionblurr.module.setting.NumberSetting;
import cc.motionblurr.module.setting.RangeSetting;
import cc.motionblurr.module.setting.Setting;
import cc.motionblurr.module.setting.StringSetting;
import cc.motionblurr.utils.keybinding.KeyUtils;
import cc.motionblurr.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGui extends Screen {

    private static final int BACKDROP = 0xCC050610;
    private static final int WINDOW = 0xEE0B0D18;
    private static final int GLASS = 0xAA141728;
    private static final int GLASS_DARK = 0xCC0E101C;
    private static final int CARD = 0xCC181B2B;
    private static final int CARD_HOVER = 0xDD24283C;
    private static final int ROW = 0xAA1A1E31;
    private static final int ROW_HOVER = 0xCC252A42;
    private static final int ACCENT = 0xFF9B5CFF;
    private static final int ACCENT_2 = 0xFF6D5CFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_SOFT = 0xFFE8EAF4;
    private static final int TEXT_MUTED = 0xFF9EA3B7;
    private static final int TEXT_DIM = 0xFF696F86;
    private static final int GREEN = 0xFF55F6A8;
    private static final int RED = 0xFFFF5D86;

    private static final int GUI_MARGIN = 22;
    private static final int SIDEBAR_WIDTH = 158;
    private static final int TOPBAR_HEIGHT = 58;
    private static final int CARD_HEIGHT = 58;
    private static final int SETTING_HEIGHT = 50;
    private static final int GAP = 10;

    private final Map<Category, Float> categoryHoverAnimations = new EnumMap<>(Category.class);
    private final Map<Module, Float> moduleHoverAnimations = new HashMap<>();
    private final Map<Module, Float> moduleToggleAnimations = new HashMap<>();
    private final Map<Module, Float> moduleSelectAnimations = new HashMap<>();
    private final Map<Setting, Float> settingHoverAnimations = new HashMap<>();
    private final Map<Setting, Float> sliderAnimations = new HashMap<>();
    private final Map<Setting, Float> settingToggleAnimations = new HashMap<>();

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule;
    private KeybindSetting listeningKeybind;
    private String searchText = "";
    private boolean searchFocused;
    private double moduleScroll;
    private double settingsScroll;
    private float searchFocusAnimation;
    private float pulseAnimation;

    public ClickGui() {
        super(Text.literal("MotionBlurr"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        stopListening();
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningKeybind != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind.setKeyCode(keyCode);
            }
            stopListening();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                moduleScroll = 0;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            moduleScroll -= 34;
            clampScrolls();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moduleScroll += 34;
            clampScrolls();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr)) {
            searchText += chr;
            moduleScroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = getLayout();
        if (isHovered(mouseX, mouseY, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH)) {
            moduleScroll -= verticalAmount * 36.0D;
            clampScrolls();
            return true;
        }
        if (isHovered(mouseX, mouseY, layout.settingsX, layout.settingsY, layout.settingsW, layout.settingsH)) {
            settingsScroll -= verticalAmount * 36.0D;
            clampScrolls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        pulseAnimation += Math.max(0.3F, delta) * 0.035F;
        searchFocusAnimation = animate(searchFocusAnimation, searchFocused ? 1.0F : 0.0F, 0.18F);

        Layout layout = getLayout();
        ensureSelectedModule();
        clampScrolls();

        drawBackdrop(context);
        drawWindow(context, layout);
        drawSidebar(context, layout, mouseX, mouseY);
        drawTopBar(context, layout, mouseX, mouseY);
        drawModulePanel(context, layout, mouseX, mouseY);
        drawSettingsPanel(context, layout, mouseX, mouseY);
    }

    private void drawBackdrop(DrawContext context) {
        context.fill(0, 0, width, height, BACKDROP);
        drawGlowRect(context, width / 2 - 180, height / 2 - 130, 360, 260, 28, withAlpha(ACCENT, 26), 5);
    }

    private void drawWindow(DrawContext context, Layout layout) {
        drawGlowRect(context, layout.x, layout.y, layout.w, layout.h, 18, withAlpha(ACCENT, 34), 5);
        RenderUtils.drawRoundedRect(context, layout.x, layout.y, layout.w, layout.h, 18, WINDOW);
        RenderUtils.drawRoundedRect(context, layout.x + 1, layout.y + 1, layout.w - 2, layout.h - 2, 17, 0x441D2140);
    }

    private void drawSidebar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.sidebarX, layout.sidebarY, layout.sidebarW, layout.sidebarH, 16, GLASS_DARK);
        drawGlowRect(context, layout.sidebarX + 12, layout.sidebarY + 15, 34, 34, 11, withAlpha(ACCENT, 42), 3);
        RenderUtils.drawRoundedRect(context, layout.sidebarX + 14, layout.sidebarY + 17, 30, 30, 10, 0xFF1B1632);
        context.drawText(textRenderer, "MB", layout.sidebarX + 23, layout.sidebarY + 28, ACCENT, false);
        context.drawText(textRenderer, "MotionBlurr", layout.sidebarX + 54, layout.sidebarY + 20, TEXT, false);
        context.drawText(textRenderer, "premium client", layout.sidebarX + 54, layout.sidebarY + 34, TEXT_DIM, false);

        int itemX = layout.sidebarX + 12;
        int itemY = layout.sidebarY + 70;
        int itemW = layout.sidebarW - 24;
        int itemH = 30;

        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hovered = isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH);
            float hover = animate(categoryHoverAnimations, category, hovered ? 1.0F : 0.0F, 0.18F);
            float selectedProgress = selected ? 1.0F : 0.0F;
            int offset = Math.round(lerp(0, 4, easeOutCubic(hover)));

            if (hover > 0.01F) {
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 9, withAlpha(0xFF252A42, (int) (90 * hover)));
            }
            if (selected) {
                drawGlowRect(context, itemX, itemY, itemW, itemH, 9, withAlpha(ACCENT, 58), 3);
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 9, 0xDD33215B);
                RenderUtils.drawRoundedRect(context, itemX + 3, itemY + 7, 3, itemH - 14, 2, ACCENT);
            }

            context.drawText(textRenderer, category.getName(), itemX + 14 + offset, itemY + 11,
                    selected ? TEXT : lerpColor(TEXT_MUTED, TEXT_SOFT, hover), false);
            itemY += itemH + 7;
        }

        int infoY = layout.sidebarY + layout.sidebarH - 78;
        RenderUtils.drawRoundedRect(context, layout.sidebarX + 12, infoY, layout.sidebarW - 24, 44, 12, 0xAA191D30);
        RenderUtils.drawRoundedRect(context, layout.sidebarX + 22, infoY + 10, 24, 24, 8, 0xFF2A2145);
        context.drawText(textRenderer, "S", layout.sidebarX + 31, infoY + 18, ACCENT, false);
        context.drawText(textRenderer, "MotionBlurr", layout.sidebarX + 54, infoY + 10, TEXT_SOFT, false);
        context.drawText(textRenderer, "local profile", layout.sidebarX + 54, infoY + 25, TEXT_DIM, false);
        context.drawText(textRenderer, "v1.0", layout.sidebarX + 16, layout.sidebarY + layout.sidebarH - 20, TEXT_DIM, false);
    }

    private void drawTopBar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.topX, layout.topY, layout.topW, layout.topH, 15, GLASS);

        int searchX = layout.topX + 14;
        int searchY = layout.topY + 13;
        int searchW = Math.max(145, layout.topW - 128);
        int searchH = 32;
        float hover = isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH) ? 1.0F : 0.0F;
        int searchBorder = withAlpha(ACCENT, (int) (45 + 115 * searchFocusAnimation + 35 * hover));
        drawGlowRect(context, searchX, searchY, searchW, searchH, 10, withAlpha(ACCENT, (int) (16 + 30 * searchFocusAnimation)), 2);
        RenderUtils.drawRoundedRect(context, searchX, searchY, searchW, searchH, 10, 0xAA0F1220);
        RenderUtils.drawRoundedRect(context, searchX, searchY, searchW, 1, 1, searchBorder);
        context.drawText(textRenderer, "Search modules...", searchX + 14, searchY + 12, searchText.isEmpty() ? TEXT_DIM : 0x00000000, false);
        if (!searchText.isEmpty()) {
            context.drawText(textRenderer, trimToWidth(searchText, searchW - 28), searchX + 14, searchY + 12, TEXT_SOFT, false);
        }

        int buttonX = layout.topX + layout.topW - 92;
        drawTopDot(context, buttonX, layout.topY + 23, 0xFFFF5F76);
        drawTopDot(context, buttonX + 28, layout.topY + 23, 0xFFFFC75F);
        drawTopDot(context, buttonX + 56, layout.topY + 23, ACCENT);
    }

    private void drawTopDot(DrawContext context, int x, int y, int color) {
        drawGlowRect(context, x - 5, y - 5, 10, 10, 5, withAlpha(color, 45), 2);
        RenderUtils.drawRoundedRect(context, x - 4, y - 4, 8, 8, 4, color);
    }

    private void drawModulePanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH, 15, GLASS);
        context.drawText(textRenderer, selectedCategory.getName(), layout.moduleX + 16, layout.moduleY + 15, TEXT, false);

        List<Module> modules = getFilteredModules();
        String count = modules.size() + " modules";
        context.drawText(textRenderer, count, layout.moduleX + layout.moduleW - textRenderer.getWidth(count) - 16, layout.moduleY + 15, TEXT_DIM, false);

        int listX = layout.moduleX + 12;
        int listY = layout.moduleY + 42;
        int listW = layout.moduleW - 24;
        int listH = layout.moduleH - 54;
        context.enableScissor(listX - 4, listY, listX + listW + 4, listY + listH);

        double currentY = listY - moduleScroll;
        for (Module module : modules) {
            if (currentY + CARD_HEIGHT >= listY && currentY <= listY + listH) {
                drawModuleCard(context, module, listX, (int) Math.round(currentY), listW, mouseX, mouseY);
            } else {
                animate(moduleHoverAnimations, module, 0.0F, 0.18F);
                animate(moduleSelectAnimations, module, module == selectedModule ? 1.0F : 0.0F, 0.16F);
            }
            currentY += CARD_HEIGHT + GAP;
        }

        if (modules.isEmpty()) {
            context.drawText(textRenderer, "No modules found.", listX + 4, listY + 8, TEXT_MUTED, false);
        }

        context.disableScissor();
        drawModuleScrollBar(context, layout, modules.size());
    }

    private void drawModuleCard(DrawContext context, Module module, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, CARD_HEIGHT);
        float hover = animate(moduleHoverAnimations, module, hovered ? 1.0F : 0.0F, 0.16F);
        float selected = animate(moduleSelectAnimations, module, module == selectedModule ? 1.0F : 0.0F, 0.14F);
        float toggle = animate(moduleToggleAnimations, module, module.isEnabled() ? 1.0F : 0.0F, 0.16F);
        int lift = Math.round(2 * easeOutCubic(hover));
        int drawY = y - lift;
        int bg = lerpColor(CARD, CARD_HOVER, hover);

        if (selected > 0.01F || module.isEnabled()) {
            int glowAlpha = (int) ((32 + 34 * pulse()) * Math.max(selected, toggle * 0.7F));
            drawGlowRect(context, x, drawY, w, CARD_HEIGHT, 12, withAlpha(ACCENT, glowAlpha), 4);
        }

        RenderUtils.drawRoundedRect(context, x, drawY, w, CARD_HEIGHT, 12, bg);
        if (selected > 0.01F) {
            drawBorder(context, x, drawY, w, CARD_HEIGHT, 12, withAlpha(ACCENT, (int) (210 * selected)));
        } else if (hover > 0.01F) {
            drawBorder(context, x, drawY, w, CARD_HEIGHT, 12, withAlpha(ACCENT, (int) (75 * hover)));
        }

        context.drawText(textRenderer, module.getDisplayName(), x + 14, drawY + 11,
                lerpColor(TEXT_SOFT, TEXT, Math.max(hover, selected)), false);
        String description = module.getDescription() == null ? "" : module.getDescription();
        context.drawText(textRenderer, trimToWidth(description, w - 92), x + 14, drawY + 29, TEXT_MUTED, false);

        context.drawText(textRenderer, module == selectedModule ? "*" : "☆", x + w - 68, drawY + 12,
                module == selectedModule ? ACCENT : TEXT_DIM, false);
        drawToggle(context, x + w - 48, drawY + 21, toggle, false);
    }

    private void drawSettingsPanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.settingsX, layout.settingsY, layout.settingsW, layout.settingsH, 15, GLASS);
        Module module = selectedModule;

        if (module == null) {
            context.drawText(textRenderer, "Select a module", layout.settingsX + 16, layout.settingsY + 18, TEXT, false);
            context.drawText(textRenderer, "Settings will appear here.", layout.settingsX + 16, layout.settingsY + 36, TEXT_MUTED, false);
            return;
        }

        float selected = moduleSelectAnimations.getOrDefault(module, 1.0F);
        drawGlowRect(context, layout.settingsX + 16, layout.settingsY + 18, 38, 38, 13, withAlpha(ACCENT, (int) (42 + 35 * selected)), 3);
        RenderUtils.drawRoundedRect(context, layout.settingsX + 18, layout.settingsY + 20, 34, 34, 12, 0xFF241B3F);
        context.drawText(textRenderer, module.getDisplayName().substring(0, 1).toUpperCase(Locale.ROOT), layout.settingsX + 31, layout.settingsY + 32, ACCENT, false);

        context.drawText(textRenderer, module.getDisplayName(), layout.settingsX + 64, layout.settingsY + 18, TEXT, false);
        context.drawText(textRenderer, trimToWidth(module.getDescription(), layout.settingsW - 132), layout.settingsX + 64, layout.settingsY + 35, TEXT_MUTED, false);
        drawToggle(context, layout.settingsX + layout.settingsW - 54, layout.settingsY + 28,
                moduleToggleAnimations.getOrDefault(module, module.isEnabled() ? 1.0F : 0.0F), true);

        int listX = layout.settingsX + 12;
        int listY = layout.settingsY + 72;
        int listW = layout.settingsW - 24;
        int listH = layout.settingsH - 84;
        context.enableScissor(listX - 2, listY, listX + listW + 2, listY + listH);

        List<Setting> settings = module.getSettings();
        if (settings.isEmpty()) {
            context.drawText(textRenderer, "No settings available.", listX + 4, listY + 8, TEXT_MUTED, false);
        } else {
            double rowY = listY - settingsScroll;
            for (Setting setting : settings) {
                if (rowY + SETTING_HEIGHT >= listY && rowY <= listY + listH) {
                    drawSettingRow(context, module, setting, listX, (int) Math.round(rowY), listW, mouseX, mouseY);
                } else {
                    animate(settingHoverAnimations, setting, 0.0F, 0.16F);
                }
                rowY += SETTING_HEIGHT + 8;
            }
        }

        context.disableScissor();
    }

    private void drawSettingRow(DrawContext context, Module module, Setting setting, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, SETTING_HEIGHT);
        float hover = animate(settingHoverAnimations, setting, hovered ? 1.0F : 0.0F, 0.16F);
        RenderUtils.drawRoundedRect(context, x, y, w, SETTING_HEIGHT, 12, lerpColor(ROW, ROW_HOVER, hover));
        if (hover > 0.01F) {
            drawBorder(context, x, y, w, SETTING_HEIGHT, 12, withAlpha(ACCENT, (int) (55 * hover)));
        }

        context.drawText(textRenderer, setting.getName(), x + 12, y + 11, TEXT_SOFT, false);

        if (setting instanceof BooleanSetting booleanSetting) {
            float progress = animate(settingToggleAnimations, setting, booleanSetting.getValue() ? 1.0F : 0.0F, 0.17F);
            drawToggle(context, x + w - 50, y + 17, progress, false);
        } else if (setting instanceof ModeSetting modeSetting) {
            drawModeSetting(context, modeSetting, x, y, w);
        } else if (setting instanceof NumberSetting numberSetting) {
            drawNumberSetting(context, numberSetting, setting, x, y, w);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            drawKeybindSetting(context, keybindSetting, x, y, w);
        } else if (setting instanceof ColorSetting colorSetting) {
            drawColorSetting(context, colorSetting, x, y, w);
        } else if (setting instanceof RangeSetting rangeSetting) {
            drawRightText(context, format(rangeSetting.getMinValue()) + " - " + format(rangeSetting.getMaxValue()), x, y + 11, w - 12, TEXT_MUTED);
        } else if (setting instanceof StringSetting stringSetting) {
            drawRightText(context, trimToWidth(stringSetting.getValue(), 92), x, y + 11, w - 12, TEXT_MUTED);
        }
    }

    private void drawModeSetting(DrawContext context, ModeSetting setting, int x, int y, int w) {
        String mode = trimToWidth(setting.getMode(), 92);
        int boxW = Math.max(58, textRenderer.getWidth(mode) + 22);
        int boxX = x + w - boxW - 10;
        RenderUtils.drawRoundedRect(context, boxX, y + 12, boxW, 24, 8, 0xCC111522);
        drawBorder(context, boxX, y + 12, boxW, 24, 8, 0x558B5CFF);
        context.drawText(textRenderer, mode, boxX + 11, y + 20, TEXT_MUTED, false);
    }

    private void drawNumberSetting(DrawContext context, NumberSetting setting, Setting key, int x, int y, int w) {
        float target = (float) ((setting.getValue() - setting.getMin()) / Math.max(0.0001D, setting.getMax() - setting.getMin()));
        float progress = animate(sliderAnimations, key, clamp(target, 0.0F, 1.0F), 0.18F);
        int barW = Math.max(70, w - 118);
        int barX = x + 12;
        int barY = y + 34;
        context.fill(barX, barY, barX + barW, barY + 3, 0xFF30354A);
        context.fill(barX, barY, barX + Math.round(barW * easeOutCubic(progress)), barY + 3, ACCENT);
        RenderUtils.drawRoundedRect(context, barX + Math.round(barW * easeOutCubic(progress)) - 3, barY - 3, 8, 8, 4, ACCENT);
        RenderUtils.drawRoundedRect(context, x + w - 58, y + 13, 46, 22, 7, 0xCC111522);
        drawRightText(context, format(setting.getValue()), x, y + 20, w - 22, TEXT_MUTED);
    }

    private void drawKeybindSetting(DrawContext context, KeybindSetting setting, int x, int y, int w) {
        String key = setting == listeningKeybind ? "..." : KeyUtils.getKey(setting.getKeyCode());
        int boxW = Math.max(58, textRenderer.getWidth(key) + 20);
        int boxX = x + w - boxW - 10;
        RenderUtils.drawRoundedRect(context, boxX, y + 12, boxW, 24, 12, setting == listeningKeybind ? 0xDD352160 : 0xCC111522);
        drawBorder(context, boxX, y + 12, boxW, 24, 12, setting == listeningKeybind ? ACCENT : 0x558B5CFF);
        context.drawText(textRenderer, key, boxX + 10, y + 20, setting == listeningKeybind ? TEXT : TEXT_MUTED, false);
    }

    private void drawColorSetting(DrawContext context, ColorSetting setting, int x, int y, int w) {
        int swatchX = x + w - 42;
        RenderUtils.drawRoundedRect(context, swatchX, y + 13, 28, 22, 8, setting.getRGB());
        drawBorder(context, swatchX, y + 13, 28, 22, 8, 0x88FFFFFF);
    }

    private void drawToggle(DrawContext context, int x, int y, float progress, boolean large) {
        int toggleW = large ? 42 : 38;
        int toggleH = large ? 18 : 16;
        int knob = toggleH - 4;
        float eased = easeOutCubic(progress);
        int bg = lerpColor(0xFF303548, ACCENT, eased);
        if (progress > 0.01F) {
            drawGlowRect(context, x, y, toggleW, toggleH, toggleH / 2, withAlpha(ACCENT, (int) (28 * progress)), 2);
        }
        RenderUtils.drawRoundedRect(context, x, y, toggleW, toggleH, toggleH / 2, bg);
        int knobX = Math.round(lerp(x + 2, x + toggleW - knob - 2, eased));
        RenderUtils.drawRoundedRect(context, knobX, y + 2, knob, knob, knob / 2, lerpColor(RED, TEXT, eased));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = getLayout();

        if (listeningKeybind != null) {
            listeningKeybind.setKeyCode(-100 - button);
            stopListening();
            return true;
        }

        searchFocused = isHovered(mouseX, mouseY, layout.searchX, layout.searchY, layout.searchW, layout.searchH);
        if (searchFocused) {
            return true;
        }

        if (handleCategoryClick(mouseX, mouseY, layout)) {
            return true;
        }

        Module module = findModuleAt(mouseX, mouseY, layout);
        if (module != null) {
            selectedModule = module;
            settingsScroll = 0;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isModuleToggleHovered(mouseX, mouseY, module, layout)) {
                module.toggle();
            }
            return true;
        }

        Setting setting = findSettingAt(mouseX, mouseY, layout);
        if (setting != null && selectedModule != null) {
            handleSettingClick(setting, button);
            return true;
        }

        if (selectedModule != null && isHovered(mouseX, mouseY, layout.settingsX + layout.settingsW - 58, layout.settingsY + 24, 48, 28)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selectedModule.toggle();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCategoryClick(double mouseX, double mouseY, Layout layout) {
        int itemX = layout.sidebarX + 12;
        int itemY = layout.sidebarY + 70;
        int itemW = layout.sidebarW - 24;
        int itemH = 30;

        for (Category category : Category.values()) {
            if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
                if (selectedCategory != category) {
                    selectedCategory = category;
                    selectedModule = null;
                    moduleScroll = 0;
                    settingsScroll = 0;
                    stopListening();
                }
                return true;
            }
            itemY += itemH + 7;
        }
        return false;
    }

    private void handleSettingClick(Setting setting, int button) {
        if (setting instanceof BooleanSetting booleanSetting && isLeftOrRight(button)) {
            booleanSetting.toggle();
        } else if (setting instanceof ModeSetting modeSetting && isLeftOrRight(button)) {
            modeSetting.cycle();
        } else if (setting instanceof NumberSetting numberSetting && isLeftOrRight(button)) {
            double direction = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0D : -1.0D;
            numberSetting.setValue(numberSetting.getValue() + numberSetting.getIncrement() * direction);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                stopListening();
                listeningKeybind = keybindSetting;
                listeningKeybind.setListening(true);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                keybindSetting.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                keybindSetting.setListening(false);
                if (listeningKeybind == keybindSetting) {
                    listeningKeybind = null;
                }
            }
        } else if (setting instanceof ColorSetting colorSetting && isLeftOrRight(button)) {
            float[] hsb = colorSetting.getHSB();
            float direction = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 0.03F : -0.03F;
            colorSetting.setFromHSB(wrap(hsb[0] + direction), hsb[1], hsb[2]);
        }
        clampScrolls();
    }

    private Module findModuleAt(double mouseX, double mouseY, Layout layout) {
        int listX = layout.moduleX + 12;
        int listY = layout.moduleY + 42;
        int listW = layout.moduleW - 24;
        double currentY = listY - moduleScroll;

        for (Module module : getFilteredModules()) {
            if (isHovered(mouseX, mouseY, listX, currentY, listW, CARD_HEIGHT)) {
                return module;
            }
            currentY += CARD_HEIGHT + GAP;
        }
        return null;
    }

    private boolean isModuleToggleHovered(double mouseX, double mouseY, Module module, Layout layout) {
        int listX = layout.moduleX + 12;
        int listY = layout.moduleY + 42;
        int listW = layout.moduleW - 24;
        double currentY = listY - moduleScroll;

        for (Module current : getFilteredModules()) {
            if (current == module) {
                return isHovered(mouseX, mouseY, listX + listW - 54, currentY + 15, 50, 30);
            }
            currentY += CARD_HEIGHT + GAP;
        }
        return false;
    }

    private Setting findSettingAt(double mouseX, double mouseY, Layout layout) {
        if (selectedModule == null || !isHovered(mouseX, mouseY, layout.settingsX, layout.settingsY, layout.settingsW, layout.settingsH)) {
            return null;
        }

        int listX = layout.settingsX + 12;
        int listY = layout.settingsY + 72;
        int listW = layout.settingsW - 24;
        int listH = layout.settingsH - 84;
        if (!isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            return null;
        }

        double rowY = listY - settingsScroll;
        for (Setting setting : selectedModule.getSettings()) {
            if (isHovered(mouseX, mouseY, listX, rowY, listW, SETTING_HEIGHT)) {
                return setting;
            }
            rowY += SETTING_HEIGHT + 8;
        }
        return null;
    }

    private void drawModuleScrollBar(DrawContext context, Layout layout, int moduleCount) {
        int listH = layout.moduleH - 54;
        int contentH = Math.max(0, moduleCount * (CARD_HEIGHT + GAP) - GAP);
        int max = Math.max(0, contentH - listH);
        if (max <= 0) return;

        int trackX = layout.moduleX + layout.moduleW - 8;
        int trackY = layout.moduleY + 42;
        int thumbH = Math.max(24, (int) (listH * (listH / (double) contentH)));
        int thumbY = trackY + (int) ((listH - thumbH) * (moduleScroll / max));
        RenderUtils.drawRoundedRect(context, trackX, trackY, 3, listH, 2, 0x5530354A);
        RenderUtils.drawRoundedRect(context, trackX, thumbY, 3, thumbH, 2, ACCENT);
    }

    private List<Module> getFilteredModules() {
        List<Module> modules = MotionBlurrClient.INSTANCE.getModuleManager().getModulesByCategory(selectedCategory);
        if (searchText.isBlank()) {
            return modules;
        }

        String query = searchText.toLowerCase(Locale.ROOT);
        List<Module> filtered = new ArrayList<>();
        for (Module module : modules) {
            String name = module.getName() == null ? "" : module.getName().toLowerCase(Locale.ROOT);
            String display = module.getDisplayName() == null ? "" : module.getDisplayName().toLowerCase(Locale.ROOT);
            String description = module.getDescription() == null ? "" : module.getDescription().toLowerCase(Locale.ROOT);
            if (name.contains(query) || display.contains(query) || description.contains(query)) {
                filtered.add(module);
            }
        }
        return filtered;
    }

    private void ensureSelectedModule() {
        List<Module> visible = getFilteredModules();
        if (selectedModule != null && visible.contains(selectedModule)) {
            return;
        }
        selectedModule = visible.isEmpty() ? null : visible.get(0);
        settingsScroll = 0;
    }

    private void clampScrolls() {
        Layout layout = getLayout();
        int moduleContent = Math.max(0, getFilteredModules().size() * (CARD_HEIGHT + GAP) - GAP);
        moduleScroll = clamp(moduleScroll, 0.0D, Math.max(0, moduleContent - (layout.moduleH - 54)));

        int settingContent = selectedModule == null ? 0 : Math.max(0, selectedModule.getSettings().size() * (SETTING_HEIGHT + 8) - 8);
        settingsScroll = clamp(settingsScroll, 0.0D, Math.max(0, settingContent - (layout.settingsH - 84)));
    }

    private Layout getLayout() {
        int guiW = Math.min(960, Math.max(320, width - GUI_MARGIN));
        int guiH = Math.min(560, Math.max(260, height - GUI_MARGIN));
        int x = (width - guiW) / 2;
        int y = (height - guiH) / 2;

        int sidebarW = guiW < 660 ? 124 : Math.min(SIDEBAR_WIDTH, Math.max(132, guiW / 5));
        int gap = 10;
        int contentX = x + sidebarW + gap;
        int contentW = guiW - sidebarW - gap - 12;
        int topX = contentX;
        int topY = y + 12;
        int topW = contentW;
        int topH = TOPBAR_HEIGHT;

        int bodyY = topY + topH + gap;
        int bodyH = guiH - TOPBAR_HEIGHT - gap - 24;
        int settingsW = Math.max(170, Math.min(286, (int) (contentW * 0.38F)));
        int moduleW = contentW - settingsW - gap;
        if (moduleW < 180) {
            settingsW = Math.max(150, contentW - gap - 180);
            moduleW = Math.max(130, contentW - settingsW - gap);
        }
        int settingsX = contentX + moduleW + gap;

        int searchX = topX + 14;
        int searchY = topY + 13;
        int searchW = Math.max(145, topW - 128);
        int searchH = 32;

        return new Layout(x, y, guiW, guiH, x + 8, y + 12, sidebarW - 4, guiH - 24,
                topX, topY, topW, topH, contentX, bodyY, moduleW, bodyH,
                settingsX, bodyY, settingsW, bodyH, searchX, searchY, searchW, searchH);
    }

    private void stopListening() {
        if (listeningKeybind != null) {
            listeningKeybind.setListening(false);
            listeningKeybind = null;
        }
    }

    private void drawGlowRect(DrawContext context, int x, int y, int w, int h, int radius, int color, int layers) {
        int alpha = (color >>> 24) & 255;
        for (int i = layers; i >= 1; i--) {
            int layerAlpha = (int) (alpha * (i / (float) layers) * 0.22F);
            RenderUtils.drawRoundedRect(context, x - i, y - i, w + i * 2, h + i * 2, radius + i, withAlpha(color, layerAlpha));
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        RenderUtils.drawRoundedRect(context, x, y, w, 1, 1, color);
        RenderUtils.drawRoundedRect(context, x, y + h - 1, w, 1, 1, color);
        RenderUtils.drawRoundedRect(context, x, y, 1, h, 1, color);
        RenderUtils.drawRoundedRect(context, x + w - 1, y, 1, h, 1, color);
    }

    private void drawRightText(DrawContext context, String text, int x, int y, int right, int color) {
        context.drawText(textRenderer, text, x + right - textRenderer.getWidth(text), y, color, false);
    }

    private boolean isLeftOrRight(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) return "";
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - textRenderer.getWidth(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && textRenderer.getWidth(trimmed) > limit) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private float animate(float current, float target, float speed) {
        float next = current + (target - current) * clamp(speed, 0.0F, 1.0F);
        return Math.abs(next - target) < 0.003F ? target : next;
    }

    private <T> float animate(Map<T, Float> map, T key, float target, float speed) {
        float current = map.getOrDefault(key, target);
        float next = animate(current, target, speed);
        map.put(key, next);
        return next;
    }

    private float easeOutCubic(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return 1.0F - (float) Math.pow(1.0F - value, 3.0D);
    }

    private float easeOutExpo(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0F * value);
    }

    private float pulse() {
        return 0.5F + 0.5F * (float) Math.sin(pulseAnimation);
    }

    private float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp(progress, 0.0F, 1.0F);
    }

    private int lerpColor(int from, int to, float progress) {
        progress = clamp(progress, 0.0F, 1.0F);
        int a = (int) (((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * progress);
        int r = (int) (((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * progress);
        int g = (int) (((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * progress);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * progress);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int withAlpha(int color, int alpha) {
        return (clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private float wrap(float value) {
        while (value < 0.0F) value += 1.0F;
        while (value > 1.0F) value -= 1.0F;
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Layout(int x, int y, int w, int h,
                          int sidebarX, int sidebarY, int sidebarW, int sidebarH,
                          int topX, int topY, int topW, int topH,
                          int moduleX, int moduleY, int moduleW, int moduleH,
                          int settingsX, int settingsY, int settingsW, int settingsH,
                          int searchX, int searchY, int searchW, int searchH) {
    }
}
