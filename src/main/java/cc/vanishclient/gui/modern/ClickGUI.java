package cc.vanishclient.gui.modern;

import cc.vanishclient.VanishClient;
import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ClickGUI extends Screen {
    private final Sidebar sidebar = new Sidebar();
    private final SearchBar searchBar = new SearchBar();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final AnimationManager animationManager = new AnimationManager();
    private final FontManager fonts = new FontManager();
    private final List<ModuleCard> cards = new ArrayList<>();
    private boolean closing;
    private long lastFrame = System.currentTimeMillis();
    private String selectedCategory = "Combat";
    private Module selectedModule;

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        for (Module module : VanishClient.INSTANCE.getModuleManager().getModules()) {
            cards.add(new ModuleCard(module));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float frameDelta = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        if (closing) {
            animationManager.update(frameDelta, true);
            if (animationManager.getOpenProgress() <= 0f) {
                if (client != null) {
                    client.setScreen(null);
                }
                return;
            }
        } else {
            animationManager.update(frameDelta, false);
        }

        float scale = 0.95f + animationManager.getScaledOpenProgress() * 0.05f;
        float alpha = animationManager.getScaledOpenProgress();

        int rootX = (width - 1400) / 2;
        int rootY = (height - 850) / 2;

        context.getMatrices().push();
        context.getMatrices().translate(width / 2f, height / 2f, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-width / 2f, -height / 2f, 0f);

        drawBackdrop(context, width, height, alpha);
        Renderer.drawRoundedRect(context, rootX, rootY, 1400, 850, 24, ColorPalette.withAlpha(ColorPalette.BACKGROUND, (int) (220 * alpha)));
        context.drawBorder(rootX, rootY, 1400, 850, ColorPalette.withAlpha(ColorPalette.BORDER, (int) (80 * alpha)).getRGB());

        int sidebarX = rootX + 24;
        int sidebarY = rootY + 24;
        int sidebarWidth = 260;
        int sidebarHeight = 802;
        sidebar.render(context, sidebarX, sidebarY, sidebarWidth, sidebarHeight, mouseX, mouseY, alpha, fonts.title(), fonts.small());

        int topBarY = rootY + 24;
        int contentX = rootX + 304;
        int contentY = rootY + 24;
        int contentWidth = 1072;
        int contentHeight = 802;

        renderTopBar(context, contentX, topBarY, contentWidth, 70, mouseX, mouseY, alpha);
        renderModules(context, contentX + 16, contentY + 96, contentWidth - 332, contentHeight - 112, mouseX, mouseY, alpha);
        settingsPanel.render(context, contentX + contentWidth - 316, contentY + 96, 300, contentHeight - 112, mouseX, mouseY, alpha, fonts.title(), fonts.body(), fonts.small());

        context.getMatrices().pop();
    }

    private void drawBackdrop(DrawContext context, int width, int height, float alpha) {
        int overlayAlpha = (int) (40 * alpha);
        context.fill(0, 0, width, height, new Color(7, 8, 13, overlayAlpha).getRGB());
        context.fillGradient(0, 0, width, height, new Color(7, 8, 13, 10).getRGB(), new Color(10, 12, 18, 60).getRGB());
    }

    private void renderTopBar(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha) {
        Renderer.drawRoundedRect(context, x, y, width, height, 20, ColorPalette.withAlpha(ColorPalette.PANEL, (int) (200 * alpha)));
        Renderer.drawText(context, fonts.body(), "Modules", x + 20, y + 24, ColorPalette.TEXT);
        searchBar.render(context, x + width - 320, y + 14, 260, 40, mouseX, mouseY, alpha, fonts.body());
        Renderer.drawText(context, fonts.small(), "GUI", x + width - 70, y + 24, ColorPalette.SECONDARY);
        Renderer.drawText(context, fonts.small(), "HUD", x + width - 120, y + 24, ColorPalette.SECONDARY);
    }

    private void renderModules(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha) {
        int cardWidth = width - 24;
        int cardHeight = 78;
        int currentY = y;
        Category selected = sidebar.getSelectedCategory();
        List<Module> visibleModules = new ArrayList<>();
        for (Module module : VanishClient.INSTANCE.getModuleManager().getModules()) {
            if (module.getModuleCategory() == selected || selected == Category.CONFIG) {
                visibleModules.add(module);
            }
        }
        if (selected == Category.CONFIG) {
            visibleModules.clear();
            for (Module module : VanishClient.INSTANCE.getModuleManager().getModules()) {
                if (module.getModuleCategory() == Category.CLIENT || module.getModuleCategory() == Category.CONFIG) {
                    visibleModules.add(module);
                }
            }
        }
        for (Module module : visibleModules) {
            ModuleCard card = new ModuleCard(module);
            card.render(context, x, currentY, cardWidth, cardHeight, mouseX, mouseY, alpha, fonts.header(), fonts.small());
            if (mouseX >= x && mouseX <= x + cardWidth && mouseY >= currentY && mouseY <= currentY + cardHeight) {
                selectedModule = module;
                settingsPanel.setSelectedModule(module);
            }
            currentY += 88;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sidebarX = (width - 1400) / 2 + 24;
        int sidebarY = (height - 850) / 2 + 24;
        int sidebarWidth = 260;
        int sidebarHeight = 802;
        if (sidebar.handleClick(mouseX, mouseY, sidebarX, sidebarY, sidebarWidth, sidebarHeight)) {
            return true;
        }
        int topX = (width - 1400) / 2 + 304;
        int topY = (height - 850) / 2 + 24;
        int topWidth = 1072;
        if (searchBar.handleClick(mouseX, mouseY, topX + topWidth - 320, topY + 14, 260, 40)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar.keyPressed(keyCode)) {
            return true;
        }
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchBar.charTyped(chr)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        closing = true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
