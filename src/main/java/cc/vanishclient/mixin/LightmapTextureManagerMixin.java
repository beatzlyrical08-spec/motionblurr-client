package cc.vanishclient.mixin;

import cc.vanishclient.VanishClient;
import cc.vanishclient.module.modules.render.FullBright;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void setBrightness(CallbackInfoReturnable<Float> ci) {
        if (VanishClient.INSTANCE.moduleManager.getModule(FullBright.class).get().isEnabled()) ci.setReturnValue(1.0F);
    }
}
