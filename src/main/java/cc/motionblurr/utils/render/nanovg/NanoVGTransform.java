package cc.motionblurr.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class NanoVGTransform {
    private static DrawContext getContext() {
        return NanoVGFrameManager.getContext();
    }

    public static void save() {
        DrawContext context = getContext();
        if (context != null) {
            context.getMatrices().push();
        }
    }

    public static void restore() {
        DrawContext context = getContext();
        if (context != null) {
            context.getMatrices().pop();
        }
    }

    public static void translate(float x, float y) {
        DrawContext context = getContext();
        if (context != null) {
            context.getMatrices().translate(x, y, 0);
        }
    }

    public static void scale(float x, float y) {
        DrawContext context = getContext();
        if (context != null) {
            context.getMatrices().scale(x, y, 1f);
        }
    }

    public static void scissor(float x, float y, float width, float height) {
        DrawContext context = getContext();
        if (context == null) {
            return;
        }

        MatrixStack matrices = context.getMatrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vector4f topLeft = new Vector4f(x, y, 0, 1).mul(matrix);
        Vector4f bottomRight = new Vector4f(x + width, y + height, 0, 1).mul(matrix);

        float minX = Math.min(topLeft.x(), bottomRight.x());
        float maxX = Math.max(topLeft.x(), bottomRight.x());
        float minY = Math.min(topLeft.y(), bottomRight.y());
        float maxY = Math.max(topLeft.y(), bottomRight.y());

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();

        float scaleX = (float) framebufferWidth / (float) scaledWidth;
        float scaleY = (float) framebufferHeight / (float) scaledHeight;

        int scissorX = Math.round(minX * scaleX);
        int scissorY = Math.round((scaledHeight - maxY) * scaleY);
        int scissorW = Math.round((maxX - minX) * scaleX);
        int scissorH = Math.round((maxY - minY) * scaleY);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
    }

    public static void resetScissor() {
        RenderSystem.disableScissor();
    }
}


