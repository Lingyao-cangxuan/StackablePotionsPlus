// 把配置里的堆叠上限写进药水物品实例。
package lingyaocangxuan.stackablepotionsplus;

import lingyaocangxuan.stackablepotionsplus.mixin.ItemMaxStackAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 配置项 {@code general.potionStackSize} → 药水物品的 {@code maxStackSize}。
 * <p>
 * <b>为什么不能只让 Mixin 在 {@code Items.<clinit>} 里读配置：</b>
 * 物品注册发生在配置加载之前，那时 {@code Config} 还没 accept 任何数据，
 * 读配置会直接抛 {@code IllegalStateException}。所以注册期只用主类上的桥字段
 * （静态初值 = 配置默认值），真实数值由这里在配置加载完成后覆盖写入。
 * <p>
 * <b>为什么改配置后必须显式调用：</b>
 * {@code maxStackSize} 在物品构造时固化，Forge 不会因为配置文件变了就回头改物品实例。
 * 因此配置加载事件与配置界面保存按钮都要调一次 {@link #apply()}。
 */
public final class StackSizeApplier {

    private StackSizeApplier() {
    }

    /** 三种可堆叠的药水物品。 */
    private static final Item[] STACKABLE_POTIONS = {
            Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION
    };

    /**
     * 读取配置并应用到三个药水物品上。
     * <p>
     * <b>调用前提：配置已加载</b>（在 {@code ModConfigEvent.Loading/Reloading} 或配置界面里调用）。
     */
    public static void apply() {
        int size = Config.potionStackSize.get();

        // 同步桥字段：万一有物品在配置加载之后才被注册（其它模组的延迟注册），
        // Mixin 走这条路时取到的也是最新值。
        StackablePotions.potionStackSize = size;

        for (Item item : STACKABLE_POTIONS) {
            setMaxStackSize(item, size);
        }
    }

    private static void setMaxStackSize(Item item, int size) {
        ItemMaxStackAccessor accessor = (ItemMaxStackAccessor) (Object) item;
        if (accessor.stackablepotionsplus$getMaxStackSize() != size) {
            accessor.stackablepotionsplus$setMaxStackSize(size);
        }
    }

    /** 读取某个物品当前的堆叠上限（供界面显示与断言使用）。 */
    public static int currentMaxStackSize(Item item) {
        return ((ItemMaxStackAccessor) (Object) item).stackablepotionsplus$getMaxStackSize();
    }
}
