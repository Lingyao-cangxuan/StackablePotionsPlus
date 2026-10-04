// 运行时改写 Item#maxStackSize 的访问器（配置界面改堆叠数量后用）。
package lingyaocangxuan.stackablepotionsplus.mixin;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code Item#maxStackSize} 是 {@code private final int}，普通反射无法直接赋值。
 * <p>
 * {@link Mutable} 让 Mixin 在应用访问器时去掉字段的 {@code final} 修饰，从而可以在运行时写入。
 * 这是「配置界面改堆叠上限后立即生效」的关键一环 —— 物品的堆叠上限在注册期就固化了，
 * 光改配置文件不会影响已注册的物品实例。
 * <p>
 * 只读写这一个字段，不接管任何方法，所以与其它模组冲突的面很小。
 */
@Mixin(Item.class)
public interface ItemMaxStackAccessor {

    @Accessor("maxStackSize")
    int stackablepotionsplus$getMaxStackSize();

    @Mutable
    @Accessor("maxStackSize")
    void stackablepotionsplus$setMaxStackSize(int value);
}
