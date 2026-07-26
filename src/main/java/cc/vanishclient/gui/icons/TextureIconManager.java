package cc.vanishclient.gui.icons;

import cc.vanishclient.utils.render.RenderUtils;
import cc.vanishclient.utils.render.nanovg.NanoVGImage;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TextureIconManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/TextureIcons");
    private static final IconKey FALLBACK = IconKey.SPARKLE;
    private static final TextureIconManager INSTANCE = new TextureIconManager();

    private final Map<IconKey, Integer> iconHandles = new EnumMap<>(IconKey.class);
    private final Set<IconKey> warnedMissing = EnumSet.noneOf(IconKey.class);
    private boolean disposed;

    private TextureIconManager() {
        preloadAll();
    }

    public static TextureIconManager getInstance() {
        return INSTANCE;
    }

    public void preloadAll() {
        if (disposed) return;
        for (IconKey key : IconKey.values()) {
            loadIcon(key);
        }
    }

    public void draw(DrawContext context, IconKey key, float x, float y, float width, float height, int argb, float alpha) {
        if (disposed || context == null || width <= 0.0F || height <= 0.0F || alpha <= 0.0F) return;

        IconKey resolvedKey = key == null ? FALLBACK : key;
        int handle = handleFor(resolvedKey);
        if (handle == NanoVGImage.INVALID_IMAGE && resolvedKey != FALLBACK) {
            warnMissingOnce(resolvedKey);
            handle = handleFor(FALLBACK);
        }

        int tintedArgb = multiplyAlpha(argb, alpha);
        if (((tintedArgb >>> 24) & 255) == 0) return;

        if (handle != NanoVGImage.INVALID_IMAGE &&
                NanoVGImage.drawImage(context, handle, x, y, width, height, new Color(tintedArgb, true))) {
            return;
        }

        warnMissingOnce(FALLBACK);
        drawFallbackSymbol(context, x, y, width, height, tintedArgb);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        iconHandles.clear();
        NanoVGImage.cleanup();
    }

    private int handleFor(IconKey key) {
        Integer cached = iconHandles.get(key);
        if (cached != null) return cached;
        return loadIcon(key);
    }

    private int loadIcon(IconKey key) {
        if (disposed) return NanoVGImage.INVALID_IMAGE;

        int handle = NanoVGImage.loadImage(key.resourcePath());
        iconHandles.put(key, handle);
        if (handle == NanoVGImage.INVALID_IMAGE) {
            warnMissingOnce(key);
        } else {
            LOGGER.debug("Loaded icon {} from {} as {}.", key, key.resourcePath(), key.textureIdentifier());
        }
        return handle;
    }

    private void warnMissingOnce(IconKey key) {
        if (warnedMissing.add(key)) {
            LOGGER.warn("Missing or invalid PNG icon {} at {}; fallback is {}.", key, key.resourcePath(), FALLBACK.resourcePath());
        }
    }

    private int multiplyAlpha(int argb, float alphaMultiplier) {
        float alpha = Math.max(0.0F, Math.min(1.0F, alphaMultiplier));
        int originalAlpha = (argb >>> 24) & 255;
        int multipliedAlpha = Math.round(originalAlpha * alpha);
        return (multipliedAlpha << 24) | (argb & 0x00FFFFFF);
    }

    private void drawFallbackSymbol(DrawContext context, float x, float y, float width, float height, int color) {
        int size = Math.max(5, Math.round(Math.min(width, height) * 0.55F));
        int drawX = Math.round(x + (width - size) * 0.5F);
        int drawY = Math.round(y + (height - size) * 0.5F);
        RenderUtils.drawRoundedRect(context, drawX, drawY, size, size, Math.max(2, size / 2), color);
    }
}
