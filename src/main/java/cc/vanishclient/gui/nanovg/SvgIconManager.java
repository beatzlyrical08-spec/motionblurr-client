package cc.vanishclient.gui.nanovg;

import cc.vanishclient.gui.icons.IconKey;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NSVGPath;
import org.lwjgl.nanovg.NSVGShape;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.nanovg.NanoSVG.*;
import static org.lwjgl.nanovg.NanoVG.*;

public final class SvgIconManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/SvgIcons");
    private static final IconKey FALLBACK = IconKey.SPARKLE;
    private static final SvgIconManager INSTANCE = new SvgIconManager();

    private final Map<IconKey, SvgIcon> icons = new EnumMap<>(IconKey.class);
    private final Set<IconKey> warnedMissing = EnumSet.noneOf(IconKey.class);
    private boolean disposed;

    private SvgIconManager() {
        preloadAll();
    }

    public static SvgIconManager getInstance() {
        return INSTANCE;
    }

    public void preloadAll() {
        for (IconKey key : IconKey.values()) {
            loadIcon(key);
        }
    }

    public void draw(IconKey key, float x, float y, float width, float height, int argb, float alpha) {
        if (disposed || !NanoVGRenderer.getInstance().isInFrame()) return;

        SvgIcon icon = icons.get(key);
        if (icon == null && key != FALLBACK) {
            warnMissingOnce(key);
            icon = icons.get(FALLBACK);
        }
        if (icon == null) {
            warnMissingOnce(FALLBACK);
            return;
        }

        float iconWidth = icon.image.width();
        float iconHeight = icon.image.height();
        if (iconWidth <= 0.0F || iconHeight <= 0.0F || width <= 0.0F || height <= 0.0F) return;

        float scale = Math.min(width / iconWidth, height / iconHeight);
        float drawWidth = iconWidth * scale;
        float drawHeight = iconHeight * scale;
        float drawX = x + (width - drawWidth) * 0.5F;
        float drawY = y + (height - drawHeight) * 0.5F;
        long vg = NanoVGRenderer.getInstance().contextHandle();

        nvgSave(vg);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            nvgTranslate(vg, drawX, drawY);
            nvgScale(vg, scale, scale);
            nvgLineCap(vg, NVG_ROUND);
            nvgLineJoin(vg, NVG_ROUND);

            for (NSVGShape shape = icon.image.shapes(); shape != null; shape = shape.next()) {
                if ((shape.flags() & NSVG_FLAGS_VISIBLE) == 0) continue;
                drawShape(vg, stack, shape, argb, alpha);
            }
        } catch (Throwable failure) {
            if (warnedMissing.add(key)) {
                LOGGER.warn("Failed to draw SVG icon {}.", key, failure);
            }
        } finally {
            nvgRestore(vg);
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        for (SvgIcon icon : icons.values()) {
            NanoSVG.nsvgDelete(icon.image);
        }
        icons.clear();
    }

    private void drawShape(long vg, MemoryStack stack, NSVGShape shape, int argb, float alpha) {
        boolean hasFill = shape.fill().type() != NSVG_PAINT_NONE;
        boolean hasStroke = shape.stroke().type() != NSVG_PAINT_NONE && shape.strokeWidth() > 0.0F;
        if (!hasFill && !hasStroke) return;

        for (NSVGPath path = shape.paths(); path != null; path = path.next()) {
            if (path.npts() <= 0) continue;
            beginPath(vg, path);

            if (hasFill) {
                nvgFillColor(vg, color(stack, argb, alpha * shape.opacity()));
                nvgFill(vg);
            }
            if (hasStroke) {
                nvgStrokeColor(vg, color(stack, argb, alpha * shape.opacity()));
                nvgStrokeWidth(vg, Math.max(0.1F, shape.strokeWidth()));
                nvgLineCap(vg, mapLineCap(shape.strokeLineCap()));
                nvgLineJoin(vg, mapLineJoin(shape.strokeLineJoin()));
                nvgStroke(vg);
            }
        }
    }

    private void beginPath(long vg, NSVGPath path) {
        var points = path.pts();
        nvgBeginPath(vg);
        nvgMoveTo(vg, points.get(0), points.get(1));

        for (int point = 1; point + 2 < path.npts(); point += 3) {
            int index = point * 2;
            nvgBezierTo(vg,
                    points.get(index), points.get(index + 1),
                    points.get(index + 2), points.get(index + 3),
                    points.get(index + 4), points.get(index + 5));
        }
        if (path.closed() != 0) {
            nvgClosePath(vg);
        }
    }

    private SvgIcon loadIcon(IconKey key) {
        if (disposed) return null;
        SvgIcon cached = icons.get(key);
        if (cached != null) return cached;

        try (InputStream stream = SvgIconManager.class.getClassLoader().getResourceAsStream(key.resourcePath())) {
            if (stream == null) {
                warnMissingOnce(key);
                return null;
            }
            String svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            NSVGImage image = NanoSVG.nsvgParse(svg, "px", 96.0F);
            if (image == null) {
                warnMissingOnce(key);
                return null;
            }
            SvgIcon icon = new SvgIcon(image);
            icons.put(key, icon);
            return icon;
        } catch (IOException | RuntimeException failure) {
            if (warnedMissing.add(key)) {
                LOGGER.warn("Failed to load SVG icon {} from {}.", key, key.resourcePath(), failure);
            }
            return null;
        }
    }

    private void warnMissingOnce(IconKey key) {
        if (warnedMissing.add(key)) {
            LOGGER.warn("Missing SVG icon {} at {}.", key, key.resourcePath());
        }
    }

    private int mapLineCap(byte cap) {
        return switch (cap) {
            case NSVG_CAP_BUTT -> NVG_BUTT;
            case NSVG_CAP_SQUARE -> NVG_SQUARE;
            default -> NVG_ROUND;
        };
    }

    private int mapLineJoin(byte join) {
        return switch (join) {
            case NSVG_JOIN_MITER -> NVG_MITER;
            case NSVG_JOIN_BEVEL -> NVG_BEVEL;
            default -> NVG_ROUND;
        };
    }

    private org.lwjgl.nanovg.NVGColor color(MemoryStack stack, int argb, float alphaMultiplier) {
        float alpha = Math.max(0.0F, Math.min(1.0F, alphaMultiplier));
        return org.lwjgl.nanovg.NVGColor.malloc(stack)
                .r(((argb >>> 16) & 255) / 255.0F)
                .g(((argb >>> 8) & 255) / 255.0F)
                .b((argb & 255) / 255.0F)
                .a(((argb >>> 24) & 255) / 255.0F * alpha);
    }

    private record SvgIcon(NSVGImage image) {
    }
}
