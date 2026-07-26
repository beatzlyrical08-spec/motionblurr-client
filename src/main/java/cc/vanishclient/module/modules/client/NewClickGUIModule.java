package cc.vanishclient.module.modules.client;

import cc.vanishclient.gui.ClickGui;
import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class NewClickGUIModule extends Module {

    public NewClickGUIModule() {
        super("NewClickGUI", "Opens the VanishClient ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
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
