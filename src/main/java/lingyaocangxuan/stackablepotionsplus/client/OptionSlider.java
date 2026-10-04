// 原版风格的整数滑块（配置界面用）。
package lingyaocangxuan.stackablepotionsplus.client;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 原版风格的整数滑块：拖动时标签实时显示「名称：数值」，<b>松手时才回调</b>。
 * <p>
 * 滑块内部只认整数（{@code value} 是 0~1 的比例），小数配置项由调用方用
 * {@code scale} 折算（例：瞬间叠加窗口 0.1~60.0 秒 → 内部 1~600，显示时除以 10）。
 * <p>
 * <b>为什么拖完才回调，而不是拖一下回调一次：</b>
 * Forge 给模组配置用的 {@code CommentedFileConfig} 带自动保存，
 * {@code ConfigValue#set()} 会顺着 {@code spec.childConfig.set()} 把整个 TOML 立刻写回磁盘。
 * 拖动时 {@code applyValue()} 每帧都会触发，若在里面直接 set，
 * 拖一次滑块就会把配置文件重写几十遍（还可能触发磁盘同步），造成明显卡顿。
 * 所以这里把「改内存数值」推迟到 {@link #onRelease}，拖动过程只更新标签。
 */
@OnlyIn(Dist.CLIENT)
public class OptionSlider extends AbstractSliderButton {

    private final Component label;
    private final int min;
    private final int max;
    private final IntFunction<String> formatter;
    private final IntConsumer onCommit;
    /** 构造期 super() 会调 updateMessage()/applyValue()，此时字段还没赋值，先屏蔽。 */
    private boolean ready;

    public OptionSlider(int x, int y, int width, int height, Component label,
                        int min, int max, int value, IntFunction<String> formatter, IntConsumer onCommit) {
        super(x, y, width, height, label, toFraction(min, max, value));
        this.label = label;
        this.min = min;
        this.max = max;
        this.formatter = formatter;
        this.onCommit = onCommit;
        this.ready = true;
        this.updateMessage();
    }

    public int getIntValue() {
        return this.min + (int) Math.round(this.value * (double) (this.max - this.min));
    }

    /** 直接设置数值，不触发回调（用于切换分组、重载、恢复默认后刷新显示）。 */
    public void setIntValue(int value) {
        this.value = toFraction(this.min, this.max, value);
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (!this.ready) {
            return;
        }
        this.setMessage(Component.translatable("stackablepotionsplus.gui.slider",
                this.label, this.formatter.apply(this.getIntValue())));
    }

    /** 拖动过程中每帧调用：只刷新标签，不碰配置（原因见类注释）。 */
    @Override
    protected void applyValue() {
        // 父类 setValue() 已经调过 updateMessage()，这里刻意留空
    }

    /** 松手（含点击轨道跳转）后提交一次。 */
    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        if (this.ready) {
            this.onCommit.accept(this.getIntValue());
        }
    }

    private static double toFraction(int min, int max, int value) {
        if (max <= min) {
            return 0.0D;
        }
        return (double) (Mth.clamp(value, min, max) - min) / (double) (max - min);
    }
}
