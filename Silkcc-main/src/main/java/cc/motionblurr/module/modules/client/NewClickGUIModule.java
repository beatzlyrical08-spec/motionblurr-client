package cc.motionblurr.module.modules.client;

import cc.motionblurr.gui.ClickGui;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class NewClickGUIModule extends Module {

    public NewClickGUIModule() {
        super("NewClickGUI", "Modern NanoVG-based ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
    }

@Override
public void onEnable() {
    if (mc.currentScreen == null) {
        mc.setScreen(new ClickGui());
    }
    setEnabled(false);
}

    public static Color getAccentColor() {
        return ClientSettingsModule.getAccentColor();
    }

    public static String getFontStyle() {
        return ClientSettingsModule.getFontStyle();
    }
}

