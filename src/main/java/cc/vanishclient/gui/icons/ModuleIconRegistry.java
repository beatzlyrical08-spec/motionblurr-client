package cc.vanishclient.gui.icons;

import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.modules.client.*;
import cc.vanishclient.module.modules.combat.*;
import cc.vanishclient.module.modules.misc.*;
import cc.vanishclient.module.modules.movement.*;
import cc.vanishclient.module.modules.player.*;
import cc.vanishclient.module.modules.render.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModuleIconRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("VanishClient/ModuleIcons");
    private static final ModuleIconRegistry INSTANCE = new ModuleIconRegistry();

    private final Map<Class<? extends Module>, IconKey> moduleIcons = new HashMap<>();
    private final Set<Class<? extends Module>> warnedFallbackModules = new HashSet<>();

    private ModuleIconRegistry() {
        registerCombat();
        registerMovement();
        registerPlayer();
        registerRender();
        registerMisc();
        registerClient();
    }

    public static ModuleIconRegistry getInstance() {
        return INSTANCE;
    }

    public IconKey iconFor(Module module) {
        if (module == null) return IconKey.SPARKLE;

        IconKey exact = moduleIcons.get(module.getClass());
        if (exact != null) return exact;

        IconKey semantic = semanticIcon(module.getClass().getSimpleName(), module.getName(), module.getDescription());
        if (semantic != null) return semantic;

        warnFallbackOnce(module);
        return categoryFallback(module.getModuleCategory());
    }

    public IconKey iconFor(Category category) {
        return categoryFallback(category);
    }

    private void registerCombat() {
        put(AutoMace.class, IconKey.GAVEL);
        put(TotemHit.class, IconKey.SPARKLE);
        put(TriggerBot.class, IconKey.SWORD);
        put(Velocity.class, IconKey.SWORDS);
        put(ShieldBreaker.class, IconKey.AXE);
        put(ThrowPot.class, IconKey.SPARKLE);
        put(ElytraHotSwap.class, IconKey.SPORT_SHOE);
        put(AntiMiss.class, IconKey.SWORDS);
        put(WTap.class, IconKey.SWORD);
        put(STap.class, IconKey.SWORD);
        put(AimAssist.class, IconKey.SWORDS);
        put(SwordHotSwap.class, IconKey.SWORD);
        put(AutoCrystal.class, IconKey.SPARKLE);
        put(SwordSwap.class, IconKey.SWORD);
        put(BreachSwap.class, IconKey.GAVEL);
        put(KeyCrystal.class, IconKey.SPARKLE);
        put(KeyAnchor.class, IconKey.ANCHOR);
        put(KeyLava.class, IconKey.SPARKLE);
        put(AutoPot.class, IconKey.SPARKLE);
        put(StunCob.class, IconKey.SPARKLE);
        put(AutoCart.class, IconKey.SWORDS);
        put(CrystalOptimizer.class, IconKey.SPARKLE);
        put(Criticals.class, IconKey.SPARKLE);
    }

    private void registerMovement() {
        put(Sprint.class, IconKey.SPORT_SHOE);
        put(AutoFirework.class, IconKey.SPORT_SHOE);
        put(AutoHeadHitter.class, IconKey.SPORT_SHOE);
        put(KeepSprint.class, IconKey.SPORT_SHOE);
    }

    private void registerPlayer() {
        put(AutoExtinguish.class, IconKey.SPARKLE);
        put(AutoTool.class, IconKey.PICKAXE);
        put(AutoWeb.class, IconKey.SPARKLE);
        put(AutoRefill.class, IconKey.APPLE);
        put(AutoDrain.class, IconKey.PICKAXE);
        put(AutoCrafter.class, IconKey.PICKAXE);
        put(FastPlace.class, IconKey.PICKAXE);
        put(FastEXP.class, IconKey.SPARKLE);
        put(TrapSave.class, IconKey.USER);
        put(PingSpoof.class, IconKey.ORBIT);
        put(AutoDoubleHand.class, IconKey.APPLE);
        put(AutoMLG.class, IconKey.APPLE);
        put(FastMine.class, IconKey.PICKAXE);
        put(ReBuffNotifier.class, IconKey.APPLE);
        put(CoverUp.class, IconKey.USER);
    }

    private void registerRender() {
        put(ContainerSlots.class, IconKey.SPARKLE);
        put(FullBright.class, IconKey.SPARKLE);
        put(Watermark.class, IconKey.SPARKLE);
        put(TargetHUD.class, IconKey.ORBIT);
        put(SwingSpeed.class, IconKey.SWORD);
        put(Notifications.class, IconKey.SPARKLE);
        put(ArrowESP.class, IconKey.ORBIT);
        put(OutlineESP.class, IconKey.SPARKLE);
        put(ESP2D.class, IconKey.SPARKLE);
        put(TargetESP.class, IconKey.ORBIT);
        put(ArrayList.class, IconKey.SPARKLE);
        put(Trajectories.class, IconKey.ORBIT);
    }

    private void registerMisc() {
        put(CartKey.class, IconKey.SWORDS);
        put(HoverTotem.class, IconKey.SPARKLE);
        put(MiddleClickFriend.class, IconKey.USER);
        put(PearlKey.class, IconKey.ORBIT);
        put(PearlCatch.class, IconKey.ORBIT);
        put(WindChargeKey.class, IconKey.SPARKLE);
        put(Teams.class, IconKey.USER);
        put(FakePlayer.class, IconKey.USER);
        put(Friends.class, IconKey.USER);
    }

    private void registerClient() {
        put(NewClickGUIModule.class, IconKey.FOLDER_OPEN);
        put(ClientSettingsModule.class, IconKey.SPARKLE);
        put(Client.class, IconKey.SPARKLE);
        put(Debugger.class, IconKey.SPARKLE);
        put(Secret.class, IconKey.SPARKLE);
        put(KeybindsModule.class, IconKey.FOLDER_OPEN);
    }

    private void put(Class<? extends Module> moduleClass, IconKey icon) {
        moduleIcons.put(moduleClass, icon);
    }

    private IconKey semanticIcon(String className, String displayName, String description) {
        String text = normalize(className + " " + displayName + " " + description);
        if (text.contains("triggerbot")) return IconKey.SWORD;
        if (text.contains("killaura")) return IconKey.SWORDS;
        if (text.contains("anchor")) return IconKey.ANCHOR;
        if (text.contains("mace")) return IconKey.GAVEL;
        if (text.contains("axe")) return IconKey.AXE;
        if (text.contains("pearl") || text.contains("trajectory") || text.contains("target") || text.contains("rotation")) return IconKey.ORBIT;
        if (text.contains("sprint") || text.contains("speed") || text.contains("movement") || text.contains("elytra") || text.contains("firework")) return IconKey.SPORT_SHOE;
        if (text.contains("eat") || text.contains("apple") || text.contains("gapple") || text.contains("food") || text.contains("refill") || text.contains("buff")) return IconKey.APPLE;
        if (text.contains("mine") || text.contains("tool") || text.contains("pickaxe") || text.contains("craft") || text.contains("place")) return IconKey.PICKAXE;
        if (text.contains("friend") || text.contains("team") || text.contains("player")) return IconKey.USER;
        if (text.contains("sword")) return IconKey.SWORD;
        if (text.contains("windcharge") || text.contains("wind charge")) return IconKey.SPARKLE;
        if (text.contains("critical") || text.contains("crystal") || text.contains("visual") || text.contains("render")) return IconKey.SPARKLE;
        if (text.contains("config") || text.contains("profile")) return IconKey.FOLDER_OPEN;
        if (text.contains("create")) return IconKey.FOLDER_PLUS;
        if (text.contains("favorite")) return IconKey.STAR_PLUS;
        return null;
    }

    private void warnFallbackOnce(Module module) {
        Class<? extends Module> moduleClass = module.getClass();
        if (warnedFallbackModules.add(moduleClass)) {
            LOGGER.warn("No icon mapping for module {} ({}); using {} fallback.",
                    module.getName(), moduleClass.getName(), categoryFallback(module.getModuleCategory()));
        }
    }

    private IconKey categoryFallback(Category category) {
        if (category == null) return IconKey.SPARKLE;
        return switch (category) {
            case COMBAT -> IconKey.SWORDS;
            case PLAYER -> IconKey.USER;
            case MOVEMENT -> IconKey.SPORT_SHOE;
            case RENDER -> IconKey.SPARKLE;
            case MISC -> IconKey.SPARKLE;
            case CLIENT -> IconKey.SPARKLE;
            case CONFIG -> IconKey.FOLDER_OPEN;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
