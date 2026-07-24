package cc.motionblurr.gui.modern;

import java.awt.Color;

public final class ColorPalette {
    public static final Color BACKGROUND = new Color(15, 16, 21, 255);
    public static final Color PANEL = new Color(23, 25, 34, 255);
    public static final Color PANEL_ALT = new Color(32, 35, 50, 255);
    public static final Color HOVER = new Color(40, 44, 58, 255);
    public static final Color ACCENT = new Color(139, 92, 246, 255);
    public static final Color ACCENT_GLOW = new Color(185, 131, 255, 255);
    public static final Color TEXT = new Color(248, 250, 252, 255);
    public static final Color SECONDARY = new Color(154, 160, 178, 255);
    public static final Color MUTED = new Color(88, 94, 112, 255);
    public static final Color BORDER = new Color(255, 255, 255, 24);
    public static final Color OVERLAY = new Color(0, 0, 0, 140);

    private ColorPalette() {}

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color mix(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t)
        );
    }
}
