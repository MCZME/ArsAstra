package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ToolbarFilterWidget extends AbstractWidget {
    private final ToolbarTabButton mainButton;
    private final List<ToolbarTabButton> options = new ArrayList<>();
    private boolean expanded = false;
    private final int buttonWidth = 20;
    private final int buttonHeight = 22;
    private int selectedIndex = 0;
    private final Consumer<Integer> onSelect;

    public ToolbarFilterWidget(int x, int y, Consumer<Integer> onSelect) {
        super(x, y, 20, 22, Component.empty());
        this.onSelect = onSelect;
        
        // 主按钮：初始显示索引 0 的图标
        this.mainButton = new ToolbarTabButton(0, 0, buttonWidth, buttonHeight, Component.empty(), 0, 0xFFFFFF, this::toggleExpand);
        
        // 添加一些硬编码的选项 (示例)
        // 0: 全部 (图标 0), 1: 类别 A (图标 1), 2: 类别 B (图标 2)
        addOption(0, 0xA0A0A0); // 全部 (灰)
        addOption(1, 0x40A040); // 植物 (绿)
        addOption(2, 0xA04040); // 矿物 (红)
    }
    
    private void addOption(int iconIndex, int color) {
        int index = options.size();
        ToolbarTabButton btn = new ToolbarTabButton(0, 0, buttonWidth, buttonHeight, Component.empty(), iconIndex, color, () -> select(index));
        options.add(btn);
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }
    
    private void select(int index) {
        this.selectedIndex = index;
        // 更新主按钮外观以匹配选中项 (除了点击事件仍是 toggle)
        // 这里我们需要 Hack 一下 ToolbarTabButton 或者给它添加 setter
        // 简单起见，我们不做视觉同步，主按钮始终作为一个“打开菜单”的按钮，或者显示当前选中的图标。
        // 为了好的 UX，主按钮应该显示选中的图标。
        // 但 ToolbarTabButton 的字段是 final 的。
        // 我需要重新创建主按钮或者修改 ToolbarTabButton。
        // 暂时保持主按钮不变，只触发回调。
        
        setExpanded(false);
        if (onSelect != null) onSelect.accept(index);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        // 展开时，高度增加以包含选项列表
        // 选项列表向下延伸? 不，工具栏在书页上方，向下延伸会盖住书页，这是对的。
        // 但是 ToolbarWidget 对齐是底部对齐。
        // 如果高度增加，ToolbarWidget 可能会把它向上推。
        // 我们需要小心。ToolbarWidget 的 arrange() 使用 (this.height - child.getHeight())。
        // 如果 child 变高，Y 会变小（向上移动）。
        // 这不是我们想要的下拉菜单行为。下拉菜单应该保持顶部位置不变，向下延伸。
        
        // 解决方案：不要改变 Widget 的 height，只在 render 中绘制溢出部分，并手动处理点击。
        // 或者修改 ToolbarWidget 的对齐逻辑。
        // 鉴于 ToolbarWidget 已经被设计为“底部对齐”以适应垂直标签，
        // 这里 FilterWidget 比较特殊。
        // 它的“根”在底部，展开的部分其实是“向下”的，也就是进入了书页区域。
        
        // 让 FilterWidget 始终保持高度 22。展开部分作为 Overlay 绘制。
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        mainButton.setX(x);
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setX(x);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        mainButton.setY(y);
        // 选项向下排列
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setY(y + (i + 1) * buttonHeight); // 在主按钮下方
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        mainButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (expanded) {
            // 绘制背景遮罩?
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100); // 提升 Z 轴以覆盖底层
            for (ToolbarTabButton btn : options) {
                btn.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expanded) {
            // 先检查选项点击
            for (ToolbarTabButton btn : options) {
                if (btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
            // 点击主按钮 toggle
            if (mainButton.mouseClicked(mouseX, mouseY, button)) return true;
            
            // 点击外部收起
            setExpanded(false);
            return true;
        } else {
            return mainButton.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
