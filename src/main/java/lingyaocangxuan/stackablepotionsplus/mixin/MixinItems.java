// 药水堆叠数：注册期先按桥字段取值，配置加载后由 StackSizeApplier 写入真实数值。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.StackablePotions;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * 把三种药水物品的 {@code stacksTo(1)} 改掉。
 * <p>
 * <b>这里读的是桥字段，不是配置。</b> {@code Items.<clinit>} 在配置加载之前执行，
 * 直接读 {@code ForgeConfigSpec} 会抛异常；桥字段的静态初值等于配置默认值，
 * 配置就绪后 {@link lingyaocangxuan.stackablepotionsplus.StackSizeApplier} 会把它
 * 以及每个物品实例的 {@code maxStackSize} 一起改成玩家设定的值。
 */
@Mixin(Items.class)
public abstract class MixinItems {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item$Properties;stacksTo(I)Lnet/minecraft/world/item/Item$Properties;", ordinal = 0), slice = @Slice(from = @At(value = "NEW", target = "Lnet/minecraft/world/item/PotionItem;")))
    private static int onPotion(int old) {
        return StackablePotions.potionStackSize;
    }

    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item$Properties;stacksTo(I)Lnet/minecraft/world/item/Item$Properties;", ordinal = 0), slice = @Slice(from = @At(value = "NEW", target = "Lnet/minecraft/world/item/SplashPotionItem;")))
    private static int onSplashPotion(int old) {
        return StackablePotions.potionStackSize;
    }

    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item$Properties;stacksTo(I)Lnet/minecraft/world/item/Item$Properties;", ordinal = 0), slice = @Slice(from = @At(value = "NEW", target = "Lnet/minecraft/world/item/LingeringPotionItem;")))
    private static int onLingeringPotion(int old) {
        return StackablePotions.potionStackSize;
    }
}
