// 酿造台批量炼药：材料不足不开火，耗时与材料按批数换算，一次炼完整批。
package lingyaocangxuan.stackablepotionsplus.mixin;

import lingyaocangxuan.stackablepotionsplus.BrewingBatchHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class MixinBrewingStandBlockEntity {
    private static final int VANILLA_BREW_TIME = 400;

    /** 读取实体槽位列表。 */
    private static NonNullList<ItemStack> batchItems(BrewingStandBlockEntity entity) {
        return ((BrewingItemsAccessor) entity).stackablepotionsplus$items();
    }

    // 材料不足以整批炼制时不点火，省下燃料。
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void stackablepotionsplus$requireFullBatch(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, CallbackInfo ci) {
        NonNullList<ItemStack> items = batchItems(entity);
        if (items.get(BrewingBatchHelper.INGREDIENT_SLOT).is(Items.BLAZE_POWDER)) {
            return;
        }
        int batch = BrewingBatchHelper.batchSize(items);
        if (batch > 1 && !BrewingBatchHelper.hasEnoughIngredient(items, batch)) {
            ci.cancel();
        }
    }

    // 开始炼制时把耗时设为 400 × 批数（原版为常量 400）。
    @ModifyConstant(method = "serverTick", constant = @Constant(intValue = VANILLA_BREW_TIME))
    private static int stackablepotionsplus$scaleBrewTime(int vanilla, Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity) {
        int batch = BrewingBatchHelper.batchSize(batchItems(entity));
        return batch > 1 ? VANILLA_BREW_TIME * batch : vanilla;
    }

    // 一次扣除整批材料，而非 1 个。
    @Redirect(method = "doBrew", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void stackablepotionsplus$shrinkWholeBatch(ItemStack ingredient, int amount, Level level, BlockPos pos, NonNullList<ItemStack> items) {
        int batch = BrewingBatchHelper.batchSize(items);
        ingredient.shrink(batch > 1 ? batch : amount);
    }

}
