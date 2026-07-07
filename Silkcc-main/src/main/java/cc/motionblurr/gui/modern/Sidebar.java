package cc.motionblurr.gui.modern;

import cc.motionblurr.MotionBlurrClient;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.module.modules.client.ClickGUIModule;
import cc.motionblurr.module.modules.client.ClientSettingsModule;
import cc.motionblurr.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public final class Sidebar {
    private static final List<Category> CATEGORIES = Arrays.asList(Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER, Category.MISC, Category.CONFIG);
    private Category selected = Category.COMBAT;

    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha, FontRenderer font, FontRenderer smallFont) {
        Renderer.drawRoundedRect(context, x, y, width, height, 24, ColorPalette.withAlpha(ColorPalette.PANEL, (int) (210 * alpha)));
        context.drawBorder(x, y, width, height, ColorPalette.withAlpha(ColorPalette.BORDER, (int) (70 * alpha)).getRGB());

        Renderer.drawText(context, font, "MotionBlurr", x + 24, y + 24, ColorPalette.TEXT);
        Renderer.drawText(context, smallFont, "Premium client UI", x + 24, y + 56, ColorPalette.SECONDARY);

        int itemY = y + 108;
        for (Category category : CATEGORIES) {
            boolean hovered = mouseX >= x + 12 && mouseX <= x + width - 12 && mouseY >= itemY && mouseY <= itemY + 44;
            boolean active = category == selected;
            int barAlpha = active ? 255 : hovered ? 90 : 0;
            Color bg = active ? ColorPalette.withAlpha(ColorPalette.PANEL_ALT, (int) (140 * alpha)) : ColorPalette.withAlpha(ColorPalette.PANEL, (int) (70 * alpha));
            Renderer.drawRoundedRect(context, x + 10, itemY, width - 20, 40, 14, bg);
            context.fill(x + 10, itemY + 2, x + 14, itemY + 38, ColorPalette.withAlpha(ColorPalette.ACCENT, barAlpha).getRGB());
            Renderer.drawText(context, smallFont, category.getName(), x + 32, itemY + 14, active ? ColorPalette.TEXT : ColorPalette.SECONDARY);
            itemY += 48;
        }

        Renderer.drawRoundedRect(context, x + 10, y + height - 96, width - 20, 78, 18, ColorPalette.withAlpha(ColorPalette.PANEL_ALT, (int) (160 * alpha)));
        Renderer.drawText(context, smallFont, "Player", x + 24, y + height - 72, ColorPalette.TEXT);
        Renderer.drawText(context, smallFont, MotionBlurrClient.mc != null && MotionBlurrClient.mc.getSession() != null ? MotionBlurrClient.mc.getSession().getUsername() : "Guest", x + 24, y + height - 50, ColorPalette.SECONDARY);
        Renderer.drawText(context, smallFont, MotionBlurrClient.CLIENT_VERSION, x + 24, y + height - 28, ColorPalette.ACCENT);
    }

    public boolean handleClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        int itemY = y + 108;
        for (Category category : CATEGORIES) {
            boolean hovered = mouseX >= x + 12 && mouseX <= x + width - 12 && mouseY >= itemY && mouseY <= itemY + 44;
            if (hovered) {
                selected = category;
                return true;
            }
            itemY += 48;
        }
        return false;
    }

    public Category getSelectedCategory() {
        return selected;
    }

    public void setSelectedCategory(Category category) {
        this.selected = category;
    }
}
