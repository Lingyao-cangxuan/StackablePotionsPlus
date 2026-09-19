// 酿造台批量炼药：批次数量与材料判定。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public final class BrewingBatchHelper {
    // 酿造台槽位：0-2 药水，3 材料，4 燃料
    public static final int POTION_SLOTS = 3;
    public static final int INGREDIENT_SLOT = 3;

    private BrewingBatchHelper() {
    }

    /** 本次批量炼制的瓶数：三槽药水中最多的那组；无药水返回 0。 */
    public static int batchSize(NonNullList<ItemStack> items) {
        int max = 0;
        for (int i = 0; i < POTION_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                max = Math.max(max, stack.getCount());
            }
        }
        return max;
    }

    /** 材料是否够整批炼制：必须达到瓶数上限，不足则不满载。 */
    public static boolean hasEnoughIngredient(NonNullList<ItemStack> items, int batchSize) {
        if (batchSize <= 0) {
            return false;
        }
        return items.get(INGREDIENT_SLOT).getCount() >= batchSize;
    }
}
