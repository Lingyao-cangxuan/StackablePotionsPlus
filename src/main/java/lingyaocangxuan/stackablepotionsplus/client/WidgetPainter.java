// 手动遍历渲染界面自带的控件，替代 super.render(...)。
package lingyaocangxuan.stackablepotionsplus.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 自绘界面渲染控件用。
 * <p>
 * <b>为什么不直接调 {@code super.render(...)}：</b> 1.20.2 起原版把 {@code renderBackground}
 * 挪进了 {@code Screen.render}，于是 {@code super.render} 会把之前画好的面板整片重画掉，
 * 表现为「面板和列表消失，只剩标题和控件」。本模组编译目标是 1.20.1（那里还没这个问题），
 * 但保持这个写法可以让渲染顺序固定为
 * 「背景 → 面板与列表 → 控件 → 标题与提示」，以后升版本不必再改。
 * <p>
 * {@code Screen.renderables} 是 private，子类拿不到；{@code Screen.children()} 是 public，
 * 且 {@code addRenderableWidget(...)} 会同时加进 children 和 renderables，
 * 所以「children 里实现了 Renderable 的成员」就等于原来的 renderables。
 */
@OnlyIn(Dist.CLIENT)
final class WidgetPainter {

    private WidgetPainter() {
    }

    static void renderWidgets(Screen screen, GuiGraphics graphics,
                              int mouseX, int mouseY, float partialTick) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
