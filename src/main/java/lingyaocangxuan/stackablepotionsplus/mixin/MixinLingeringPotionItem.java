// 滞留药水使用冷却（可配置，默认关闭）。
//
// 为什么不能只注入 ThrowablePotionItem：喷溅与滞留两个子类都**各自覆盖**了 use，
// 注入到父类的方法体上不会被执行（调用走的是子类的重写）。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.ThrowCooldownHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LingeringPotionItem.class)
public abstract class MixinLingeringPotionItem extends PotionItem {
    private MixinLingeringPotionItem(Item.Properties settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> info) {
        ThrowCooldownHelper.apply(user, this);
    }
}
