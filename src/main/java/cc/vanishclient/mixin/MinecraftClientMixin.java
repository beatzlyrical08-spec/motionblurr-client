package cc.vanishclient.mixin;

import cc.vanishclient.VanishClient;
import cc.vanishclient.event.impl.network.DisconnectEvent;
import cc.vanishclient.event.impl.player.DoAttackEvent;
import cc.vanishclient.event.impl.player.ItemUseEvent;
import cc.vanishclient.utils.IMinecraft;
import cc.vanishclient.event.impl.player.TickEvent;
import cc.vanishclient.event.impl.world.WorldChangeEvent;
import cc.vanishclient.gui.ClickGui;
import cc.vanishclient.module.modules.client.ClickGUIModule;
import cc.vanishclient.module.modules.client.Client;
import cc.vanishclient.profiles.ProfileManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraft {

    @Shadow
    public ClientWorld world;
    @Shadow
    public HitResult crosshairTarget;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    @Final
    private RenderTickCounter.Dynamic renderTickCounter;

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void setTitle(CallbackInfoReturnable<String> cir) {
        if (VanishClient.INSTANCE == null || VanishClient.mc == null) return;

        var optionalClientModule = VanishClient.INSTANCE.getModuleManager().getModule(Client.class);
        if (optionalClientModule.isPresent()) {
            Client client = optionalClientModule.get();
            if (client.isEnabled() && client.getTitle()) {
                cir.setReturnValue("VanishClient 1.21.1");
            }
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    public void runInject(CallbackInfo ci) {
        if (VanishClient.INSTANCE != null) {
            ProfileManager profileManager = VanishClient.INSTANCE.getProfileManager();
            profileManager.loadProfile("default");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (VanishClient.INSTANCE == null || VanishClient.mc == null) return;

        if (world != null) {
            VanishClient.INSTANCE.getEventBus().post(new TickEvent());
        }

        var optionalClickGuiModule = VanishClient.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (optionalClickGuiModule.isPresent()) {
            ClickGUIModule clickGuiModule = optionalClickGuiModule.get();
            if (clickGuiModule.isEnabled() && VanishClient.mc.currentScreen == null && world != null) {
                VanishClient.mc.setScreen(new ClickGui());
            }
            else if (!clickGuiModule.isEnabled() && VanishClient.mc.currentScreen instanceof ClickGui) {
                VanishClient.mc.setScreen(null);
            }
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    public final void doAttackInject(CallbackInfoReturnable<Boolean> cir) {
        try {
            var antiMissOpt = VanishClient.INSTANCE.getModuleManager().getModule(cc.vanishclient.module.modules.combat.AntiMiss.class);
            if (antiMissOpt.isPresent() && antiMissOpt.get().isEnabled()) {
                if (crosshairTarget == null || crosshairTarget.getType() == HitResult.Type.MISS) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        DoAttackEvent event = new DoAttackEvent();
        VanishClient.INSTANCE.getEventBus().post(event);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stopInject(CallbackInfo ci) {
        if (VanishClient.INSTANCE != null) {
            ProfileManager profileManager = VanishClient.INSTANCE.getProfileManager();
            profileManager.saveProfile("default", true);
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void onWorldChangeInject(ClientWorld newWorld, CallbackInfo ci) {
        if (VanishClient.INSTANCE != null && VanishClient.mc != null) {
            VanishClient.INSTANCE.getEventBus().post(new WorldChangeEvent(newWorld));
        }
    }
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public final void onDisconnected(CallbackInfo ci) {
        DisconnectEvent event = new DisconnectEvent();
        VanishClient.INSTANCE.getEventBus().post(event);
    }
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    public final void doItemUseInject(CallbackInfo ci) {
        ItemUseEvent event = new ItemUseEvent();

        VanishClient.INSTANCE.getEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}


