package cc.vanishclient.gui.modern;

import cc.vanishclient.VanishClient;
import cc.vanishclient.module.modules.client.ClickGUIModule;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public final class SearchBar {
    private String query = "";
    private boolean focused;

    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha, FontRenderer font) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        Color bg = ColorPalette.withAlpha(ColorPalette.PANEL_ALT, (int) (180 * alpha));
        Color border = focused ? ColorPalette.withAlpha(ColorPalette.ACCENT, (int) (210 * alpha)) : ColorPalette.withAlpha(ColorPalette.BORDER, (int) (70 * alpha));
        Renderer.drawRoundedRect(context, x, y, width, height, 16, bg);
        context.drawBorder(x, y, width, height, border.getRGB());
        Renderer.drawText(context, font, focused || !query.isEmpty() ? query : "Search modules...", x + 14, y + 8, focused || !query.isEmpty() ? ColorPalette.TEXT : ColorPalette.SECONDARY);
        if (focused && System.currentTimeMillis() % 1000 < 500 && !query.isEmpty()) {
            int cursorX = x + 14 + (int) font.getStringWidth(query);
            context.fill(cursorX, y + 5, cursorX + 1, y + height - 5, ColorPalette.TEXT.getRGB());
        }
        if (hovered) {
            context.fill(x + width - 20, y + 8, x + width - 8, y + height - 8, ColorPalette.withAlpha(ColorPalette.SECONDARY, 80).getRGB());
        }
    }

    public boolean handleClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        focused = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        return focused;
    }

    public boolean keyPressed(int keyCode) {
        if (!focused) return false;
        if (keyCode == 259) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (!focused) return false;
        if (chr >= 32 && chr < 127) {
            query += chr;
            return true;
        }
        return false;
    }

    public String getQuery() {
        return query;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
