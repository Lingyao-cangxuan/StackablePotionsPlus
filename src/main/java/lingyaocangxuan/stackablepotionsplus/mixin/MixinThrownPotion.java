// 喷溅/滞留药水命中：打开「主动使用」来源标记，并叠加瞬间效果。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.InstantStackHelper;
import lingyaocangxuan.stackablepotionsplus.StackSourceContext;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ThrownPotion.class)
public abstract class MixinThrownPotion {
    private static final String APPLY_SPLASH = "applySplash(Ljava/util/List;Lnet/minecraft/world/entity/Entity;)V";

    // 命中结算时对范围内每个目标逐个 addEffect，整段都算作「玩家主动投掷」的结果，
    // 因此在方法入口打开标记、所有 return 之前关闭。
    // 注意：AreaEffectCloud#tick（滞留云持续施放）不在此列，故待在云里不会刷等级。
    @Inject(method = APPLY_SPLASH, at = @At("HEAD"))
    private void stackablepotionsplus$enterStackContext(List<LivingEntity> affected, Entity source, CallbackInfo ci) {
        StackSourceContext.enter();
    }

    @Inject(method = APPLY_SPLASH, at = @At("RETURN"))
    private void stackablepotionsplus$exitStackContext(List<LivingEntity> affected, Entity source, CallbackInfo ci) {
        StackSourceContext.exit();
    }

    @Redirect(method = APPLY_SPLASH, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyInstantenousEffect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V"))
    private void stackablepotionsplus$redirectInstant(MobEffect effect, Entity source, Entity indirect, LivingEntity target, int amplifier, double multiplier) {
        int stacked = InstantStackHelper.stackedAmplifier(target, effect, amplifier);
        effect.applyInstantenousEffect(source, indirect, target, stacked, multiplier);
    }
}
