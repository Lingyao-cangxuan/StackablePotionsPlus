// 投掷类药水的「使用冷却」判定（喷溅 / 滞留共用）。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * 配置项 {@code cooldown.enableCooldown} → 投掷药水后的使用冷却。
 * <p>
 * 喷溅与滞留药水的 {@code use} 各自声明在子类上（{@code ThrowablePotionItem} 虽然也有 {@code use}，
 * 但两个子类都覆盖了它，注入到父类抓不到），所以两处 Mixin 都调用这里，
 * 保证「冷却时长」与「配置开关」只有一份定义。
 * <p>
 * 两个端都会执行 {@code use}，因此这里在两侧都会挂上冷却：
 * 服务端挂上是权威值，并由原版 {@code ItemCooldowns#onCooldownStarted} 把冷却同步给客户端；
 * 客户端挂上则让本地的「冷却中不能再用」判定立刻生效，不必等包回来。
 */
public final class ThrowCooldownHelper {

    /** 冷却时长（tick）。20 tick = 1 秒，与原版投掷物（末影珍珠）一致。 */
    public static final int COOLDOWN_TICKS = 20;

    private ThrowCooldownHelper() {
    }

    /** 配置开着就给这次使用挂上冷却；关着就什么都不做。 */
    public static void apply(Player user, Item item) {
        if (Config.enableCooldown.get()) {
            user.getCooldowns().addCooldown(item, COOLDOWN_TICKS);
        }
    }
}
