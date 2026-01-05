package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ToolbarFilterWidget extends AbstractWidget {
    private final ToolbarTabButton mainButton;
    private final EditBox elementsInput;
    private final EditBox tagsInput;
    private final Runnable onFilterChanged;

    private boolean expanded = false;
    private float animationProgress = 0.0f;
    
    // 弹出窗口的规格
    private final int popupWidth = 140;
    private final int popupHeight = 80;

    public ToolbarFilterWidget(int x, int y, Runnable onFilterChanged) {
        super(x, y, 20, 22, Component.empty());
        this.onFilterChanged = onFilterChanged;
        
        // 主切换按钮 (使用图标索引 1 代表筛选)
        this.mainButton = new ToolbarTabButton(x, y, 20, 22, Component.empty(), 1, 0xA0A0A0, null);
        
        // 输入框初始化
        var font = Minecraft.getInstance().font;
        // 初始位置暂定，将在 updatePopupLayout 中精确设置
        this.elementsInput = new EditBox(font, 0, 0, 130, 14, Component.literal("Elements"));
        this.elementsInput.setMaxLength(50);
        this.elementsInput.setBordered(false);
        this.elementsInput.setTextColor(0xFFFFFF);
        this.elementsInput.setHint(Component.translatable("gui.ars_astra.filter.elements_hint"));
        this.elementsInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });
        
        this.tagsInput = new EditBox(font, 0, 0, 130, 14, Component.literal("Tags"));
        this.tagsInput.setMaxLength(50);
        this.tagsInput.setBordered(false);
        this.tagsInput.setTextColor(0xFFFFFF);
        this.tagsInput.setHint(Component.translatable("gui.ars_astra.filter.tags_hint"));
        this.tagsInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });

        // 初始状态隐藏
        this.elementsInput.setVisible(false);
        this.tagsInput.setVisible(false);
        
        updatePopupLayout();
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (expanded) {
            this.elementsInput.setVisible(true);
            this.tagsInput.setVisible(true);
            // 移除自动聚焦
        } else {
            this.elementsInput.setVisible(false);
            this.tagsInput.setVisible(false);
            this.elementsInput.setFocused(false);
            this.tagsInput.setFocused(false);
        }
    }
    
    public String getElementFilter() {
        return elementsInput.getValue();
    }
    
    public String getTagFilter() {
        return tagsInput.getValue();
    }

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
    
    private void updatePopupLayout() {
        // bgY = this.getY() + 22
        // Elements Box: bgY + 18
        // Tags Box: bgY + 48
        int bgY = this.getY() + 22;
        
        this.elementsInput.setX(this.getX() + 5);
        this.elementsInput.setY(bgY + 18 + 3); // 增加偏移至 +3
        
        this.tagsInput.setX(this.getX() + 5);
        this.tagsInput.setY(bgY + 48 + 3); // 增加偏移至 +3
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 更新动画进度
        float target = expanded ? 1.0f : 0.0f;
        float step = 0.2f;
        if (animationProgress < target) {
            animationProgress = Math.min(animationProgress + step, target);
        } else if (animationProgress > target) {
            animationProgress = Math.max(animationProgress - step, target);
        }
        
        // 实时更新位置以确保动画过程中的正确性 (虽然位置是固定的，但以防万一)
        updatePopupLayout();

        // 渲染主按钮
        mainButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染弹出面板
        if (animationProgress > 0.01f) {
            int currentW = (int) (popupWidth * animationProgress);
            int currentH = (int) (popupHeight * animationProgress);
            
            // 向右下角展开，面板顶部与按钮底部对齐
            int bgX = this.getX();
            int bgY = this.getY() + 22; 
            
            guiGraphics.pose().pushPose();
            // 提升到 400，确保高于 tooltip 层级
            guiGraphics.pose().translate(0, 0, 400); 
            
            // 临时禁用深度测试，强制覆盖
            RenderSystem.disableDepthTest();
            
            try {
                // 背景装饰：三层嵌套矩形
                guiGraphics.fill(bgX, bgY, bgX + currentW, bgY + currentH, 0xFFA08060); // 边框
                guiGraphics.fill(bgX + 1, bgY + 1, bgX + currentW - 1, bgY + currentH - 1, 0xFF4A3525); // 内边框
                guiGraphics.fill(bgX + 2, bgY + 2, bgX + currentW - 2, bgY + currentH - 2, 0xFF302015); // 主面板背景
                
                // 当动画接近完成时显示文本和输入框
                if (animationProgress > 0.8f) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.elements_label"), bgX + 5, bgY + 5, 0xAAAAAA, false);
                    // 要素输入框背景 (高 16)
                    guiGraphics.fill(bgX + 4, bgY + 18, bgX + currentW - 4, bgY + 18 + 16, 0xFF000000);
                    elementsInput.render(guiGraphics, mouseX, mouseY, partialTick);

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.tags_label"), bgX + 5, bgY + 38, 0xAAAAAA, false);
                    // 标签输入框背景 (高 16)
                    guiGraphics.fill(bgX + 4, bgY + 48, bgX + currentW - 4, bgY + 48 + 16, 0xFF000000);
                    tagsInput.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            } finally {
                // 恢复深度测试
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

        // 2. 如果已展开，检查弹出面板内的点击
        if (expanded) {
            // 面板范围判断
            boolean inPopup = mouseX >= this.getX() && mouseX <= this.getX() + popupWidth 
                           && mouseY >= this.getY() + 22 && mouseY <= this.getY() + 22 + popupHeight;

            if (inPopup) {
                // 显式检查是否点击在输入框的视觉范围内 (带一些宽容度)
                int bgY = this.getY() + 22;
                
                // 检查要素输入框区域 (y: 18~34)
                if (mouseY >= bgY + 18 && mouseY <= bgY + 34) {
                    elementsInput.setFocused(true);
                    tagsInput.setFocused(false);
                    // 转发给输入框以处理光标
                    elementsInput.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
                
                // 检查标签输入框区域 (y: 48~64)
                if (mouseY >= bgY + 48 && mouseY <= bgY + 64) {
                    tagsInput.setFocused(true);
                    elementsInput.setFocused(false);
                    tagsInput.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
                
                // 点击了面板其他空白处 -> 清除焦点但保持展开
                elementsInput.setFocused(false);
                tagsInput.setFocused(false);
                return true; 
            } else {
                // 点击了面板和按钮之外的区域 -> 关闭面板
                setExpanded(false);
                return false; 
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (elementsInput.isFocused() && elementsInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (tagsInput.isFocused() && tagsInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == 256) { // ESC 键关闭
                setExpanded(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            if (elementsInput.isFocused()) return elementsInput.charTyped(codePoint, modifiers);
            if (tagsInput.isFocused()) return tagsInput.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}