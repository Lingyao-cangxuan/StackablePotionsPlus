// 配置加载 / 重载后，把数值同步到运行时。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 配置就绪后把「堆叠上限」写进药水物品。
 * <p>
 * Forge 只负责把 TOML 读进 {@code ForgeConfigSpec}，不会主动把数值应用到已注册的物品上，
 * 所以这一步必须自己做。两个时机都要覆盖：
 * <ul>
 *   <li>{@code Loading} —— 游戏启动时首次读配置；</li>
 *   <li>{@code Reloading} —— 玩家在外部编辑器改完 TOML 后，Forge 的文件监视器触发重载。</li>
 * </ul>
 * 配置界面里的「保存」按钮不走这两个事件（是程序内直接 set 值），所以那边会单独调一次
 * {@link StackSizeApplier#apply()}。
 */
@Mod.EventBusSubscriber(modid = StackablePotions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {

    private ConfigEvents() {
    }

    @SubscribeEvent
    public static void onLoading(ModConfigEvent.Loading event) {
        applyIfOurs(event);
    }

    @SubscribeEvent
    public static void onReloading(ModConfigEvent.Reloading event) {
        applyIfOurs(event);
    }

    private static void applyIfOurs(ModConfigEvent event) {
        // 本模组只注册了 COMMON，但按 spec 判断更稳：将来加客户端配置也不会误触发。
        if (event.getConfig().getSpec() == Config.SPEC) {
            StackSizeApplier.apply();
        }
    }
}
