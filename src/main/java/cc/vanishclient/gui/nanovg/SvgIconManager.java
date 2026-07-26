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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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

        float iconWidth = icon.width();
        float iconHeight = icon.height();
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

            for (SvgShape shape : icon.shapes()) {
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
        icons.clear();
    }

    private void drawShape(long vg, MemoryStack stack, SvgShape shape, int argb, float alpha) {
        if (!shape.hasFill() && !shape.hasStroke()) return;

        nvgBeginPath(vg);
        for (SvgPath path : shape.paths()) {
            if (path.points().length < 2) continue;
            beginPath(vg, path);
        }

        if (shape.hasFill()) {
            nvgFillColor(vg, color(stack, argb, alpha * shape.opacity()));
            nvgFill(vg);
        }
        if (shape.hasStroke()) {
            nvgStrokeColor(vg, color(stack, argb, alpha * shape.opacity()));
            nvgStrokeWidth(vg, Math.max(0.1F, shape.strokeWidth()));
            nvgLineCap(vg, shape.lineCap());
            nvgLineJoin(vg, shape.lineJoin());
            nvgStroke(vg);
        }
    }

    private void beginPath(long vg, SvgPath path) {
        float[] points = path.points();
        nvgMoveTo(vg, points[0], points[1]);

        for (int point = 1; point + 2 < path.pointCount(); point += 3) {
            int index = point * 2;
            nvgBezierTo(vg,
                    points[index], points[index + 1],
                    points[index + 2], points[index + 3],
                    points[index + 4], points[index + 5]);
        }
        if (path.closed()) {
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
            try {
                SvgIcon icon = convert(image);
                icons.put(key, icon);
                return icon;
            } finally {
                NanoSVG.nsvgDelete(image);
            }
        } catch (IOException | RuntimeException failure) {
            if (warnedMissing.add(key)) {
                LOGGER.warn("Failed to load SVG icon {} from {}.", key, key.resourcePath(), failure);
            }
            return null;
        }
    }

    private SvgIcon convert(NSVGImage image) {
        List<SvgShape> shapes = new ArrayList<>();
        for (NSVGShape shape = image.shapes(); shape != null; shape = shape.next()) {
            if ((shape.flags() & NSVG_FLAGS_VISIBLE) == 0) continue;

            List<SvgPath> paths = new ArrayList<>();
            for (NSVGPath path = shape.paths(); path != null; path = path.next()) {
                if (path.npts() <= 0 || path.pts() == null) continue;

                int floatCount = path.npts() * 2;
                float[] points = new float[floatCount];
                for (int index = 0; index < floatCount; index++) {
                    points[index] = path.pts().get(index);
                }
                paths.add(new SvgPath(points, path.npts(), path.closed() != 0));
            }

            if (paths.isEmpty()) continue;

            boolean hasFill = shape.fill().type() != NSVG_PAINT_NONE;
            boolean hasStroke = shape.stroke().type() != NSVG_PAINT_NONE && shape.strokeWidth() > 0.0F;
            if (!hasFill && !hasStroke) continue;

            shapes.add(new SvgShape(
                    List.copyOf(paths),
                    hasFill,
                    hasStroke,
                    Math.max(0.1F, shape.strokeWidth()),
                    mapLineCap(shape.strokeLineCap()),
                    mapLineJoin(shape.strokeLineJoin()),
                    clamp01(shape.opacity())));
        }

        return new SvgIcon(Math.max(1.0F, image.width()), Math.max(1.0F, image.height()), List.copyOf(shapes));
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

    private float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record SvgIcon(float width, float height, List<SvgShape> shapes) {
    }

    private record SvgShape(List<SvgPath> paths, boolean hasFill, boolean hasStroke, float strokeWidth,
                            int lineCap, int lineJoin, float opacity) {
    }

    private record SvgPath(float[] points, int pointCount, boolean closed) {
    }
}
