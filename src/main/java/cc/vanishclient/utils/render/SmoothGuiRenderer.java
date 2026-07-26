package cc.vanishclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * Smooth 2D primitives for GUI rendering. Uses Minecraft's position/color
 * pipeline and ordinary triangles, which remain compatible with VulkanMod.
 */
public final class SmoothGuiRenderer {
    private SmoothGuiRenderer() {
    }

    public static void roundedRect(DrawContext context, float x, float y, float width, float height,
                                   float radius, int color) {
        if (width <= 0.0F || height <= 0.0F) return;
        float r = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        if (r < 0.5F) {
            context.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
            return;
        }

        int segments = Math.max(8, (int) Math.ceil(r * 2.0F));
        int perimeterCount = (segments + 1) * 4;
        float[] px = new float[perimeterCount];
        float[] py = new float[perimeterCount];
        int point = 0;
        point = appendArc(px, py, point, x + width - r, y + r, r, -90.0F, 0.0F, segments);
        point = appendArc(px, py, point, x + width - r, y + height - r, r, 0.0F, 90.0F, segments);
        point = appendArc(px, py, point, x + r, y + height - r, r, 90.0F, 180.0F, segments);
        point = appendArc(px, py, point, x + r, y + r, r, 180.0F, 270.0F, segments);

        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float centerX = x + width * 0.5F;
        float centerY = y + height * 0.5F;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < point; i++) {
            int next = (i + 1) % point;
            vertex(buffer, matrix, centerX, centerY, red, green, blue, alpha);
            vertex(buffer, matrix, px[i], py[i], red, green, blue, alpha);
            vertex(buffer, matrix, px[next], py[next], red, green, blue, alpha);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        CompatShaders.usePositionColor();
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static int appendArc(float[] x, float[] y, int offset, float centerX, float centerY,
                                 float radius, float startDegrees, float endDegrees, int segments) {
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startDegrees + (endDegrees - startDegrees) * i / segments);
            x[offset] = centerX + (float) Math.cos(angle) * radius;
            y[offset] = centerY + (float) Math.sin(angle) * radius;
            offset++;
        }
        return offset;
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y,
                               float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha);
    }
}
