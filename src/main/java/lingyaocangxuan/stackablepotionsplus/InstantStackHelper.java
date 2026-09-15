// 瞬间效果（治疗/伤害）短窗口叠加的计数工具。
package lingyaocangxuan.stackablepotionsplus;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

public final class InstantStackHelper {
    // 窗口记忆：实体 → 效果 → [时刻, 强度]
    private static final Map<LivingEntity, Map<MobEffect, long[]>> TRACKER = new WeakHashMap<>();

    private InstantStackHelper() {
    }

    /** 返回本次瞬间效果应使用的 amplifier。 */
    public static int stackedAmplifier(LivingEntity target, MobEffect effect, int incoming) {
        if (effect == null || target == null || !Config.enableInstantStacking.get()) {
            return incoming;
        }
        Level level = target.level();
        if (level.isClientSide) {
            return incoming;
        }
        long now = level.getGameTime();
        long windowTicks = (long) (Config.instantStackWindowSeconds.get() * 20.0);
        if (windowTicks <= 0) {
            return incoming;
        }
        int cap = Math.min(Config.maxAmplifier.get(), 127);

        int result;
        synchronized (TRACKER) {
            Map<MobEffect, long[]> byEffect = TRACKER.computeIfAbsent(target, k -> new WeakHashMap<>());
            long[] state = byEffect.get(effect);
            if (state == null || now - state[0] > windowTicks) {
                state = new long[]{now, incoming};
                byEffect.put(effect, state);
                result = incoming;
            } else {
                state[0] = now;
                state[1] = Math.min(Math.max(state[1] + 1, incoming), cap);
                result = (int) state[1];
            }
        }
        return result;
    }

    /** 复制实例并替换 amplifier。 */
    public static MobEffectInstance rebuildWithAmplifier(MobEffectInstance original, int amplifier) {
        return new MobEffectInstance(original.getEffect(), original.getDuration(), amplifier,
                original.isAmbient(), original.isVisible(), original.showIcon());
    }
}
