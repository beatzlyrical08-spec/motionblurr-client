package cc.vanishclient.module.modules.client;

import cc.vanishclient.gui.ClickGui;
import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.setting.ModeSetting;
import cc.vanishclient.gui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(),
            ThemeManager.getThemeNamesArray());

    public ClickGUIModule() {
        super("Click Gui", "Toggles the VanishClient GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(theme);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.setScreen(new ClickGui());
        }
        setEnabled(false);
    }
}

