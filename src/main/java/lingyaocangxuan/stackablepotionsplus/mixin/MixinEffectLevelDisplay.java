// 效果等级显示：超过 X 级改用数字。
package lingyaocangxuan.stackablepotionsplus.mixin;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class MixinEffectLevelDisplay {
    @Inject(method = "getEffectName(Lnet/minecraft/world/effect/MobEffectInstance;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void stackablepotionsplus$getEffectName(MobEffectInstance instance, CallbackInfoReturnable<Component> cir) {
        MutableComponent name = instance.getEffect().getDisplayName().copy();
        int amplifier = instance.getAmplifier();
        if (amplifier >= 1) {
            int level = amplifier + 1;
            name.append(Component.literal(" "));
            if (level <= 10) {
                name.append(Component.translatable("enchantment.level." + level));
            } else {
                name.append(Component.literal(String.valueOf(level)));
            }
        }
        cir.setReturnValue(name);
    }
}
