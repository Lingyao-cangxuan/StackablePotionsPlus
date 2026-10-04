// 模组配置界面（原版风格，布局参考矿脉重生 VeinRebirth）。
package lingyaocangxuan.stackablepotionsplus.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lingyaocangxuan.stackablepotionsplus.Config;
import lingyaocangxuan.stackablepotionsplus.ConfigFileIO;
import lingyaocangxuan.stackablepotionsplus.StackSizeApplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 药水模组配置界面。整体结构对齐原版风格与矿脉重生的界面：
 * <p>
 * <b>左侧</b>是可滚动的配置项列表：顶部搜索框，按分组分节显示，选中行高亮，
 * 列表用 scissor 裁剪，行数再多也不会溢出面板。
 * <p>
 * <b>右侧</b>「设置：&lt;分组名&gt;」下面列出该分组的全部控件 —— 数值项是滑块，
 * 开关项是可点击切换的按钮。改动会立即写入运行时的配置数值（因此「堆叠数量」拖完松手就生效），
 * 而 Forge 的配置文件对象带自动保存，所以数值同时也会落盘；
 * 点「保存配置」是手动的兜底入口，功能上等价于再写一次。
 * <p>
 * 注意滑块<b>拖动过程中不写配置</b>，松手才提交一次 —— 否则拖一次滑块会让整个 TOML
 * 重写几十遍（见 {@link OptionSlider} 的类注释）。
 * <p>
 * <b>底部</b>除了「完成」按钮，还有配置文件路径与生效说明。
 * <p>
 * 布局全部按可用空间算出来，最小逻辑分辨率 320×240 也不会重叠。
 */
@OnlyIn(Dist.CLIENT)
public class PotionConfigScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger("stackablepotionsplus-gui");

    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_BORDER = 0xFF5A5A5A;
    private static final int COLOR_TITLE = 0xFFE0E0E0;
    private static final int COLOR_HINT = 0xFF9A9A9A;
    private static final int COLOR_DIRTY = 0xFFFFAA00;
    private static final int COLOR_HEADER = 0xFFFFC060;
    private static final int COLOR_TEXT_ON = 0xFFDCDCDC;
    private static final int COLOR_SELECTED_BG = 0xFF2E5C8A;
    private static final int COLOR_HOVER_BG = 0x40FFFFFF;
    private static final int COLOR_DOT_ON = 0xFF57D75A;
    private static final int COLOR_DOT_OFF = 0xFF5A5A5A;

    /** 列表行高与滚动条宽度。 */
    private static final int LIST_ROW_H = 13;
    private static final int SCROLLBAR_W = 6;
    /** 搜索框高度。 */
    private static final int SEARCH_H = 12;

    /** 分组顺序（与 TOML 中的顺序一致）。 */
    private static final String[] GROUPS = {"general", "brewing", "effect_stacking", "cooldown"};

    private final Screen parent;
    private final List<Option> options = buildOptions();
    private final List<Row> rows = new ArrayList<>();
    /** 右侧当前这组控件（含末尾三个按钮），切换分组时整体重建。 */
    private final List<AbstractWidget> optionWidgets = new ArrayList<>();

    private Option selected;
    private boolean dirty;
    private Component status = Component.empty();
    private long statusTime;

    // ---------------- 布局（init 时算好） ----------------
    private int leftX;
    private int topY;
    private int listW;
    private int rightX;
    private int rightW;
    private int panelH;
    private int rowH;
    private int rowStep;
    private int doneY;

    private int listTop;
    private int listBottom;

    // ---------------- 列表状态 ----------------
    private EditBox searchBox;
    private String filter = "";
    private int scroll;
    private boolean draggingScrollbar;
    private int dragGrab;
    private Option hovered;

    /** 右侧面板当前展示的分组，避免同组切换条目时反复重建控件。 */
    private String builtGroup = "";

    /** 列表里的一行：要么是分组标题，要么是一项配置。 */
    private static final class Row {
        final Component header;
        final Option option;

        Row(Component header) {
            this.header = header;
            this.option = null;
        }

        Row(Option option) {
            this.header = null;
            this.option = option;
        }
    }

    public PotionConfigScreen(Screen parent) {
        super(Component.translatable("stackablepotionsplus.gui.title"));
        this.parent = parent;
    }

    // ================================================================== 初始化

    @Override
    protected void init() {
        int contentW = Math.min(this.width - 20, 470);
        this.leftX = this.width / 2 - contentW / 2;
        // 列表要放得下配置项名，宽度按比例给，并夹在合理区间里
        this.listW = Mth.clamp((int) (contentW * 0.42F), 118, 190);
        this.rightX = this.leftX + this.listW + 10;
        this.rightW = contentW - this.listW - 10;
        int available = this.height - 84;

        // 右侧行数 = 最大的那个分组的条目数 + 1 行按钮
        int maxRows = 1;
        for (String group : GROUPS) {
            maxRows = Math.max(maxRows, countIn(group));
        }
        maxRows += 1;

        // 行高按可用空间自适应：优先 20，不够就压到 18 / 16。
        // 末尾的 +5 是底部留白：控件底边距面板底边太近时视觉上会「贴着边框」。
        this.rowH = 20;
        this.rowStep = 22;
        int needed = 16 + maxRows * this.rowStep + 5;
        if (needed > available) {
            this.rowH = 18;
            this.rowStep = 20;
            needed = 16 + maxRows * this.rowStep + 5;
        }
        if (needed > available) {
            this.rowH = 16;
            this.rowStep = 17;
            needed = 16 + maxRows * this.rowStep + 5;
        }
        this.panelH = Math.min(needed, Math.max(110, available));
        this.topY = Math.max(14, (this.height - (this.panelH + 58)) / 2 + 10);

        if (this.selected == null && !this.options.isEmpty()) {
            this.selected = this.options.get(0);
        }

        // 搜索框：列表顶部，输入即时过滤
        this.searchBox = new EditBox(this.font, this.leftX + 3, this.topY + 16, this.listW - 6, SEARCH_H,
                Component.translatable("stackablepotionsplus.gui.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setHint(Component.translatable("stackablepotionsplus.gui.search.hint"));
        this.searchBox.setValue(this.filter);
        this.searchBox.setResponder(text -> {
            this.filter = text == null ? "" : text;
            this.scroll = 0;
            rebuildRows();
        });
        this.addRenderableWidget(this.searchBox);

        this.listTop = this.topY + 16 + SEARCH_H + 2;
        this.listBottom = this.topY + this.panelH - 3;
        rebuildRows();

        // 底部「完成」
        this.doneY = this.topY + this.panelH + 6;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("stackablepotionsplus.gui.button.done"), b -> this.onClose())
                .bounds(this.width / 2 - 60, this.doneY, 120, 20).build());

        // 右侧面板：resize 后 children 已被清空，缓存也要一起失效
        this.optionWidgets.clear();
        this.builtGroup = "";
        rebuildRightPanel();
    }

    private int countIn(String group) {
        int count = 0;
        for (Option option : this.options) {
            if (option.group.equals(group)) {
                count++;
            }
        }
        return count;
    }

    // ================================================================== 右侧控件

    /** 按当前选中项所属分组重建右侧控件。同一分组内切换条目不重建。 */
    private void rebuildRightPanel() {
        if (this.selected == null) {
            return;
        }
        if (this.selected.group.equals(this.builtGroup) && !this.optionWidgets.isEmpty()) {
            return;
        }

        for (AbstractWidget widget : this.optionWidgets) {
            this.removeWidget(widget);
        }
        this.optionWidgets.clear();
        this.builtGroup = this.selected.group;

        int sx = this.rightX + 4;
        int sw = this.rightW - 8;
        int y = this.topY + 16;

        for (Option option : this.options) {
            if (!option.group.equals(this.builtGroup)) {
                continue;
            }
            AbstractWidget widget = option.createWidget(sx, y, sw, this.rowH, this::markDirty);
            this.optionWidgets.add(widget);
            this.addRenderableWidget(widget);
            y += this.rowStep;
        }

        // 最后一行：保存 / 重新载入 / 恢复默认
        int buttonW = (sw - 8) / 3;
        this.optionWidgets.add(this.addRenderableWidget(Button.builder(
                        Component.translatable("stackablepotionsplus.gui.button.save"), b -> this.save())
                .bounds(sx, y, buttonW, this.rowH).build()));
        this.optionWidgets.add(this.addRenderableWidget(Button.builder(
                        Component.translatable("stackablepotionsplus.gui.button.reload"), b -> this.reload())
                .bounds(sx + buttonW + 4, y, buttonW, this.rowH).build()));
        this.optionWidgets.add(this.addRenderableWidget(Button.builder(
                        Component.translatable("stackablepotionsplus.gui.button.restore"), b -> this.restoreDefaults())
                .bounds(sx + (buttonW + 4) * 2, y, buttonW, this.rowH).build()));
    }

    /** 把控件显示值刷成配置里的当前值（重载 / 恢复默认后用）。 */
    private void syncOptionWidgets() {
        int index = 0;
        for (Option option : this.options) {
            if (!option.group.equals(this.builtGroup)) {
                continue;
            }
            if (index >= this.optionWidgets.size()) {
                return;
            }
            option.sync(this.optionWidgets.get(index));
            index++;
        }
    }

    // ================================================================== 列表数据

    private void rebuildRows() {
        this.rows.clear();
        String keyword = this.filter == null ? "" : this.filter.trim().toLowerCase(Locale.ROOT);

        if (!keyword.isEmpty()) {
            for (Option option : this.options) {
                if (option.label.getString().toLowerCase(Locale.ROOT).contains(keyword)
                        || option.path.toLowerCase(Locale.ROOT).contains(keyword)) {
                    this.rows.add(new Row(option));
                }
            }
            clampScroll();
            return;
        }

        String lastGroup = null;
        for (Option option : this.options) {
            if (!option.group.equals(lastGroup)) {
                lastGroup = option.group;
                this.rows.add(new Row(groupTitle(lastGroup)));
            }
            this.rows.add(new Row(option));
        }
        clampScroll();
    }

    private static Component groupTitle(String group) {
        return Component.translatable("stackablepotionsplus.configuration.group." + group);
    }

    private int contentHeight() {
        return this.rows.size() * LIST_ROW_H;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (this.listBottom - this.listTop));
    }

    private void clampScroll() {
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
    }

    /** 命中测试：返回鼠标所在行下标，不在列表内返回 -1。 */
    private int rowIndexAt(double mouseX, double mouseY) {
        if (mouseX < this.leftX || mouseX >= this.leftX + this.listW - SCROLLBAR_W) {
            return -1;
        }
        if (mouseY < this.listTop || mouseY >= this.listBottom) {
            return -1;
        }
        int index = (int) ((mouseY - this.listTop + this.scroll) / LIST_ROW_H);
        return (index >= 0 && index < this.rows.size()) ? index : -1;
    }

    private boolean inScrollbar(double mouseX, double mouseY) {
        return maxScroll() > 0
                && mouseX >= this.leftX + this.listW - SCROLLBAR_W && mouseX < this.leftX + this.listW
                && mouseY >= this.listTop && mouseY < this.listBottom;
    }

    private void selectOption(Option option) {
        if (option == null || option == this.selected) {
            return;
        }
        this.selected = option;
        rebuildRightPanel();
    }

    // ================================================================== 动作

    private void markDirty() {
        this.dirty = true;
        this.status = Component.empty();
    }

    private void save() {
        try {
            Config.SPEC.save();
        } catch (RuntimeException e) {
            LOGGER.error("写回配置文件失败", e);
            this.setStatus(Component.translatable("stackablepotionsplus.gui.status.save_failed"));
            return;
        }
        StackSizeApplier.apply();
        this.dirty = false;
        this.setStatus(Component.translatable("stackablepotionsplus.gui.status.saved", Config.FILE_NAME));
    }

    /**
     * 从磁盘重新读配置。
     * <p>
     * 语义是「丢弃界面上的未保存改动，回到文件里的值」—— Forge 自己也会在检测到文件被外部
     * 修改时重载，但那个时机不确定，手动改完 TOML 想立刻生效就用这个按钮。
     * <p>
     * 实际读取与校正放在 {@link ConfigFileIO#reloadFromDisk} 里，那里同时说明了
     * 为什么不能直接用 {@code ForgeConfigSpec#acceptConfig}。
     */
    private void reload() {
        Path path = ConfigFileIO.path();
        if (!Files.isRegularFile(path)) {
            this.setStatus(Component.translatable("stackablepotionsplus.gui.status.reload_failed"));
            return;
        }
        try {
            ConfigFileIO.reloadFromDisk(path);
        } catch (RuntimeException e) {
            LOGGER.error("重新读取配置文件失败", e);
            this.setStatus(Component.translatable("stackablepotionsplus.gui.status.reload_failed"));
            return;
        }
        StackSizeApplier.apply();
        syncOptionWidgets();
        this.dirty = false;
        this.setStatus(Component.translatable("stackablepotionsplus.gui.status.reloaded"));
    }

    private void restoreDefaults() {
        for (Option option : this.options) {
            option.reset();
        }
        StackSizeApplier.apply();
        syncOptionWidgets();
        this.dirty = true;
        this.setStatus(Component.translatable("stackablepotionsplus.gui.status.restored"));
    }

    private void setStatus(Component text) {
        this.status = text;
        this.statusTime = System.currentTimeMillis();
    }

    // ================================================================== 交互

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll() > 0
                && mouseX >= this.leftX && mouseX < this.leftX + this.listW
                && mouseY >= this.listTop && mouseY < this.listBottom) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.round(delta * LIST_ROW_H * 2), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inScrollbar(mouseX, mouseY)) {
                this.draggingScrollbar = true;
                this.dragGrab = (int) mouseY - thumbTop();
                return true;
            }
            int index = rowIndexAt(mouseX, mouseY);
            if (index >= 0) {
                Option option = this.rows.get(index).option;
                if (option != null) {
                    selectOption(option);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            scrollThumbTo((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int trackHeight() {
        return this.listBottom - this.listTop;
    }

    private int thumbHeight() {
        int content = contentHeight();
        if (content <= 0) {
            return trackHeight();
        }
        return Math.max(10, trackHeight() * trackHeight() / content);
    }

    private int thumbTop() {
        int max = maxScroll();
        if (max <= 0) {
            return this.listTop;
        }
        return this.listTop + (trackHeight() - thumbHeight()) * this.scroll / max;
    }

    private void scrollThumbTo(int mouseY) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int travel = trackHeight() - thumbHeight();
        if (travel <= 0) {
            return;
        }
        int top = Mth.clamp(mouseY - this.dragGrab, this.listTop, this.listTop + travel);
        this.scroll = (top - this.listTop) * max / travel;
    }

    // ================================================================== 渲染

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        drawPanel(graphics, this.leftX, this.topY, this.listW, this.panelH);
        drawPanel(graphics, this.rightX, this.topY, this.rightW, this.panelH);

        renderList(graphics, mouseX, mouseY);

        // 用自绘遍历替代 super.render：见 WidgetPainter 的说明
        WidgetPainter.renderWidgets(this, graphics, mouseX, mouseY, partialTick);

        renderHeadings(graphics);

        // 底部提示：有状态消息时优先显示状态。
        // 窄屏（320×240）下完整提示会宽过屏幕、被边缘切成半截话，所以超宽就换短版。
        graphics.drawCenteredString(this.font, fit(
                        Component.translatable("stackablepotionsplus.gui.hint.file", Config.FILE_NAME),
                        Component.literal("config/" + Config.FILE_NAME)),
                this.width / 2, this.doneY + 24, COLOR_HINT);
        if (!this.status.getString().isEmpty() && System.currentTimeMillis() - this.statusTime < 5000L) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, this.doneY + 36, 0xFFFFFF);
        } else if (this.doneY + 45 <= this.height) {
            graphics.drawCenteredString(this.font, fit(
                            Component.translatable("stackablepotionsplus.gui.hint.instant"),
                            Component.translatable("stackablepotionsplus.gui.hint.instant.short")),
                    this.width / 2, this.doneY + 36, COLOR_HINT);
        }

        // 悬停提示放在最后，避免被其它元素盖住
        if (this.hovered != null) {
            graphics.renderTooltip(this.font,
                    Component.literal(this.hovered.path), mouseX, mouseY);
        }
    }

    /** 放得下就用长的，放不下换短的（宁可信息少一点，也不要被屏幕边缘切成半截话）。 */
    private Component fit(Component full, Component fallback) {
        return this.font.width(full) > this.width - 8 ? fallback : full;
    }

    /** 标题、面板标题、未保存标记。 */
    private void renderHeadings(GuiGraphics graphics) {
        int titleY = Math.max(4, this.topY - 12);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, COLOR_TITLE);

        // 左侧：列表标题 + 计数
        graphics.drawString(this.font, Component.translatable("stackablepotionsplus.gui.list.title"),
                this.leftX + 4, this.topY + 5, COLOR_TITLE, false);
        // 计数只算配置项，不含分组标题行
        Component count = Component.translatable("stackablepotionsplus.gui.list.count", this.options.size());
        graphics.drawString(this.font, count,
                this.leftX + this.listW - 5 - this.font.width(count), this.topY + 5, COLOR_HINT, false);

        // 右侧：当前分组名。用 scissor 限制在标题条内，避免文字溢到面板外面。
        Component name = Component.translatable("stackablepotionsplus.gui.panel.title",
                this.selected == null ? Component.empty() : groupTitle(this.selected.group));
        graphics.enableScissor(this.rightX + 1, this.topY, this.rightX + this.rightW - 1, this.topY + 15);
        try {
            graphics.drawString(this.font, name, this.rightX + 4, this.topY + 5, COLOR_TITLE, false);
        } finally {
            graphics.disableScissor();
        }

        if (this.dirty) {
            Component text = Component.translatable("stackablepotionsplus.gui.dirty");
            graphics.drawString(this.font, Component.literal("§6").append(text),
                    this.width - 4 - this.font.width(text), titleY, COLOR_DIRTY, false);
        }
    }

    /** 左侧可滚动列表：scissor 裁剪 + 只绘制可见行。 */
    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        this.hovered = null;
        if (this.listBottom <= this.listTop || this.rows.isEmpty()) {
            return;
        }

        int clipRight = this.leftX + this.listW - SCROLLBAR_W;
        graphics.enableScissor(this.leftX + 1, this.listTop, clipRight, this.listBottom);
        try {
            int first = Math.max(0, this.scroll / LIST_ROW_H);
            int last = Math.min(this.rows.size() - 1, (this.scroll + trackHeight()) / LIST_ROW_H);
            for (int index = first; index <= last; index++) {
                Row row = this.rows.get(index);
                int y = this.listTop + index * LIST_ROW_H - this.scroll;

                if (row.header != null) {
                    graphics.drawString(this.font, row.header, this.leftX + 4, y + 2, COLOR_HEADER, false);
                    graphics.fill(this.leftX + 3, y + LIST_ROW_H - 1, clipRight - 2, y + LIST_ROW_H, 0x50FFC060);
                    continue;
                }

                Option option = row.option;
                boolean selectedRow = option == this.selected;
                boolean rowHovered = mouseX >= this.leftX && mouseX < clipRight
                        && mouseY >= Math.max(y, this.listTop) && mouseY < Math.min(y + LIST_ROW_H, this.listBottom);

                if (selectedRow) {
                    graphics.fill(this.leftX + 1, y, clipRight, y + LIST_ROW_H, COLOR_SELECTED_BG);
                } else if (rowHovered) {
                    graphics.fill(this.leftX + 1, y, clipRight, y + LIST_ROW_H, COLOR_HOVER_BG);
                }

                // 左侧状态色条：绿=该组正在右侧显示
                boolean shown = this.selected != null && option.group.equals(this.selected.group);
                graphics.fill(this.leftX + 2, y + 2, this.leftX + 4, y + LIST_ROW_H - 2,
                        shown ? COLOR_DOT_ON : COLOR_DOT_OFF);

                int color = selectedRow ? 0xFFFFFFFF : COLOR_TEXT_ON;
                graphics.drawString(this.font, option.label, this.leftX + 7, y + 2, color, false);

                if (rowHovered) {
                    this.hovered = option;
                }
            }
        } finally {
            graphics.disableScissor();
        }

        renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (maxScroll() <= 0) {
            return;
        }
        int trackX = this.leftX + this.listW - SCROLLBAR_W;
        int top = thumbTop();
        graphics.fill(trackX, this.listTop, trackX + SCROLLBAR_W - 1, this.listBottom, 0x50000000);
        graphics.fill(trackX, top, trackX + SCROLLBAR_W - 1, top + thumbHeight(), 0xFF9A9A9A);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    // ================================================================== 生命周期

    @Override
    public void removed() {
        // 关了界面但没点保存：脏改动直接落盘，避免「界面里改了却重启后消失」
        if (this.dirty) {
            try {
                Config.SPEC.save();
                StackSizeApplier.apply();
                this.dirty = false;
            } catch (RuntimeException e) {
                LOGGER.error("关闭界面时写回配置文件失败", e);
            }
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ================================================================== 配置项定义

    /** 界面里的一项配置。 */
    private abstract static class Option {
        final String group;
        final Component label;
        /** TOML 里的完整键路径，悬停时显示。 */
        final String path;

        Option(String group, Component label, String path) {
            this.group = group;
            this.label = label;
            this.path = path;
        }

        abstract AbstractWidget createWidget(int x, int y, int w, int h, Runnable onChanged);

        abstract void sync(AbstractWidget widget);

        abstract void reset();
    }

    /** 数值型配置项：滑块。 */
    private static final class SliderOption extends Option {
        final int min;
        final int max;
        final int defaultValue;
        final IntSupplier getter;
        final IntConsumer setter;
        final IntFunction<String> formatter;

        SliderOption(String group, Component label, String path, int min, int max, int defaultValue,
                     IntSupplier getter, IntConsumer setter, IntFunction<String> formatter) {
            super(group, label, path);
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
            this.formatter = formatter;
        }

        @Override
        AbstractWidget createWidget(int x, int y, int w, int h, Runnable onChanged) {
            return new OptionSlider(x, y, w, h, this.label, this.min, this.max, current(),
                    this.formatter, value -> {
                        this.setter.accept(value);
                        onChanged.run();
                    });
        }

        @Override
        void sync(AbstractWidget widget) {
            ((OptionSlider) widget).setIntValue(current());
        }

        @Override
        void reset() {
            this.setter.accept(this.defaultValue);
        }

        /** 配置值可能超出滑块量程（例如在 TOML 里手写了更大的时长上限），显示时夹紧。 */
        private int current() {
            return Mth.clamp(this.getter.getAsInt(), this.min, this.max);
        }
    }

    /** 开关型配置项：点击切换的按钮（布尔值或二选一枚举）。 */
    private static final class ToggleOption extends Option {
        final boolean defaultValue;
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;
        final Supplier<Component> valueText;

        ToggleOption(String group, Component label, String path, boolean defaultValue,
                     BooleanSupplier getter, Consumer<Boolean> setter, Supplier<Component> valueText) {
            super(group, label, path);
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
            this.valueText = valueText;
        }

        @Override
        AbstractWidget createWidget(int x, int y, int w, int h, Runnable onChanged) {
            return Button.builder(display(), button -> {
                this.setter.accept(!this.getter.getAsBoolean());
                onChanged.run();
                button.setMessage(display());
            }).bounds(x, y, w, h).build();
        }

        @Override
        void sync(AbstractWidget widget) {
            ((Button) widget).setMessage(display());
        }

        @Override
        void reset() {
            this.setter.accept(this.defaultValue);
        }

        private Component display() {
            return Component.translatable("stackablepotionsplus.gui.toggle", this.label, this.valueText.get());
        }
    }

    private static Supplier<Component> onOff(BooleanSupplier value) {
        return () -> Component.translatable(value.getAsBoolean()
                ? "stackablepotionsplus.gui.on"
                : "stackablepotionsplus.gui.off");
    }

    private static List<Option> buildOptions() {
        List<Option> list = new ArrayList<>();

        // ---------------- 通用 ----------------
        list.add(new SliderOption("general",
                Component.translatable("stackablepotionsplus.gui.option.potionStackSize"),
                "general.potionStackSize", 1, 64, Config.potionStackSize.getDefault(),
                Config.potionStackSize::get,
                value -> {
                    Config.potionStackSize.set(value);
                    // 堆叠上限写在物品实例上，改完必须重新应用，否则要重启才生效
                    StackSizeApplier.apply();
                },
                Integer::toString));

        // ---------------- 酿造台 ----------------
        list.add(new SliderOption("brewing",
                Component.translatable("stackablepotionsplus.gui.option.potionSlotCapacity"),
                "brewing.potionSlotCapacity", 1, 64, Config.potionSlotCapacity.getDefault(),
                Config.potionSlotCapacity::get, Config.potionSlotCapacity::set,
                Integer::toString));

        // ---------------- 效果叠加（顺序与 TOML 一致） ----------------
        list.add(new SliderOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.maxAmplifier"),
                "effect_stacking.maxAmplifier", 0, 127, Config.maxAmplifier.getDefault(),
                Config.maxAmplifier::get, Config.maxAmplifier::set,
                Integer::toString));

        list.add(new SliderOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.durationCapSeconds"),
                "effect_stacking.durationCapSeconds", 0, 3600, Config.durationCapSeconds.getDefault(),
                Config.durationCapSeconds::get, Config.durationCapSeconds::set,
                value -> value <= 0
                        ? Component.translatable("stackablepotionsplus.gui.unlimited").getString()
                        : Integer.toString(value)));

        list.add(new ToggleOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.infiniteDuration"),
                "effect_stacking.infiniteDuration", Config.infiniteDuration.getDefault(),
                Config.infiniteDuration::get, Config.infiniteDuration::set,
                onOff(Config.infiniteDuration::get)));

        list.add(new ToggleOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.stackNegativeEffects"),
                "effect_stacking.stackNegativeEffects", Config.stackNegativeEffects.getDefault(),
                Config.stackNegativeEffects::get, Config.stackNegativeEffects::set,
                onOff(Config.stackNegativeEffects::get)));

        list.add(new ToggleOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.enableInstantStacking"),
                "effect_stacking.enableInstantStacking", Config.enableInstantStacking.getDefault(),
                Config.enableInstantStacking::get, Config.enableInstantStacking::set,
                onOff(Config.enableInstantStacking::get)));

        list.add(new SliderOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.instantStackWindowSeconds"),
                "effect_stacking.instantStackWindowSeconds", 1, 600,
                (int) Math.round(Config.instantStackWindowSeconds.getDefault() * 10.0D),
                () -> (int) Math.round(Config.instantStackWindowSeconds.get() * 10.0D),
                value -> Config.instantStackWindowSeconds.set(value / 10.0D),
                value -> String.format(Locale.ROOT, "%.1f", value / 10.0D)));

        list.add(new ToggleOption("effect_stacking",
                Component.translatable("stackablepotionsplus.gui.option.stackTrigger"),
                "effect_stacking.stackTrigger",
                Config.stackTrigger.getDefault() == Config.StackTrigger.ALL,
                () -> Config.stackTrigger.get() == Config.StackTrigger.ALL,
                value -> Config.stackTrigger.set(value ? Config.StackTrigger.ALL
                        : Config.StackTrigger.POTION_USE_ONLY),
                () -> Component.translatable(Config.stackTrigger.get() == Config.StackTrigger.ALL
                        ? "stackablepotionsplus.gui.trigger.all"
                        : "stackablepotionsplus.gui.trigger.potion_only")));

        // ---------------- 使用冷却 ----------------
        list.add(new ToggleOption("cooldown",
                Component.translatable("stackablepotionsplus.gui.option.enableCooldown"),
                "cooldown.enableCooldown", Config.enableCooldown.getDefault(),
                Config.enableCooldown::get, Config.enableCooldown::set,
                onOff(Config.enableCooldown::get)));

        return list;
    }
}
