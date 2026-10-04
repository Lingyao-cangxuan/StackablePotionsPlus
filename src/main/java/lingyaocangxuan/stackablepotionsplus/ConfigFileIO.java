// 配置文件的磁盘读取助手：把 TOML 里的值灌回内存中的配置对象。
package lingyaocangxuan.stackablepotionsplus;

import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 「重新载入」按钮背后的实现。
 * <p>
 * <b>为什么不直接用 {@code ForgeConfigSpec#acceptConfig}：</b>
 * 那个方法会把 spec 的后备 {@code Config} 整个换成传进来的对象
 * （{@code setConfig()} 里第一行就是 {@code this.childConfig = config}）。
 * 我们传的是自己 new 出来的临时 {@code CommentedFileConfig}，用完 {@code close()} 掉以后，
 * spec 的后备对象就是一个已关闭的文件对象 —— 之后每一次 {@code SPEC.save()} 都会抛
 * {@code IllegalStateException: Cannot save a closed FileConfig}。
 * 症状是「点过一次『重新载入』之后，『保存』永远失败」。
 * <p>
 * <b>这里改成</b>「读临时文件 → {@code correct()} 校正 → 逐项 {@code set()} 灌回」：
 * spec 的后备对象始终是 Forge 自己创建并持有的那个，{@code save()} 一直可用，
 * 临时文件也能安全关闭。
 * <p>
 * {@code set()} 会同时刷新 {@code ConfigValue} 的缓存（{@code ConfigValue#set} 里
 * {@code this.cachedValue = value}），所以之后 {@code get()} 拿到的就是新值。
 */
public final class ConfigFileIO {

    private static final Logger LOGGER = LogManager.getLogger(StackablePotions.MOD_ID);

    private static final List<String> P_STACK_SIZE = List.of("general", "potionStackSize");
    private static final List<String> P_SLOT_CAPACITY = List.of("brewing", "potionSlotCapacity");
    private static final List<String> P_MAX_AMPLIFIER = List.of("effect_stacking", "maxAmplifier");
    private static final List<String> P_DURATION_CAP = List.of("effect_stacking", "durationCapSeconds");
    private static final List<String> P_INFINITE = List.of("effect_stacking", "infiniteDuration");
    private static final List<String> P_NEGATIVE = List.of("effect_stacking", "stackNegativeEffects");
    private static final List<String> P_INSTANT = List.of("effect_stacking", "enableInstantStacking");
    private static final List<String> P_INSTANT_WINDOW = List.of("effect_stacking", "instantStackWindowSeconds");
    private static final List<String> P_TRIGGER = List.of("effect_stacking", "stackTrigger");
    private static final List<String> P_COOLDOWN = List.of("cooldown", "enableCooldown");

    private ConfigFileIO() {
    }

    /** 本模组配置文件的绝对路径。 */
    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(Config.FILE_NAME);
    }

    /**
     * 从磁盘重读配置，把值写进内存中的 {@link Config} 对象。
     * <p>
     * 越界/非法的值会先经过 {@code ForgeConfigSpec#correct}：数字被夹到区间端点
     * （{@code 200 → 64}、{@code -5 → 1}，见 {@code Range#correct}），
     * 非法枚举/类型不符的项被换成默认值，并打一条 WARN 日志。
     * <p>
     * 本方法<b>不自己写文件</b>：这里只负责把校正后的值灌进内存。
     * 至于落盘 —— Forge 给模组配置用的 {@code CommentedFileConfig} 带自动保存，
     * {@code ConfigValue#set()} 会顺手把整份 TOML 写回磁盘，所以校正结果实际也会被写出去
     * （和游戏启动时 Forge 自己校正＋回写的行为一致），但那是配置对象自己的机制，
     * 不是我们在这里手动 {@code save()}。
     *
     * @param path 配置文件路径
     * @return 被自动修正的键数量；0 表示文件本来就合法
     */
    public static int reloadFromDisk(Path path) {
        CommentedFileConfig file = CommentedFileConfig.builder(path).build();
        try {
            file.load();

            int corrected = Config.SPEC.correct(file, (action, keyPath, wrongValue, rightValue) ->
                    LOGGER.warn("[{}] 配置项 {} 的值 {} 不合法，已修正为 {}（点界面上的「保存配置」可写回文件）",
                            StackablePotions.MOD_ID, String.join(".", keyPath), wrongValue, rightValue));

            Config.potionStackSize.set(file.getIntOrElse(P_STACK_SIZE, Config.potionStackSize.getDefault()));
            Config.potionSlotCapacity.set(file.getIntOrElse(P_SLOT_CAPACITY, Config.potionSlotCapacity.getDefault()));
            Config.maxAmplifier.set(file.getIntOrElse(P_MAX_AMPLIFIER, Config.maxAmplifier.getDefault()));
            Config.durationCapSeconds.set(file.getIntOrElse(P_DURATION_CAP, Config.durationCapSeconds.getDefault()));
            Config.infiniteDuration.set(file.getOrElse(P_INFINITE, Config.infiniteDuration.getDefault()));
            Config.stackNegativeEffects.set(file.getOrElse(P_NEGATIVE, Config.stackNegativeEffects.getDefault()));
            Config.enableInstantStacking.set(file.getOrElse(P_INSTANT, Config.enableInstantStacking.getDefault()));
            // night-config 没有 getDouble()，只能先取 Object 再判类型（Forge 自己也是这么写的）
            Object window = file.get(P_INSTANT_WINDOW);
            Config.instantStackWindowSeconds.set(window instanceof Number number
                    ? number.doubleValue()
                    : Config.instantStackWindowSeconds.getDefault());
            Config.stackTrigger.set(file.getEnumOrElse(P_TRIGGER, Config.stackTrigger.getDefault()));
            Config.enableCooldown.set(file.getOrElse(P_COOLDOWN, Config.enableCooldown.getDefault()));

            return corrected;
        } finally {
            file.close();
        }
    }
}
