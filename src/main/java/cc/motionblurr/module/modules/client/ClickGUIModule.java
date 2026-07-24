package cc.motionblurr.module.modules.client;

import cc.motionblurr.gui.ClickGui;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.module.setting.ModeSetting;
import cc.motionblurr.gui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(),
            ThemeManager.getThemeNamesArray());

    public ClickGUIModule() {
        super("Click Gui", "Toggles the MotionBlurr GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
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

