package cc.motionblurr.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;

public class NanoVGFrameManager {
    private static DrawContext currentContext = null;

    public static void beginFrame(DrawContext context) {
        RenderSystem.assertOnRenderThread();
        NanoVGContext.init();

        if (currentContext != null) {
            return;
        }

        currentContext = context;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void endFrame() {
        if (currentContext == null) {
            return;
        }

        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        currentContext = null;
    }

    public static boolean isInFrame() {
        return currentContext != null;
    }

    public static DrawContext getContext() {
        return currentContext;
    }

    public static void resetInFrame() {
        currentContext = null;
    }
}

