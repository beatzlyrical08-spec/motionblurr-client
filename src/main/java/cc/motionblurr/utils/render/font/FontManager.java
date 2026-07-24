package cc.motionblurr.utils.render.font;


import cc.motionblurr.MotionBlurrClient;
import cc.motionblurr.utils.render.font.fonts.FontRenderer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FontManager {

    private final Map<FontKey, FontRenderer> fontCache = new HashMap<>();
    private static final boolean SYSTEM_ARIAL_AVAILABLE = hasSystemFont("Arial");

    public void initialize() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }

@SneakyThrows
    public FontRenderer create(float size, String name) {
    if ("Arial".equals(name)) {
        String family = SYSTEM_ARIAL_AVAILABLE ? "Arial" : Font.SANS_SERIF;
        return new FontRenderer(new Font[]{new Font(family, Font.PLAIN, 1)}, size, 256, 3);
    }
    String path = "assets/motionblurr/fonts/" + name + ".ttf";

    InputStream inputStream = MotionBlurrClient.class.getClassLoader().getResourceAsStream(path);

    if (inputStream == null) {
        throw new RuntimeException("Could not find font: " + path);
    }

    Font[] font = Font.createFonts(inputStream);
    return new FontRenderer(font, size, 256, 2);
}

    private static boolean hasSystemFont(String requestedFamily) {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (family.equalsIgnoreCase(requestedFamily)) return true;
        }
        return false;
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
