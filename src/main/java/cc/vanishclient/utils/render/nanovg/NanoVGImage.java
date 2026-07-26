package cc.vanishclient.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NanoVGImage {
    private static final Map<Integer, ImageData> imageCache = new HashMap<>();

    private record ImageData(Identifier identifier, int width, int height) {
    }

    public static int loadImage(String path) {
        if (!NanoVGContext.isValid()) {
            NanoVGContext.init();
        }

        int key = path.hashCode();
        if (imageCache.containsKey(key)) {
            return key;
        }

        Identifier textureId = createTextureIdentifier(path);
        if (textureId == null) {
            return -1;
        }

        try (InputStream inputStream = getResourceStream(path)) {
            if (inputStream == null) {
                return -1;
            }

            NativeImage image = NativeImage.read(inputStream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
            imageCache.put(key, new ImageData(textureId, image.getWidth(), image.getHeight()));
            return key;
        } catch (IOException ignored) {
            return -1;
        }
    }

    public static void drawImage(int imageId, float x, float y, float width, float height, Color tint) {
        if (!NanoVGFrameManager.isInFrame() || !NanoVGContext.isValid() || imageId == -1) {
            return;
        }

        ImageData data = imageCache.get(imageId);
        if (data == null) {
            return;
        }

        DrawContext context = NanoVGFrameManager.getContext();
        if (context == null) {
            return;
        }

        float red = tint.getRed() / 255f;
        float green = tint.getGreen() / 255f;
        float blue = tint.getBlue() / 255f;
        float alpha = tint.getAlpha() / 255f;

        RenderSystem.setShaderColor(red, green, blue, alpha);
        context.drawTexture(RenderLayer::getGuiTextured, data.identifier(), Math.round(x), Math.round(y), 0f, 0f, Math.round(width), Math.round(height), data.width(), data.height());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static Identifier createTextureIdentifier(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String sanitized = path.replaceFirst("^assets/", "");
        String namespace = "minecraft";
        String resourcePath = sanitized;

        if (sanitized.contains("/")) {
            int index = sanitized.indexOf('/');
            namespace = sanitized.substring(0, index);
            resourcePath = sanitized.substring(index + 1);
        }

        return Identifier.of(namespace, resourcePath);
    }

    private static InputStream getResourceStream(String path) {
        InputStream is = NanoVGImage.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        }
        return is;
    }
}


