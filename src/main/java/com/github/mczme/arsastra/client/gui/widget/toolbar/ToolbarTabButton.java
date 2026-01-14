package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ToolbarTabButton extends AbstractButton {
    private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/toolbar_icons.png");
    
    private int color;
    private int iconIndex;
    private final Runnable onPress;
    private boolean isSelected;
    private boolean forceLeftAlign = false;
    private boolean forceRightAlign = false;
    private Direction direction = Direction.DOWN;

    public enum Direction {
        UP, DOWN, RIGHT
    }

    public ToolbarTabButton(int x, int y, int width, int height, Component message, int iconIndex, int color, Runnable onPress) {
        super(x, y, width, height, message);
        this.iconIndex = iconIndex;
        this.color = color;
        this.onPress = onPress;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    public void setIconIndex(int index) {
        this.iconIndex = index;
    }
    
    public void setColor(int color) {
        this.color = color;
    }

    public void setForceLeftAlign(boolean leftAlign) {
        this.forceLeftAlign = leftAlign;
        if (leftAlign) this.forceRightAlign = false;
    }

    public void setForceRightAlign(boolean rightAlign) {
        this.forceRightAlign = rightAlign;
        if (rightAlign) this.forceLeftAlign = false;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // 1. 计算颜色层级
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        if (!isSelected) {
            r = (int)(r * 0.7f);
            g = (int)(g * 0.7f);
            b = (int)(b * 0.7f);
        }
        
        if (isHovered()) {
            r = Math.min(255, (int)(r * 1.2f));
            g = Math.min(255, (int)(g * 1.2f));
            b = Math.min(255, (int)(b * 1.2f));
        }

        int mainColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        int borderR = (int)(r * 0.4f);
        int borderG = (int)(g * 0.4f);
        int borderB = (int)(b * 0.4f);
        int borderColor = 0xFF000000 | (borderR << 16) | (borderG << 8) | borderB;
        int accentR = (int)(r * 0.7f);
        int accentG = (int)(g * 0.7f);
        int accentB = (int)(b * 0.7f);
        int accentColor = 0xFF000000 | (accentR << 16) | (accentG << 8) | accentB;

        // 2. 绘制图层
        guiGraphics.fill(x, y, x + w, y + h, borderColor);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, mainColor);
        
        // Layer 3: 装饰条 (根据方向)
        if (direction == Direction.DOWN) {
            // 底部装饰
            guiGraphics.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, accentColor);
        } else if (direction == Direction.UP) {
            // 顶部装饰
            guiGraphics.fill(x + 2, y + 2, x + w - 2, y + 4, accentColor);
        } else if (direction == Direction.RIGHT) {
            // 左侧装饰 (作为标签根部)
            guiGraphics.fill(x + 2, y + 2, x + 4, y + h - 2, accentColor);
        }

        // 3. 渲染图标
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // 4x4 网格逻辑 (64x64 纹理, 每个图标 16x16)
        int iconU = (iconIndex % 4) * 16;
        int iconV = (iconIndex / 4) * 16; 
        
        int iconX;
        if (forceLeftAlign) {
            iconX = x + 2; // 左对齐，留出2px边框
        } else if (forceRightAlign) {
            iconX = x + w - 18; // 右对齐 (width - 16 - 2)
        } else {
            iconX = x + (w - 16) / 2; // 居中
        }
        int iconY = y + (h - 16) / 2;
        
        guiGraphics.blit(ICONS_TEXTURE, iconX, iconY, iconU, iconV, 16, 16, 64, 64);
        
        RenderSystem.disableBlend();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
