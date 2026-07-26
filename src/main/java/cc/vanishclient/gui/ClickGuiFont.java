package cc.vanishclient.gui;

import cc.vanishclient.VanishClient;
import cc.vanishclient.utils.render.font.FontManager;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Lazy, fail-safe font facade used only by the active ClickGUI.
 */
final class ClickGuiFont {
    private static final String ATLAS_WARMUP_TEXT =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?+-_()[]/%";
    enum Size {
        TITLE,
        NORMAL,
        SMALL
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/ClickGuiFont");

    private FontRenderer title;
    private FontRenderer normal;
    private FontRenderer small;
    private boolean initialized;
    private boolean disabled;
    private boolean failureLogged;

    void initializeIfReady() {
        if (initialized || disabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || !client.isOnThread()) return;

        try {
            FontManager manager = VanishClient.INSTANCE.getFontManager();
            title = manager.getSize(11, FontManager.Type.Arial);
            normal = manager.getSize(9, FontManager.Type.Arial);
            small = manager.getSize(8, FontManager.Type.Arial);

            // Width lookup generates and synchronously registers the ASCII glyph
            // page on the client thread. Do not expose the custom path before all
            // three atlases and their linearly filtered textures are ready.
            title.getStringWidth(ATLAS_WARMUP_TEXT);
            normal.getStringWidth(ATLAS_WARMUP_TEXT);
            small.getStringWidth(ATLAS_WARMUP_TEXT);
            initialized = true;
        } catch (Throwable failure) {
            disable(failure);
        }
    }

    int draw(DrawContext context, TextRenderer fallback, String text, float x, float y,
             int color, boolean shadow, Size size) {
        if (initialized && !disabled) {
            try {
                FontRenderer renderer = renderer(size);
                renderer.drawString(context.getMatrices(), text, x, y, new Color(color, true));
                return Math.round(x + renderer.getStringWidth(text));
            } catch (Throwable failure) {
                disable(failure);
            }
        }
        return context.drawText(fallback, text, Math.round(x), Math.round(y), color, shadow);
    }

    int width(TextRenderer fallback, String text, Size size) {
        if (initialized && !disabled) {
            try {
                return Math.round(renderer(size).getStringWidth(text));
            } catch (Throwable failure) {
                disable(failure);
            }
        }
        return fallback.getWidth(text);
    }

    int drawCentered(DrawContext context, TextRenderer fallback, String text, float centerX,
                     float y, int color, boolean shadow, Size size) {
        return draw(context, fallback, text, centerX - width(fallback, text, size) / 2.0F,
                y, color, shadow, size);
    }

    int height(TextRenderer fallback, Size size) {
        if (initialized && !disabled) {
            try {
                return Math.round(renderer(size).getStringHeight("Ag"));
            } catch (Throwable failure) {
                disable(failure);
            }
        }
        return fallback.fontHeight;
    }

    private FontRenderer renderer(Size size) {
        return switch (size) {
            case TITLE -> title;
            case NORMAL -> normal;
            case SMALL -> small;
        };
    }

    private void disable(Throwable failure) {
        disabled = true;
        initialized = false;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.error("Bundled Arial ClickGUI font failed; using Minecraft's text renderer", failure);
        }
    }
}
