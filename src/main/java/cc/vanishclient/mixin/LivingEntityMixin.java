package cc.vanishclient.mixin;

import cc.vanishclient.VanishClient;
import cc.vanishclient.module.modules.render.SwingSpeed;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    public void getHandSwingDurationInject(CallbackInfoReturnable<Integer> cir) {
        if (VanishClient.INSTANCE == null || VanishClient.mc == null) return;

        var optionalModule = VanishClient.INSTANCE.getModuleManager().getModule(SwingSpeed.class);
        if (optionalModule.isPresent()) {
            SwingSpeed module = optionalModule.get();
            if (module.isEnabled()) {
                cir.setReturnValue(module.getSwingSpeed());
            }
        }
    }
}

