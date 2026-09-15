// 喷溅药水使用冷却（可配置，默认关闭）。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.Config;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashPotionItem.class)
public abstract class MixinSplashPotionItem extends PotionItem {
    private MixinSplashPotionItem(Item.Properties settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> info) {
        if (Config.enableCooldown.get()) {
            user.getCooldowns().addCooldown(this, 20);
        }
    }
}
