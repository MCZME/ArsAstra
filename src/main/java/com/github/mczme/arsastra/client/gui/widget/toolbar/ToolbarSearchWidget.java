package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToolbarSearchWidget extends AbstractWidget {
    private final ToolbarTabButton button;
    private final ToolbarSearchBox searchBox;
    private boolean expanded = false;
    private final int collapsedWidth = 20;
    private final int expandedWidth = 100;
    private final int fixedHeight = 22; // 稍微高一点以容纳标签

    public ToolbarSearchWidget(int x, int y, Consumer<String> onSearch) {
        super(x, y, 20, 22, Component.empty());
        
        // 按钮状态: index 3 (罗盘图标), 金色
        this.button = new ToolbarTabButton(0, 0, collapsedWidth, fixedHeight, Component.empty(), 3, 0xA08030, this::toggleExpand);
        
        // 搜索框状态
        this.searchBox = new ToolbarSearchBox(Minecraft.getInstance().font, expandedWidth, 16, Component.empty());
        this.searchBox.setResponder(onSearch);
        this.searchBox.setVisible(false);
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        this.searchBox.setVisible(expanded);
        this.button.visible = !expanded;
        this.width = expanded ? expandedWidth : collapsedWidth;
        
        if (expanded) {
            this.searchBox.setFocused(true);
        } else {
            this.searchBox.setFocused(false);
            this.searchBox.setValue(""); // 收起时清空? 或者保留? 用户习惯通常保留或清空。这里先不清空，但如果用户意图是“关闭搜索”，可能需要清空。
            // 按照需求：点击按钮打开搜索框。通常意味着“开始搜索”。
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x);
        this.searchBox.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y);
        // 搜索框垂直居中于底部对齐
        this.searchBox.setY(y + (this.height - this.searchBox.getHeight())); 
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (expanded) {
            searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expanded) {
            // 如果点击在搜索框内，正常处理
            if (searchBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            
            // 如果点击在搜索框外
            // 逻辑：如果内容为空，则收起
            if (searchBox.getValue().isEmpty()) {
                setExpanded(false);
                return false; // 返回 false 让点击事件继续传递给可能的其他组件（如物品格）
            }
            
            // 如果内容不为空且点击在外部，通常保持展开但失去焦点（或者也可以选择不收起）
            // 这里我们选择不收起，除非用户清空内容或按 ESC。
        } else {
            if (this.button.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == 256) { // ESC
                setExpanded(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
