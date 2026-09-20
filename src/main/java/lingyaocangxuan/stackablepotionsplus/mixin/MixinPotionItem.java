// 饮用：只消耗 1 瓶并返还玻璃瓶；瞬间效果叠加；打开「主动使用」来源标记。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.InstantStackHelper;
import lingyaocangxuan.stackablepotionsplus.StackSourceContext;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public abstract class MixinPotionItem {
    private static final String FINISH_USING = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;";

    // 来源标记：原版在这里才真正把效果加给使用者的（applyEffect → LivingEntity#addEffect），
    // 所以必须在方法入口就打开标记，在任意 return 之前关闭，才能覆盖整条施加链路。
    // 只覆盖「通过药水物品使用」这一条路径；饰品模组、/effect 命令等直调 addEffect 的
    // 来源不经过此处，因而不会触发叠加（详见 StackSourceContext 的说明）。
    @Inject(method = FINISH_USING, at = @At("HEAD"))
    private void stackablepotionsplus$enterStackContext(ItemStack stack, Level level, LivingEntity user,
                                                        CallbackInfoReturnable<ItemStack> cir) {
        StackSourceContext.enter();
    }

    @Inject(method = FINISH_USING, at = @At("RETURN"))
    private void stackablepotionsplus$exitStackContext(ItemStack stack, Level level, LivingEntity user,
                                                       CallbackInfoReturnable<ItemStack> cir) {
        StackSourceContext.exit();
    }

    @Inject(method = FINISH_USING, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z", shift = At.Shift.BEFORE))
    public void finishUsing(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        try {
            if (user instanceof Player) {
                ((Player) user).getInventory().add(Items.GLASS_BOTTLE.getDefaultInstance());
            }
            // 原版在 `return stack` 之前会广播 GameEvent.DRINK（幽匿感测体/监守者靠它侦测饮用）。
            // 本注入点在它之前且会 cancel，必须在此手动补回，否则该事件整条丢失。
            // 注：单瓶（shrink 后为空）时原版在到达本注入点之前就 areturn 了，走不到这里。
            user.gameEvent(GameEvent.DRINK);
            cir.setReturnValue(stack);
        } finally {
            // 必须在 cancel 前收尾来源标记。cir.setReturnValue 会让 Mixin 在注入点直接
            // areturn，而这条 return 是在 @At("RETURN") 注入之后才生成的，**不会被其覆盖**，
            // 因此上面的 exitStackContext 在这条路径上不可达。
            // 漏掉这里会导致每次饮用堆叠药水都泄漏一次标记，使随后的外部施加被误判为玩家主动使用。
            // （单瓶路径由 @At("RETURN") 正常收尾，两条路径互斥，不会重复 exit。）
            StackSourceContext.exit();
        }
    }

    @Redirect(method = FINISH_USING, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyInstantenousEffect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V"))
    private void stackablepotionsplus$redirectInstant(MobEffect effect, Entity source, Entity indirect, LivingEntity target, int amplifier, double multiplier) {
        int stacked = InstantStackHelper.stackedAmplifier(target, effect, amplifier);
        effect.applyInstantenousEffect(source, indirect, target, stacked, multiplier);
    }
}
