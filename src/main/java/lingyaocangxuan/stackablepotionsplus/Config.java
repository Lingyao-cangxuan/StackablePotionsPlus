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
    public static final ForgeConfigSpec.EnumValue<StackTrigger> stackTrigger;
    public static final ForgeConfigSpec.IntValue potionSlotCapacity;

    /** 决定哪些来源施加的效果会触发「叠加时长与等级」。 */
    public enum StackTrigger {
        /** 仅玩家主动饮用/投掷药水时叠加；其余来源走原版行为。（默认，推荐） */
        POTION_USE_ONLY,
        /** 任何来源施加同种效果都叠加。（旧行为，有失控风险） */
        ALL
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("酿造台批量炼药设置", "Brewing stand batch settings")
                .push("brewing");
        potionSlotCapacity = builder
                .comment("酿造台每个药水槽的容量上限（单位：瓶）。原版硬编码为 1，这是「堆叠药水放不进去」的直接原因。",
                        "药水物品自身的堆叠上限（64）由本模组另行设置，此项独立控制槽位容量。",
                        "设为 1 恢复原版行为。",
                        "Vanilla hardcodes the potion slot capacity to 1, which blocks stacked potions.")
                .translation("stackablepotionsplus.configuration.potionSlotCapacity")
                .defineInRange("potionSlotCapacity", 64, 1, 64);
        builder.pop();

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
        stackTrigger = builder
                .comment("决定哪些来源施加的药水效果会触发「叠加时长 + 提升等级」。",
                        "",
                        "POTION_USE_ONLY（默认，推荐）：",
                        "  仅当玩家主动饮用或投掷药水时叠加。",
                        "  其他模组（饰品、被动效果）、/effect 命令、环境效果等直接施加的效果",
                        "  一律走原版行为（取更强/更久，不累加）。",
                        "  这一项是为了防止「每游戏刻施加一次效果」的模组把等级瞬间顶到 maxAmplifier 上限。",
                        "  注意：叠加需要「有玩家参与」——生物自行饮用（如女巫）不触发。",
                        "",
                        "ALL：",
                        "  任何来源施加同种效果都叠加，等同于本模组 1.4.4 及更早的行为。",
                        "  警告：若整合包内存在每 tick 施加效果的模组（常见于饰品类），",
                        "  等级会在数秒内冲到 maxAmplifier 上限，通常不是你想要的结果。",
                        "",
                        "Which sources may trigger effect stacking.",
                        "POTION_USE_ONLY: only when a player drinks or throws a potion (recommended).",
                        "ALL: any source stacks (legacy behaviour, may run away).")
                .translation("stackablepotionsplus.configuration.stackTrigger")
                .defineEnum("stackTrigger", StackTrigger.POTION_USE_ONLY);
        builder.pop();

        builder.comment("使用冷却设置", "Cooldown settings")
                .push("cooldown");
        enableCooldown = builder
                .comment("为喷溅药水启用 1 秒（20 tick）使用冷却。默认关闭。",
                        "注意：原版 1.20.1 的投掷类药水本身没有使用冷却，本项是「新增」而非「恢复」原版限制。",
                        "当前仅对喷溅药水生效，滞留药水不受影响。")
                .translation("stackablepotionsplus.configuration.enableCooldown")
                .define("enableCooldown", false);
        builder.pop();

        SPEC = builder.build();
    }
}
