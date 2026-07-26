package cc.vanishclient.gui;

import cc.vanishclient.VanishClient;
import cc.vanishclient.gui.animation.SmoothAnimation;
import cc.vanishclient.gui.icons.IconKey;
import cc.vanishclient.gui.icons.ModuleIconRegistry;
import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.setting.BooleanSetting;
import cc.vanishclient.module.setting.ColorSetting;
import cc.vanishclient.module.setting.KeybindSetting;
import cc.vanishclient.module.setting.ModeSetting;
import cc.vanishclient.module.setting.NumberSetting;
import cc.vanishclient.module.setting.RangeSetting;
import cc.vanishclient.module.setting.Setting;
import cc.vanishclient.module.setting.StringSetting;
import cc.vanishclient.profiles.ProfileManager;
import cc.vanishclient.utils.friend.FriendManager;
import cc.vanishclient.utils.keybinding.KeyUtils;
import cc.vanishclient.utils.render.RenderUtils;
import cc.vanishclient.gui.nanovg.NanoVGRenderer;
import cc.vanishclient.gui.nanovg.SvgIconManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClickGui extends Screen {
    private static final boolean NANOVG_VALIDATION_MODE = false;
    private final NanoVGRenderer nanoVG = NanoVGRenderer.getInstance();
    private final SvgIconManager svgIcons = SvgIconManager.getInstance();
    private final ModuleIconRegistry moduleIcons = ModuleIconRegistry.getInstance();
    private static final int BACKDROP = 0xD9050610;
    private static final int WINDOW = 0xF2181A24;
    private static final int GLASS = 0xF21D202C;
    private static final int GLASS_DARK = 0xF21B1D29;
    private static final int CARD = 0xF2242734;
    private static final int CARD_HOVER = 0xF22B2F40;
    private static final int ROW = 0xF2242734;
    private static final int ROW_HOVER = 0xF22B2F40;
    private int ACCENT = 0xFF4C8DFF;
    private int ACCENT_2 = 0xFF2E65C8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_SOFT = 0xFFE8EAF4;
    private static final int TEXT_MUTED = 0xFF9EA3B7;
    private static final int TEXT_DIM = 0xFF696F86;
    private static final int GREEN = 0xFF55F6A8;
    private static final int RED = 0xFFFF5D86;

    private static final int GUI_MARGIN = 20;
    private static final int SIDEBAR_WIDTH = 126;
    private static final int TOPBAR_HEIGHT = 38;
    private static final int BOTTOMBAR_HEIGHT = 30;
    private static final int CARD_HEIGHT = 28;
    private static final int SETTING_HEIGHT = 34;
    private static final int GAP = 5;
    private static final String[] SETTINGS_TABS = {"General", "Targets", "Weapon", "Rotation", "Render"};

    private final Map<Category, Float> categoryHoverAnimations = new EnumMap<>(Category.class);
    private final Map<Module, Float> moduleHoverAnimations = new IdentityHashMap<>();
    private final Map<Module, Float> moduleToggleAnimations = new IdentityHashMap<>();
    private final Map<Module, Float> moduleSelectAnimations = new IdentityHashMap<>();
    private final Map<Module, Float> favoriteAnimations = new IdentityHashMap<>();
    private final Map<Setting, Float> settingHoverAnimations = new IdentityHashMap<>();
    private final Map<Setting, Float> sliderAnimations = new IdentityHashMap<>();
    private final Map<Setting, Float> settingToggleAnimations = new IdentityHashMap<>();
    private final Map<Object, Float> stableHoverAnimations = new HashMap<>();
    private final Set<Module> favorites = new HashSet<>();

    private Category selectedCategory = Category.COMBAT;
    private ViewMode viewMode = ViewMode.CATEGORY;
    private GuiSize guiSize = GuiSize.SMALL;
    private AccentPreset accentPreset = AccentPreset.BLUE;
    private Module selectedModule;
    private Module popupModule;
    private KeybindSetting listeningKeybind;
    private String searchText = "";
    private String profileInput = "";
    private String friendInput = "";
    private String selectedProfile = "Default";
    private String modalProfileInput = "";
    private String statusMessage = "";
    private long statusMessageUntil;
    private boolean searchFocused;
    private boolean profileFocused;
    private boolean profileCreateModal;
    private boolean profileCreateFocused;
    private boolean confirmDeleteProfile;
    private boolean friendFocused;
    private double moduleScroll;
    private double popupScroll;
    private NumberSetting draggingNumberSetting;
    private int draggingSliderX;
    private int draggingSliderWidth;
    private int popupX;
    private int popupY;
    private int popupW;
    private int popupH;
    private long popupAnimationStart;
    private boolean popupClosing;
    private float popupAnimationProgress;
    private float searchFocusAnimation;
    private float pulseAnimation;
    private float guiOpenAnimation;
    private long lastRenderNanos = System.nanoTime();
    private float animationDeltaSeconds = 1.0F / 60.0F;

    public ClickGui() {
        super(Text.literal("VanishClient"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        ProfileManager manager = VanishClient.INSTANCE.getProfileManager();
        selectedProfile = manager.getActiveProfile();
        ACCENT = accentPreset.color;
        ACCENT_2 = accentPreset.secondary;
        guiOpenAnimation = 0.0F;
        lastRenderNanos = System.nanoTime();
        super.init();
    }

    @Override
    public void close() {
        stopListening();
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningKeybind != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind.setKeyCode(keyCode);
                markProfileDirty();
            }
            stopListening();
            return true;
        }

        if (profileCreateModal) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeProfileCreateModal(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                createProfileFromModal();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !modalProfileInput.isEmpty()) {
                modalProfileInput = modalProfileInput.substring(0, modalProfileInput.length() - 1);
                return true;
            }
            return true;
        }

        if ((profileFocused || friendFocused) && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (profileFocused && !profileInput.isEmpty()) {
                profileInput = profileInput.substring(0, profileInput.length() - 1);
            } else if (friendFocused && !friendInput.isEmpty()) {
                friendInput = friendInput.substring(0, friendInput.length() - 1);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (popupModule != null) {
                closeSettingsPopup();
                return true;
            }
            if (searchFocused || profileFocused || friendFocused) {
                searchFocused = false;
                profileFocused = false;
                friendFocused = false;
                return true;
            }
            close();
            return true;
        }

        if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                moduleScroll = 0;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            moduleScroll -= 34;
            clampScrolls();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moduleScroll += 34;
            clampScrolls();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (profileCreateModal && !Character.isISOControl(chr)) {
            modalProfileInput += chr;
            return true;
        }
        if (profileFocused && !Character.isISOControl(chr)) {
            profileInput += chr;
            return true;
        }
        if (friendFocused && !Character.isISOControl(chr)) {
            friendInput += chr;
            return true;
        }
        if (searchFocused && !Character.isISOControl(chr)) {
            searchText += chr;
            moduleScroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (draggingNumberSetting != null) {
            return true;
        }
        Layout layout = getLayout();
        if (popupModule != null && isHovered(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
            popupScroll -= verticalAmount * 30.0D;
            clampScrolls();
            return true;
        }
        if (isHovered(mouseX, mouseY, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH)) {
            moduleScroll -= verticalAmount * 36.0D;
            clampScrolls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        animationDeltaSeconds = clamp((now - lastRenderNanos) / 1_000_000_000.0F, 0.0F, 0.05F);
        lastRenderNanos = now;
        pulseAnimation += animationDeltaSeconds * 2.1F;
        searchFocusAnimation = animate(searchFocusAnimation, searchFocused ? 1.0F : 0.0F, 0.18F);
        guiOpenAnimation = animate(guiOpenAnimation, 1.0F, 0.18F);

        Layout layout = getLayout();
        ensureSelectedModule();
        clampScrolls();

        boolean nanoFrame = nanoVG.beginFrame(width, height);
        try {
            if (nanoFrame && NANOVG_VALIDATION_MODE) {
                drawNanoVGValidation(layout);
            } else {
                renderGuiContents(context, layout, mouseX, mouseY);
            }
        } catch (Throwable failure) {
            if (nanoFrame) {
                nanoVG.disableAfterRenderFailure(failure);
                nanoFrame = false;
                renderGuiContents(context, layout, mouseX, mouseY);
            } else {
                throw failure;
            }
        } finally {
            if (nanoFrame) nanoVG.endFrame();
        }
    }

    private void renderGuiContents(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawBackdrop(context);
        boolean transformed = beginGuiOpenTransform(layout);
        try {
            drawWindow(context, layout);
            drawSidebar(context, layout, mouseX, mouseY);
            drawTopBar(context, layout, mouseX, mouseY);
            if (viewMode == ViewMode.CONFIG) {
                drawProfilePanel(context, layout, mouseX, mouseY);
                drawCustomizationPanel(context, layout, mouseX, mouseY);
            } else if (viewMode == ViewMode.FRIENDS) {
                drawFriendPanel(context, layout, mouseX, mouseY);
                drawFriendHelpPanel(context, layout);
            } else if (viewMode == ViewMode.SETTINGS) {
                drawGuiSettingsPage(context, layout, mouseX, mouseY);
            } else {
                drawModulePanel(context, layout, mouseX, mouseY);
                drawSettingsPopup(context, layout, mouseX, mouseY);
            }
            drawBottomBar(context, layout, mouseX, mouseY);
            drawProfileCreateModal(context, layout, mouseX, mouseY);
        } finally {
            if (transformed) nanoVG.restore();
        }
    }

    private boolean beginGuiOpenTransform(Layout layout) {
        if (!nanoVG.isInFrame()) return false;
        float progress = SmoothAnimation.smoothstep(guiOpenAnimation);
        if (progress >= 0.999F) return false;
        float scale = lerp(0.96F, 1.0F, progress);
        float centerX = layout.x + layout.w * 0.5F;
        float centerY = layout.y + layout.h * 0.5F;
        nanoVG.save();
        nanoVG.globalAlpha(progress);
        nanoVG.translate(centerX, centerY + lerp(7.0F, 0.0F, progress));
        nanoVG.scale(scale, scale);
        nanoVG.translate(-centerX, -centerY);
        return true;
    }

    private void drawNanoVGValidation(Layout layout) {
        float x = layout.x + 120;
        float y = layout.y + 75;
        nanoVG.shadow(x, y, 330, 150, 14, 0x55200040, 18);
        nanoVG.roundedRect(x, y, 330, 150, 14, WINDOW);
        nanoVG.text("VanishClient", x + 18, y + 16, 18, TEXT);
        drawNanoToggle(x + 24, y + 60, true);
        drawNanoToggle(x + 24, y + 88, false);
        nanoVG.roundedRect(x + 105, y + 76, 150, 5, 3, 0xFF30354A);
        nanoVG.roundedRect(x + 105, y + 76, 88, 5, 3, ACCENT);
        nanoVG.circle(x + 193, y + 78.5F, 5, TEXT);
        nanoVG.roundedRect(x + 275, y + 14, 35, 24, 8, 0xCC252A42);
        nanoVG.centeredText("X", x + 292.5F, y + 20, 10, TEXT);
    }

    private void drawNanoToggle(float x, float y, boolean enabled) {
        nanoVG.roundedRect(x, y, 30, 14, 7, enabled ? ACCENT : 0xFF30354A);
        nanoVG.circle(x + (enabled ? 23 : 7), y + 7, 5, TEXT);
    }

    private void drawBackdrop(DrawContext context) {
        drawRect(context, 0, 0, width, height, BACKDROP);
    }

    private void drawWindow(DrawContext context, Layout layout) {
        if (nanoVG.isInFrame()) {
            nanoVG.shadow(layout.x, layout.y + 3, layout.w, layout.h, 18, 0x33000000, 18);
        }
        RenderUtils.drawRoundedRect(context, layout.x, layout.y, layout.w, layout.h, 18, WINDOW);
        RenderUtils.drawRoundedRect(context, layout.x + 4, layout.y + 4, layout.w - 8, layout.h - 8, 15, 0xF01A1C27);
        drawBorder(context, layout.x, layout.y, layout.w, layout.h, 18, 0x2EFFFFFF);
    }

    private void drawSidebar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.sidebarX, layout.sidebarY, layout.sidebarW, layout.sidebarH, 12, GLASS_DARK);
        drawGlowRect(context, layout.sidebarX + 12, layout.sidebarY + 12, 24, 24, 8, withAlpha(ACCENT, 58), 3);
        RenderUtils.drawRoundedRect(context, layout.sidebarX + 13, layout.sidebarY + 13, 22, 22, 7, 0xFF1B1632);
        drawText(context, "MB", layout.sidebarX + 18, layout.sidebarY + 21, ACCENT, false);
        drawTitleText(context, "VanishClient", layout.sidebarX + 44, layout.sidebarY + 13, TEXT);
        drawSmallText(context, "rise above.", layout.sidebarX + 44, layout.sidebarY + 27, TEXT_MUTED);

        int itemX = layout.sidebarX + 8;
        int itemY = layout.sidebarY + 50;
        int itemW = layout.sidebarW - 16;
        int itemH = 23;

        for (Category category : Category.values()) {
            boolean selected = viewMode == ViewMode.CATEGORY && category == selectedCategory;
            if (category == Category.CONFIG) {
                selected = viewMode == ViewMode.CONFIG;
            }
            boolean hovered = isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH);
            float hover = animate(categoryHoverAnimations, category, hovered ? 1.0F : 0.0F, 0.18F);
            int offset = Math.round(lerp(0, 4, easeOutCubic(hover)));

            if (hover > 0.01F) {
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 7, withAlpha(0xFF252A42, (int) (90 * hover)));
            }
            if (selected) {
                drawGlowRect(context, itemX, itemY, itemW, itemH, 7, withAlpha(ACCENT, 50), 2);
                RenderUtils.drawRoundedRect(context, itemX, itemY, itemW, itemH, 7, 0xDD33215B);
                RenderUtils.drawRoundedRect(context, itemX + 3, itemY + 6, 2, itemH - 12, 2, ACCENT);
            }

            drawCategoryMark(context, category, itemX + 10 + offset, itemY + 5, 13, selected, hover);
            drawText(context, category.getName(), itemX + 28 + offset, itemY + 8,
                    selected ? TEXT : lerpColor(TEXT_MUTED, TEXT_SOFT, hover), false);
            itemY += itemH + 4;
        }

        itemY += 2;
        itemY = drawSidebarAction(context, "Favorites", IconKey.STAR_PLUS, viewMode == ViewMode.FAVORITES, itemX, itemY, itemW, itemH, mouseX, mouseY);
        itemY = drawSidebarAction(context, "Friends", IconKey.USER, viewMode == ViewMode.FRIENDS, itemX, itemY, itemW, itemH, mouseX, mouseY);
        drawSidebarAction(context, "Settings", IconKey.SPARKLE, viewMode == ViewMode.SETTINGS, itemX, itemY, itemW, itemH, mouseX, mouseY);

        if (layout.sidebarH > 330) {
            int infoY = layout.sidebarY + layout.sidebarH - 66;
            RenderUtils.drawRoundedRect(context, layout.sidebarX + 8, infoY, layout.sidebarW - 16, 42, 8, 0xAA191D30);
            RenderUtils.drawRoundedRect(context, layout.sidebarX + 16, infoY + 10, 22, 22, 6, 0xFF2A2145);
            drawText(context, "M", layout.sidebarX + 24, infoY + 18, ACCENT, false);
            drawText(context, "VanishClient", layout.sidebarX + 46, infoY + 9, TEXT_SOFT, false);
            drawText(context, "Premium", layout.sidebarX + 46, infoY + 23, ACCENT, false);
        }
        drawSmallText(context, "v1.0.0", layout.sidebarX + 12, layout.sidebarY + layout.sidebarH - 16, TEXT_MUTED);
    }

    private int drawSidebarAction(DrawContext context, String label, IconKey icon, boolean selected, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, h);
        float hover = animate(stableHoverAnimations, label, hovered ? 1.0F : 0.0F, 0.18F);
        int offset = Math.round(lerp(0, 4, easeOutCubic(hover)));

        if (hover > 0.01F) RenderUtils.drawRoundedRect(context, x, y, w, h, 7, withAlpha(0xFF252A42, (int) (90 * hover)));
        if (selected) {
            drawGlowRect(context, x, y, w, h, 7, withAlpha(ACCENT, 50), 2);
            RenderUtils.drawRoundedRect(context, x, y, w, h, 7, 0xDD33215B);
            RenderUtils.drawRoundedRect(context, x + 3, y + 6, 2, h - 12, 2, ACCENT);
        }

        drawSvgIcon(context, icon, x + 10 + offset, y + 5, 13,
                selected ? ACCENT : lerpColor(TEXT_DIM, TEXT_MUTED, hover), 1.0F);
        drawText(context, label, x + 28 + offset, y + 8,
                selected ? TEXT : lerpColor(TEXT_MUTED, TEXT_SOFT, hover), false);
        return y + h + 4;
    }

    private void drawTopBar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(context, layout.topX, layout.topY, layout.topW, layout.topH, 10, 0xF21B1E29);

        int searchX = layout.topX + 8;
        int searchY = layout.topY + 6;
        int searchW = Math.min(190, Math.max(130, layout.moduleW - 12));
        int searchH = 26;
        float hover = isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH) ? 1.0F : 0.0F;
        int searchBorder = withAlpha(ACCENT, (int) (45 + 115 * searchFocusAnimation + 35 * hover));
        drawGlowRect(context, searchX, searchY, searchW, searchH, 10, withAlpha(ACCENT, (int) (16 + 30 * searchFocusAnimation)), 2);
        RenderUtils.drawRoundedRect(context, searchX, searchY, searchW, searchH, 8, 0xAA0F1220);
        drawBorder(context, searchX, searchY, searchW, searchH, 8, searchBorder);
        drawText(context, "Search modules...", searchX + 9, searchY + 9, searchText.isEmpty() ? TEXT_DIM : 0x00000000, false);
        if (!searchText.isEmpty()) {
            drawText(context, trimToWidth(searchText, searchW - 34), searchX + 9, searchY + 9, TEXT_SOFT, false);
        }
        drawText(context, "?", searchX + searchW - 18, searchY + 9, TEXT_MUTED, false);

        int y = layout.topY + 7;
        int x = layout.topX + layout.topW - 108;
        drawTopIconButton(context, "H", x, y, false, mouseX, mouseY);
        drawTopIconButton(context, "S", x + 26, y, false, mouseX, mouseY);
        drawTopIconButton(context, "-", x + 56, y, false, mouseX, mouseY);
        drawTopIconButton(context, "X", x + 82, y, true, mouseX, mouseY);
    }

    private void drawTopIconButton(DrawContext context, String label, int x, int y, boolean accent, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, 22, 22);
        float hover = animate(stableHoverAnimations, label, hovered ? 1.0F : 0.0F, 0.18F);
        RenderUtils.drawRoundedRect(context, x, y, 22, 22, 7, lerpColor(0x44111523, 0xAA1F2436, hover));
        drawCenteredText(context, label, x + 11, y + 7,
                accent ? lerpColor(ACCENT, TEXT, hover) : lerpColor(TEXT_MUTED, TEXT_SOFT, hover));
    }

    private void drawModulePanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawSoftPanel(context, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH, 15);
        String title = viewMode == ViewMode.FAVORITES ? "Favorites" : selectedCategory.getName();
        drawText(context, title, layout.moduleX + 10, layout.moduleY + 10, TEXT, false);

        List<Module> modules = getFilteredModules();
        String count = modules.size() + " modules";
        drawText(context, count, layout.moduleX + layout.moduleW - textWidth(count) - 10, layout.moduleY + 10, TEXT_DIM, false);

        int listX = layout.moduleX + 8;
        int listY = layout.moduleY + 30;
        int listW = layout.moduleW - 16;
        int listH = layout.moduleH - 38;
        pushScissor(context, listX - 4, listY, listW + 8, listH);

        double currentY = listY - moduleScroll;
        for (Module module : modules) {
            if (currentY + CARD_HEIGHT >= listY && currentY <= listY + listH) {
                drawModuleCard(context, module, listX, (int) Math.round(currentY), listW, mouseX, mouseY);
            } else {
                animate(moduleHoverAnimations, module, 0.0F, 0.18F);
                animate(moduleSelectAnimations, module, module == selectedModule ? 1.0F : 0.0F, 0.16F);
            }
            currentY += CARD_HEIGHT + GAP;
        }

        if (modules.isEmpty()) {
            drawText(context, "No modules found.", listX + 4, listY + 8, TEXT_MUTED, false);
        }

        popScissor(context);
        drawModuleScrollBar(context, layout, modules.size());
    }

    private void drawModuleCard(DrawContext context, Module module, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, CARD_HEIGHT);
        float hover = animate(moduleHoverAnimations, module, hovered ? 1.0F : 0.0F, 0.16F);
        float selected = animate(moduleSelectAnimations, module, module == selectedModule || module == popupModule ? 1.0F : 0.0F, 0.14F);
        float toggle = animate(moduleToggleAnimations, module, module.isEnabled() ? 1.0F : 0.0F, 0.16F);
        int drawY = y;
        int bg = lerpColor(CARD, CARD_HOVER, hover);

        RenderUtils.drawRoundedRect(context, x, drawY, w, CARD_HEIGHT, 9, bg);
        if (selected > 0.01F || module.isEnabled()) {
            RenderUtils.drawRoundedRect(context, x, drawY, 3, CARD_HEIGHT, 2, withAlpha(ACCENT, (int) (170 * Math.max(selected, toggle))));
        }

        int iconColor = module.isEnabled()
                ? lerpColor(TEXT_MUTED, ACCENT, Math.max(0.72F, toggle))
                : lerpColor(TEXT_MUTED, TEXT_SOFT, Math.max(hover, selected));
        drawModuleIcon(context, module, x + 8, drawY + 7, 14, iconColor, 1.0F);
        drawText(context, trimToWidth(module.getName(), w - 104), x + 29, drawY + 10,
                lerpColor(TEXT_SOFT, TEXT, Math.max(hover, selected)), false);

        boolean favorite = favorites.contains(module);
        float fav = animate(favoriteAnimations, module, favorite ? 1.0F : 0.0F, 0.18F);
        drawToggle(context, x + w - 58, drawY + 8, toggle, false);
        RenderUtils.drawRoundedRect(context, x + w - 17, drawY + 10, 7, 7, favorite ? 4 : 2,
                lerpColor(TEXT_DIM, ACCENT, Math.max(fav, module == selectedModule ? 0.7F : 0.0F)));
    }

    private void drawSettingsPopup(DrawContext context, Layout layout, int mouseX, int mouseY) {
        if (popupModule == null) return;
        float target = popupClosing ? 0.0F : 1.0F;
        float progress = animate(popupAnimationProgress, target, 0.28F);
        popupAnimationProgress = progress;
        if (popupClosing && progress <= 0.01F) {
            popupModule = null;
            selectedModule = null;
            popupClosing = false;
            popupScroll = 0;
            return;
        }

        Module module = popupModule;
        popupW = Math.min(230, Math.max(190, layout.w / 3));
        popupH = Math.min(layout.moduleH - 14, Math.max(150, module.getSettings().size() * (SETTING_HEIGHT + 6) + 56));
        popupX = clamp(popupX, layout.x + 8, layout.x + layout.w - popupW - 8);
        popupY = clamp(popupY, layout.y + 8, layout.y + layout.h - popupH - 34);
        int alpha = (int) (235 * easeOutCubic(progress));
        int slide = Math.round(6 * (1.0F - easeOutCubic(progress)));
        int x = popupX + slide;
        int y = popupY;

        RenderUtils.drawRoundedRect(context, x, y, popupW, popupH, 10, withAlpha(0xFF191D29, alpha));
        RenderUtils.drawRoundedRect(context, x + 2, y + 2, popupW - 4, popupH - 4, 8, withAlpha(0xFF202432, alpha));
        drawText(context, trimToWidth(module.getName(), popupW - 72), x + 12, y + 13, withAlpha(TEXT, alpha), false);
        drawToggle(context, x + popupW - 42, y + 12,
                moduleToggleAnimations.getOrDefault(module, module.isEnabled() ? 1.0F : 0.0F), false);

        int listX = x + 8;
        int listY = y + 40;
        int listW = popupW - 16;
        int listH = popupH - 48;
        pushScissor(context, listX - 2, listY, listW + 4, listH);
        if (module.getSettings().isEmpty()) {
            drawText(context, "No settings", listX + 4, listY + 8, withAlpha(TEXT_MUTED, alpha), false);
        } else {
            double rowY = listY - popupScroll;
            for (Setting setting : module.getSettings()) {
                if (rowY + SETTING_HEIGHT >= listY && rowY <= listY + listH) {
                    drawSettingRow(context, module, setting, listX, (int) Math.round(rowY), listW, mouseX, mouseY);
                } else {
                    animate(settingHoverAnimations, setting, 0.0F, 0.16F);
                }
                rowY += SETTING_HEIGHT + 6;
            }
        }
        popScissor(context);
    }

    private void drawSettingRow(DrawContext context, Module module, Setting setting, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, SETTING_HEIGHT);
        float hover = animate(settingHoverAnimations, setting, hovered ? 1.0F : 0.0F, 0.16F);
        RenderUtils.drawRoundedRect(context, x, y, w, SETTING_HEIGHT, 8, lerpColor(ROW, ROW_HOVER, hover));
        if (hover > 0.01F) {
            drawBorder(context, x, y, w, SETTING_HEIGHT, 8, withAlpha(ACCENT, (int) (55 * hover)));
        }

        drawText(context, trimToWidth(setting.getName(), w - 76), x + 8, y + 7, TEXT_SOFT, false);

        if (setting instanceof BooleanSetting booleanSetting) {
            float progress = animate(settingToggleAnimations, setting, booleanSetting.getValue() ? 1.0F : 0.0F, 0.17F);
            drawToggle(context, x + w - 36, y + 13, progress, false);
        } else if (setting instanceof ModeSetting modeSetting) {
            drawModeSetting(context, modeSetting, x, y, w);
        } else if (setting instanceof NumberSetting numberSetting) {
            drawNumberSetting(context, numberSetting, setting, x, y, w);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            drawKeybindSetting(context, keybindSetting, x, y, w);
        } else if (setting instanceof ColorSetting colorSetting) {
            drawColorSetting(context, colorSetting, x, y, w);
        } else if (setting instanceof RangeSetting rangeSetting) {
            drawRightText(context, format(rangeSetting.getMinValue()) + " - " + format(rangeSetting.getMaxValue()), x, y + 7, w - 8, TEXT_MUTED);
        } else if (setting instanceof StringSetting stringSetting) {
            drawRightText(context, trimToWidth(stringSetting.getValue(), 70), x, y + 7, w - 8, TEXT_MUTED);
        }
    }

    private void drawModeSetting(DrawContext context, ModeSetting setting, int x, int y, int w) {
        String mode = trimToWidth(setting.getMode(), 92);
        int boxW = Math.min(76, Math.max(48, textWidth(mode) + 16));
        int boxX = x + w - boxW - 8;
        RenderUtils.drawRoundedRect(context, boxX, y + 8, boxW, 22, 7, 0xCC111522);
        drawBorder(context, boxX, y + 8, boxW, 22, 7, 0x558B5CFF);
        drawText(context, trimToWidth(mode, boxW - 12), boxX + 8, y + 15, TEXT_MUTED, false);
    }

    private void drawNumberSetting(DrawContext context, NumberSetting setting, Setting key, int x, int y, int w) {
        SliderBounds bounds = getSliderBounds(x, y, w);
        double range = setting.getMax() - setting.getMin();
        double progress = range <= 0.0D ? 0.0D : clamp((setting.getValue() - setting.getMin()) / range, 0.0D, 1.0D);
        float visualProgress = animate(sliderAnimations, key, (float) progress, 0.18F);
        int fillWidth = (int) Math.round(bounds.width * visualProgress);
        int knobCenter = bounds.x + fillWidth;
        int knobX = clamp(knobCenter - 4, bounds.x - 1, bounds.x + bounds.width - 7);
        RenderUtils.drawRoundedRect(context, bounds.x, bounds.y, bounds.width, bounds.height, bounds.height / 2, 0xFF30354A);
        if (fillWidth > 0) {
            RenderUtils.drawRoundedRect(context, bounds.x, bounds.y, fillWidth, bounds.height, bounds.height / 2, ACCENT);
        }
        RenderUtils.drawRoundedRect(context, knobX, bounds.y - 3, 8, 10, 4, 0xFFF4F0FF);
        RenderUtils.drawRoundedRect(context, x + w - 48, y + 9, 40, 20, 7, 0xCC111522);
        drawRightText(context, format(setting.getValue()), x, y + 15, w - 18, TEXT_MUTED);
    }

    private SliderBounds getSliderBounds(int rowX, int rowY, int rowWidth) {
        return new SliderBounds(rowX + 8, rowY + 27, Math.max(48, rowWidth - 88), 4);
    }

    private void drawKeybindSetting(DrawContext context, KeybindSetting setting, int x, int y, int w) {
        String key = setting == listeningKeybind ? "..." : KeyUtils.getKey(setting.getKeyCode());
        int boxW = Math.min(76, Math.max(46, textWidth(key) + 16));
        int boxX = x + w - boxW - 8;
        RenderUtils.drawRoundedRect(context, boxX, y + 8, boxW, 22, 10, setting == listeningKeybind ? 0xDD352160 : 0xCC111522);
        drawBorder(context, boxX, y + 8, boxW, 22, 10, setting == listeningKeybind ? ACCENT : 0x558B5CFF);
        drawText(context, trimToWidth(key, boxW - 12), boxX + 8, y + 15, setting == listeningKeybind ? TEXT : TEXT_MUTED, false);
    }

    private void drawColorSetting(DrawContext context, ColorSetting setting, int x, int y, int w) {
        int swatchX = x + w - 32;
        RenderUtils.drawRoundedRect(context, swatchX, y + 9, 22, 20, 7, setting.getRGB());
        drawBorder(context, swatchX, y + 9, 22, 20, 7, 0x88FFFFFF);
    }

    private void drawToggle(DrawContext context, int x, int y, float progress, boolean large) {
        int toggleW = large ? 32 : 28;
        int toggleH = large ? 15 : 13;
        int knob = toggleH - 4;
        float eased = easeOutCubic(progress);
        int bg = lerpColor(0xFF303548, ACCENT, eased);
        if (progress > 0.01F) {
            drawGlowRect(context, x, y, toggleW, toggleH, toggleH / 2, withAlpha(ACCENT, (int) (28 * progress)), 2);
        }
        RenderUtils.drawRoundedRect(context, x, y, toggleW, toggleH, toggleH / 2, bg);
        int knobX = Math.round(lerp(x + 2, x + toggleW - knob - 2, eased));
        RenderUtils.drawRoundedRect(context, knobX, y + 2, knob, knob, knob / 2, lerpColor(RED, TEXT, eased));
    }

    private void drawProfilePanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawSoftPanel(context, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH, 15);
        drawText(context, "Profiles", layout.moduleX + 16, layout.moduleY + 15, TEXT, false);
        ProfileManager manager = VanishClient.INSTANCE.getProfileManager();
        String activeProfile = manager.getActiveProfile();
        if (manager.isDirty()) {
            drawText(context, "Unsaved changes", layout.moduleX + layout.moduleW - 108, layout.moduleY + 15, ACCENT, false);
        }

        int inputX = layout.moduleX + 14;
        int inputY = layout.moduleY + 42;
        int inputW = layout.moduleW - 64;
        drawInput(context, inputX, inputY, inputW, 26, profileInput, "Import path", profileFocused);
        drawPill(context, layout.moduleX + layout.moduleW - 42, inputY, 28, 26, "+", true);

        int buttonY = inputY + 34;
        int buttonW = Math.max(46, (layout.moduleW - 44) / 4);
        drawPill(context, layout.moduleX + 14, buttonY, buttonW, 24, "Save", true);
        drawPill(context, layout.moduleX + 18 + buttonW, buttonY, buttonW, 24, "Load", true);
        drawPill(context, layout.moduleX + 22 + buttonW * 2, buttonY, buttonW, 24, "Import", true);
        drawPill(context, layout.moduleX + 26 + buttonW * 3, buttonY, buttonW, 24, confirmDeleteProfile ? "Confirm" : "Delete", true);

        int y = inputY + 68;
        for (String profile : getProfiles()) {
            boolean active = profile.equalsIgnoreCase(activeProfile);
            boolean selected = profile.equalsIgnoreCase(selectedProfile);
            boolean hovered = isHovered(mouseX, mouseY, layout.moduleX + 14, y, layout.moduleW - 28, 34);
            float hover = animate(stableHoverAnimations, profile, hovered ? 1.0F : 0.0F, 0.16F);
            float fill = Math.max(hover, active ? 0.75F : selected ? 0.45F : 0.0F);
            RenderUtils.drawRoundedRect(context, layout.moduleX + 14, y, layout.moduleW - 28, 30, 8, lerpColor(ROW, active ? withAlpha(ACCENT, 120) : ROW_HOVER, fill));
            drawText(context, profile, layout.moduleX + 26, y + 13, active ? TEXT : TEXT_SOFT, false);
            drawText(context, active ? "active" : selected ? "selected" : "", layout.moduleX + layout.moduleW - 66, y + 13, active ? TEXT : TEXT_DIM, false);
            y += 35;
            if (y > layout.moduleY + layout.moduleH - 36) break;
        }
        if (statusMessageUntil > System.currentTimeMillis()) {
            drawText(context, trimToWidth(statusMessage, layout.moduleW - 28), layout.moduleX + 16, layout.moduleY + layout.moduleH - 18, TEXT_MUTED, false);
        }
    }

    private void drawCustomizationPanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawSoftPanel(context, layout.settingsX, layout.settingsY, layout.settingsW, layout.settingsH, 15);
        drawText(context, "GUI Settings", layout.settingsX + 16, layout.settingsY + 16, TEXT, false);

        int y = layout.settingsY + 46;
        drawText(context, "Size", layout.settingsX + 16, y, TEXT_MUTED, false);
        y += 16;
        for (GuiSize size : GuiSize.values()) {
            drawOptionRow(context, size, size.label, guiSize == size, layout.settingsX + 14, y, layout.settingsW - 28, mouseX, mouseY);
            y += 34;
        }

        y += 8;
        drawText(context, "Accent", layout.settingsX + 16, y, TEXT_MUTED, false);
        y += 16;
        for (AccentPreset preset : AccentPreset.values()) {
            drawOptionRow(context, preset, preset.label, accentPreset == preset, layout.settingsX + 14, y, layout.settingsW - 28, mouseX, mouseY);
            RenderUtils.drawRoundedRect(context, layout.settingsX + layout.settingsW - 42, y + 8, 20, 14, 5, preset.color);
            y += 34;
            if (y > layout.settingsY + layout.settingsH - 40) break;
        }
    }

    private void drawProfileCreateModal(DrawContext context, Layout layout, int mouseX, int mouseY) {
        if (!profileCreateModal) return;
        drawRect(context, 0, 0, width, height, 0x66000000);
        int modalW = Math.min(260, Math.max(220, layout.w - 80));
        int modalH = 116;
        int modalX = layout.x + (layout.w - modalW) / 2;
        int modalY = layout.y + (layout.h - modalH) / 2;
        RenderUtils.drawRoundedRect(context, modalX, modalY, modalW, modalH, 10, 0xF2202430);
        RenderUtils.drawRoundedRect(context, modalX + 2, modalY + 2, modalW - 4, modalH - 4, 8, 0xF2282C3A);
        drawText(context, "Create profile", modalX + 14, modalY + 14, TEXT, false);
        drawInput(context, modalX + 14, modalY + 40, modalW - 28, 28, modalProfileInput, "Profile name", profileCreateFocused);
        drawPill(context, modalX + modalW - 128, modalY + 78, 54, 24, "Cancel", false);
        drawPill(context, modalX + modalW - 68, modalY + 78, 54, 24, "Create", true);
        if (statusMessageUntil > System.currentTimeMillis()) {
            drawText(context, trimToWidth(statusMessage, modalW - 28), modalX + 14, modalY + 84, RED, false);
        }
    }

    private void drawFriendPanel(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawSoftPanel(context, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH, 15);
        drawText(context, "Friends", layout.moduleX + 16, layout.moduleY + 15, TEXT, false);

        int inputX = layout.moduleX + 14;
        int inputY = layout.moduleY + 42;
        int inputW = layout.moduleW - 82;
        drawInput(context, inputX, inputY, inputW, 30, friendInput, "Player name", friendFocused);
        drawPill(context, layout.moduleX + layout.moduleW - 58, inputY, 44, 30, "+", true);

        int y = inputY + 44;
        List<String> friends = getFriendRows();
        if (friends.isEmpty()) {
            drawText(context, "No friends added yet.", layout.moduleX + 18, y + 8, TEXT_MUTED, false);
            return;
        }

        for (String friend : friends) {
            boolean hovered = isHovered(mouseX, mouseY, layout.moduleX + 14, y, layout.moduleW - 28, 34);
            float hover = animate(stableHoverAnimations, friend, hovered ? 1.0F : 0.0F, 0.16F);
            RenderUtils.drawRoundedRect(context, layout.moduleX + 14, y, layout.moduleW - 28, 34, 10, lerpColor(ROW, ROW_HOVER, hover));
            drawText(context, trimToWidth(friend, layout.moduleW - 92), layout.moduleX + 26, y + 13, TEXT_SOFT, false);
            drawText(context, "x", layout.moduleX + layout.moduleW - 42, y + 13, RED, false);
            y += 40;
            if (y > layout.moduleY + layout.moduleH - 36) break;
        }
    }

    private void drawFriendHelpPanel(DrawContext context, Layout layout) {
        drawSoftPanel(context, layout.settingsX, layout.settingsY, layout.settingsW, layout.settingsH, 15);
        drawText(context, "Friend List", layout.settingsX + 16, layout.settingsY + 16, TEXT, false);
        drawText(context, "MiddleClickFriend uses UUIDs.", layout.settingsX + 16, layout.settingsY + 42, TEXT_MUTED, false);
        drawText(context, "Names added here are kept", layout.settingsX + 16, layout.settingsY + 60, TEXT_MUTED, false);
        drawText(context, "in memory for GUI use.", layout.settingsX + 16, layout.settingsY + 76, TEXT_MUTED, false);
    }

    private void drawGuiSettingsPage(DrawContext context, Layout layout, int mouseX, int mouseY) {
        drawSoftPanel(context, layout.moduleX, layout.moduleY, layout.moduleW, layout.moduleH, 15);
        drawText(context, "Settings", layout.moduleX + 16, layout.moduleY + 16, TEXT, false);
        drawText(context, "Customize the ClickGUI.", layout.moduleX + 16, layout.moduleY + 42, TEXT_MUTED, false);
        drawText(context, "Theme", layout.moduleX + 16, layout.moduleY + 76, TEXT_MUTED, false);
        drawText(context, cc.vanishclient.module.modules.client.ClickGUIModule.theme.getMode(),
                layout.moduleX + 16, layout.moduleY + 94, ACCENT, false);
        drawText(context, "Animation speed follows real time.", layout.moduleX + 16,
                layout.moduleY + 126, TEXT_MUTED, false);
        drawCustomizationPanel(context, layout, mouseX, mouseY);
    }

    private void drawSettingsTabs(DrawContext context, Layout layout) {
        int x = layout.settingsX + 10;
        int y = layout.settingsY + 52;
        int maxRight = layout.settingsX + layout.settingsW - 10;
        drawRect(context, layout.settingsX, y - 10, layout.settingsW, 1, 0x3330354A);
        for (int i = 0; i < SETTINGS_TABS.length; i++) {
            String tab = SETTINGS_TABS[i];
            int tabW = Math.max(38, textWidth(tab) + 10);
            if (x + tabW > maxRight) break;
            boolean active = i == 0;
            float hover = animate(stableHoverAnimations, tab, active ? 1.0F : 0.0F, 0.18F);
            drawText(context, tab, x + 3, y, active ? ACCENT : TEXT_MUTED, false);
            if (hover > 0.01F) {
                RenderUtils.drawRoundedRect(context, x, y + 14, tabW, 2, 1, withAlpha(ACCENT, (int) (205 * hover)));
            }
            x += tabW + 5;
        }
    }

    private void drawBottomBar(DrawContext context, Layout layout, int mouseX, int mouseY) {
        int y = layout.y + layout.h - BOTTOMBAR_HEIGHT;
        RenderUtils.drawRoundedRect(context, layout.x + 6, y, layout.w - 12, BOTTOMBAR_HEIGHT - 4, 9, 0xF21B1D29);
        drawRect(context, layout.x + 1, y, layout.w - 2, 1, 0x33464C65);
        int x = layout.moduleX;
        drawBottomTab(context, "GUI", "[]", x, y + 5, 58, true, mouseX, mouseY);
        drawBottomTab(context, "ArrayList", "=", x + 64, y + 5, 82, false, mouseX, mouseY);
        drawBottomTab(context, "HUD", "O", x + 152, y + 5, 58, false, mouseX, mouseY);

        int pillW = 82;
        int pillX = layout.x + layout.w - pillW - 10;
        RenderUtils.drawRoundedRect(context, pillX, y + 5, pillW, 22, 8, 0xAA111522);
        drawBorder(context, pillX, y + 5, pillW, 22, 8, withAlpha(ACCENT, 40));
        drawText(context, "Dark", pillX + 10, y + 12, TEXT_SOFT, false);
        drawToggle(context, pillX + pillW - 34, y + 10, 1.0F, false);
    }

    private void drawBottomTab(DrawContext context, String label, String icon, int x, int y, int w, boolean active, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, 20);
        float hover = animate(stableHoverAnimations, label, hovered || active ? 1.0F : 0.0F, 0.18F);
        RenderUtils.drawRoundedRect(context, x, y, w, 20, 7, active ? 0xDD241A43 : lerpColor(0x88111523, 0xAA1C2132, hover));
        drawBorder(context, x, y, w, 20, 7, withAlpha(ACCENT, active ? 180 : (int) (55 * hover)));
        RenderUtils.drawRoundedRect(context, x + 9, y + 7, 7, 7, 3, active ? ACCENT : TEXT_MUTED);
        drawText(context, trimToWidth(label, w - 26), x + 24, y + 6, active ? TEXT : TEXT_SOFT, false);
    }

    private void drawModuleIcon(DrawContext context, Module module, int x, int y, int size, int color, float alpha) {
        drawSvgIcon(context, moduleIcons.iconFor(module), x, y, size, color, alpha);
    }

    private void drawCategoryMark(DrawContext context, Category category, int x, int y, int size, boolean selected, float hover) {
        int color = selected ? ACCENT : lerpColor(withAlpha(ACCENT, 165), TEXT_MUTED, hover * 0.2F);
        drawSvgIcon(context, moduleIcons.iconFor(category), x, y, size, color, 1.0F);
    }

    private void drawSvgIcon(DrawContext context, IconKey icon, int x, int y, int size, int color, float alpha) {
        if (nanoVG.isInFrame()) {
            svgIcons.draw(icon, x, y, size, size, color, alpha);
            return;
        }
        int fallbackSize = Math.max(5, size / 2);
        int fallbackX = x + (size - fallbackSize) / 2;
        int fallbackY = y + (size - fallbackSize) / 2;
        RenderUtils.drawRoundedRect(context, fallbackX, fallbackY, fallbackSize, fallbackSize, Math.max(2, fallbackSize / 2), color);
    }

    private String iconForCategory(Category category) {
        return switch (category) {
            case COMBAT -> "X";
            case MOVEMENT -> ">";
            case PLAYER -> "P";
            case RENDER -> "R";
            case MISC -> "?";
            case CLIENT -> "C";
            case CONFIG -> "S";
        };
    }

    private void drawInput(DrawContext context, int x, int y, int w, int h, String value, String placeholder, boolean focused) {
        drawGlowRect(context, x, y, w, h, 9, withAlpha(ACCENT, focused ? 42 : 12), 2);
        RenderUtils.drawRoundedRect(context, x, y, w, h, 9, 0xAA0F1220);
        drawBorder(context, x, y, w, h, 9, withAlpha(ACCENT, focused ? 150 : 55));
        drawText(context, value.isEmpty() ? placeholder : trimToWidth(value, w - 18), x + 10, y + 11, value.isEmpty() ? TEXT_DIM : TEXT_SOFT, false);
    }

    private void drawOptionRow(DrawContext context, Object key, String label, boolean selected, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, 28);
        float hover = animate(stableHoverAnimations, key, hovered ? 1.0F : 0.0F, 0.16F);
        RenderUtils.drawRoundedRect(context, x, y, w, 28, 9, lerpColor(ROW, ROW_HOVER, Math.max(hover, selected ? 0.72F : 0.0F)));
        if (selected) drawBorder(context, x, y, w, 28, 9, withAlpha(ACCENT, 150));
        drawText(context, label, x + 10, y + 10, selected ? TEXT : TEXT_MUTED, false);
    }

    private void drawPill(DrawContext context, int x, int y, int w, int h, String text, boolean active) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, h / 2, active ? 0xDD33215B : 0xAA111522);
        drawBorder(context, x, y, w, h, h / 2, withAlpha(ACCENT, active ? 145 : 70));
        drawCenteredText(context, text, x + w / 2.0F, y + 11, active ? TEXT : TEXT_MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = getLayout();

        if (listeningKeybind != null) {
            listeningKeybind.setKeyCode(-100 - button);
            markProfileDirty();
            stopListening();
            return true;
        }

        if (profileCreateModal) {
            return handleProfileCreateModalClick(mouseX, mouseY, button, layout);
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && isHovered(mouseX, mouseY, layout.topX + layout.topW - 26, layout.topY + 7, 22, 22)) {
            close();
            return true;
        }

        searchFocused = isHovered(mouseX, mouseY, layout.searchX, layout.searchY, layout.searchW, layout.searchH);
        profileFocused = viewMode == ViewMode.CONFIG && isHovered(mouseX, mouseY, layout.moduleX + 14, layout.moduleY + 42, layout.moduleW - 82, 30);
        friendFocused = viewMode == ViewMode.FRIENDS && isHovered(mouseX, mouseY, layout.moduleX + 14, layout.moduleY + 42, layout.moduleW - 82, 30);
        if (searchFocused) {
            return true;
        }
        if (profileFocused || friendFocused) {
            return true;
        }

        if (handleCategoryClick(mouseX, mouseY, layout)) {
            return true;
        }

        if (viewMode == ViewMode.CONFIG) {
            return handleConfigClick(mouseX, mouseY, button, layout) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (viewMode == ViewMode.FRIENDS) {
            return handleFriendClick(mouseX, mouseY, button, layout) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (viewMode == ViewMode.SETTINGS) {
            return handleConfigClick(mouseX, mouseY, button, layout) || super.mouseClicked(mouseX, mouseY, button);
        }

        SettingRow settingRow = findSettingRowAt(mouseX, mouseY, layout);
        if (settingRow != null && popupModule != null) {
            if (settingRow.setting instanceof NumberSetting numberSetting) {
                SliderBounds bounds = getSliderBounds(settingRow.x, settingRow.y, settingRow.width);
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        && isHovered(mouseX, mouseY, bounds.x - 4, bounds.y - 5, bounds.width + 8, bounds.height + 10)
                        && popupAnimationProgress > 0.75F) {
                    draggingNumberSetting = numberSetting;
                    draggingSliderX = bounds.x;
                    draggingSliderWidth = bounds.width;
                    updateDraggedNumber(mouseX);
                    markProfileDirty();
                }
                return true;
            }
            if (settingRow.setting instanceof BooleanSetting
                    && !isHovered(mouseX, mouseY, settingRow.x + settingRow.width - 36, settingRow.y + 10, 28, 13)) {
                return true;
            }
            handleSettingClick(settingRow.setting, button);
            markProfileDirty();
            return true;
        }

        if (popupModule != null && !popupClosing && isHovered(mouseX, mouseY, popupX + popupW - 42, popupY + 12, 28, 13)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            popupModule.toggle();
            markProfileDirty();
            return true;
        }

        Module module = findModuleAt(mouseX, mouseY, layout);
        if (module != null) {
            selectedModule = module;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (popupModule == module && !popupClosing) {
                    closeSettingsPopup();
                } else {
                    openSettingsPopup(module, mouseX, mouseY, layout);
                }
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isModuleFavoriteHovered(mouseX, mouseY, module, layout)) {
                if (favorites.contains(module)) {
                    favorites.remove(module);
                } else {
                    favorites.add(module);
                }
                markProfileDirty();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isModuleToggleHovered(mouseX, mouseY, module, layout)) {
                module.toggle();
                markProfileDirty();
            }
            return true;
        }

        if (popupModule != null && !isHovered(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
            closeSettingsPopup();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingNumberSetting != null) {
            updateDraggedNumber(mouseX);
            markProfileDirty();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingNumberSetting != null) {
            updateDraggedNumber(mouseX);
            draggingNumberSetting = null;
            draggingSliderX = 0;
            draggingSliderWidth = 0;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateDraggedNumber(double mouseX) {
        if (draggingNumberSetting == null || draggingSliderWidth <= 0) return;
        double progress = clamp((mouseX - draggingSliderX) / draggingSliderWidth, 0.0D, 1.0D);
        double min = draggingNumberSetting.getMin();
        double max = draggingNumberSetting.getMax();
        double rawValue = min + progress * (max - min);
        double increment = draggingNumberSetting.getIncrement();
        double snapped = increment > 0.0D
                ? Math.round((rawValue - min) / increment) * increment + min
                : rawValue;
        draggingNumberSetting.setValue(clamp(snapped, min, max));
    }

    private boolean handleCategoryClick(double mouseX, double mouseY, Layout layout) {
        int itemX = layout.sidebarX + 8;
        int itemY = layout.sidebarY + 50;
        int itemW = layout.sidebarW - 16;
        int itemH = 23;

        for (Category category : Category.values()) {
            if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
                if (category == Category.CONFIG) {
                    viewMode = ViewMode.CONFIG;
                    selectedModule = null;
                } else if (selectedCategory != category || viewMode != ViewMode.CATEGORY) {
                    viewMode = ViewMode.CATEGORY;
                    selectedCategory = category;
                    selectedModule = null;
                }
                moduleScroll = 0;
                closeSettingsPopup();
                stopListening();
                return true;
            }
            itemY += itemH + 4;
        }

        itemY += 2;
        if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
            viewMode = ViewMode.FAVORITES;
            selectedModule = null;
            moduleScroll = 0;
            closeSettingsPopup();
            stopListening();
            return true;
        }
        itemY += itemH + 4;

        if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
            viewMode = ViewMode.FRIENDS;
            selectedModule = null;
            moduleScroll = 0;
            closeSettingsPopup();
            stopListening();
            return true;
        }
        itemY += itemH + 4;

        if (isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH)) {
            viewMode = ViewMode.SETTINGS;
            selectedModule = null;
            moduleScroll = 0;
            closeSettingsPopup();
            stopListening();
            return true;
        }
        return false;
    }

    private boolean handleConfigClick(double mouseX, double mouseY, int button, Layout layout) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (isHovered(mouseX, mouseY, layout.moduleX + layout.moduleW - 42, layout.moduleY + 42, 28, 26)) {
            openProfileCreateModal();
            return true;
        }

        int buttonY = layout.moduleY + 76;
        int buttonW = Math.max(46, (layout.moduleW - 44) / 4);
        if (isHovered(mouseX, mouseY, layout.moduleX + 14, buttonY, buttonW, 24)) {
            saveProfile(selectedProfile);
            return true;
        }
        if (isHovered(mouseX, mouseY, layout.moduleX + 18 + buttonW, buttonY, buttonW, 24)) {
            loadProfile(selectedProfile);
            return true;
        }
        if (isHovered(mouseX, mouseY, layout.moduleX + 22 + buttonW * 2, buttonY, buttonW, 24)) {
            if (VanishClient.INSTANCE.getProfileManager().importProfile(profileInput)) {
                selectedProfile = VanishClient.INSTANCE.getProfileManager().getActiveProfile();
                profileInput = "";
                showStatus("Imported profile");
            } else {
                showStatus("Import failed");
            }
            return true;
        }
        if (isHovered(mouseX, mouseY, layout.moduleX + 26 + buttonW * 3, buttonY, buttonW, 24)) {
            if (!confirmDeleteProfile) {
                confirmDeleteProfile = true;
                showStatus("Click Confirm to delete");
            } else if (VanishClient.INSTANCE.getProfileManager().deleteProfile(selectedProfile)) {
                selectedProfile = VanishClient.INSTANCE.getProfileManager().getActiveProfile();
                confirmDeleteProfile = false;
                showStatus("Deleted profile");
            }
            return true;
        }

        int y = layout.moduleY + 110;
        for (String profile : getProfiles()) {
            if (isHovered(mouseX, mouseY, layout.moduleX + 14, y, layout.moduleW - 28, 34)) {
                selectedProfile = profile;
                confirmDeleteProfile = false;
                return true;
            }
            y += 35;
        }

        y = layout.settingsY + 62;
        for (GuiSize size : GuiSize.values()) {
            if (isHovered(mouseX, mouseY, layout.settingsX + 14, y, layout.settingsW - 28, 28)) {
                guiSize = size;
                markProfileDirty();
                return true;
            }
            y += 34;
        }

        y += 24;
        for (AccentPreset preset : AccentPreset.values()) {
            if (isHovered(mouseX, mouseY, layout.settingsX + 14, y, layout.settingsW - 28, 28)) {
                accentPreset = preset;
                ACCENT = preset.color;
                ACCENT_2 = preset.secondary;
                markProfileDirty();
                return true;
            }
            y += 34;
        }
        return false;
    }

    private boolean handleFriendClick(double mouseX, double mouseY, int button, Layout layout) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (isHovered(mouseX, mouseY, layout.moduleX + layout.moduleW - 58, layout.moduleY + 42, 44, 30)) {
            if (!friendInput.isBlank()) {
                FriendManager.addFriendName(friendInput.trim());
                friendInput = "";
            }
            return true;
        }

        int y = layout.moduleY + 86;
        for (String friend : getFriendRows()) {
            if (isHovered(mouseX, mouseY, layout.moduleX + layout.moduleW - 52, y, 38, 34)) {
                removeFriendRow(friend);
                return true;
            }
            y += 40;
        }
        return false;
    }

    private boolean handleProfileCreateModalClick(double mouseX, double mouseY, int button, Layout layout) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        int modalW = Math.min(260, Math.max(220, layout.w - 80));
        int modalH = 116;
        int modalX = layout.x + (layout.w - modalW) / 2;
        int modalY = layout.y + (layout.h - modalH) / 2;
        profileCreateFocused = isHovered(mouseX, mouseY, modalX + 14, modalY + 40, modalW - 28, 28);
        if (isHovered(mouseX, mouseY, modalX + modalW - 128, modalY + 78, 54, 24)) {
            closeProfileCreateModal(false);
            return true;
        }
        if (isHovered(mouseX, mouseY, modalX + modalW - 68, modalY + 78, 54, 24)) {
            createProfileFromModal();
            return true;
        }
        if (!isHovered(mouseX, mouseY, modalX, modalY, modalW, modalH) && modalProfileInput.isBlank()) {
            closeProfileCreateModal(false);
        }
        return true;
    }

    private void openProfileCreateModal() {
        profileCreateModal = true;
        profileCreateFocused = true;
        modalProfileInput = "";
        searchFocused = false;
        profileFocused = false;
        friendFocused = false;
    }

    private void closeProfileCreateModal(boolean keepTypedText) {
        profileCreateModal = false;
        profileCreateFocused = false;
        if (!keepTypedText) {
            modalProfileInput = "";
        }
    }

    private void createProfileFromModal() {
        ProfileManager manager = VanishClient.INSTANCE.getProfileManager();
        if (!manager.isValidProfileName(modalProfileInput)) {
            showStatus("Invalid profile name");
            return;
        }
        if (manager.profileExists(modalProfileInput)) {
            showStatus("Profile already exists");
            return;
        }
        if (manager.createProfile(modalProfileInput)) {
            selectedProfile = manager.getActiveProfile();
            closeProfileCreateModal(false);
            showStatus("Profile created");
        } else {
            showStatus("Profile creation failed");
        }
    }

    private void openSettingsPopup(Module module, double mouseX, double mouseY, Layout layout) {
        popupModule = module;
        selectedModule = module;
        popupClosing = false;
        popupAnimationProgress = 0.0F;
        popupScroll = 0;
        popupW = Math.min(230, Math.max(190, layout.w / 3));
        popupH = Math.min(layout.moduleH - 14, Math.max(150, module.getSettings().size() * (SETTING_HEIGHT + 6) + 56));
        int preferredX = (int) mouseX + 10;
        if (preferredX + popupW > layout.x + layout.w - 8) {
            preferredX = (int) mouseX - popupW - 10;
        }
        popupX = clamp(preferredX, layout.x + 8, layout.x + layout.w - popupW - 8);
        popupY = clamp((int) mouseY - 16, layout.y + 8, layout.y + layout.h - popupH - 34);
        popupAnimationStart = System.currentTimeMillis();
    }

    private void closeSettingsPopup() {
        if (popupModule == null) return;
        popupClosing = true;
        popupAnimationStart = System.currentTimeMillis();
        stopListening();
    }

    private void showStatus(String message) {
        statusMessage = message == null ? "" : message;
        statusMessageUntil = System.currentTimeMillis() + 2200L;
    }

    private void markProfileDirty() {
        VanishClient.INSTANCE.getProfileManager().markDirty();
    }

    private void handleSettingClick(Setting setting, int button) {
        if (setting instanceof BooleanSetting booleanSetting && isLeftOrRight(button)) {
            booleanSetting.toggle();
        } else if (setting instanceof ModeSetting modeSetting && isLeftOrRight(button)) {
            modeSetting.cycle();
        } else if (setting instanceof NumberSetting numberSetting && isLeftOrRight(button)) {
            double direction = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0D : -1.0D;
            numberSetting.setValue(numberSetting.getValue() + numberSetting.getIncrement() * direction);
        } else if (setting instanceof KeybindSetting keybindSetting) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                stopListening();
                listeningKeybind = keybindSetting;
                listeningKeybind.setListening(true);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                keybindSetting.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                keybindSetting.setListening(false);
                if (listeningKeybind == keybindSetting) {
                    listeningKeybind = null;
                }
            }
        } else if (setting instanceof ColorSetting colorSetting && isLeftOrRight(button)) {
            float[] hsb = colorSetting.getHSB();
            float direction = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 0.03F : -0.03F;
            colorSetting.setFromHSB(wrap(hsb[0] + direction), hsb[1], hsb[2]);
        }
        clampScrolls();
    }

    private Module findModuleAt(double mouseX, double mouseY, Layout layout) {
        int listX = layout.moduleX + 8;
        int listY = layout.moduleY + 30;
        int listW = layout.moduleW - 16;
        double currentY = listY - moduleScroll;

        for (Module module : getFilteredModules()) {
            if (isHovered(mouseX, mouseY, listX, currentY, listW, CARD_HEIGHT)) {
                return module;
            }
            currentY += CARD_HEIGHT + GAP;
        }
        return null;
    }

    private boolean isModuleToggleHovered(double mouseX, double mouseY, Module module, Layout layout) {
        int listX = layout.moduleX + 8;
        int listY = layout.moduleY + 30;
        int listW = layout.moduleW - 16;
        double currentY = listY - moduleScroll;

        for (Module current : getFilteredModules()) {
            if (current == module) {
                int drawY = (int) Math.round(currentY);
                return isHovered(mouseX, mouseY, listX + listW - 58, drawY + 8, 28, 13);
            }
            currentY += CARD_HEIGHT + GAP;
        }
        return false;
    }

    private boolean isModuleFavoriteHovered(double mouseX, double mouseY, Module module, Layout layout) {
        int listX = layout.moduleX + 8;
        int listY = layout.moduleY + 30;
        int listW = layout.moduleW - 16;
        double currentY = listY - moduleScroll;

        for (Module current : getFilteredModules()) {
            if (current == module) {
                return isHovered(mouseX, mouseY, listX + listW - 24, currentY + 4, 22, 20);
            }
            currentY += CARD_HEIGHT + GAP;
        }
        return false;
    }

    private SettingRow findSettingRowAt(double mouseX, double mouseY, Layout layout) {
        if (popupModule == null || popupClosing || !isHovered(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
            return null;
        }

        int listX = popupX + 8;
        int listY = popupY + 40;
        int listW = popupW - 16;
        int listH = popupH - 48;
        if (!isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            return null;
        }

        double rowY = listY - popupScroll;
        for (Setting setting : popupModule.getSettings()) {
            if (isHovered(mouseX, mouseY, listX, rowY, listW, SETTING_HEIGHT)) {
                return new SettingRow(setting, listX, (int) Math.round(rowY), listW);
            }
            rowY += SETTING_HEIGHT + 6;
        }
        return null;
    }

    private void drawModuleScrollBar(DrawContext context, Layout layout, int moduleCount) {
        int listH = layout.moduleH - 38;
        int contentH = Math.max(0, moduleCount * (CARD_HEIGHT + GAP) - GAP);
        int max = Math.max(0, contentH - listH);
        if (max <= 0) return;

        int trackX = layout.moduleX + layout.moduleW - 8;
        int trackY = layout.moduleY + 30;
        int thumbH = Math.max(24, (int) (listH * (listH / (double) contentH)));
        int thumbY = trackY + (int) ((listH - thumbH) * (moduleScroll / max));
        RenderUtils.drawRoundedRect(context, trackX, trackY, 3, listH, 2, 0x5530354A);
        RenderUtils.drawRoundedRect(context, trackX, thumbY, 3, thumbH, 2, ACCENT);
    }

    private List<Module> getFilteredModules() {
        List<Module> modules = viewMode == ViewMode.FAVORITES
                ? new ArrayList<>(favorites)
                : VanishClient.INSTANCE.getModuleManager().getModulesByCategory(selectedCategory);
        if (viewMode == ViewMode.CONFIG || viewMode == ViewMode.FRIENDS || viewMode == ViewMode.SETTINGS) {
            return List.of();
        }
        if (searchText.isBlank()) {
            return modules;
        }

        String query = searchText.toLowerCase(Locale.ROOT);
        List<Module> filtered = new ArrayList<>();
        for (Module module : modules) {
            String name = module.getName() == null ? "" : module.getName().toLowerCase(Locale.ROOT);
            String description = module.getDescription() == null ? "" : module.getDescription().toLowerCase(Locale.ROOT);
            if (name.contains(query) || description.contains(query)) {
                filtered.add(module);
            }
        }
        return filtered;
    }

    private void ensureSelectedModule() {
        if (viewMode == ViewMode.CONFIG || viewMode == ViewMode.FRIENDS || viewMode == ViewMode.SETTINGS) {
            selectedModule = null;
            return;
        }
        List<Module> visible = getFilteredModules();
        if (selectedModule != null && visible.contains(selectedModule)) {
            return;
        }
        selectedModule = visible.isEmpty() ? null : visible.get(0);
    }

    private void clampScrolls() {
        Layout layout = getLayout();
        int moduleContent = Math.max(0, getFilteredModules().size() * (CARD_HEIGHT + GAP) - GAP);
        moduleScroll = clamp(moduleScroll, 0.0D, Math.max(0, moduleContent - (layout.moduleH - 38)));

        int settingContent = popupModule == null ? 0 : Math.max(0, popupModule.getSettings().size() * (SETTING_HEIGHT + 6) - 6);
        popupScroll = clamp(popupScroll, 0.0D, Math.max(0, settingContent - Math.max(1, popupH - 48)));
    }

    private Layout getLayout() {
        int targetW = guiSize == GuiSize.SMALL ? 590 : guiSize == GuiSize.LARGE ? 760 : 670;
        int targetH = guiSize == GuiSize.SMALL ? 330 : guiSize == GuiSize.LARGE ? 440 : 380;
        int maxW = Math.max(1, width - 20);
        int maxH = Math.max(1, height - 20);
        int guiW = Math.min(targetW, maxW);
        int guiH = Math.min(targetH, maxH);
        if (maxW >= 320) guiW = Math.max(320, guiW);
        if (maxH >= 240) guiH = Math.max(240, guiH);
        int x = (width - guiW) / 2;
        int y = (height - guiH) / 2;

        int sidebarW = Math.min(SIDEBAR_WIDTH, Math.max(115, guiW / 5));
        int gap = GAP;
        int contentX = x + sidebarW + gap;
        int contentW = guiW - sidebarW - gap - 10;
        int topX = contentX;
        int topY = y + 8;
        int topW = contentW;
        int topH = TOPBAR_HEIGHT;

        int bodyY = topY + topH + gap;
        int bodyH = guiH - TOPBAR_HEIGHT - BOTTOMBAR_HEIGHT - gap - 18;
        int settingsW = viewMode == ViewMode.CATEGORY || viewMode == ViewMode.FAVORITES ? 0 : Math.max(170, Math.min(210, (int) (contentW * 0.45F)));
        int moduleW = settingsW == 0 ? contentW : contentW - settingsW - gap;
        if (settingsW > 0 && moduleW < 160) {
            settingsW = Math.max(165, contentW - gap - 160);
            moduleW = Math.max(145, contentW - settingsW - gap);
        }
        int settingsX = contentX + moduleW + gap;

        int searchX = topX + 8;
        int searchY = topY + 6;
        int searchW = Math.min(190, Math.max(130, moduleW - 12));
        int searchH = 26;

        return new Layout(x, y, guiW, guiH, x + 6, y + 8, sidebarW - 4, guiH - 16,
                topX, topY, topW, topH, contentX, bodyY, moduleW, bodyH,
                settingsX, bodyY, settingsW, bodyH, searchX, searchY, searchW, searchH);
    }

    private void stopListening() {
        if (listeningKeybind != null) {
            listeningKeybind.setListening(false);
            listeningKeybind = null;
        }
    }

    private List<String> getProfiles() {
        return VanishClient.INSTANCE.getProfileManager().listProfiles();
    }

    private void saveProfile(String profile) {
        if (profile == null || profile.isBlank()) return;
        if (VanishClient.INSTANCE.getProfileManager().saveProfile(profile.trim(), true)) {
            selectedProfile = VanishClient.INSTANCE.getProfileManager().getActiveProfile();
            showStatus("Profile saved");
        }
    }

    private void loadProfile(String profile) {
        if (profile == null || profile.isBlank()) return;
        if (VanishClient.INSTANCE.getProfileManager().loadProfile(profile.trim())) {
            selectedProfile = VanishClient.INSTANCE.getProfileManager().getActiveProfile();
            showStatus("Profile loaded");
        }
    }

    private List<String> getFriendRows() {
        List<String> rows = new ArrayList<>(FriendManager.getFriendNames());
        for (UUID uuid : FriendManager.getFriends()) {
            rows.add(uuid.toString());
        }
        rows.sort(String.CASE_INSENSITIVE_ORDER);
        return rows;
    }

    private void removeFriendRow(String friend) {
        try {
            FriendManager.removeFriend(UUID.fromString(friend));
        } catch (IllegalArgumentException ignored) {
            FriendManager.removeFriendName(friend);
        }
    }

    private void drawGlowRect(DrawContext context, int x, int y, int w, int h, int radius, int color, int layers) {
        // Intentionally flat: glow effects were removed from the minimalist NanoVG redesign.
    }

    private void drawCornerGlow(DrawContext context, int x, int y, int w, int h) {
        drawGlowRect(context, x, y, 34, 34, 15, withAlpha(ACCENT, 36), 4);
        drawGlowRect(context, x + w - 34, y, 34, 34, 15, withAlpha(ACCENT, 36), 4);
        drawGlowRect(context, x, y + h - 34, 34, 34, 15, withAlpha(ACCENT_2, 30), 4);
        drawGlowRect(context, x + w - 34, y + h - 34, 34, 34, 15, withAlpha(ACCENT_2, 30), 4);
    }

    private void drawSoftPanel(DrawContext context, int x, int y, int w, int h, int radius) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, radius, GLASS);
        RenderUtils.drawRoundedRect(context, x + 3, y + 3, w - 6, h - 6, Math.max(1, radius - 3), 0xF01B1E29);
    }

    private void drawBorderedRoundedRect(DrawContext context, int x, int y, int w, int h, int radius, int fill, int border) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, radius, fill);
        drawBorder(context, x, y, w, h, radius, border);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || ((color >>> 24) & 255) == 0) return;
        if (nanoVG.isInFrame()) {
            nanoVG.roundedRectOutline(x, y, w, h, radius, 1.0F, color);
            return;
        }
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawRightText(DrawContext context, String text, int x, int y, int right, int color) {
        drawText(context, text, x + right - textWidth(text), y, color, false);
    }

    private int drawText(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        if (nanoVG.isInFrame()) {
            nanoVG.text(text, x, y, 9.0F, color);
            return Math.round(x + nanoVG.textWidth(text, 9.0F));
        }
        return context.drawText(textRenderer, text, Math.round(x), Math.round(y), color, shadow);
    }

    private int drawTitleText(DrawContext context, String text, float x, float y, int color) {
        if (nanoVG.isInFrame()) {
            nanoVG.text(text, x, y, 11.0F, color);
            return Math.round(x + nanoVG.textWidth(text, 11.0F));
        }
        return context.drawText(textRenderer, text, Math.round(x), Math.round(y), color, false);
    }

    private int drawSmallText(DrawContext context, String text, float x, float y, int color) {
        if (nanoVG.isInFrame()) {
            nanoVG.text(text, x, y, 8.0F, color);
            return Math.round(x + nanoVG.textWidth(text, 8.0F));
        }
        return context.drawText(textRenderer, text, Math.round(x), Math.round(y), color, false);
    }

    private int drawCenteredText(DrawContext context, String text, float centerX, float y, int color) {
        if (nanoVG.isInFrame()) {
            nanoVG.centeredText(text, centerX, y, 9.0F, color);
            return Math.round(centerX + nanoVG.textWidth(text, 9.0F) / 2.0F);
        }
        float x = centerX - textRenderer.getWidth(text) / 2.0F;
        return context.drawText(textRenderer, text, Math.round(x), Math.round(y), color, false);
    }

    private int textWidth(String text) {
        if (nanoVG.isInFrame()) return Math.round(nanoVG.textWidth(text, 9.0F));
        return textRenderer.getWidth(text);
    }

    private int fontHeight() {
        if (nanoVG.isInFrame()) return Math.round(nanoVG.fontHeight(9.0F));
        return textRenderer.fontHeight;
    }

    private void drawRect(DrawContext context, float x, float y, float w, float h, int color) {
        if (nanoVG.isInFrame()) nanoVG.rect(x, y, w, h, color);
        else context.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
    }

    private void pushScissor(DrawContext context, float x, float y, float w, float h) {
        if (nanoVG.isInFrame()) nanoVG.pushScissor(x, y, w, h);
        else context.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
    }

    private void popScissor(DrawContext context) {
        if (nanoVG.isInFrame()) nanoVG.popScissor();
        else context.disableScissor();
    }

    private boolean isLeftOrRight(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private String trimToWidth(String text, int maxWidth) {
        return trimToWidth(text, maxWidth, 9.0F);
    }

    private String trimToWidth(String text, int maxWidth, float fontSize) {
        if (text == null) return "";
        if (measureText(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - measureText(ellipsis, fontSize));
        String trimmed = text;
        while (!trimmed.isEmpty() && measureText(trimmed, fontSize) > limit) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private int measureText(String text, float fontSize) {
        if (nanoVG.isInFrame()) return Math.round(nanoVG.textWidth(text, fontSize));
        return textRenderer.getWidth(text);
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private float animate(float current, float target, float speed) {
        float resolvedSpeed = speed <= 1.0F ? speed * 90.0F : speed;
        return SmoothAnimation.approach(current, target, resolvedSpeed, animationDeltaSeconds);
    }

    private <T> float animate(Map<T, Float> map, T key, float target, float speed) {
        float current = map.getOrDefault(key, target);
        float next = animate(current, target, speed);
        map.put(key, next);
        return next;
    }

    private float easeOutCubic(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return 1.0F - (float) Math.pow(1.0F - value, 3.0D);
    }

    private float easeOutExpo(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0F * value);
    }

    private float easeInOutCubic(float value) {
        value = clamp(value, 0.0F, 1.0F);
        return value < 0.5F ? 4.0F * value * value * value : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0D) / 2.0F;
    }

    private float pulse() {
        return 0.5F + 0.5F * (float) Math.sin(pulseAnimation);
    }

    private float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp(progress, 0.0F, 1.0F);
    }

    private int lerpColor(int from, int to, float progress) {
        progress = clamp(progress, 0.0F, 1.0F);
        int a = (int) (((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * progress);
        int r = (int) (((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * progress);
        int g = (int) (((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * progress);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * progress);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int withAlpha(int color, int alpha) {
        return (clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private float wrap(float value) {
        while (value < 0.0F) value += 1.0F;
        while (value > 1.0F) value -= 1.0F;
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ViewMode {
        CATEGORY,
        FAVORITES,
        CONFIG,
        FRIENDS,
        SETTINGS
    }

    private enum GuiSize {
        SMALL("Small"),
        MEDIUM("Medium"),
        LARGE("Large");

        private final String label;

        GuiSize(String label) {
            this.label = label;
        }
    }

    private enum AccentPreset {
        PURPLE("Purple", 0xFF9B5CFF, 0xFF6D5CFF),
        BLUE("Blue", 0xFF5C8DFF, 0xFF48D8FF),
        RED("Red", 0xFFFF5D86, 0xFFFF755C),
        GREEN("Green", 0xFF55F6A8, 0xFF5CFFDB),
        PINK("Pink", 0xFFFF5CE1, 0xFFFF73A9),
        WHITE("White", 0xFFEDEBFF, 0xFF9EA3B7);

        private final String label;
        private final int color;
        private final int secondary;

        AccentPreset(String label, int color, int secondary) {
            this.label = label;
            this.color = color;
            this.secondary = secondary;
        }
    }

    private record Layout(int x, int y, int w, int h,
                          int sidebarX, int sidebarY, int sidebarW, int sidebarH,
                          int topX, int topY, int topW, int topH,
                          int moduleX, int moduleY, int moduleW, int moduleH,
                          int settingsX, int settingsY, int settingsW, int settingsH,
                          int searchX, int searchY, int searchW, int searchH) {
    }

    private record SliderBounds(int x, int y, int width, int height) {
    }

    private record SettingRow(Setting setting, int x, int y, int width) {
    }
}
