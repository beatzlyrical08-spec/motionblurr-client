package cc.vanishclient.gui.modern;

import cc.vanishclient.VanishClient;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;

public final class FontManager {
    private final cc.vanishclient.utils.render.font.FontManager delegate;
    private final FontRenderer titleFont;
    private final FontRenderer headerFont;
    private final FontRenderer bodyFont;
    private final FontRenderer smallFont;

    public FontManager() {
        this.delegate = VanishClient.INSTANCE.getFontManager();
        this.titleFont = delegate.getSize(24, cc.vanishclient.utils.render.font.FontManager.Type.Poppins);
        this.headerFont = delegate.getSize(16, cc.vanishclient.utils.render.font.FontManager.Type.Inter);
        this.bodyFont = delegate.getSize(14, cc.vanishclient.utils.render.font.FontManager.Type.Inter);
        this.smallFont = delegate.getSize(12, cc.vanishclient.utils.render.font.FontManager.Type.Inter);
    }

    public FontRenderer title() {
        return titleFont;
    }

    public FontRenderer header() {
        return headerFont;
    }

    public FontRenderer body() {
        return bodyFont;
    }

    public FontRenderer small() {
        return smallFont;
    }
}
