package cc.motionblurr.gui.newgui.components;

import cc.motionblurr.module.Category;
import cc.motionblurr.utils.render.nanovg.NanoVGRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CategoryIcon {
    private static final Map<Category, Integer> ICON_MAP = new HashMap<>();
    private static final int ICON_SIZE = 12;
    private static boolean initialized = false;

    public static void init() {
        if (initialized)
            return;

        ICON_MAP.put(Category.COMBAT, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/sword.png"));
        ICON_MAP.put(Category.PLAYER, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/user.png"));
        ICON_MAP.put(Category.MOVEMENT, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/accessibility.png"));
        ICON_MAP.put(Category.MISC, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/harddrive.png"));
        ICON_MAP.put(Category.RENDER, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/eye.png"));
        ICON_MAP.put(Category.CLIENT, NanoVGRenderer.loadImage("assets/motionblurr/textures/icons/computer.png"));

        initialized = true;
    }

    public static void drawIcon(Category category, float x, float y, Color color) {
        if (!initialized)
            init();

        Integer imageId = ICON_MAP.get(category);
        if (imageId == null || imageId == -1)
            return;

        NanoVGRenderer.drawImage(imageId, x, y, ICON_SIZE, ICON_SIZE, Color.WHITE);
    }

    public static int getIconSize() {
        return ICON_SIZE;
    }
}

