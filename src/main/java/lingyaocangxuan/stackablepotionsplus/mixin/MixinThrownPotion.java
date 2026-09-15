// 喷溅药水命中时的瞬间效果叠加。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.InstantStackHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownPotion.class)
public abstract class MixinThrownPotion {
    @Redirect(method = "applySplash(Ljava/util/List;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyInstantenousEffect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V"))
    private void stackablepotionsplus$redirectInstant(MobEffect effect, Entity source, Entity indirect, LivingEntity target, int amplifier, double multiplier) {
        int stacked = InstantStackHelper.stackedAmplifier(target, effect, amplifier);
        effect.applyInstantenousEffect(source, indirect, target, stacked, multiplier);
    }
}
