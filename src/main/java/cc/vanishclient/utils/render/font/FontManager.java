package cc.vanishclient.utils.render.font;


import cc.vanishclient.VanishClient;
import cc.vanishclient.utils.render.font.fonts.FontRenderer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {

    private final Map<FontKey, FontRenderer> fontCache = new HashMap<>();
    public void initialize() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }

    public FontRenderer create(float size, String name) {
        String path = "assets/vanishclient/fonts/" + name + ".ttf";
        try (InputStream stream = VanishClient.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Could not find font: " + path);
            }
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, stream);
            int padding = "Arial".equals(name) ? 3 : 2;
            return new FontRenderer(new Font[]{baseFont}, size, 256, padding);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException("Could not load font: " + path, exception);
        }
    }

    public FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    public enum Type {
        Inter("Inter"),
        JetbrainsMono("JetbrainsMono"),
        Poppins("Poppins-Medium"),
        Arial("Arial");

        private final String type;

        Type(String type) {
            this.type = type;
        }
    }

    private record FontKey(int size, Type type) {

        @Override
        public @NotNull String toString() {
            return "FontKey[" +
                    "size=" + size + ", " +
                    "type=" + type + ']';
        }

    }
}
