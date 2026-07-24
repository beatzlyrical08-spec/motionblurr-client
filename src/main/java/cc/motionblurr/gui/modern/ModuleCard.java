package cc.motionblurr.gui.modern;

import cc.motionblurr.module.Module;
import cc.motionblurr.module.modules.client.ClickGUIModule;
import cc.motionblurr.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public final class ModuleCard {
    private final Module module;

    public ModuleCard(Module module) {
        this.module = module;
    }

    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha, FontRenderer headerFont, FontRenderer smallFont) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean active = module.isEnabled();
        Color base = hovered ? ColorPalette.withAlpha(ColorPalette.HOVER, (int) (220 * alpha)) : ColorPalette.withAlpha(ColorPalette.PANEL, (int) (190 * alpha));
        Color border = active ? ColorPalette.withAlpha(ColorPalette.ACCENT, (int) (180 * alpha)) : ColorPalette.withAlpha(ColorPalette.BORDER, (int) (60 * alpha));
        Renderer.drawRoundedRect(context, x, y, width, height, 18, base);
        context.drawBorder(x, y, width, height, border.getRGB());

        Renderer.drawText(context, headerFont, module.getDisplayName(), x + 16, y + 16, active ? ColorPalette.TEXT : ColorPalette.SECONDARY);
        if (module.getDescription() != null && !module.getDescription().isEmpty()) {
            Renderer.drawText(context, smallFont, module.getDescription(), x + 16, y + 38, ColorPalette.SECONDARY);
        }

        int toggleX = x + width - 44;
        int toggleY = y + 16;
        Renderer.drawRoundedRect(context, toggleX, toggleY, 30, 16, 8, active ? ColorPalette.ACCENT : ColorPalette.PANEL_ALT);
        context.fill(toggleX + (active ? 14 : 2), toggleY + 2, toggleX + (active ? 28 : 16), toggleY + 14, ColorPalette.TEXT.getRGB());
    }

    public Module getModule() {
        return module;
    }
}
