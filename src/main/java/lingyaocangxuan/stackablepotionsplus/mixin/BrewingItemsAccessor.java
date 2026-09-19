// 便于读取酿造台私有槽位列表。
package lingyaocangxuan.stackablepotionsplus.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class)
public interface BrewingItemsAccessor {
    @Accessor("items")
    NonNullList<ItemStack> stackablepotionsplus$items();
}
