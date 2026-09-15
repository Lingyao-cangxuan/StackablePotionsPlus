// 滞留药水云的瞬间效果叠加。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.InstantStackHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AreaEffectCloud.class)
public abstract class MixinAreaEffectCloud {
    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyInstantenousEffect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V"))
    private void stackablepotionsplus$redirectInstant(MobEffect effect, Entity source, Entity indirect, LivingEntity target, int amplifier, double multiplier) {
        int stacked = InstantStackHelper.stackedAmplifier(target, effect, amplifier);
        effect.applyInstantenousEffect(source, indirect, target, stacked, multiplier);
    }
}
