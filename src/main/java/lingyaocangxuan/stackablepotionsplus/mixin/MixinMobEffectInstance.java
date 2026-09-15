// 重复获得同种效果时叠加时长与等级（含上限与开关）。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MixinMobEffectInstance {
    // 硬性上限（=128 级）
    private static final int HARD_MAX_AMPLIFIER = 127;

    @Shadow
    @Final
    private MobEffect effect;
    @Shadow
    private int duration;
    @Shadow
    private int amplifier;

    @Inject(method = "update(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void stackEffectOnUpdate(MobEffectInstance other, CallbackInfoReturnable<Boolean> cir) {
        if (other == null || other.getEffect() != this.effect) {
            return;
        }
        if (!Config.stackNegativeEffects.get() && !this.effect.isBeneficial()) {
            return;
        }
        if (Config.infiniteDuration.get() || this.duration == -1) {
            this.duration = -1;
        } else {
            long capTicks = Config.durationCapSeconds.get() > 0
                    ? Config.durationCapSeconds.get() * 20L
                    : (long) Integer.MAX_VALUE;
            long totalDuration = (long) this.duration + other.getDuration();
            this.duration = (int) Math.min(totalDuration, capTicks);
        }
        int stackedAmplifier = Math.max(this.amplifier, other.getAmplifier()) + 1;
        int effectiveCap = Math.min(Config.maxAmplifier.get(), HARD_MAX_AMPLIFIER);
        this.amplifier = Math.min(stackedAmplifier, effectiveCap);
        cir.setReturnValue(true);
    }
}
