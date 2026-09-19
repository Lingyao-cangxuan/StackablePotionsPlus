// 酿造台药水槽容量：原版写死 1（字节码 iconst_1; ireturn），改为跟随配置/物品堆叠上限。
// 药水物品的上限已由 MixinItems 提到 64，这里放开槽位层限制后两者才能对上。
//
// 注意：BrewingStandMenu$PotionSlot 是包级私有内部类，无法用 class 字面量引用，
// 因此只能用字符串 targets；Slot#getItem 是 public 的，直接强转即可。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.Config;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class MixinPotionSlot {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void stackablepotionsplus$useItemLimit(CallbackInfoReturnable<Integer> cir) {
        int configured = Config.potionSlotCapacity.get();
        if (configured <= 1) {
            return; // 配置为 1 时保持原版行为
        }
        // 槽里已有药水时按其自身堆叠上限收敛，空槽时直接用配置值（决定"能放多少瓶"）。
        ItemStack current = ((Slot) (Object) this).getItem();
        if (!current.isEmpty()) {
            int itemLimit = current.getMaxStackSize();
            cir.setReturnValue(itemLimit > 1 ? Math.min(configured, itemLimit) : configured);
            return;
        }
        cir.setReturnValue(configured);
    }
}
