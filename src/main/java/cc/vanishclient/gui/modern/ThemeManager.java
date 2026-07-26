package cc.vanishclient.gui.modern;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThemeManager {
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();

    static {
        register(new Theme("Default", new Color(15, 16, 21), new Color(23, 25, 34), new Color(139, 92, 246)));
        register(new Theme("Aurora", new Color(10, 12, 20), new Color(20, 24, 34), new Color(168, 85, 247)));
        register(new Theme("Nebula", new Color(12, 15, 24), new Color(24, 27, 38), new Color(99, 102, 241)));
    }

    private ThemeManager() {}

    public static void register(Theme theme) {
        THEMES.put(theme.name(), theme);
    }

    public static Theme getTheme(String name) {
        return THEMES.getOrDefault(name, THEMES.values().iterator().next());
    }

    public static String[] getThemeNames() {
        return THEMES.keySet().toArray(new String[0]);
    }

    public static String getDefaultName() {
        return THEMES.keySet().iterator().next();
    }

    public record Theme(String name, Color background, Color panel, Color accent) {}
}
