package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ToolbarWidget extends AbstractWidget {
    protected final List<AbstractWidget> children = new ArrayList<>();
    private final int padding = 2;

    public ToolbarWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void addChild(AbstractWidget widget) {
        this.children.add(widget);
        this.arrange();
    }
    
    public void clear() {
        this.children.clear();
    }

    public void arrange() {
        int currentX = this.getX() + padding;
        for (AbstractWidget child : children) {
            if (!child.visible) continue;
            
            // 设置位置：X 紧跟前一个，Y 底部对齐
            child.setX(currentX);
            child.setY(this.getY() + (this.height - child.getHeight()));
            
            currentX += child.getWidth() + padding;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 每一帧都重新排列，以适应子组件的宽度变化（如搜索框展开动画）
        arrange();
        
        for (AbstractWidget child : children) {
            if (child.visible) {
                child.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;
        boolean handled = false;
        for (AbstractWidget child : children) {
            if (child.visible && child.mouseClicked(mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }
        
        // 无论是否 handle，都调用一次 arrange，因为子组件可能通过“点击外部”改变了状态（如搜索框收起）
        this.arrange(); 
        
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.visible && child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
         for (AbstractWidget child : children) {
            if (child.visible && child.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
