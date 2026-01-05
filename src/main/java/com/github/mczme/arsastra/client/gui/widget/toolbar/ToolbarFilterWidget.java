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
        
        // 主按钮：index 1 (筛选图标), 浅灰色
        this.mainButton = new ToolbarTabButton(0, 0, buttonWidth, buttonHeight, Component.empty(), 1, 0xA0A0A0, this::toggleExpand);
        
        // 添加一些硬编码的选项 (示例)
        addOption(0, 0xA0A0A0); // 全部
        addOption(1, 0x40A040); // 植物
        addOption(2, 0xA04040); // 矿物
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
        setExpanded(false);
        if (onSelect != null) onSelect.accept(index);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
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
