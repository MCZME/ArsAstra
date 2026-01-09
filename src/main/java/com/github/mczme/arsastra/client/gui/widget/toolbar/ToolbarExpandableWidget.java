package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class ToolbarExpandableWidget extends AbstractWidget {
    public enum ExpandDirection {
        RIGHT, LEFT
    }

    protected final ToolbarTabButton mainButton;
    protected boolean expanded = false;
    protected float animationProgress = 0.0f;
    protected final int popupWidth;
    protected final int popupHeight;
    protected ExpandDirection expandDirection = ExpandDirection.RIGHT;

    public ToolbarExpandableWidget(int x, int y, int popupWidth, int popupHeight, int iconIndex, int color) {
        super(x, y, 20, 22, Component.empty());
        this.popupWidth = popupWidth;
        this.popupHeight = popupHeight;
        this.mainButton = new ToolbarTabButton(x, y, 20, 22, Component.empty(), iconIndex, color, null);
    }

    public void setExpandDirection(ExpandDirection direction) {
        this.expandDirection = direction;
        updatePopupLayout();
    }

    protected int getPopupX() {
        if (expandDirection == ExpandDirection.LEFT) {
            return this.getX() + this.width - popupWidth;
        }
        return this.getX();
    }

    protected void toggleExpand() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (!expanded) {
            onCollapse();
        } else {
            onExpand();
        }
    }

    protected void onCollapse() {}
    protected void onExpand() {}

    protected abstract void updatePopupLayout();
    
    // 如果点击被弹出层内容处理了，返回 true
    protected abstract boolean mouseClickedInPopup(double mouseX, double mouseY, int button);

    protected abstract void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY);

    @Override
    public void setX(int x) {
        super.setX(x);
        mainButton.setX(x);
        updatePopupLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        mainButton.setY(y);
        updatePopupLayout();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 动画逻辑
        float target = expanded ? 1.0f : 0.0f;
        float step = 0.2f;
        if (animationProgress < target) {
            animationProgress = Math.min(animationProgress + step, target);
        } else if (animationProgress > target) {
            animationProgress = Math.max(animationProgress - step, target);
        }
        
        updatePopupLayout();

        // 渲染主按钮
        mainButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染弹出面板
        if (animationProgress > 0.01f) {
            int currentW = (int) (popupWidth * animationProgress);
            int currentH = (int) (popupHeight * animationProgress);
            
            int bgX = this.getX();
            if (expandDirection == ExpandDirection.LEFT) {
                bgX = this.getX() + this.width - currentW;
            }
            int bgY = this.getY() + 22; 
            
            guiGraphics.pose().pushPose();
            // 提升层级，确保在其他组件之上
            guiGraphics.pose().translate(0, 0, 400); 
            
            RenderSystem.disableDepthTest();
            
            try {
                // 背景渲染
                guiGraphics.fill(bgX, bgY, bgX + currentW, bgY + currentH, 0xFFA08060);
                guiGraphics.fill(bgX + 1, bgY + 1, bgX + currentW - 1, bgY + currentH - 1, 0xFF4A3525);
                guiGraphics.fill(bgX + 2, bgY + 2, bgX + currentW - 2, bgY + currentH - 2, 0xFF302015);
                
                // 渲染内容（仅当面板展开足够大时）
                if (animationProgress > 0.8f) {
                    guiGraphics.enableScissor(bgX + 2, bgY + 2, bgX + currentW - 2, bgY + currentH - 2);
                    // 传递完全展开时的 X 坐标 (getPopupX)，以确保内容位置固定
                    renderPopupContent(guiGraphics, mouseX, mouseY, partialTick, getPopupX(), bgY);
                    guiGraphics.disableScissor();
                }
            } finally {
                RenderSystem.enableDepthTest();
            }
            
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. 检查主按钮点击
        if (mainButton.mouseClicked(mouseX, mouseY, button)) {
            toggleExpand();
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        // 2. 检查弹出面板点击
        if (expanded) {
            int finalBgX = getPopupX();
            boolean inPopup = mouseX >= finalBgX && mouseX <= finalBgX + popupWidth 
                           && mouseY >= this.getY() + 22 && mouseY <= this.getY() + 22 + popupHeight;

            if (inPopup) {
                if (mouseClickedInPopup(mouseX, mouseY, button)) return true;
                return true; // 拦截背景点击，防止穿透
            } else {
                setExpanded(false); // 点击外部关闭
                return false; 
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (keyCode == 256) { // ESC 键
                setExpanded(false);
                return true;
            }
            // 默认情况下，展开时拦截所有按键以防止触发其他 UI 操作
            return true; 
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
