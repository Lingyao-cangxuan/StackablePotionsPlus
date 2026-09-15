// 配置项定义与默认值。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue maxAmplifier;
    public static final ForgeConfigSpec.IntValue durationCapSeconds;
    public static final ForgeConfigSpec.BooleanValue enableCooldown;
    public static final ForgeConfigSpec.BooleanValue infiniteDuration;
    public static final ForgeConfigSpec.BooleanValue stackNegativeEffects;
    public static final ForgeConfigSpec.BooleanValue enableInstantStacking;
    public static final ForgeConfigSpec.DoubleValue instantStackWindowSeconds;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("药水效果叠加设置", "Settings for stacking potion effects")
                .push("effect_stacking");
        maxAmplifier = builder
                .comment("效果可叠加到的最高等级（amplifier），I级=0，II级=1，以此类推。V级=4",
                        "硬性上限：最大 amplifier=127（即128级）。原版在 amplifier 过高时跳跃提升等效果会出现无法跳跃的 bug，故设置此硬上限",
                        "Hard cap: amplifier max 127 (= level 128). Higher values break jump boost etc. in vanilla")
                .translation("stackablepotionsplus.configuration.maxAmplifier")
                .defineInRange("maxAmplifier", 4, 0, 127);
        durationCapSeconds = builder
                .comment("重复获得同种效果时，叠加后的总时长上限（单位：秒）。设为 0 表示不限时（仅受游戏整数上限约束）")
                .translation("stackablepotionsplus.configuration.durationCapSeconds")
                .defineInRange("durationCapSeconds", 0, 0, Integer.MAX_VALUE / 20);
        infiniteDuration = builder
                .comment("开启后，叠加的药水效果将永不衰减（无限时长），不再受 durationCapSeconds 限制。默认关闭")
                .translation("stackablepotionsplus.configuration.infiniteDuration")
                .define("infiniteDuration", false);
        stackNegativeEffects = builder
                .comment("是否允许负面效果（中毒、缓慢、虚弱等）也叠加等级与时长。默认关闭：负面效果保持原版行为（取更强/更久，不逐级叠加）")
                .translation("stackablepotionsplus.configuration.stackNegativeEffects")
                .define("stackNegativeEffects", false);
        enableInstantStacking = builder
                .comment("瞬间效果叠加：允许治疗/伤害等瞬间效果（Instant Health/Damage）在短时间内被连续施加时逐级增强。",
                        "默认关闭。开启后，在下方时间窗口内重复施加同种瞬间效果，每次强度 +1（受 maxAmplifier 硬上限约束）")
                .translation("stackablepotionsplus.configuration.enableInstantStacking")
                .define("enableInstantStacking", false);
        instantStackWindowSeconds = builder
                .comment("瞬间效果叠加的时间窗口（单位：秒）。在该窗口内连续施加同种瞬间效果才会逐级增强，超过窗口则重新计数",
                        "Instant stacking time window in seconds")
                .translation("stackablepotionsplus.configuration.instantStackWindowSeconds")
                .defineInRange("instantStackWindowSeconds", 1.0, 0.1, 60.0);
        builder.pop();

        builder.comment("使用冷却设置", "Cooldown settings")
                .push("cooldown");
        enableCooldown = builder
                .comment("为喷溅/滞留药水启用 1 秒（20 tick）使用冷却。默认关闭（模组已移除原版此限制）")
                .translation("stackablepotionsplus.configuration.enableCooldown")
                .define("enableCooldown", false);
        builder.pop();

        SPEC = builder.build();
    }
}
