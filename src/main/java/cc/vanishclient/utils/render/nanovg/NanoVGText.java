package cc.vanishclient.utils.render.nanovg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class NanoVGText {
    private static boolean isReady() {
        return NanoVGFrameManager.isInFrame() && NanoVGContext.isValid();
    }

    private static TextRenderer getTextRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static float getBaseFontHeight() {
        TextRenderer textRenderer = getTextRenderer();
        return Math.max(1, textRenderer.fontHeight);
    }

    public static void drawText(String text, float x, float y, float size, Color color, boolean bold) {
        if (!isReady()) {
            return;
        }
        DrawContext context = NanoVGFrameManager.getContext();
        if (context == null) {
            return;
        }

        TextRenderer textRenderer = getTextRenderer();
        String displayText = bold ? "§l" + text : text;
        float scale = size / getBaseFontHeight();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.drawText(textRenderer, displayText, 0, 0, color.getRGB(), false);
        context.getMatrices().pop();
    }

    public static void drawText(String text, float x, float y, float size, Color color) {
        drawText(text, x, y, size, color, false);
    }

    public static void drawTextWithFont(String text, float x, float y, float size, Color color, int fontId) {
        drawText(text, x, y, size, color, false);
    }

    public static void drawIcon(String icon, float x, float y, float size, Color color) {
        drawText(icon, x, y, size, color, false);
    }

    public static float getTextWidth(String text, float size, boolean bold) {
        if (!NanoVGContext.isValid()) {
            return 0;
        }
        float scale = size / getBaseFontHeight();
        return getTextRenderer().getWidth(text) * scale;
    }

    public static float getTextWidth(String text, float size) {
        return getTextWidth(text, size, false);
    }

    public static float getTextWidthWithFont(String text, float size, int fontId) {
        return getTextWidth(text, size, false);
    }

    public static float getTextHeight(float size) {
        if (!NanoVGContext.isValid()) {
            return 0;
        }
        return getBaseFontHeight() * (size / getBaseFontHeight());
    }
}


