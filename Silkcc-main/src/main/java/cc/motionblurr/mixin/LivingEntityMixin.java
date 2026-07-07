package cc.motionblurr.mixin;

import cc.motionblurr.MotionBlurrClient;
import cc.motionblurr.module.modules.render.SwingSpeed;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    public void getHandSwingDurationInject(CallbackInfoReturnable<Integer> cir) {
        if (MotionBlurrClient.INSTANCE == null || MotionBlurrClient.mc == null) return;

        var optionalModule = MotionBlurrClient.INSTANCE.getModuleManager().getModule(SwingSpeed.class);
        if (optionalModule.isPresent()) {
            SwingSpeed module = optionalModule.get();
            if (module.isEnabled()) {
                cir.setReturnValue(module.getSwingSpeed());
            }
        }
    }
}

