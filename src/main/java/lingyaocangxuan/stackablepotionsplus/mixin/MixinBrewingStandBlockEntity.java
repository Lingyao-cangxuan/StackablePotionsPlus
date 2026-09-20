// 酿造台批量炼药。
//
// ⚠️ 核心前提：Forge 的整套酿造配方系统都硬性要求「输入槽只有 1 瓶」——
// BrewingRecipeRegistry.getOutput 开头即 `if (input.getCount() != 1) return EMPTY;`，
// 而 canBrew / hasOutput 都经由它。所以堆叠药水在 Forge 眼里是「不可酿」的：
// isBrewable 恒为 false → serverTick 永远不会把 brewTime 置为 400 → 完全不开始。
// 这也正是原版把药水槽容量写死成 1 的根本原因（见 MixinPotionSlot）。
//
// 因此本 Mixin 在「批数 > 1」时完全接管 isBrewable / doBrew：
// 内部一律用「只含 1 瓶的副本」去查配方，拿到产物后再把整批瓶数写回。
// 批数 <= 1 时不介入，完全保持原版行为。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.BrewingBatchHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class MixinBrewingStandBlockEntity {
    // 原版 doBrew 结尾播放的酿造音效/粒子事件。
    private static final int BREWING_STAND_BREW_EVENT = 1035;

    /** 读取实体槽位列表。 */
    private static NonNullList<ItemStack> batchItems(BrewingStandBlockEntity entity) {
        return ((BrewingItemsAccessor) entity).stackablepotionsplus$items();
    }

    /** 配方查询用的单瓶副本：Forge 的 getOutput 要求 getCount() == 1，NBT 必须原样保留。 */
    private static ItemStack probe(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    // ① 材料不足以整批炼制时不点火，省下燃料；补足后自动开始。
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void requireFullBatch(Level level, BlockPos pos, BlockState state,
                                         BrewingStandBlockEntity entity, CallbackInfo ci) {
        NonNullList<ItemStack> items = batchItems(entity);
        int batch = BrewingBatchHelper.batchSize(items);
        if (batch > 1 && !BrewingBatchHelper.hasEnoughIngredient(items, batch)) {
            ci.cancel();
        }
    }

    // ② 原版 isBrewable 对堆叠药水恒为 false，批量时自行判断是否有任一槽可酿。
    @Inject(method = "isBrewable", at = @At("HEAD"), cancellable = true)
    private static void batchIsBrewable(NonNullList<ItemStack> items, CallbackInfoReturnable<Boolean> cir) {
        int batch = BrewingBatchHelper.batchSize(items);
        if (batch <= 1) {
            return; // 单瓶走原版
        }
        ItemStack ingredient = items.get(BrewingBatchHelper.INGREDIENT_SLOT);
        if (ingredient.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        for (int i = 0; i < BrewingBatchHelper.POTION_SLOTS; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty()) {
                continue;
            }
            if (!BrewingRecipeRegistry.getOutput(probe(potion), ingredient).isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    // ③ 批量炼制：产物保留整批瓶数，材料一次扣掉整批。
    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void batchDoBrew(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo ci) {
        int batch = BrewingBatchHelper.batchSize(items);
        if (batch <= 1) {
            return; // 单瓶走原版
        }
        if (ForgeEventFactory.onPotionAttemptBrew(items)) {
            ci.cancel();
            return;
        }
        ItemStack ingredient = items.get(BrewingBatchHelper.INGREDIENT_SLOT);
        for (int i = 0; i < BrewingBatchHelper.POTION_SLOTS; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty()) {
                continue;
            }
            int count = potion.getCount();
            ItemStack output = BrewingRecipeRegistry.getOutput(probe(potion), ingredient);
            if (!output.isEmpty()) {
                output.setCount(count); // 关键：把 1 瓶的产物还原成整批
                items.set(i, output);
            }
        }
        ForgeEventFactory.onPotionBrewed(items);
        ingredient.shrink(batch);
        items.set(BrewingBatchHelper.INGREDIENT_SLOT, ingredient);
        level.levelEvent(BREWING_STAND_BREW_EVENT, pos, 0);
        ci.cancel();
    }
}
