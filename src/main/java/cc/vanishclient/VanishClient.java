package cc.vanishclient;

import cc.vanishclient.module.ModuleManager;
import cc.vanishclient.module.events.MouseModuleHandler;
import cc.vanishclient.profiles.ProfileManager;
import cc.vanishclient.utils.jvm.ModMenuHider;
import cc.vanishclient.utils.notification.NotificationManager;
import cc.vanishclient.utils.render.font.FontManager;
import io.github.racoondog.norbit.EventBus;
import lombok.Getter;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Getter
public final class VanishClient implements ClientModInitializer {
    public static final String MOD_ID = "vanishclient";
    public static final String CLIENT_NAME = "VanishClient";
    public static final String CLIENT_VERSION = "v1.0";
    public static final boolean shouldUseMouseEvent = System.getProperty("os.name").toLowerCase().contains("windows");
    public static VanishClient INSTANCE;
    public static MinecraftClient mc;
    public final IEventBus eventBus;
    public final ModuleManager moduleManager;
    public final FontManager fontManager;
    public final ProfileManager profileManager;
    public final MouseModuleHandler mouseModuleHandler;
    public final NotificationManager notificationManager;
    private final Logger logger = LoggerFactory.getLogger(CLIENT_NAME);

    public VanishClient() {
        INSTANCE = this;
        mc = MinecraftClient.getInstance();
        eventBus = EventBus.threadSafe();
        eventBus.registerLambdaFactory("cc.vanishclient", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        this.moduleManager = new ModuleManager();
        this.fontManager = new FontManager();
        // this.fontManager.initialize();
        this.profileManager = new ProfileManager();
        this.mouseModuleHandler = new MouseModuleHandler();
        this.notificationManager = NotificationManager.getInstance();

        eventBus.subscribe(mouseModuleHandler);
        eventBus.subscribe(notificationManager);
        new Thread(() -> {
            try {
                ModMenuHider.hideFromModMenu();
                Thread.sleep(1000);
                ModMenuHider.hideFromModMenu();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void onInitializeClient() {
        // Double initialization prevention, it's already initializing in the constructor
    }
}
