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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClickGui extends Screen {

    private static final int BG = 0xEE101116;
    private static final int PANEL = 0xFF171922;
    private static final int PANEL_DARK = 0xFF111217;
    private static final int CARD = 0xFF1C1F2A;
    private static final int CARD_HOVER = 0xFF242838;
    private static final int ROW = 0xFF202331;
    private static final int ROW_HOVER = 0xFF292D3D;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_SOFT = 0xFFE7E8EE;
    private static final int TEXT_MUTED = 0xFF9AA0B2;
    private static final int GREEN = 0xFF5CFF9B;
    private static final int RED = 0xFFFF5C7A;

    private static final int GUI_MARGIN = 24;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int HEADER_HEIGHT = 46;
    private static final int CARD_HEIGHT = 44;
    private static final int SETTING_HEIGHT = 28;
    private static final int GAP = 8;

    private final Map<Module, Float> hoverAnimations = new HashMap<>();
    private final Map<Module, Float> toggleAnimations = new HashMap<>();
    private final Map<Module, Float> expandAnimations = new HashMap<>();
    private final Map<Category, Float> categoryAnimations = new EnumMap<>(Category.class);
    private final Set<Module> expandedModules = new HashSet<>();

    private Category selectedCategory = Category.COMBAT;
    private KeybindSetting listeningKeybind;
    private double scroll;

    public ClickGui() {
        super(Text.literal("MotionBlurr"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        listeningKeybind = null;
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
            listeningKeybind.setListening(false);
            listeningKeybind = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            scroll -= 30;
            clampScroll();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scroll += 30;
            clampScroll();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = getLayout();
        if (isHovered(mouseX, mouseY, layout.modulesX, layout.contentY, layout.modulesW, layout.contentH)) {
            scroll -= verticalAmount * 34.0D;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Layout layout = getLayout();

        drawBackground(context);
        drawMainWindow(context, layout.guiX, layout.guiY, layout.guiW, layout.guiH);
        drawHeader(context, layout);
        drawSidebar(context, layout, mouseX, mouseY);
        drawModules(context, layout, mouseX, mouseY);
    }

    private void drawBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0x88000000);
    }

    private void drawMainWindow(DrawContext context, int x, int y, int w, int h) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, 14, BG);
        RenderUtils.drawRoundedRect(context, x + 4, y + 4, w - 8, h - 8, 12, 0x66171922);
    }

    private void drawHeader(DrawContext context, Layout layout) {
        RenderUtils.drawRoundedRect(context, layout.guiX + 8, layout.guiY + 8, layout.guiW - 16, HEADER_HEIGHT - 10, 10, PANEL_DARK);
        context.drawText(textRenderer, "MotionBlurr", layout.guiX + 22, layout.guiY + 21, TEXT, false);

        String keyHint = listeningKeybind == null ? "ESC" : "Listening...";
        context.drawText(textRenderer, keyHint, layout.guiX + layout.guiW - textRenderer.getWidth(keyHint) - 22, layout.guiY + 22, TEXT_MUTED, false);
    }

    private void drawSidebar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.guiX + 8, layout.contentY + 6, SIDEBAR_WIDTH - 14, layout.contentH - 14, 12, PANEL_DARK);

        int itemX = layout.guiX + 16;
        int itemY = layout.contentY + 18;
        int itemW = SIDEBAR_WIDTH - 30;
        int itemH = 25;

        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hovered = isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH);
            float progress = animate(categoryAnimations, category, selected || hovered ? 1.0F : 0.0F, 0.22F);

            if (progress > 0.01F) {
                int color = blend(CARD_HOVER, ACCENT, selected ? 0.88F : 0.0F);
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 7, withAlpha(color, (int) (255 * progress)));
            }

            context.drawText(textRenderer, category.getName(), itemX + 10, itemY + 8, selected ? TEXT : TEXT_MUTED, false);
            itemY += itemH + 6;
        }
    }

    private void drawModules(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.modulesX, layout.contentY + 6, layout.modulesW, layout.contentH - 14, 12, PANEL);
        context.drawText(textRenderer, selectedCategory.getName(), layout.modulesX + 18, layout.contentY + 20, TEXT, false);

        int clipX = layout.modulesX + 10;
        int clipY = layout.listY;
        int clipW = layout.modulesW - 20;
        int clipH = layout.listH;
        context.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);

        List<Module> modules = getModules();
        int cardX = layout.modulesX + 14;
        int cardW = layout.modulesW - 28;
        double currentY = layout.listY - scroll;

        for (Module module : modules) {
            float expanded = animate(expandAnimations, module, expandedModules.contains(module) ? 1.0F : 0.0F, 0.2F);
            int settingsHeight = getSettingsHeight(module);
            int totalHeight = CARD_HEIGHT + Math.round(settingsHeight * ease(expanded));

            if (currentY + totalHeight >= layout.listY && currentY <= layout.listY + layout.listH) {
                drawModuleCard(context, module, cardX, (int) Math.round(currentY), cardW, totalHeight, settingsHeight, expanded, mouseX, mouseY);
            } else {
                updateModuleAnimations(module, false);
            }

            currentY += totalHeight + GAP;
        }

        if (modules.isEmpty()) {
            context.drawText(textRenderer, "No modules in this category.", cardX, layout.listY + 8, TEXT_MUTED, false);
        }

        context.disableScissor();
        drawScrollBar(context, layout);
    }

    private void drawModuleCard(DrawContext context, Module module, int x, int y, int w, int h, int settingsHeight, float expanded, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, CARD_HEIGHT);
        float hover = updateModuleAnimations(module, hovered);
        float toggle = animate(toggleAnimations, module, module.isEnabled() ? 1.0F : 0.0F, 0.18F);
        int bg = blend(CARD, CARD_HOVER, hover);

        RenderUtils.drawRoundedRect(context, x, y, w, h, 9, bg);

        if (module.isEnabled() || toggle > 0.01F) {
            RenderUtils.drawRoundedRect(context, x, y, 4, Math.min(h, CARD_HEIGHT), 3, withAlpha(ACCENT, (int) (80 + 175 * toggle)));
        }

        context.drawText(textRenderer, module.getDisplayName(), x + 14, y + 9, module.isEnabled() ? TEXT : TEXT_SOFT, false);

        String description = module.getDescription();
        if (description != null && !description.isEmpty()) {
            context.drawText(textRenderer, trimToWidth(description, w - 92), x + 14, y + 25, TEXT_MUTED, false);
        }

        drawToggle(context, x + w - 52, y + 14, toggle);

        if (expanded > 0.01F && settingsHeight > 0) {
            int settingAlpha = (int) (255 * ease(expanded));
            int settingsClipTop = y + CARD_HEIGHT;
            int settingsClipBottom = settingsClipTop + Math.round(settingsHeight * ease(expanded));
            context.enableScissor(x, settingsClipTop, x + w, settingsClipBottom);
            drawSettings(context, module, x + 10, y + CARD_HEIGHT, w - 20, settingAlpha, mouseX, mouseY);
            context.disableScissor();
        }
    }

    private float updateModuleAnimations(Module module, boolean hovered) {
        return animate(hoverAnimations, module, hovered ? 1.0F : 0.0F, 0.22F);
    }

    private void drawToggle(DrawContext context, int x, int y, float progress) {
        RenderUtils.drawRoundedRect(context, x, y, 38, 16, 8, blend(0xFF303442, ACCENT, progress * 0.85F));
        int knobX = Math.round(lerp(x + 2, x + 24, ease(progress)));
        RenderUtils.drawRoundedRect(context, knobX, y + 2, 12, 12, 6, blend(RED, GREEN, progress));
    }

    private void drawSettings(DrawContext context, Module module, int x, int y, int w, int alpha, int mouseX, int mouseY) {
        int rowY = y;
        for (Setting setting : module.getSettings()) {
            boolean hovered = isHovered(mouseX, mouseY, x, rowY + 2, w, SETTING_HEIGHT - 4);
            RenderUtils.drawRoundedRect(context, x, rowY + 2, w, SETTING_HEIGHT - 4, 7, withAlpha(hovered ? ROW_HOVER : ROW, alpha));

            int textColor = withAlpha(TEXT_SOFT, alpha);
            int mutedColor = withAlpha(TEXT_MUTED, alpha);
            context.drawText(textRenderer, setting.getName(), x + 9, rowY + 10, textColor, false);

            if (setting instanceof BooleanSetting booleanSetting) {
                drawToggle(context, x + w - 47, rowY + 7, booleanSetting.getValue() ? 1.0F : 0.0F);
            } else if (setting instanceof ModeSetting modeSetting) {
                drawRightText(context, trimToWidth(modeSetting.getMode(), 110), x, rowY + 10, w - 9, mutedColor);
            } else if (setting instanceof NumberSetting numberSetting) {
                drawNumberSetting(context, numberSetting, x, rowY, w, alpha);
            } else if (setting instanceof KeybindSetting keybindSetting) {
                String keyText = keybindSetting == listeningKeybind ? "..." : KeyUtils.getKey(keybindSetting.getKeyCode());
                drawRightText(context, keyText, x, rowY + 10, w - 9, keybindSetting == listeningKeybind ? withAlpha(ACCENT, alpha) : mutedColor);
            } else if (setting instanceof ColorSetting colorSetting) {
                drawColorSetting(context, colorSetting, x, rowY, w, alpha);
            } else if (setting instanceof RangeSetting rangeSetting) {
                drawRightText(context, format(rangeSetting.getMinValue()) + " - " + format(rangeSetting.getMaxValue()), x, rowY + 10, w - 9, mutedColor);
            } else if (setting instanceof StringSetting stringSetting) {
                drawRightText(context, trimToWidth(stringSetting.getValue(), 120), x, rowY + 10, w - 9, mutedColor);
            }

            rowY += SETTING_HEIGHT;
        }
    }

    private void drawNumberSetting(DrawContext context, NumberSetting setting, int x, int y, int w, int alpha) {
        int barW = 88;
        int barX = x + w - barW - 9;
        int barY = y + 19;
        float progress = (float) ((setting.getValue() - setting.getMin()) / Math.max(0.0001D, setting.getMax() - setting.getMin()));
        context.fill(barX, barY, barX + barW, barY + 2, withAlpha(0xFF3A3E4F, alpha));
        context.fill(barX, barY, barX + Math.round(barW * clamp(progress, 0.0F, 1.0F)), barY + 2, withAlpha(ACCENT, alpha));
        drawRightText(context, format(setting.getValue()), x, y + 8, w - 9, withAlpha(TEXT_MUTED, alpha));
    }

    private void drawColorSetting(DrawContext context, ColorSetting setting, int x, int y, int w, int alpha) {
        int swatch = withAlpha(setting.getRGB(), alpha);
        RenderUtils.drawRoundedRect(context, x + w - 31, y + 7, 22, 14, 5, swatch);
        RenderUtils.drawRoundedRect(context, x + w - 32, y + 6, 24, 16, 5, withAlpha(0x44FFFFFF, alpha));
    }

    private void drawRightText(DrawContext context, String text, int x, int y, int right, int color) {
        context.drawText(textRenderer, text, x + right - textRenderer.getWidth(text), y, color, false);
    }

    private void drawScrollBar(DrawContext context, Layout layout) {
        int contentHeight = getContentHeight();
        int maxScroll = Math.max(0, contentHeight - layout.listH);
        if (maxScroll <= 0) return;

        int trackX = layout.modulesX + layout.modulesW - 10;
        int trackY = layout.listY;
        int trackH = layout.listH;
        int thumbH = Math.max(24, (int) (trackH * (trackH / (double) contentHeight)));
        int thumbY = trackY + (int) ((trackH - thumbH) * (scroll / maxScroll));

        RenderUtils.drawRoundedRect(context, trackX, trackY, 3, trackH, 2, 0x66303442);
        RenderUtils.drawRoundedRect(context, trackX, thumbY, 3, thumbH, 2, ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = getLayout();

        if (handleCategoryClick(mouseX, mouseY, layout)) {
            return true;
        }

        if (isHovered(mouseX, mouseY, layout.modulesX, layout.contentY, layout.modulesW, layout.contentH)) {
            ModuleHit hit = findModuleAt(mouseX, mouseY, layout);
            if (hit != null) {
                if (hit.setting != null) {
                    handleSettingClick(hit.module, hit.setting, button);
                    return true;
                }

                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    hit.module.toggle();
                    return true;
                }

                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !hit.module.getSettings().isEmpty()) {
                    toggleExpanded(hit.module);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCategoryClick(double mouseX, double mouseY, Layout layout) {
        int itemX = layout.guiX + 16;
        int itemY = layout.contentY + 18;
        int itemW = SIDEBAR_WIDTH - 30;
        int itemH = 25;

        for (Category category : Category.values()) {
            if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
                if (selectedCategory != category) {
                    selectedCategory = category;
                    scroll = 0;
                    listeningKeybind = null;
                }
                return true;
            }
            itemY += itemH + 6;
        }
        return false;
    }

    private void handleSettingClick(Module module, Setting setting, int button) {
        if (setting instanceof BooleanSetting booleanSetting && isLeftOrRight(button)) {
            booleanSetting.toggle();
        } else if (setting instanceof ModeSetting modeSetting && isLeftOrRight(button)) {
            modeSetting.cycle();
        } else if (setting instanceof NumberSetting numberSetting && isLeftOrRight(button)) {
            double direction = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0D : -1.0D;
            numberSetting.setValue(numberSetting.getValue() + numberSetting.getIncrement() * direction);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (listeningKeybind != null) {
                    listeningKeybind.setListening(false);
                }
                listeningKeybind = keybindSetting;
                keybindSetting.setListening(true);
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
        clampScroll();
    }

    private ModuleHit findModuleAt(double mouseX, double mouseY, Layout layout) {
        List<Module> modules = getModules();
        int cardX = layout.modulesX + 14;
        int cardW = layout.modulesW - 28;
        double currentY = layout.listY - scroll;

        for (Module module : modules) {
            float expanded = expandAnimations.getOrDefault(module, expandedModules.contains(module) ? 1.0F : 0.0F);
            int settingsHeight = getSettingsHeight(module);
            int totalHeight = CARD_HEIGHT + Math.round(settingsHeight * ease(expanded));

            if (isHovered(mouseX, mouseY, cardX, currentY, cardW, CARD_HEIGHT)) {
                return new ModuleHit(module, null);
            }

            if (expanded > 0.5F && isHovered(mouseX, mouseY, cardX + 10, currentY + CARD_HEIGHT, cardW - 20, totalHeight - CARD_HEIGHT)) {
                int settingIndex = (int) ((mouseY - currentY - CARD_HEIGHT) / SETTING_HEIGHT);
                if (settingIndex >= 0 && settingIndex < module.getSettings().size()) {
                    return new ModuleHit(module, module.getSettings().get(settingIndex));
                }
            }

            currentY += totalHeight + GAP;
        }
        return null;
    }

    private void toggleExpanded(Module module) {
        if (expandedModules.contains(module)) {
            expandedModules.remove(module);
        } else {
            expandedModules.add(module);
        }
        clampScroll();
    }

    private List<Module> getModules() {
        return MotionBlurrClient.INSTANCE.getModuleManager().getModulesByCategory(selectedCategory);
    }

    private int getContentHeight() {
        int height = 0;
        for (Module module : getModules()) {
            float expanded = expandAnimations.getOrDefault(module, expandedModules.contains(module) ? 1.0F : 0.0F);
            height += CARD_HEIGHT + Math.round(getSettingsHeight(module) * ease(expanded)) + GAP;
        }
        return Math.max(0, height - GAP);
    }

    private int getSettingsHeight(Module module) {
        return module.getSettings().size() * SETTING_HEIGHT + 6;
    }

    private void clampScroll() {
        if (client == null) return;
        Layout layout = getLayout();
        scroll = clamp(scroll, 0.0D, Math.max(0, getContentHeight() - layout.listH));
    }

    private Layout getLayout() {
        int guiW = Math.max(320, Math.min(760, width - GUI_MARGIN));
        int guiH = Math.max(260, Math.min(430, height - GUI_MARGIN));
        int guiX = (width - guiW) / 2;
        int guiY = (height - guiH) / 2;
        int contentY = guiY + HEADER_HEIGHT;
        int contentH = guiH - HEADER_HEIGHT;
        int modulesX = guiX + SIDEBAR_WIDTH;
        int modulesW = guiW - SIDEBAR_WIDTH - 8;
        int listY = contentY + 43;
        int listH = Math.max(40, contentH - 62);
        return new Layout(guiX, guiY, guiW, guiH, contentY, contentH, modulesX, modulesW, listY, listH);
    }

    private <T> float animate(Map<T, Float> map, T key, float target, float speed) {
        float current = map.getOrDefault(key, target);
        current = lerp(current, target, speed);
        if (Math.abs(current - target) < 0.01F) {
            current = target;
        }
        map.put(key, current);
        return current;
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
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private float ease(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private float lerp(float from, float to, float speed) {
        return from + (to - from) * clamp(speed, 0.0F, 1.0F);
    }

    private int blend(int from, int to, float progress) {
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

    private record Layout(int guiX, int guiY, int guiW, int guiH, int contentY, int contentH, int modulesX, int modulesW, int listY, int listH) {
    }

    private record ModuleHit(Module module, Setting setting) {
    }
}
