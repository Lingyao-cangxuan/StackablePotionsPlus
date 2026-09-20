// 「玩家主动使用药水」调用链的上下文标记。
//
// 背景：效果叠加逻辑挂在 MobEffectInstance#update(MobEffectInstance) 上，而这是
// 任何模组给实体施加效果时都会经过的汇聚点，方法签名里没有「来源」这一维信息。
// 结果：若有模组（如饰品）每个游戏刻都施加一次同种效果，等级会在几秒内冲到上限。
//
// 做法：只在玩家主动使用的调用链上打开标记，update() 处据此决定是否接管。
//   - PotionItem#finishUsingItem  → 玩家饮用（含其它模组继承 PotionItem 的药水）
//   - ThrownPotion#applySplash    → 玩家投掷的喷溅/滞留药水命中
// 其余一切来源（饰品、命令、环境效果等）不打开标记，走原版「取更强/更久」。
package lingyaocangxuan.stackablepotionsplus;

public final class StackSourceContext {
    // 泄漏看门狗：标记的进出分别由两个独立的注入点负责（HEAD / RETURN），
    // 若方法中途抛出异常，exit 不会执行。超过此时长仍处于「激活」状态即视为泄漏并重置。
    private static final long LEAK_TIMEOUT_NANOS = 2_000_000_000L;

    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private StackSourceContext() {
    }

    /** 进入一条「玩家主动使用药水」的调用链。可嵌套。 */
    public static void enter() {
        State state = STATE.get();
        long now = System.nanoTime();
        if (state.depth > 0 && now - state.stamp > LEAK_TIMEOUT_NANOS) {
            state.depth = 0;
        }
        state.depth++;
        state.stamp = now;
    }

    /** 离开当前调用链。与 {@link #enter()} 成对调用。 */
    public static void exit() {
        State state = STATE.get();
        if (state.depth > 0) {
            state.depth--;
        }
        if (state.depth == 0) {
            state.stamp = 0L;
        }
    }

    /** 当前线程是否位于玩家主动使用药水的调用链内。 */
    public static boolean isActive() {
        State state = STATE.get();
        if (state.depth <= 0) {
            return false;
        }
        if (System.nanoTime() - state.stamp > LEAK_TIMEOUT_NANOS) {
            state.depth = 0;
            state.stamp = 0L;
            return false;
        }
        return true;
    }

    private static final class State {
        int depth;
        long stamp;
    }
}
