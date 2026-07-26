package cc.vanishclient.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import cc.vanishclient.VanishClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NanoVGImage {
    public static final int INVALID_IMAGE = -1;

    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/Images");
    private static final Map<String, Integer> pathToHandle = new HashMap<>();
    private static final Map<Integer, ImageData> handleToImage = new HashMap<>();
    private static final Set<String> warnedFailures = new HashSet<>();
    private static int nextHandle = 1;

    private record ImageData(String resourcePath, Identifier identifier, int width, int height) {
    }

    public static int loadImage(String path) {
        String normalizedPath = normalizeResourcePath(path);
        if (normalizedPath == null) {
            warnLoadFailure(String.valueOf(path), false, false, INVALID_IMAGE, 0, 0,
                    new IllegalArgumentException("Image resource path is empty"));
            return INVALID_IMAGE;
        }
        if (normalizedPath.endsWith(".svg")) {
            warnLoadFailure(normalizedPath, false, false, INVALID_IMAGE, 0, 0,
                    new IllegalArgumentException("SVG resources are not raster images; use a pre-rendered PNG"));
            return INVALID_IMAGE;
        }

        Integer cached = pathToHandle.get(normalizedPath);
        if (cached != null) {
            return cached;
        }

        Identifier textureId = createTextureIdentifier(normalizedPath);
        if (textureId == null) {
            warnLoadFailure(normalizedPath, false, false, INVALID_IMAGE, 0, 0,
                    new IllegalArgumentException("Unable to create texture identifier"));
            return INVALID_IMAGE;
        }

        boolean streamExists = false;
        try (InputStream inputStream = getResourceStream(normalizedPath)) {
            if (inputStream == null) {
                warnLoadFailure(normalizedPath, false, false, INVALID_IMAGE, 0, 0, null);
                return INVALID_IMAGE;
            }
            streamExists = true;

            NativeImage image = NativeImage.read(inputStream);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                image.close();
                warnLoadFailure(normalizedPath, true, false, INVALID_IMAGE, width, height,
                        new IllegalStateException("Decoded image has invalid dimensions"));
                return INVALID_IMAGE;
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
            int handle = nextHandle++;
            pathToHandle.put(normalizedPath, handle);
            handleToImage.put(handle, new ImageData(normalizedPath, textureId, width, height));
            LOGGER.debug("Loaded image resource path={}, streamExists=true, parsed=true, imageId={}, identifier={}, width={}, height={}.",
                    normalizedPath, handle, textureId, width, height);
            return handle;
        } catch (IOException | RuntimeException failure) {
            warnLoadFailure(normalizedPath, streamExists, false, INVALID_IMAGE, 0, 0, failure);
            return INVALID_IMAGE;
        }
    }

    public static void drawImage(int imageId, float x, float y, float width, float height, Color tint) {
        if (!NanoVGFrameManager.isInFrame() || imageId == INVALID_IMAGE) {
            return;
        }

        DrawContext context = NanoVGFrameManager.getContext();
        if (context == null) {
            return;
        }

        drawImage(context, imageId, x, y, width, height, tint);
    }

    public static boolean drawImage(DrawContext context, int imageId, float x, float y, float width, float height, Color tint) {
        if (context == null || imageId == INVALID_IMAGE || width <= 0.0F || height <= 0.0F || tint == null) {
            return false;
        }

        ImageData data = handleToImage.get(imageId);
        if (data == null || data.width() <= 0 || data.height() <= 0 || tint.getAlpha() <= 0) {
            return false;
        }

        float red = tint.getRed() / 255f;
        float green = tint.getGreen() / 255f;
        float blue = tint.getBlue() / 255f;
        float alpha = tint.getAlpha() / 255f;

        RenderSystem.setShaderColor(red, green, blue, alpha);
        try {
            context.drawTexture(RenderLayer::getGuiTextured, data.identifier(), Math.round(x), Math.round(y),
                    0f, 0f, Math.round(width), Math.round(height), data.width(), data.height());
            return true;
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    public static void cleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getTextureManager() != null) {
            for (ImageData data : handleToImage.values()) {
                client.getTextureManager().destroyTexture(data.identifier());
            }
        }
        pathToHandle.clear();
        handleToImage.clear();
        warnedFailures.clear();
    }

    private static Identifier createTextureIdentifier(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String sanitized = path.replace('\\', '/').replaceFirst("^assets/", "");
        String namespace = VanishClient.MOD_ID;
        String resourcePath = sanitized;

        if (sanitized.contains(":")) {
            int index = sanitized.indexOf(':');
            namespace = sanitized.substring(0, index);
            resourcePath = sanitized.substring(index + 1);
        } else if (sanitized.contains("/")) {
            int index = sanitized.indexOf('/');
            namespace = sanitized.substring(0, index);
            resourcePath = sanitized.substring(index + 1);
        }

        return Identifier.of(namespace, resourcePath);
    }

    private static String normalizeResourcePath(String path) {
        if (path == null || path.isBlank()) return null;
        return path.replace('\\', '/').replaceFirst("^/+", "");
    }

    private static InputStream getResourceStream(String path) {
        InputStream is = NanoVGImage.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        }
        return is;
    }

    private static void warnLoadFailure(String path, boolean streamExists, boolean parsed, int imageId, int width, int height, Throwable failure) {
        if (!warnedFailures.add(path)) return;
        if (failure == null) {
            LOGGER.warn("Failed to load image resource path={}, streamExists={}, parsed={}, imageId={}, width={}, height={}.",
                    path, streamExists, parsed, imageId, width, height);
        } else {
            LOGGER.warn("Failed to load image resource path={}, streamExists={}, parsed={}, imageId={}, width={}, height={}.",
                    path, streamExists, parsed, imageId, width, height, failure);
        }
    }
}

