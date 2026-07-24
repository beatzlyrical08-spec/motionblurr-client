package cc.motionblurr.gui.modern;

import cc.motionblurr.module.Module;
import cc.motionblurr.module.setting.*;
import cc.motionblurr.utils.render.font.fonts.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.List;

public final class SettingsPanel {
    private Module selectedModule;
    private Setting selectedSetting;

    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float alpha, FontRenderer titleFont, FontRenderer bodyFont, FontRenderer smallFont) {
        Renderer.drawRoundedRect(context, x, y, width, height, 24, ColorPalette.withAlpha(ColorPalette.PANEL, (int) (210 * alpha)));
        context.drawBorder(x, y, width, height, ColorPalette.withAlpha(ColorPalette.BORDER, (int) (70 * alpha)).getRGB());

        if (selectedModule == null) {
            Renderer.drawText(context, titleFont, "No module selected", x + 24, y + 24, ColorPalette.TEXT);
            Renderer.drawText(context, bodyFont, "Choose a module from the list to inspect its settings.", x + 24, y + 56, ColorPalette.SECONDARY);
            return;
        }

        Renderer.drawText(context, titleFont, selectedModule.getName(), x + 24, y + 24, ColorPalette.TEXT);
        Renderer.drawText(context, bodyFont, selectedModule.getDescription(), x + 24, y + 56, ColorPalette.SECONDARY);

        int currentY = y + 112;
        List<Setting> settings = selectedModule.getSettings();
        if (settings != null) {
            for (Setting setting : settings) {
                if (currentY + 56 > y + height - 24) break;
                renderSettingCard(context, setting, x + 16, currentY, width - 32, 48, mouseX, mouseY, alpha, bodyFont, smallFont);
                currentY += 58;
            }
        }
    }

    private void renderSettingCard(DrawContext context, Setting setting, int x, int y, int width, int height, int mouseX, int mouseY, float alpha, FontRenderer bodyFont, FontRenderer smallFont) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean active = selectedSetting == setting;
        Color base = active ? ColorPalette.withAlpha(ColorPalette.HOVER, (int) (200 * alpha)) : ColorPalette.withAlpha(ColorPalette.PANEL_ALT, (int) (180 * alpha));
        Renderer.drawRoundedRect(context, x, y, width, height, 16, base);
        Renderer.drawText(context, bodyFont, setting.getName(), x + 12, y + 12, ColorPalette.TEXT);
        if (setting instanceof BooleanSetting booleanSetting) {
            boolean isOn = booleanSetting.getValue();
            Renderer.drawRoundedRect(context, x + width - 44, y + 10, 30, 16, 8, isOn ? ColorPalette.ACCENT : ColorPalette.PANEL);
            context.fill(x + width - 30 + (isOn ? -12 : 0), y + 12, x + width - 16 + (isOn ? -12 : 0), y + 24, ColorPalette.TEXT.getRGB());
        } else if (setting instanceof NumberSetting numberSetting) {
            Renderer.drawText(context, smallFont, String.format("%.1f", numberSetting.getValue()), x + width - 72, y + 12, ColorPalette.SECONDARY);
        } else if (setting instanceof ModeSetting modeSetting) {
            Renderer.drawText(context, smallFont, modeSetting.getMode(), x + width - 112, y + 12, ColorPalette.SECONDARY);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            Renderer.drawText(context, smallFont, keybindSetting.isListening() ? "..." : String.valueOf(keybindSetting.getKeyCode()), x + width - 80, y + 12, ColorPalette.SECONDARY);
        } else if (setting instanceof ColorSetting colorSetting) {
            context.fill(x + width - 28, y + 8, x + width - 12, y + 24, colorSetting.getValue().getRGB());
        }
    }

    public boolean handleClick(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (selectedModule == null) {
            return false;
        }

        int currentY = y + 112;
        List<Setting> settings = selectedModule.getSettings();
        if (settings == null) {
            return false;
        }

        for (Setting setting : settings) {
            if (mouseX >= x + 16 && mouseX <= x + width - 16 && mouseY >= currentY && mouseY <= currentY + 48) {
                selectedSetting = setting;
                if (setting instanceof BooleanSetting booleanSetting) {
                    booleanSetting.toggle();
                } else if (setting instanceof NumberSetting numberSetting) {
                    if (mouseX < x + width - 16 - ((width - 32) / 2)) {
                        numberSetting.setValue(numberSetting.getValue() - numberSetting.getIncrement());
                    } else {
                        numberSetting.setValue(numberSetting.getValue() + numberSetting.getIncrement());
                    }
                } else if (setting instanceof ModeSetting modeSetting) {
                    modeSetting.cycle();
                } else if (setting instanceof KeybindSetting keybindSetting) {
                    keybindSetting.toggleListening();
                }
                return true;
            }
            currentY += 58;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedSetting instanceof KeybindSetting keybindSetting && keybindSetting.isListening()) {
            keybindSetting.setKeyCode(keyCode);
            keybindSetting.toggleListening();
            return true;
        }
        return false;
    }

    public void setSelectedModule(Module module) {
        this.selectedModule = module;
        this.selectedSetting = null;
    }
}
