package cc.motionblurr.gui;

import cc.motionblurr.MotionBlurrClient;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ClickGui extends Screen {

    private Category selectedCategory = Category.COMBAT;

    private int scrollOffset = 0;

    private static final int BG = 0xEE101116;
    private static final int PANEL = 0xFF171922;
    private static final int PANEL_DARK = 0xFF111217;
    private static final int CARD = 0xFF1C1F2A;
    private static final int CARD_HOVER = 0xFF242838;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFF9AA0B2;
    private static final int GREEN = 0xFF5CFF9B;
    private static final int RED = 0xFFFF5C7A;

    public ClickGui() {
        super(Text.literal("MotionBlurr"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollOffset++;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int guiWidth = Math.min(760, width - 24);
        int guiHeight = Math.min(430, height - 24);

        int x = (width - guiWidth) / 2;
        int y = (height - guiHeight) / 2;

        int sidebarWidth = 150;
        int headerHeight = 46;

        drawBackground(context);
        drawMainWindow(context, x, y, guiWidth, guiHeight);
        drawHeader(context, x, y, guiWidth, headerHeight);
        drawSidebar(context, x, y + headerHeight, sidebarWidth, guiHeight - headerHeight, mouseX, mouseY);
        drawModules(context, x + sidebarWidth, y + headerHeight, guiWidth - sidebarWidth, guiHeight - headerHeight, mouseX, mouseY);
    }

    private void drawBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0x88000000);
    }

    private void drawMainWindow(DrawContext context, int x, int y, int w, int h) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, 14, BG);
        RenderUtils.drawRoundedRect(context, x + 4, y + 4, w - 8, h - 8, 12, 0x66171922);
    }

    private void drawHeader(DrawContext context, int x, int y, int w, int h) {
        RenderUtils.drawRoundedRect(context, x + 8, y + 8, w - 16, h - 10, 10, PANEL_DARK);

        context.drawText(
                textRenderer,
                "MotionBlurr",
                x + 22,
                y + 21,
                TEXT,
                false
        );

        context.drawText(
                textRenderer,
                "ClickGUI rewrite v1",
                x + 105,
                y + 22,
                TEXT_MUTED,
                false
        );

        String closeText = "ESC to close";
        context.drawText(
                textRenderer,
                closeText,
                x + w - textRenderer.getWidth(closeText) - 22,
                y + 22,
                TEXT_MUTED,
                false
        );
    }

    private void drawSidebar(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, x + 8, y + 6, w - 14, h - 14, 12, PANEL_DARK);

        int itemX = x + 16;
        int itemY = y + 18;
        int itemW = w - 30;
        int itemH = 25;

        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hovered = isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH);

            int color = selected ? ACCENT : hovered ? CARD_HOVER : 0x00000000;

            if (selected || hovered) {
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 7, color);
            }

            context.drawText(
                    textRenderer,
                    category.getName(),
                    itemX + 10,
                    itemY + 8,
                    selected ? TEXT : TEXT_MUTED,
                    false
            );

            itemY += itemH + 6;
        }
    }

    private void drawModules(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, x + 4, y + 6, w - 12, h - 14, 12, PANEL);

        context.drawText(
                textRenderer,
                selectedCategory.getName(),
                x + 22,
                y + 20,
                TEXT,
                false
        );

        List<Module> modules = MotionBlurrClient.INSTANCE
                .getModuleManager()
                .getModulesByCategory(selectedCategory);

        int startX = x + 18;
        int startY = y + 46;
        int cardW = w - 42;
        int cardH = 42;
        int gap = 8;

        int maxVisible = Math.max(1, (h - 60) / (cardH + gap));
        int maxScroll = Math.max(0, modules.size() - maxVisible);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        int visibleIndex = 0;

        for (int i = scrollOffset; i < modules.size(); i++) {
            if (visibleIndex >= maxVisible) break;

            Module module = modules.get(i);

            int cardY = startY + visibleIndex * (cardH + gap);
            drawModuleCard(context, module, startX, cardY, cardW, cardH, mouseX, mouseY);

            visibleIndex++;
        }

        if (modules.isEmpty()) {
            context.drawText(
                    textRenderer,
                    "No modules in this category.",
                    startX,
                    startY,
                    TEXT_MUTED,
                    false
            );
        }

        if (modules.size() > maxVisible) {
            String scrollText = "Use UP / DOWN arrows to scroll";
            context.drawText(
                    textRenderer,
                    scrollText,
                    x + w - textRenderer.getWidth(scrollText) - 24,
                    y + h - 28,
                    TEXT_MUTED,
                    false
            );
        }
    }

    private void drawModuleCard(DrawContext context, Module module, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, h);
        boolean enabled = module.isEnabled();

        int bg = hovered ? CARD_HOVER : CARD;

        RenderUtils.drawRoundedRect(context, x, y, w, h, 9, bg);

        if (enabled) {
            RenderUtils.drawRoundedRect(context, x, y, 4, h, 3, ACCENT);
        }

        String name = module.getDisplayName();
        String description = module.getDescription();

        context.drawText(
                textRenderer,
                name,
                x + 14,
                y + 9,
                enabled ? TEXT : 0xFFE6E6E6,
                false
        );

        if (description != null && !description.isEmpty()) {
            context.drawText(
                    textRenderer,
                    shorten(description, 58),
                    x + 14,
                    y + 25,
                    TEXT_MUTED,
                    false
            );
        }

        int toggleW = 38;
        int toggleH = 16;
        int toggleX = x + w - toggleW - 14;
        int toggleY = y + 13;

        RenderUtils.drawRoundedRect(
                context,
                toggleX,
                toggleY,
                toggleW,
                toggleH,
                8,
                enabled ? 0xAA8B5CF6 : 0xFF303442
        );

        int knobSize = 12;
        int knobX = enabled ? toggleX + toggleW - knobSize - 2 : toggleX + 2;
        int knobY = toggleY + 2;

        RenderUtils.drawRoundedRect(
                context,
                knobX,
                knobY,
                knobSize,
                knobSize,
                6,
                enabled ? GREEN : RED
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int guiWidth = Math.min(760, width - 24);
        int guiHeight = Math.min(430, height - 24);

        int x = (width - guiWidth) / 2;
        int y = (height - guiHeight) / 2;

        int sidebarWidth = 150;
        int headerHeight = 46;

        int itemX = x + 16;
        int itemY = y + headerHeight + 18;
        int itemW = sidebarWidth - 30;
        int itemH = 25;

        for (Category category : Category.values()) {
            if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
                selectedCategory = category;
                scrollOffset = 0;
                return true;
            }

            itemY += itemH + 6;
        }

        List<Module> modules = MotionBlurrClient.INSTANCE
                .getModuleManager()
                .getModulesByCategory(selectedCategory);

        int modulesX = x + sidebarWidth;
        int modulesY = y + headerHeight;
        int modulesW = guiWidth - sidebarWidth;
        int modulesH = guiHeight - headerHeight;

        int startX = modulesX + 18;
        int startY = modulesY + 46;
        int cardW = modulesW - 42;
        int cardH = 42;
        int gap = 8;

        int maxVisible = Math.max(1, (modulesH - 60) / (cardH + gap));
        int maxScroll = Math.max(0, modules.size() - maxVisible);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        int visibleIndex = 0;

        for (int i = scrollOffset; i < modules.size(); i++) {
            if (visibleIndex >= maxVisible) break;

            Module module = modules.get(i);

            int cardY = startY + visibleIndex * (cardH + gap);

            if (isHovered(mouseX, mouseY, startX, cardY, cardW, cardH)) {
                module.toggle();
                return true;
            }

            visibleIndex++;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}