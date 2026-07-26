package cc.vanishclient.gui.modern;

import cc.vanishclient.utils.render.RenderUtils;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public final class Renderer {
    private Renderer() {}

    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, Color color) {
        RenderUtils.drawRoundedRect(context, x, y, width, height, radius, color.getRGB());
    }

    public static void drawGlow(DrawContext context, int x, int y, int width, int height, int radius, Color color, int glowRadius) {
        RenderUtils.drawGlow(context, x, y, width, height, radius, color.getRGB(), glowRadius);
    }

    public static void drawText(DrawContext context, FontRenderer font, String text, int x, int y, Color color) {
        font.drawString(context.getMatrices(), text, x, y, color);
    }

    public static void drawCenteredText(DrawContext context, FontRenderer font, String text, int x, int y, int width, Color color) {
        int textWidth = (int) font.getStringWidth(text);
        font.drawString(context.getMatrices(), text, x + (width - textWidth) / 2, y, color);
    }
}
