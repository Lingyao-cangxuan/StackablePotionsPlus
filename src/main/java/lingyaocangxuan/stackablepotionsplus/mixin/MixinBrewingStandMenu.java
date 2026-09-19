// 酿造台 Shift 快捷移动：让药水整组进三个药水槽。
//
// 原版 quickMoveStack 里药水走 `moveItemStackTo(stack, 0, 3, false)`，
// 而 moveItemStackTo 的容量取 min(Slot.getMaxStackSize(), ItemStack.getMaxStackSize())，
// 所以 MixinPotionSlot 放开槽位上限后，原版逻辑本身就能整组搬运。
//
// 本类只做一件事：原版在药水分支上用 `PotionSlot.mayPlaceItem(stack)` 做前置判定，
// 失败就直接 `return ItemStack.EMPTY`（表现为"搬不动"）。这里把它换成一个更宽松的判定，
// 让"槽内已有同种药水但没满"的情况也能继续走搬运逻辑。
package lingyaocangxuan.stackablepotionsplus.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewingStandMenu.class)
public abstract class MixinBrewingStandMenu extends AbstractContainerMenu {
    private MixinBrewingStandMenu(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    // 原版用 mayPlaceItem 判定能否进药水槽；这里放宽为"空槽 或 同种药水未堆满"。
    // 返回 true 让原版继续执行 moveItemStackTo，由其自身完成合并/放入。
    @Redirect(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BrewingStandMenu$PotionSlot;mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean stackablepotionsplus$allowStackingIntoPotionSlot(ItemStack stack, Player player, int index) {
        for (int i = 0; i < 3; i++) {
            Slot potionSlot = this.getSlot(i);
            if (!potionSlot.mayPlace(stack)) {
                continue;
            }
            ItemStack existing = potionSlot.getItem();
            if (existing.isEmpty()) {
                return true;
            }
            int slotLimit = potionSlot.getMaxStackSize();
            int itemLimit = stack.getMaxStackSize();
            int limit = Math.min(slotLimit, itemLimit);
            if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < limit) {
                return true;
            }
        }
        return false;
    }
}
