package cc.vanishclient.gui.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Real NanoVG OpenGL 3 renderer for the active ClickGUI.
 *
 * <p>The context and font memory are process-long resources. Initialization is
 * lazy and restricted to Minecraft's render thread after the window exists.</p>
 */
public final class NanoVGRenderer {
    public static final String ARIAL_RESOURCE = "assets/vanishclient/fonts/Arial.ttf";
    public static final String ARIAL_FACE = "vanishclient-arial";
    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/NanoVG");
    private static final NanoVGRenderer INSTANCE = new NanoVGRenderer();

    private long vg;
    private int arialFont = -1;
    private ByteBuffer arialData;
    private boolean initialized;
    private boolean disabled;
    private boolean failureLogged;
    private boolean inFrame;

    private NanoVGRenderer() {
    }

    public static NanoVGRenderer getInstance() {
        return INSTANCE;
    }

    public boolean beginFrame(float logicalWidth, float logicalHeight) {
        if (!initializeIfReady() || inFrame || logicalWidth <= 0.0F || logicalHeight <= 0.0F) return false;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            int framebufferWidth = client.getWindow().getFramebufferWidth();
            float pixelRatio = framebufferWidth / logicalWidth;
            nvgBeginFrame(vg, logicalWidth, logicalHeight, pixelRatio);
            inFrame = true;
            return true;
        } catch (Throwable failure) {
            fail(failure);
            return false;
        }
    }

    public void endFrame() {
        if (!inFrame) return;
        try {
            nvgResetScissor(vg);
            nvgEndFrame(vg);
        } catch (Throwable failure) {
            fail(failure);
        } finally {
            inFrame = false;
            restoreMinecraftState();
        }
    }

    public void abortFrame() {
        if (!inFrame) return;
        try {
            nvgCancelFrame(vg);
        } catch (Throwable failure) {
            fail(failure);
        } finally {
            inFrame = false;
            restoreMinecraftState();
        }
    }

    public boolean isInFrame() {
        return inFrame && initialized && !disabled;
    }

    long contextHandle() {
        requireFrame();
        return vg;
    }

    public void disableAfterRenderFailure(Throwable failure) {
        if (inFrame) {
            try {
                nvgCancelFrame(vg);
            } catch (Throwable cancelFailure) {
                failure.addSuppressed(cancelFailure);
            } finally {
                inFrame = false;
                restoreMinecraftState();
            }
        }
        fail(failure);
    }

    public void roundedRect(float x, float y, float width, float height, float radius, int argb) {
        requireFrame();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, Math.max(0.0F, width), Math.max(0.0F, height), Math.max(0.0F, radius));
            nvgFillColor(vg, color(stack, argb));
            nvgFill(vg);
        }
    }

    public void roundedRectOutline(float x, float y, float width, float height, float radius, float strokeWidth, int argb) {
        requireFrame();
        if (width <= 0.0F || height <= 0.0F || strokeWidth <= 0.0F || ((argb >>> 24) & 255) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float inset = strokeWidth * 0.5F;
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + inset, y + inset,
                    Math.max(0.0F, width - strokeWidth),
                    Math.max(0.0F, height - strokeWidth),
                    Math.max(0.0F, radius - inset));
            nvgStrokeWidth(vg, strokeWidth);
            nvgStrokeColor(vg, color(stack, argb));
            nvgStroke(vg);
        }
    }

    public void rect(float x, float y, float width, float height, int argb) {
        requireFrame();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            nvgBeginPath(vg);
            nvgRect(vg, x, y, Math.max(0.0F, width), Math.max(0.0F, height));
            nvgFillColor(vg, color(stack, argb));
            nvgFill(vg);
        }
    }

    public void circle(float centerX, float centerY, float radius, int argb) {
        requireFrame();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            nvgBeginPath(vg);
            nvgCircle(vg, centerX, centerY, Math.max(0.0F, radius));
            nvgFillColor(vg, color(stack, argb));
            nvgFill(vg);
        }
    }

    public void shadow(float x, float y, float width, float height, float radius, int argb, float blur) {
        requireFrame();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor inner = color(stack, argb);
            NVGColor outer = color(stack, argb & 0x00FFFFFF);
            NVGPaint paint = NVGPaint.malloc(stack);
            nvgBoxGradient(vg, x, y, width, height, radius, blur, inner, outer, paint);
            nvgBeginPath(vg);
            nvgRect(vg, x - blur, y - blur, width + blur * 2.0F, height + blur * 2.0F);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }
    }

    public void text(String text, float x, float topY, float size, int argb) {
        requireFrame();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            configureText(size);
            nvgFillColor(vg, color(stack, argb));
            nvgText(vg, x, topY, text == null ? "" : text);
        }
    }

    public void centeredText(String text, float centerX, float topY, float size, int argb) {
        text(text, centerX - textWidth(text, size) / 2.0F, topY, size, argb);
    }

    public float textWidth(String text, float size) {
        requireFrame();
        configureText(size);
        return nvgTextBounds(vg, 0.0F, 0.0F, text == null ? "" : text, (float[]) null);
    }

    public float fontHeight(float size) {
        requireFrame();
        configureText(size);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ascender = stack.mallocFloat(1);
            var descender = stack.mallocFloat(1);
            var lineHeight = stack.mallocFloat(1);
            nvgTextMetrics(vg, ascender, descender, lineHeight);
            return lineHeight.get(0);
        }
    }

    public void pushScissor(float x, float y, float width, float height) {
        requireFrame();
        nvgSave(vg);
        nvgIntersectScissor(vg, x, y, Math.max(0.0F, width), Math.max(0.0F, height));
    }

    public void popScissor() {
        requireFrame();
        nvgRestore(vg);
    }

    public void save() {
        requireFrame();
        nvgSave(vg);
    }

    public void restore() {
        requireFrame();
        nvgRestore(vg);
    }

    public void translate(float x, float y) {
        requireFrame();
        nvgTranslate(vg, x, y);
    }

    public void scale(float x, float y) {
        requireFrame();
        nvgScale(vg, x, y);
    }

    public void globalAlpha(float alpha) {
        requireFrame();
        nvgGlobalAlpha(vg, Math.max(0.0F, Math.min(1.0F, alpha)));
    }

    private boolean initializeIfReady() {
        if (initialized && !disabled) return true;
        if (disabled) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || !RenderSystem.isOnRenderThread()) return false;

        try {
            vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
            if (vg == MemoryUtil.NULL) throw new IllegalStateException("nvgCreate(OpenGL3) returned NULL");

            arialData = readDirectResource(ARIAL_RESOURCE);
            arialFont = nvgCreateFontMem(vg, ARIAL_FACE, arialData, false);
            if (arialFont < 0) {
                throw new IllegalStateException("NanoVG rejected font resource: " + ARIAL_RESOURCE);
            }
            initialized = true;
            return true;
        } catch (Throwable failure) {
            fail(failure);
            return false;
        }
    }

    private ByteBuffer readDirectResource(String path) throws IOException {
        try (InputStream stream = NanoVGRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing font resource: " + path);
            byte[] bytes = stream.readAllBytes();
            if (bytes.length == 0) throw new IllegalStateException("Empty font resource: " + path);
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        }
    }

    private void configureText(float size) {
        nvgFontFaceId(vg, arialFont);
        nvgFontSize(vg, Math.max(1.0F, size));
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgFontBlur(vg, 0.0F);
        nvgTextLetterSpacing(vg, 0.0F);
    }

    private NVGColor color(MemoryStack stack, int argb) {
        return NVGColor.malloc(stack)
                .r(((argb >>> 16) & 255) / 255.0F)
                .g(((argb >>> 8) & 255) / 255.0F)
                .b((argb & 255) / 255.0F)
                .a(((argb >>> 24) & 255) / 255.0F);
    }

    private void requireFrame() {
        if (!isInFrame()) throw new IllegalStateException("NanoVG operation outside an active frame");
    }

    private void restoreMinecraftState() {
        RenderSystem.disableScissor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void fail(Throwable failure) {
        disabled = true;
        initialized = false;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.error("NanoVG ClickGUI disabled; DrawContext fallback will be used", failure);
        }
    }
}
