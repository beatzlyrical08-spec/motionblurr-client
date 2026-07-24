package cc.motionblurr.utils.render.nanovg;

import cc.motionblurr.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class NanoVGDrawing {
    private static boolean isReady() {
        return NanoVGFrameManager.isInFrame() && NanoVGContext.isValid();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        if (!isReady()) {
            return;
        }
        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), Math.round(x), Math.round(y), Math.round(width), Math.round(height), Math.round(radius), color.getRGB());
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius,
            float strokeWidth, Color color) {
        if (!isReady()) {
            return;
        }
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        int thickness = Math.max(1, Math.round(strokeWidth));

        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), ix, iy, iw, thickness, Math.round(radius), color.getRGB());
        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), ix, iy + ih - thickness, iw, thickness, Math.round(radius), color.getRGB());
        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), ix, iy, thickness, ih, Math.round(radius), color.getRGB());
        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), ix + iw - thickness, iy, thickness, ih, Math.round(radius), color.getRGB());
    }

    public static void drawRoundedRectGradient(float x, float y, float width, float height, float radius,
            Color colorTop, Color colorBottom) {
        if (!isReady()) {
            return;
        }
        RenderUtils.drawRoundedRectGradient(NanoVGFrameManager.getContext(), Math.round(x), Math.round(y), Math.round(width), Math.round(height), Math.round(radius), colorTop.getRGB(), colorBottom.getRGB());
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        if (!isReady()) {
            return;
        }
        RenderUtils.drawFilledCircle(NanoVGFrameManager.getContext(), Math.round(x), Math.round(y), Math.max(1, Math.round(radius)), color.getRGB());
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        if (!isReady()) {
            return;
        }
        NanoVGFrameManager.getContext().fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color.getRGB());
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float strokeWidth, Color color) {
        if (!isReady()) {
            return;
        }

        DrawContext context = NanoVGFrameManager.getContext();
        int thickness = Math.max(1, Math.round(strokeWidth));
        int startX = Math.round(x1);
        int startY = Math.round(y1);
        int endX = Math.round(x2);
        int endY = Math.round(y2);

        if (startX == endX) {
            context.fill(startX, Math.min(startY, endY), startX + thickness, Math.max(startY, endY), color.getRGB());
            return;
        }
        if (startY == endY) {
            context.fill(Math.min(startX, endX), startY, Math.max(startX, endX), startY + thickness, color.getRGB());
            return;
        }

        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);
        int sx = startX < endX ? 1 : -1;
        int sy = startY < endY ? 1 : -1;
        int err = dx - dy;
        int x = startX;
        int y = startY;

        while (true) {
            context.fill(x, y, x + thickness, y + thickness, color.getRGB());
            if (x == endX && y == endY) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    public static void drawRoundedRectWithShadow(float x, float y, float width, float height, float radius, Color color, Color shadowColor, float shadowBlur, float shadowSpread) {
        if (!isReady()) {
            return;
        }

        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        int ir = Math.round(radius);
        int shadowRgb = new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), shadowColor.getAlpha() / 2).getRGB();

        RenderUtils.drawGlow(NanoVGFrameManager.getContext(), ix, iy, iw, ih, ir, shadowRgb, Math.max(1, Math.round(shadowBlur)));
        RenderUtils.drawRoundedRect(NanoVGFrameManager.getContext(), ix, iy, iw, ih, ir, color.getRGB());
    }
}


