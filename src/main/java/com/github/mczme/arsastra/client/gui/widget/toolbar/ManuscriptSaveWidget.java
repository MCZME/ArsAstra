package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class ManuscriptSaveWidget extends ToolbarSideExpandableWidget {
    // 新的图标纹理
    private static final ResourceLocation MANUSCRIPT_ICONS = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_icons.png");
    
    private final EditBox nameInput;
    private final IconButton confirmButton;
    private final IconButton iconDisplayButton; 
    
    private boolean showIconDropdown = false;
    private int selectedIconIndex = 0; 
    // 可选图标索引
    private final int[] availableIcons = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}; 
    // 确认按钮图标索引
    private static final int CONFIRM_ICON_INDEX = 15;

    public ManuscriptSaveWidget(int x, int y, BiConsumer<String, Integer> onSaveConfirm) {
        // 使用原始保存图标 index=8, 颜色=0x888888 (基类用于渲染背景按钮的图标)
        super(x, y, 150, ExpandDirection.LEFT, 8, 0x888888);
        // 关键：设置背景按钮图标右对齐，确保展开后它依然在最右侧（原始位置）
        this.backgroundButton.setForceRightAlign(true);
        
        Font font = Minecraft.getInstance().font;
        
        // 1. 标签图标选择器 (IconButton)
        this.iconDisplayButton = new IconButton(0, 0, 20, 20, selectedIconIndex, () -> {
            showIconDropdown = !showIconDropdown;
        });

        // 2. 名称输入框
        this.nameInput = new EditBox(font, 0, 0, 80, 14, Component.empty());
        this.nameInput.setHint(Component.translatable("gui.ars_astra.workshop.save_manuscript.name_hint"));
        this.nameInput.setBordered(false);
        this.nameInput.setTextColor(0xFFFFFF);
        this.nameInput.setMaxLength(20);
        
        // 3. 确定按钮 (IconButton)
        this.confirmButton = new IconButton(0, 0, 20, 20, CONFIRM_ICON_INDEX, () -> {
            if (!nameInput.getValue().isEmpty()) {
                onSaveConfirm.accept(nameInput.getValue(), selectedIconIndex);
                setExpanded(false);
            }
        });
    }
    
    @Override
    protected void onExpand() {
        this.nameInput.setVisible(true);
        this.nameInput.setValue("");
        this.showIconDropdown = false;
    }

    @Override
    protected void onCollapse() {
        this.nameInput.setFocused(false);
        this.showIconDropdown = false;
    }

    @Override
    protected void updateContentLayout(int x, int y, int width, int height) {
        // 布局从左到右: [Icon(20)] [Name(InputW)] [Confirm(20)] [SaveIcon(20, rendered by base)]
        
        // 标签选择器
        this.iconDisplayButton.setX(x + 2);
        this.iconDisplayButton.setY(y + 1);
        this.iconDisplayButton.setIconIndex(selectedIconIndex);

        // 输入框
        int inputW = width - 68; 
        this.nameInput.setX(x + 24);
        this.nameInput.setY(y + 7);
        this.nameInput.setWidth(Math.max(0, inputW));
        
        // 确定按钮
        this.confirmButton.setX(x + 24 + inputW + 2);
        this.confirmButton.setY(y + 1);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染标签选择器
        this.iconDisplayButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 2. 渲染输入框
        guiGraphics.fill(nameInput.getX(), nameInput.getY() + 10, nameInput.getX() + nameInput.getWidth(), nameInput.getY() + 11, 0xFFFFFFFF);
        this.nameInput.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 3. 渲染确定按钮
        this.confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 4. 不需要手动渲染最右侧图标，backgroundButton 会自动处理（因为设置了 setForceRightAlign）
    }

    @Override
    protected void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (showIconDropdown) {
            int dropdownX = this.iconDisplayButton.getX();
            int dropdownY = this.iconDisplayButton.getY() + 22;
            int itemSize = 18;
            int cols = 4;
            int rows = (availableIcons.length + cols - 1) / cols;
            int w = cols * itemSize + 2;
            int h = rows * itemSize + 2;
            
            guiGraphics.fill(dropdownX, dropdownY, dropdownX + w, dropdownY + h, 0xFF202020);
            guiGraphics.renderOutline(dropdownX, dropdownY, w, h, 0xFF808080);
            
            for (int i = 0; i < availableIcons.length; i++) {
                int iconIdx = availableIcons[i];
                int col = i % cols;
                int row = i / cols;
                int ix = dropdownX + 1 + col * itemSize;
                int iy = dropdownY + 1 + row * itemSize;
                
                boolean hovered = mouseX >= ix && mouseX < ix + itemSize && mouseY >= iy && mouseY < iy + itemSize;
                if (hovered) {
                    guiGraphics.fill(ix, iy, ix + itemSize, iy + itemSize, 0xFF505050);
                }
                
                RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
                int u = (iconIdx % 4) * 16;
                int v = (iconIdx / 4) * 16;
                guiGraphics.blit(MANUSCRIPT_ICONS, ix + 1, iy + 1, u, v, 16, 16, 64, 64);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showIconDropdown) {
            int dropdownX = this.iconDisplayButton.getX();
            int dropdownY = this.iconDisplayButton.getY() + 22;
            int itemSize = 18;
            int cols = 4;
            int w = cols * itemSize + 2;
            int rows = (availableIcons.length + cols - 1) / cols;
            int h = rows * itemSize + 2;
            
            if (mouseX >= dropdownX && mouseX <= dropdownX + w && mouseY >= dropdownY && mouseY <= dropdownY + h) {
                int col = (int)(mouseX - dropdownX - 1) / itemSize;
                int row = (int)(mouseY - dropdownY - 1) / itemSize;
                int index = row * cols + col;
                if (index >= 0 && index < availableIcons.length) {
                    this.selectedIconIndex = availableIcons[index];
                    this.showIconDropdown = false; 
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true; 
            }
            this.showIconDropdown = false;
            return true;
        }

        if (this.isHovered()) {
             if (expanded) {
                 if (iconDisplayButton.mouseClicked(mouseX, mouseY, button)) return true;
                 
                 // 显式处理输入框焦点逻辑
                 if (nameInput.isMouseOver(mouseX, mouseY)) {
                     nameInput.setFocused(true);
                     nameInput.mouseClicked(mouseX, mouseY, button);
                     return true;
                 } else {
                     nameInput.setFocused(false);
                 }
                 
                 if (confirmButton.mouseClicked(mouseX, mouseY, button)) return true;
                 
                 // 点击最右侧原始按钮区域收起
                 if (mouseX > this.getX() + this.width - 20) {
                     toggleExpand();
                     return true;
                 }
                 return true;
             } else {
                 toggleExpand();
                 return true;
             }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            return nameInput.charTyped(codePoint, modifiers);
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (keyCode == 256) { // ESC
                if (showIconDropdown) showIconDropdown = false;
                else setExpanded(false);
                return true;
            }
            if (nameInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    /**
     * 内部类：简单的图标按钮，使用 MANUSCRIPT_ICONS
     */
    private class IconButton extends AbstractButton {
        private int iconIndex;
        private final Runnable onPress;

        public IconButton(int x, int y, int width, int height, int iconIndex, Runnable onPress) {
            super(x, y, width, height, Component.empty());
            this.iconIndex = iconIndex;
            this.onPress = onPress;
        }
        
        public void setIconIndex(int index) {
            this.iconIndex = index;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            
            // 简单的悬停背景
            if (isHovered()) {
                guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x40FFFFFF);
            }
            
            RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
            int u = (iconIndex % 4) * 16;
            int v = (iconIndex / 4) * 16;
            
            int ix = getX() + (width - 16) / 2;
            int iy = getY() + (height - 16) / 2;
            
            guiGraphics.blit(MANUSCRIPT_ICONS, ix, iy, u, v, 16, 16, 64, 64);
            
            RenderSystem.disableBlend();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
