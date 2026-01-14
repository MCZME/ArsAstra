package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class ToolbarSideExpandableWidget extends AbstractWidget {
    public enum ExpandDirection {
        RIGHT, LEFT
    }

    protected final ToolbarTabButton backgroundButton;
    protected boolean expanded = false;
    protected float animationProgress = 0.0f;
    protected final int collapsedWidth = 20;
    protected final int expandedWidth;
    protected final ExpandDirection expandDirection;
    
    // 基础锚点坐标 (X轴)
    // RIGHT: 左边缘
    // LEFT: 右边缘
    protected int anchorX;

    public ToolbarSideExpandableWidget(int x, int y, int expandedWidth, ExpandDirection direction, int iconIndex, int color) {
        super(x, y, 20, 22, Component.empty());
        this.expandedWidth = expandedWidth;
        this.expandDirection = direction;
        
        // 初始化 anchorX
        if (direction == ExpandDirection.LEFT) {
            this.anchorX = x + collapsedWidth;
        } else {
            this.anchorX = x;
        }
        
        this.backgroundButton = new ToolbarTabButton(x, y, 20, 22, Component.empty(), iconIndex, color, null);
        this.backgroundButton.setForceLeftAlign(true);
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (!expanded) {
            onCollapse();
        } else {
            onExpand();
        }
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    protected void toggleExpand() {
        setExpanded(!expanded);
    }
    
    protected void onCollapse() {}
    protected void onExpand() {}

    private void updateLayout() {
        int currentW = (int) Mth.lerp(animationProgress, collapsedWidth, expandedWidth);
        this.width = currentW;
        
        if (expandDirection == ExpandDirection.LEFT) {
            // 向左展开：右边缘固定在 anchorX
            super.setX(anchorX - currentW);
        } else {
            // 向右展开：左边缘固定在 anchorX
            super.setX(anchorX);
        }
        
        this.backgroundButton.setX(this.getX());
        this.backgroundButton.setY(this.getY());
        this.backgroundButton.setWidth(currentW);
        
        updateContentLayout(this.getX(), this.getY(), currentW, this.height);
    }
    
    protected abstract void updateContentLayout(int x, int y, int width, int height);
    
    /**
     * 渲染内部内容。此时已应用 Scissor 裁剪。
     */
    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 动画逻辑
        float target = expanded ? 1.0f : 0.0f;
        float step = 0.15f; 
        if (animationProgress < target) {
            animationProgress = Math.min(animationProgress + step, target);
        } else if (animationProgress > target) {
            animationProgress = Math.max(animationProgress - step, target);
        }
        
        updateLayout();
        
        // 渲染背景
        this.backgroundButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染内容 (使用 Scissor 裁剪)
        if (animationProgress > 0.1f) {
            guiGraphics.enableScissor(this.getX() + 2, this.getY(), this.getX() + this.width - 2, this.getY() + this.height);
            renderContent(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.disableScissor();
        }
        
        // 渲染悬浮层 (如下拉菜单)，不受 Scissor 限制
        if (expanded && animationProgress > 0.9f) {
            renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
    protected void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
    
    @Override
    public void setX(int x) {
        // 当外部（如父容器）重新设置位置时，我们假设它是基于组件当前的左边缘设置的。
        // 我们需要据此更新 anchorX。
        if (expandDirection == ExpandDirection.LEFT) {
            this.anchorX = x + this.width;
        } else {
            this.anchorX = x;
        }
        super.setX(x);
        // updateLayout() will be called in render
    }
    
    @Override
    public void setY(int y) {
        super.setY(y);
        // updateLayout() will be called in render
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
