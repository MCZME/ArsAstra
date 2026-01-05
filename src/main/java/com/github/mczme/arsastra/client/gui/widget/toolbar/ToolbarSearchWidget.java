package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class ToolbarSearchWidget extends AbstractWidget {
    private final ToolbarTabButton backgroundButton; 
    private final ToolbarSearchBox searchBox;
    private boolean expanded = false;
    
    private final int collapsedWidth = 20;
    private final int expandedWidth = 100;
    private final int fixedHeight = 22;
    
    private float animationProgress = 0.0f; // 0.0 (Collapsed) -> 1.0 (Expanded)

    public ToolbarSearchWidget(int x, int y, Consumer<String> onSearch) {
        super(x, y, 20, 22, Component.empty());
        
        this.backgroundButton = new ToolbarTabButton(x, y, collapsedWidth, fixedHeight, Component.empty(), 0, 0xA08030, null);
        this.backgroundButton.setForceLeftAlign(true);
        
        this.searchBox = new ToolbarSearchBox(Minecraft.getInstance().font, 10, 16, Component.empty());
        this.searchBox.setResponder(onSearch);
        this.searchBox.setVisible(false);
        this.searchBox.setBordered(false);
        
        updateInternalLayout();
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (expanded) {
            this.searchBox.setVisible(true);
        } else {
            this.searchBox.setFocused(false);
        }
    }

    private void updateInternalLayout() {
        int currentW = (int) Mth.lerp(animationProgress, collapsedWidth, expandedWidth);
        this.width = currentW;
        
        this.backgroundButton.setX(this.getX());
        this.backgroundButton.setY(this.getY());
        this.backgroundButton.setWidth(currentW);

        // 搜索框位置同步
        int boxX = this.getX() + 20;
        // 再次增加偏移: +2 -> +4，使输入文字显著下移
        int boxY = this.getY() + (this.height - 16) / 2 + 4; 
        int boxW = currentW - 24;
        
        this.searchBox.setX(boxX);
        this.searchBox.setY(boxY);
        this.searchBox.setWidth(boxW);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateInternalLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateInternalLayout();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 更新动画进度
        float target = expanded ? 1.0f : 0.0f;
        float step = 0.15f; 
        if (animationProgress < target) {
            animationProgress = Math.min(animationProgress + step, target);
        } else if (animationProgress > target) {
            animationProgress = Math.max(animationProgress - step, target);
        }
        
        // 2. 更新内部组件布局
        updateInternalLayout();

        // 3. 渲染背景 (按钮)
        this.backgroundButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // 4. 渲染搜索框 (如果宽度足够容纳)
        if (animationProgress > 0.1f) {
            this.searchBox.setVisible(true);
            guiGraphics.enableScissor(this.getX() + 2, this.getY(), this.getX() + this.width - 2, this.getY() + this.height);
            this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.disableScissor();
        } else {
             this.searchBox.setVisible(false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;

        // 1. 判断是否点击在组件范围内
        if (this.isHovered()) {
            if (expanded) {
                // 已展开状态
                if (mouseX < this.getX() + 20) {
                    // 点击左侧图标区域 -> 切换收起
                    this.toggleExpand();
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                } else {
                    // 点击右侧搜索区域 -> 聚焦
                    // 无论点击是否精准落在输入框文字上，只要在右侧，都强制聚焦
                    this.searchBox.setFocused(true);
                    // 转发点击事件给输入框以处理光标定位 (仅当确实在输入框内时，避免EditBox副作用)
                    if (this.searchBox.isMouseOver(mouseX, mouseY)) {
                        this.searchBox.mouseClicked(mouseX, mouseY, button);
                    }
                }
            } else {
                // 未展开状态 -> 点击任意位置展开
                this.toggleExpand();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        
        // 2. 点击在组件外部
        if (expanded) {
             // 如果内容为空则自动收起，否则仅失焦
             if (searchBox.getValue().isEmpty()) {
                setExpanded(false);
            } else {
                searchBox.setFocused(false);
            }
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
