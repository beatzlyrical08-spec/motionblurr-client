package cc.vanishclient.gui.newgui.components;

import cc.vanishclient.module.Category;
import cc.vanishclient.utils.render.nanovg.NanoVGRenderer;

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

        ICON_MAP.put(Category.COMBAT, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/sword.png"));
        ICON_MAP.put(Category.PLAYER, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/user.png"));
        ICON_MAP.put(Category.MOVEMENT, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/sport-shoe.png"));
        ICON_MAP.put(Category.MISC, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/sparkle.png"));
        ICON_MAP.put(Category.RENDER, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/sparkle.png"));
        ICON_MAP.put(Category.CLIENT, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/folder-open.png"));
        ICON_MAP.put(Category.CONFIG, NanoVGRenderer.loadImage("assets/vanishclient/textures/icons/folder-open.png"));

        initialized = true;
    }

    public static void drawIcon(Category category, float x, float y, Color color) {
        if (!initialized)
            init();

        Integer imageId = ICON_MAP.get(category);
        if (imageId == null || imageId == -1)
            return;

        NanoVGRenderer.drawImage(imageId, x, y, ICON_SIZE, ICON_SIZE, color);
    }

    public static int getIconSize() {
        return ICON_SIZE;
    }
}
